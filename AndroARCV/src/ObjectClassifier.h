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
};

class ObjectClassifier {
public:
	ObjectClassifier();
	virtual ~ObjectClassifier();

	void train();
	double matchObject(const Mat& image, string object_id, ObjectBoundingBox& bounding_box);

	void getDetectorAndExtractor(FeatureDetector* detector, DescriptorExtractor* extractor);

private:
	map<string, Features>* features_map;

};

#endif /* OBJECTCLASSIFIER_H_ */
