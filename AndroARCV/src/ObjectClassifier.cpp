/*
 * ObjectClassifier.cpp
 *
 *  Created on: Apr 5, 2012
 *      Author: alex.m.damian@gmail.com
 */

#include "ObjectClassifier.h"

#include <map>
#include <vector>

#include "Constants.h"

using std::map;
using std::vector;

ObjectClassifier::ObjectClassifier() {
	features_map = new map<string, Features >();
}

ObjectClassifier::~ObjectClassifier() {
}

// This method will classify objects against features and feature vectors that were already
// extracted beforehand
double ObjectClassifier::matchObject(
		const Mat& image, string object_id, ObjectBoundingBox& bounding_box) {
	Features features;
	if (features_map->find(object_id) == features_map->end()) {
		return 0;
	} else {
		features = features_map->find(object_id)->second;
	}

	FeatureDetector* detector;
	DescriptorExtractor* extractor;
	getDetectorAndExtractor(detector, extractor);

	Features current_features;
	// Detect key points
	detector->detect(image, current_features.key_points);
	// Extract feature vectors
	extractor->compute(image, current_features.key_points, current_features.descriptor);
	// Match feature vectors against what we have
	FlannBasedMatcher matcher;
	vector<DMatch> matches;
	matcher.match(current_features.descriptor, features.descriptor, matches);

	// Compute the min and max distances between key points
	int min_dist = 10000, max_dist = 0, mean_dist = 0;
	for (int i = 0; i < current_features.descriptor.rows; ++i) {
		double dist = matches[i].distance;
		if (dist < min_dist) {
			min_dist = dist;
		}
		if (dist > max_dist) {
			max_dist = dist;
		}
		mean_dist += dist;
	}
	mean_dist /= current_features.descriptor.rows;

	// Find the number of good matches
	vector<DMatch> good_matches;
	for (int i = 0; i < current_features.descriptor.rows; ++i) {
		if (matches[i].distance <
				min_dist + Constants::FEATURE_VECTORS_THRESHOLD_DISTANCE * (mean_dist - min_dist)) {
			good_matches.push_back(matches[i]);
		}
	}

	if (good_matches.empty()) {
		return 0;
	}

	// Find the bounding box
	int minx = 10000, miny = 10000, maxx = 0, maxy = 0;
	for (int i = 0; i < good_matches.size(); ++i) {
		// Get the keypoints from the good matches
		Point2f point = current_features.key_points[good_matches[i].queryIdx].pt;
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
	double certainty = 1. * good_matches.size() / current_features.descriptor.rows;
	return certainty;
	// Mat H = findHomography(obj, scene, CV_RANSAC);
}

void ObjectClassifier::getDetectorAndExtractor(
		FeatureDetector* detector, DescriptorExtractor* extractor) {
	detector = new SurfFeatureDetector(400);
	extractor = new SurfDescriptorExtractor();
	return;
}

void ObjectClassifier::train() {
	// We'll have to read all the images and train our classifiers
	return;
}
