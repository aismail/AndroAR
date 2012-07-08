/*
 * VideoFeedParser.h
 *
 *  Created on: Jun 29, 2012
 *      Author: alex.m.damian@gmail.com
 */

#include <string>
#include "image_features.pb.h"

#include "opencv2/opencv.hpp"

using namespace androar;
using namespace cv;
using namespace std;

#ifndef VIDEOFEEDPARSER_H_
#define VIDEOFEEDPARSER_H_

class VideoFeedParser {
public:
	VideoFeedParser();
	virtual ~VideoFeedParser();

	static bool parseVideo(string filename, Image& image_template, string object_id,
			int once_every_frames = 30, int start_frame = 0, int stop_frame = 32000);

	static const int THUMBNAIL_SIZE = 100;
	static const int NUM_THUMBNAILS_PER_ROW = 10;


};

#endif /* VIDEOFEEDPARSER_H_ */
