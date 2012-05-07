/*
 * ObjectClassifier.cpp
 *
 *  Created on: Apr 5, 2012
 *      Author: alex.m.damian@gmail.com
 */

#include "ObjectClassifier.h"

#include <cmath>
#include <cstdlib>
#include <cstdio>
#include <fstream>
#include <map>
#include <streambuf>
#include <vector>

#include <iostream>

#include "Constants.h"
#include "opencv2/highgui/highgui.hpp"
#include "opencv2/features2d/features2d.hpp"

using namespace std;

FeatureDetector* ObjectClassifier::detector_ = NULL;
DescriptorExtractor* ObjectClassifier::extractor_ = NULL;

ObjectClassifier::ObjectClassifier() {
	features_map = new map<string, Features >();
}

ObjectClassifier::~ObjectClassifier() {
}

Features ObjectClassifier::computeFeatureDescriptor(const string& image_content) {
	// Create a Mat from the image we got and compute the features for that.
	const char* image_contents = image_content.data();
	int image_contents_size = image_content.size();
	Mat image_mat = imdecode(vector<char>(image_contents, image_contents + image_contents_size), 0);

	FeatureDetector* detector;
	DescriptorExtractor* extractor;
	getDetectorAndExtractor(&detector, &extractor);

	Features current_features;
	// Detect key points
	detector->detect(image_mat, current_features.key_points);
	// Extract feature vectors
	extractor->compute(image_mat, current_features.key_points, current_features.descriptor);

	return current_features;
}

Features ObjectClassifier::computeFeatureDescriptor(const ImageContents& image_contents) {
	return computeFeatureDescriptor(image_contents.image_contents());
}

void ObjectClassifier::parseToFeatures(const OpenCVFeatures& from, Features* to) {
	// TODO(alex): See if we can parse keypoints and descriptors directly from string rather than
	// from cv::FileStorage
	to->key_points.clear();

	// Create temp file
	char filename[] = "featuresXXXXXX";
	int fd = mkstemp(filename);
	close(fd);
	// Keypoints
	// Write the cassandra string to disk
	FILE* f = fopen(filename, "wt");
	fprintf(f, "%s", from.keypoints().c_str());
	fclose(f);
	// Read it with a FileStorage
	FileStorage fs1(filename, FileStorage::READ);
	cv::read(fs1.getFirstTopLevelNode(), to->key_points);
	fs1.release();

	// Descriptor
	// Write the cassandra string to disk
	f = fopen(filename, "wt");
	fprintf(f, "%s", from.feature_descriptor().c_str());
	fclose(f);
	// Read it with a FileStorage
	FileStorage fs2(filename, FileStorage::READ);
	cv::read(fs2.getFirstTopLevelNode(), to->descriptor, to->descriptor);
	fs2.release();
	// Unlink the file
	unlink(filename);
	return;
}

void ObjectClassifier::parseToOpenCVFeatures(const Features& from, OpenCVFeatures* to) {
	// TODO(alex): See if we can parse keypoints and descriptors directly from string rather than
	// from cv::FileStorage

	// Create temp file
	char filename[] = "featuresXXXXXX";
	int fd = mkstemp(filename);
	close(fd);
	// Keypoints
	// Write the keypoints to disk
	FileStorage fs1(filename, FileStorage::WRITE);
	cv::write(fs1, "", from.key_points);
	fs1.release();
	// Read it into cassandra format
	ifstream f1(filename);
	string str((std::istreambuf_iterator<char>(f1)),
	                 std::istreambuf_iterator<char>());
	f1.close();
	to->set_keypoints(str);
	// Descriptor
	// Truncate the file
	FILE *ff = fopen(filename, "wt");
	fclose(ff);
	// Write the descriptor to disk
	FileStorage fs2(filename, FileStorage::WRITE);
	cv::write(fs2, "", from.descriptor);
	fs2.release();
	// Read it into cassandra format
	ifstream f2(filename);
	string str2((std::istreambuf_iterator<char>(f2)),
			std::istreambuf_iterator<char>());
	f2.close();
	to->set_feature_descriptor(str2);
	// Unlink the file
	unlink(filename);
	return;
}


