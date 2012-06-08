/*
 * Common.h
 *
 *  Created on: Jun 8, 2012
 *      Author: alex
 */

#ifndef COMMON_H_
#define COMMON_H_

#include <vector>
#include "opencv2/features2d/features2d.hpp"

using namespace cv;

struct Features {
	vector<KeyPoint> key_points;
	Mat descriptor;
	Mat query_image;

	Features() : key_points(), descriptor() {}
};

#endif /* COMMON_H_ */
