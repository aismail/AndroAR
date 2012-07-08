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
#include "GeometryMatchPurger.h"
#include "ImageNormalizer.h"
#include "KNNMatchPurger.h"
#include "RANSACMatchPurger.h"

using namespace std;

FeatureDetector* ObjectClassifier::detector_ = NULL;
DescriptorExtractor* ObjectClassifier::extractor_ = NULL;

ObjectClassifier::ObjectClassifier() {
	match_purger = new GeometryMatchPurger();
}

ObjectClassifier::~ObjectClassifier() {
	delete match_purger;
}

Features ObjectClassifier::computeFeatureDescriptor(const string& image_content) {
	// Create a Mat from the image we got and compute the features for that.
	const char* image_contents = image_content.data();
	int image_contents_size = image_content.size();
	cout << "[ObjectClassifier] Computing features for an image of size " << image_contents_size << " bytes." << endl;
	Mat image_mat_raw = imdecode(vector<char>(image_contents, image_contents + image_contents_size), 0);

	Mat image_mat = ImageNormalizer::normalizeImage(image_mat_raw);

	FeatureDetector* detector;
	DescriptorExtractor* extractor;
	getDetectorAndExtractor(&detector, &extractor);

	Features current_features;
	if (Constants::DEBUG) {
		current_features.query_image = image_mat;
	} else {
		current_features.query_image = Mat();
	}
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

	// Initial features
	if (from.has_cropped_image() && Constants::DEBUG) {
		const char* image_contents = from.cropped_image().data();
		int image_contents_size = from.cropped_image().size();
		to->query_image =
				imdecode(vector<char>(image_contents, image_contents + image_contents_size), 0);
	}
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

	if (Constants::DEBUG) {
		char filename[] = "featuresXXXXXX.jpg";
		int fd = mkstemps(filename, 4);
		close(fd);
		// Write the image to disk
		imwrite(filename, from.query_image);
		// Read it into cassandra format
		ifstream f(filename);
		string str((std::istreambuf_iterator<char>(f)),
				std::istreambuf_iterator<char>());
		f.close();
		// Unlink the file
		unlink(filename);
		to->set_cropped_image(str);
	}
	// Unlink the file
	unlink(filename);
	return;
}

namespace {

	void findBoundingBox(const vector<KeyPoint>& key_points,
			const vector<DMatch>& matches,
			ObjectBoundingBox* box) {
		// Find the bounding box by associating this image to the best match
			int minx = 10000, miny = 10000, maxx = 0, maxy = 0;
			for (unsigned int i = 0; i < matches.size(); ++i) {
				// Get the keypoints from the good matches
				Point2f point = key_points[matches[i].queryIdx].pt;
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
			box->Clear();
			box->set_bottom(minx);
			box->set_top(maxx);
			box->set_left(miny);
			box->set_right(maxy);
	}
} // anonymous namespace


// This method will classify objects against features and feature vectors that were already
// extracted beforehand
double ObjectClassifier::matchObject(const Features& current_features, const PossibleObject& object,
		ObjectBoundingBox* bounding_box, PossibleObject* updated_possible_object) {
	// Match the current features against what we got from storage
	FlannBasedMatcher matcher;
	KNNMatchPurger knn_match_purger;
	RANSACMatchPurger ransac_match_purger;

	vector<DMatch> matches;
	vector<vector<DMatch> > knn_matches1, knn_matches2;
	vector<DMatch> best_matches;

	float certainty = 0;

	for (int features_num = 0; features_num < object.features_size(); ++features_num) {
		cout << "[ObjectClassifier] Matching image against object " << object.id() << " " << features_num << endl;
		Features features;
		parseToFeatures(object.features(features_num), &features);

		// STEP (1): KNN match purger
		// Match feature vectors against what we have. We have decided to find matches using the
		// KNN algorithm. Specifically, we are targetting the following case, also stated in the
		// thesis doc: buildings that have repetitive patterns.
		// query -> train
		knn_matches1.clear();
		matcher.knnMatch(current_features.descriptor, features.descriptor, knn_matches1, 2);
		vector<vector<DMatch> > good_knn_matches1 =
				knn_match_purger.purgeMatches(knn_matches1, current_features, features);
		// train -> query
		knn_matches2.clear();
		matcher.knnMatch(features.descriptor, current_features.descriptor, knn_matches2, 2);
		vector<vector<DMatch> > good_knn_matches2 =
				knn_match_purger.purgeMatches(knn_matches2, features, current_features);
		// get the good matches, by getting the intersection between good_knn_matches1 and
		// good_knn_matches2 (based on symmetry)
		vector<DMatch> matches_subset;
		for (unsigned int i = 0; i < good_knn_matches1.size(); ++i) {
			for (unsigned int j = i; j < good_knn_matches2.size(); ++j) {
				const DMatch& m1 = good_knn_matches1[i][0];
				const DMatch& m2 = good_knn_matches2[j][0];
				if (m1.queryIdx == m2.trainIdx && m1.trainIdx == m2.queryIdx) {
					matches_subset.push_back(m1);
				}
			}
		}
		// STEP (2): RANSAC match purger
		matches_subset =
				ransac_match_purger.purgeMatches(matches_subset, current_features, features);
		// THESE ARE THE GOOD MATCHES
		vector<DMatch> good_matches = matches_subset;

		if (good_matches.size() > best_matches.size()) {
			best_matches = good_matches;
		}
		double current_certainty =
				min<float>(1, 1. * good_matches.size() / Constants::MAX_MATCHES_FOR_BEST_CONFIDENCE);
		certainty = max<float>(certainty, current_certainty);
		if (Constants::DEBUG && updated_possible_object != NULL) {
			Mat overall_matches;
			drawMatches(
					current_features.query_image,
					current_features.key_points,
					features.query_image,
					features.key_points,
					good_matches,
					overall_matches,
					Scalar::all(-1),
					Scalar::all(-1),
					vector<char>(),
					DrawMatchesFlags::NOT_DRAW_SINGLE_POINTS);

			char filename[] = "featuresXXXXXX.jpg";
			int fd = mkstemps(filename, 4);
			close(fd);
			// Write the image to disk
			imwrite(filename, overall_matches);
			// Read it into cassandra format
			ifstream f(filename);
			string str((std::istreambuf_iterator<char>(f)),
					std::istreambuf_iterator<char>());
			f.close();
			// Unlink the file
			unlink(filename);

			updated_possible_object->mutable_features(features_num)->set_result_match(str);
			updated_possible_object->mutable_features(features_num)->
					set_certainty(current_certainty);
		}
	}

	findBoundingBox(current_features.key_points, best_matches, bounding_box);
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
		PossibleObject* mutable_possible_object = image->mutable_possible_present_objects(i);
		ObjectBoundingBox box;
		double confidence;
		if (Constants::DEBUG) {
			confidence = matchObject(current_features, possible_object, &box, mutable_possible_object);
		} else {
			confidence = matchObject(current_features, possible_object, &box);
		}
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
	if (Constants::DEBUG == false) {
		image->clear_possible_present_objects();
	}
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
