/*
 * VideoFeedParser.cpp
 *
 *  Created on: Jun 29, 2012
 *      Author: alex.m.damian@gmail.com
 */
#include "VideoFeedParser.h"

#include <cmath>
#include <string>
#include <iostream>
#include <sys/time.h>
#include <fstream>

#include "Constants.h"
#include "opencv2/opencv.hpp"
#include "image_features.pb.h"
#include "ObjectClassifier.h"

using namespace androar;
using namespace cv;
using namespace std;

namespace {
template <typename T>
string tostr(const T& t) {
	ostringstream os;
	os<<t;
	return os.str();
}

string getImageContents(const Mat& frame) {
	char filename[] = "videoframeXXXXXX.jpg";
	int fd = mkstemps(filename, 4);
	close(fd);
	// Write the image to disk
	imwrite(filename, frame);
	ifstream f(filename);
	string str((std::istreambuf_iterator<char>(f)),
			std::istreambuf_iterator<char>());
	f.close();
	// Unlink the file
	unlink(filename);
	return str;
}
}

VideoFeedParser::VideoFeedParser() {}

VideoFeedParser::~VideoFeedParser() {}

bool VideoFeedParser::parseVideo(string video_filename, Image& image_template, string object_id,
		int once_every_frames, int start_frame, int stop_frame) {
	ObjectClassifier classifier;
	// Open the video feed
	VideoCapture video_capture(video_filename);
	if (!video_capture.isOpened()) {
		return false;
	}
	Size video_size = cvSize(1920, 1080);
	int output_fps = 100 / once_every_frames;
	if (output_fps == 0) {
		output_fps = 1;
	}
	VideoWriter video_writer(
			"video_test.avi", 0, once_every_frames / 10, video_size, false);

	// Just get the first frame to set some values;
	Mat big_frame, frame;
	video_capture >> big_frame;
	resize(big_frame, frame, Size(), 1./3, 1./3);

	double scale = min<double>(
			1. * THUMBNAIL_SIZE / frame.rows,
			1. * THUMBNAIL_SIZE / frame.cols);
	int row_height = ceil(frame.rows * scale);
	int col_width = ceil(frame.cols * scale);
	Mat valid_matches(row_height, col_width * NUM_THUMBNAILS_PER_ROW, CV_8UC3);

	long total_time = 0;
	for (int fr = 0, num_frames = 0, num_matches = 0; ;++fr) {
		video_capture >> big_frame;
		if (big_frame.rows == 0) {
			break;
		}
		resize(big_frame, frame, Size(), 1./3, 1./3);

		if ((fr < start_frame) || (fr % once_every_frames != 0)) {
			continue;
		}
		if (fr > stop_frame) {
			break;
		}
		cout << "********** FRAME " << (fr + 1) << " **********" << endl;
		++num_frames;
		// Create message
		string str = getImageContents(frame);
		Image image;
		image.CopyFrom(image_template);
		image.mutable_image()->set_image_hash("HASH");
		image.mutable_image()->set_image_contents(str);

		// Process the image
		struct timeval start_time, end_time;
		gettimeofday(&start_time, NULL);
		classifier.processImage(&image);
		gettimeofday(&end_time, NULL);

		image.mutable_image()->clear_image_contents();
		// Get the result image
		Mat result;
		for (int i = 0; i < image.possible_present_objects_size(); ++i) {
			const PossibleObject& obj = image.possible_present_objects(i);
			if (obj.id() != object_id) {
				continue;
			}
			string ri =	obj.features(0).result_match();
			double certainty = obj.features(0).certainty();
			for (int j = 1; j < obj.features_size(); ++j) {
				if (certainty < obj.features(i).certainty()) {
					certainty = obj.features(i).certainty();
					ri = obj.features(i).result_match();
				}
			}
			Mat decoded_result = imdecode(vector<char>(ri.data(), ri.data() + ri.size()), 0);
			result = decoded_result;
		}
		// Compute statistics
		// FPS
		total_time += MILLISEC(start_time, end_time);
		double fps = 1000. * num_frames / total_time;
		string sfps = "FPS: " + tostr(fps);
		cout << fps << " " << sfps << endl;
		// MATCHES
		Mat thumbnail;
		for (int i = 0; i < image.detected_objects_size(); ++i) {
			if (image.detected_objects(i).id() == object_id) {
				++num_matches;
				// Valid results
				Mat thumbnail_color;
				resize(frame, thumbnail_color, Size(), scale, scale);
				thumbnail_color.convertTo(thumbnail, CV_8UC3);
				if (num_matches % NUM_THUMBNAILS_PER_ROW == 0) {
					// Jump to another row
					Mat old_valid_matches = valid_matches.clone();
					valid_matches = Mat(
							old_valid_matches.rows + row_height,
							old_valid_matches.cols,
							old_valid_matches.type());
					Mat first = valid_matches(
							Rect(0, 0, old_valid_matches.cols, old_valid_matches.rows));
					old_valid_matches.copyTo(first);
				}
				// Continue on the same row
				Mat second = valid_matches(
						Rect(
								(num_matches % NUM_THUMBNAILS_PER_ROW) * col_width,
								num_matches / NUM_THUMBNAILS_PER_ROW * row_height,
								thumbnail.cols,
								thumbnail.rows));
				thumbnail.copyTo(second);
				break;
			}
		}

		// Make image to be displayed
		Mat displayed_frame(
				result.rows + valid_matches.rows,
				max<int>(result.cols, valid_matches.cols),
				0);
		// Result
		Mat first = displayed_frame(Rect(0, valid_matches.rows, result.cols, result.rows));
		result.copyTo(first);
		// Valid previous results
		Mat second = displayed_frame(Rect(0, 0, valid_matches.cols, valid_matches.rows));
		string sv_m = getImageContents(valid_matches);
		Mat decoded_valid_matches = imdecode(vector<char>(sv_m.data(), sv_m.data() + sv_m.size()), 0);
		putText(decoded_valid_matches, "MATCHES:", cvPoint(5, 30), FONT_HERSHEY_SIMPLEX, 0.5, cvScalar(255, 255, 255), 1, CV_AA);
		decoded_valid_matches.copyTo(second);
		// Stats
		string smatches = "MATCHES: " + tostr(num_matches) + " / " + tostr(num_frames);
		cout << smatches << endl;
		putText(displayed_frame, sfps, cvPoint(50, valid_matches.rows + 50), FONT_HERSHEY_SIMPLEX, 0.5, cvScalar(0, 0, 0), 1, CV_AA);
		putText(displayed_frame, smatches, cvPoint(50, valid_matches.rows + 70), FONT_HERSHEY_SIMPLEX, 0.5, cvScalar(0, 0, 0), 1, CV_AA);
		// Bounding boxes
		for (int i = 0; i < image.detected_objects_size(); ++i) {
			const DetectedObject& obj = image.detected_objects(i);
			if (obj.id() != object_id) {
				continue;
			}
			rectangle(
					displayed_frame,
					cvPoint(obj.bounding_box().top(), valid_matches.rows + obj.bounding_box().left()),
					cvPoint(obj.bounding_box().bottom(), valid_matches.rows + obj.bounding_box().right()),
					cvScalar(0, 0, 0));
		}
		Mat video_frame(video_size, 0);
		first = video_frame(Rect(0, 0, displayed_frame.cols, displayed_frame.rows));
		displayed_frame.copyTo(first);
		video_writer << video_frame;
		if(waitKey(30) >= 0) break;
	}
	return true;
}
