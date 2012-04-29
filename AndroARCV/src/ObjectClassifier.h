/*
 * ObjectClassifier.h
 *
 *  Created on: Apr 5, 2012
 *      Author: alex.m.damian@gmail.com
 */

#ifndef OBJECTCLASSIFIER_H_
#define OBJECTCLASSIFIER_H_

#include <map>
#include <vector>

#include "opencv2/core/core.hpp"
#include "opencv2/features2d/features2d.hpp"
#include "image_features.pb.h"

using namespace androar;
using namespace cv;
using std::map;
using std::vector;

struct Features {
	vector<KeyPoint> key_points;
	Mat descriptor;

	Features() : key_points(), descriptor() {}
};

class ObjectClassifier {
public:
	ObjectClassifier();
	virtual ~ObjectClassifier();

	double matchObject(const Features& current_features, const PossibleObject& object,
			ObjectBoundingBox& bounding_box);
	static Features computeFeatureDescriptor(const Image& image);

	static void getDetectorAndExtractor(FeatureDetector** detector,
			DescriptorExtractor** extractor);

	static void parseToOpenCVFeatures(const Features& from, OpenCVFeatures* to);
	static void parseToFeatures(const OpenCVFeatures& from, Features* to);

private:
	map<string, Features>* features_map;

};

#endif /* OBJECTCLASSIFIER_H_ */
