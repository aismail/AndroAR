#ifndef _IMAGING_H
#define _IMAGING_H

#include "opencv2/opencv.hpp"
#include "opencv2/core/core.hpp"

#include "commons.h"

// Function for changing constants specific to method
void changeMethod(int method);

// Extracts features for the given image using the current global method
// returns a RefObject with the computed information
RefObject extractFeatures(cv::Mat image);

// Decides if a given object is presend in the given scene by comparing their
// respective features
// returns the number of valid matches if there are enough to be considered a
// positive match
// returns 0 otherwise
int objectInScene(RefObject obj, RefObject scene);

// Returns a char array representing the name of the current detection method
char* getDetectionName();

// Returns and image showing matching points between object and scene
// Not currently used
cv::Mat showMatching(RefObject obj, RefObject scene);

#endif