// This method will classify objects against features and feature vectors that were already
// extracted beforehand
double ObjectClassifier::matchObject(const Features& current_features, const PossibleObject& object,
		ObjectBoundingBox& bounding_box) {
	// Match the current features against what we got from storage
	//FlannBasedMatcher matcher;
	BruteForceMatcher<L2<float> > matcher;
	vector<DMatch> matches;
	double certainty = 0;
	vector<double> match_percentages;
	vector<DMatch> best_matches;
	for (int i = 0; i < object.features_size(); ++i) {
		Features features;
		parseToFeatures(object.features(i), &features);

		// Match feature vectors against what we have
		matches.clear();
		matcher.match(current_features.descriptor, features.descriptor, matches);
		// Compute the min and max distances between key points
		double min_dist = 10000., max_dist = 0., mean_dist = 0., dist;
		int total_features = 0;
		for (unsigned int i = 0; i < matches.size(); ++i) {
			dist = matches[i].distance;
			if (isnan(dist) || isnan(-dist)) {
				--total_features;
				continue;
			}
			if (dist < min_dist) {
				min_dist = dist;
			}
			if (dist > max_dist) {
				max_dist = dist;
			}
			mean_dist += dist;
			++total_features;
		}
		if (total_features != 0) {
			mean_dist /= (total_features);
		} else {
			mean_dist = 0;
		}

		double good_matches_threshold = min_dist +
				Constants::FEATURE_VECTORS_THRESHOLD_DISTANCE * (mean_dist - min_dist);

		// Find the number of good matches
		unsigned int num_good_matches = 0;
		for (unsigned int i = 0; i < matches.size(); ++i) {
			if (matches[i].distance <= good_matches_threshold) {
				++num_good_matches;
			}
		}
		if (num_good_matches > best_matches.size()) {
			for (unsigned int i = 0; i < matches.size(); ++i) {
				if (matches[i].distance <= good_matches_threshold) {
					best_matches.push_back(matches[i]);
				}
			}
		}
		if (matches.size() != 0) {
			match_percentages.push_back(1. * num_good_matches / matches.size());
		}
	}
	sort(match_percentages.begin(), match_percentages.end(), std::greater<double>());

	certainty = match_percentages[0];

	// Find the bounding box by associating this image to the best match
	int minx = 10000, miny = 10000, maxx = 0, maxy = 0;
	for (unsigned int i = 0; i < best_matches.size(); ++i) {
		// Get the keypoints from the good matches
		Point2f point = current_features.key_points[best_matches[i].queryIdx].pt;
		if (minx > point.x) {
			minx = point.x;
		}
		if (miny > point.y) {
			miny = point.y;
		}
		if (maxx < point.x) {
			maxx = point.x;
		}
		if (maxy < point.y) {
			maxy = point.y;
		}
	}
	// Create the bounding box
	bounding_box.Clear();
	bounding_box.set_bottom(minx);
	bounding_box.set_top(maxx);
	bounding_box.set_left(miny);
	bounding_box.set_right(maxy);

	return certainty;
}

void ObjectClassifier::getDetectorAndExtractor(
		FeatureDetector** detector, DescriptorExtractor** extractor) {
	if (ObjectClassifier::detector_ == NULL) {
		detector_ = new SurfFeatureDetector();
	}
	if (ObjectClassifier::extractor_ == NULL) {
		extractor_ = new SurfDescriptorExtractor();
	}
	*detector = detector_;
	*extractor = extractor_;
	return;
}

void ObjectClassifier::processImage(Image* image) {
	image->clear_detected_objects();
	Features current_features = ObjectClassifier::computeFeatureDescriptor(image->image());
	for (int i = 0; i < image->possible_present_objects_size(); ++i) {
		const PossibleObject& possible_object = image->possible_present_objects(i);
		ObjectBoundingBox box;
		double confidence = matchObject(current_features, possible_object, box);
		if (confidence >= Constants::CONFIDENCE_THRESHOLD) {
			DetectedObject detected_object;
			detected_object.set_object_type(DetectedObject::BUILDING);
			detected_object.mutable_bounding_box()->CopyFrom(box);
			detected_object.set_id(possible_object.id());
			ObjectMetadata metadata;
			metadata.set_description("Found by OPENCV");
			metadata.set_name(possible_object.id());
			detected_object.mutable_metadata()->CopyFrom(metadata);
			image->add_detected_objects()->CopyFrom(detected_object);
		}
	}
	image->clear_possible_present_objects();
}

MultipleOpenCVFeatures ObjectClassifier::getAllOpenCVFeatures(const Image& image) {
	MultipleOpenCVFeatures ret;
	OpenCVFeatures* opencv_features;
	// Compute the features for the image. For the big image, we won't set the object_id field
	opencv_features = ret.add_features();
	Features features = computeFeatureDescriptor(image.image());
	ObjectClassifier::parseToOpenCVFeatures(features, opencv_features);
	// Compute the features for all the cropped images (objects) in this image
	for (int i = 0; i < image.detected_objects_size(); ++i) {
		const DetectedObject& detected_object = image.detected_objects(i);
		String cropped_image = detected_object.cropped_image();
		// We should also set the object_id field to the id of the detected object
		opencv_features = ret.add_features();
		features = computeFeatureDescriptor(cropped_image);
		ObjectClassifier::parseToOpenCVFeatures(features, opencv_features);
		opencv_features->set_object_id(detected_object.id());
	}
	return ret;
}

