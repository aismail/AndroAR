/*
 * ObjectClassifier.cpp
 *
 *  Created on: Apr 5, 2012
 *      Author: alex.m.damian@gmail.com
 */

#include "ObjectClassifier.h"

#include <cstdlib>
#include <cstdio>
#include <map>
#include <vector>

#include "Constants.h"
#include "opencv2/highgui/highgui.hpp"
#include "opencv2/features2d/features2d.hpp"

using std::map;
using std::vector;

ObjectClassifier::ObjectClassifier() {
	features_map = new map<string, Features >();
}

ObjectClassifier::~ObjectClassifier() {
}

Features ObjectClassifier::computeFeatureDescriptor(const Image& image) {
	// Create a Mat from the image we got and compute the features for that.
	const char* image_contents = image.image().image_contents().data();
	int image_contents_size = image.image().image_contents().size();
	Mat image_mat = imdecode(vector<char>(image_contents, image_contents + image_contents_size), 0);

	FeatureDetector* detector = NULL;
	DescriptorExtractor* extractor = NULL;
	getDetectorAndExtractor(detector, extractor);

	Features current_features;
	// Detect key points
	detector->detect(image_mat, current_features.key_points);
	// Extract feature vectors
	extractor->compute(image_mat, current_features.key_points, current_features.descriptor);

	return current_features;
}

void parseToFeatures(const OpenCVFeatures& from, Features* to) {
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
	// unlink(filename);
	return;
}

// This method will classify objects against features and feature vectors that were already
// extracted beforehand
double ObjectClassifier::matchObject(const Features& current_features, const PossibleObject& object,
		ObjectBoundingBox& bounding_box) {
	// Match the current features against what we got from storage
	FlannBasedMatcher matcher;
	vector<DMatch> matches;
	double certainty = 0;
	vector<DMatch> best_matches;
	for (int i = 0; i < object.features_size(); ++i) {
		Features features;
		parseToFeatures(object.features(i), &features);

		// Match feature vectors against what we have
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
		unsigned int num_good_matches = 0;
		for (int i = 0; i < current_features.descriptor.rows; ++i) {
			if (matches[i].distance < min_dist +
					Constants::FEATURE_VECTORS_THRESHOLD_DISTANCE * (mean_dist - min_dist)) {
				//good_matches.push_back(matches[i]);
				++num_good_matches;
			}
		}
		if (num_good_matches > best_matches.size()) {
			for (int i = 0; i < current_features.descriptor.rows; ++i) {
				if (matches[i].distance < min_dist +
						Constants::FEATURE_VECTORS_THRESHOLD_DISTANCE * (mean_dist - min_dist)) {
					best_matches.push_back(matches[i]);
				}
			}
		}
		certainty += 1. * num_good_matches / current_features.descriptor.rows;
	}

	certainty /= object.features_size();

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
		FeatureDetector* detector, DescriptorExtractor* extractor) {
	detector = new SurfFeatureDetector(400);
	extractor = new SurfDescriptorExtractor();
	return;
}

