#include <stdio.h>
#include "opencv2/opencv.hpp"
#include "opencv2/core/core.hpp"
#include "opencv2/features2d/features2d.hpp"

#include "imaging.h"
#include "commons.h"

using namespace cv;
using namespace std;

// By default using SURF detection
int detectionMethod = DET_SURF;

// Constants used in detection
float detectionThreshold;
float step;

// Vector needed for bounding box, defined in main.cpp
extern vector<KeyPoint> objectPoints;

// Function for changing constants specific to method
void changeMethod(int method){
	
	switch(method){
		case DET_SURF:
			detectionThreshold = 0.17f;
			step = 0.01f;
			break;
		
		case DET_SIFT:
			detectionThreshold = 300.0f;
			step = 10.0f;
			break;
		
		case DET_FAST:
			detectionThreshold = 0.2f;
			step = 0.01f;
			break;
			
		default:
			detectionThreshold = 0.17f;
			step = 0.01f;
	}
}

// Extracts features for the given image using the current global method
// returns a RefObject with the computed information
RefObject extractFeatures(Mat image){
	RefObject obj;
	FeatureDetector *detector;
	DescriptorExtractor *extractor;
	
	switch(detectionMethod){
		case DET_SURF:
			detector = new SurfFeatureDetector(400);
			extractor = new SurfDescriptorExtractor;
			break;
		
		case DET_SIFT:
			detector = new SiftFeatureDetector();
			extractor = new SiftDescriptorExtractor();
			break;
			
		case DET_FAST:
			detector = new FastFeatureDetector();
			extractor = new SurfDescriptorExtractor();
		
		default:
			detector = new SurfFeatureDetector(400);
			extractor = new SurfDescriptorExtractor;
	}
	//TODO remove me after implementing ORB
	//OrbFeatureDetector detector;
    //OrbDescriptorExtractor extractor;
    
    detector->detect(image, obj.keyPoints);
	extractor->compute(image, obj.keyPoints, obj.descriptors);
    
    obj.image = image.clone();
    
    delete detector;
    delete extractor;
    
    return obj;	
}

// Decides if a given object is presend in the given scene by comparing their
// respective features
// returns the number of valid matches if there are enough to be considered a
// positive match
// returns 0 otherwise
int objectInScene(RefObject obj, RefObject scene){

    BruteForceMatcher<L2<float> > matcher;
    //FlannBasedMatcher matcher;
    
    vector<DMatch> matches;
    matcher.match(obj.descriptors, scene.descriptors, matches);
	int count = 0;
	
	objectPoints.clear();
	for(int i = 0; i < matches.size(); i++){
		KeyPoint aux;
		if(matches[i].distance < detectionThreshold){
			count++;
		}
		aux.size = matches[i].distance;
		aux.pt.x = scene.keyPoints[matches[i].trainIdx].pt.x;
		aux.pt.y = scene.keyPoints[matches[i].trainIdx].pt.y;
		
		if(aux.pt.x > 0 && aux.pt.y > 0)
			objectPoints.push_back(aux);
		
	}
	
    printf("Found %d/%d corresponding pairs. %d (%d)\n", (int) count, (int) matches.size(), (int)(obj.keyPoints.size() * 0.8), (int) obj.keyPoints.size());
    if( count < 5)//(int)(obj.keyPoints.size() * 0.4f) )
        return 0;

	return count;
	//TODO remove me
/*
	vector<vector<DMatch> > matches; 
    matcher.radiusMatch(obj.descriptors, scene.descriptors, matches, 0.5f);
    
    int count = 0;
    for(int i = 0; i < matches.size(); i++){
		if(matches[i].size() > 1)
			count++;
		
	}
	
	printf("Found %d corresponding pairs. %d (%d)\n", count, (int)(obj.keyPoints.size() * 0.9), obj.keyPoints.size());
    if( count < (int)(obj.keyPoints.size() * 0.9) )
        return 0;

	return count;
	*/
}


// Returns a char array representing the name of the current detection method
char* getDetectionName(){
	switch(detectionMethod){
		case DET_SURF:
			return DET_NAME_SURF;
		case DET_SIFT:
			return DET_NAME_SIFT;
		case DET_FAST:
			return DET_NAME_FAST;
		
	}
	return DET_NAME_UNKNOWN;
}

// Returns and image showing matching points between object and scene
// Not currently used
Mat showMatching(RefObject obj, RefObject scene){
	Mat img_matches;
	BruteForceMatcher<L2<float> > matcher;
    //vector<vector<DMatch> > matches;
     
    //matcher.match(obj.descriptors, scene.descriptors, matches);

	vector<DMatch> matches;
     
    matcher.match(obj.descriptors, scene.descriptors, matches);
	drawMatches(obj.image, obj.keyPoints, scene.image, scene.keyPoints, matches, img_matches);
	
	return img_matches;
}

