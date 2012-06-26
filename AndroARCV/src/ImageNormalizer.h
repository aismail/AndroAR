/*
 * ImageNormalizer.h
 *
 *  Created on: Jun 26, 2012
 *      Author: alex.m.damian@gmail.com
 */

#ifndef IMAGENORMALIZER_H_
#define IMAGENORMALIZER_H_

#include "opencv2/highgui/highgui.hpp"

using namespace cv;

class ImageNormalizer {
public:
	ImageNormalizer() {}
	virtual ~ImageNormalizer() {}

	static Mat normalizeImage(Mat& raw_image) {
		return raw_image;
	}
};

#endif /* IMAGENORMALIZER_H_ */
