#ifndef _COMMONS_H
#define _COMMONS_H

#include "opencv2/opencv.hpp"
#include "opencv2/core/core.hpp"

#include <vector>

#define DET_SURF 0
#define DET_SIFT 1
#define DET_FAST 2

#define DET_NAME_SURF "Detection: SURF"
#define DET_NAME_SIFT "Detection: SIFT"
#define DET_NAME_FAST "Detection: FAST"
#define DET_NAME_UNKNOWN "Detecion: unknown"

#define WINDOW_SIZE 10
#define MAX_OBJECTS 10

typedef struct RefObject{
	cv::Mat image;
    std::vector<cv::KeyPoint> keyPoints;
    cv::Mat descriptors;
    int id;
} RefObject;


#endif
