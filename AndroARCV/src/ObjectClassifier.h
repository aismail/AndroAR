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
#include "Common.h"
#include "MatchPurger.h"
#include "image_features.pb.h"

using namespace androar;
using namespace cv;
using std::map;
using std::vector;

class ObjectClassifier {
public:
	ObjectClassifier();
	virtual ~ObjectClassifier();

	double matchObject(const Features& current_features, const PossibleObject& object,
			ObjectBoundingBox* bounding_box, PossibleObject* updated_possible_object = NULL);
	static Features computeFeatureDescriptor(const ImageContents& image_contents);
	static Features computeFeatureDescriptor(const string& image_content);
	static void getDetectorAndExtractor(FeatureDetector** detector,
			DescriptorExtractor** extractor);
	static void parseToOpenCVFeatures(const Features& from, OpenCVFeatures* to);
	static void parseToFeatures(const OpenCVFeatures& from, Features* to);
	void processImage(Image* image);
	MultipleOpenCVFeatures getAllOpenCVFeatures(const Image& image);

private:
	static FeatureDetector* detector_;
	static DescriptorExtractor* extractor_;

	MatchPurger* match_purger;

};

#endif /* OBJECTCLASSIFIER_H_ */
