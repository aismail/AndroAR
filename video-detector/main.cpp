#include <stdio.h>
#include <iostream>
#include <vector>
#include <algorithm>
#include "opencv2/opencv.hpp"
#include "opencv2/core/core.hpp"
#include "opencv2/highgui/highgui.hpp"

#include "commons.h"
#include "netutils.h"
#include "imaging.h"
	
using namespace std;
using namespace cv;

// Global variables defined in imaging.cpp
extern int detectionMethod;
extern float detectionThreshold;
extern float step;

// Object pool
vector<RefObject> objects;

// Vector needed for showing matches and bounding box
vector<KeyPoint> objectPoints;

// Using a sliding window with detection results for every frame
int window[WINDOW_SIZE];

// By default do not enable network services
bool networkEnabled = false;

// By default do not start detection
int detecting = 0;

// Number of distinct object ids
int objectIds = 0;

// Clears all reference object images
void emptyObjectPool(){
	int i;
	for(i = 0; i < objects.size(); i++){
		objects[i].keyPoints.clear();
	}
	objects.clear();
	objectIds = 0;
}

// Based on the values in the sliding window decides the id of the detected object
// returns the object id if concludent (enough frames with the same id)
// returns -1 otherwise
char decide(int window[]){
	int count[MAX_OBJECTS], misses = 0, max = -1, maxi = 0, i;
	for(i = 0; i < MAX_OBJECTS; i++)
		count[i] = 0;
	cout<<"WINDOW: ";
	for(i = 0; i < WINDOW_SIZE; i++){
		cout<<window[i]<<" ";
		if(window[i] == -1){
			misses++;
		} else {
			count[window[i]]++;
			if(count[window[i]] > max){
				max = count[window[i]];
				maxi = window[i];
			}
		}
	}
	cout<<endl<<"misses: "<<misses<<" max: "<<max<<" maxi: "<<maxi<<endl;
	if(misses > 0.1 * WINDOW_SIZE || max < 0.9 * WINDOW_SIZE)
		return 126;
	
	return (char) maxi;
}

// Registers the given image and id as an object in the object pool 
void registerObject(Mat img, int id){
	RefObject obj;
	obj = extractFeatures(img);
	obj.id = id;
	cout<<"reg id:"<<id<<endl;
	objects.push_back(obj);
}


// Comparison function needed for sorting
bool comp(KeyPoint a, KeyPoint b){
	return (a.size < b.size);
}

// Enables and disables network features
void networkSwitch(){
	
	if(networkEnabled){
		closeServer();
		networkEnabled = false;
	} else {
		Mat message(Size(800,600),CV_8UC3);
		putText (message, "Waiting for network client...", cvPoint(200,400), FONT_HERSHEY_SIMPLEX, 3, cvScalar(0,0,255), 2);
		imshow("Video", message);
		imshow("Result", message);
		initServer();
		networkEnabled = true;
	}
}

// Various initializations
void init(){
	
	// By default using SURF
	detectionMethod = DET_SURF;
	changeMethod(detectionMethod);
	
	// prepare sliding window
	for(int i = 0; i < WINDOW_SIZE; i++)
		window[i] = -1;
	
	// create working windows
	namedWindow("Video", 1);
    namedWindow("Result", 1);
    
    // init network related features
    if(networkEnabled){
		Mat message(Size(800,600),CV_8UC3);
		putText (message, "Waiting for network client...", cvPoint(200,400), FONT_HERSHEY_SIMPLEX, 3, cvScalar(0,0,255), 2);
		imshow("Video", message);
		imshow("Result", message);
		initServer();
	}
}

// Nicely exit program
void exitProgram(){
	exit(0);
}

// Shows a bounding box around the points matched
// img = image onto which to draw the box
// count = number of points to be considered
void showBoundingBox(Mat img, int count){
	
	std::sort(objectPoints.begin(), objectPoints.end(), comp);
	
	float oldDiff = 9999, diff;
	float maxy, maxx, miny, minx, centerx, centery, meanDistance;
	
	maxx = maxy = -1;
	minx = miny = 10000;
	
	centerx = centery = meanDistance = 0;
	
	for(int i = 1; i < count; i++){
		centerx +=objectPoints[i].pt.x;
		centery +=objectPoints[i].pt.y;
	}
	centerx /= count;
	centery /= count;
	
	for(int i = 1; i < count; i++){
		float d1 = objectPoints[i].pt.x - centerx;
		float d2 = objectPoints[i].pt.y - centery;
		meanDistance += sqrt((d1 * d1) + (d2 * d2));
	}
	
	meanDistance /= count;
	
	for(int i = 1; i < count; i++){

		diff = objectPoints[i].size - objectPoints[i-1].size;
		oldDiff = diff;
		
		float d1 = objectPoints[i].pt.x - centerx;
		float d2 = objectPoints[i].pt.y - centery;
		float dist = sqrt((d1 * d1) + (d2 * d2));
		
		if(dist > 2 * meanDistance)
			continue;
		
		if(objectPoints[i].pt.x < minx)
			minx = objectPoints[i].pt.x;
		if(objectPoints[i].pt.y < miny)
			miny = objectPoints[i].pt.y;
		if(objectPoints[i].pt.x > maxx)
			maxx = objectPoints[i].pt.x;
		if(objectPoints[i].pt.y > maxy)
			maxy = objectPoints[i].pt.y;
		
	}
	
	rectangle(img, Point2f(minx * 0.9f, miny * 0.9f), Point2f(maxx * 1.1f, maxy * 1.1f), CV_RGB(0,0,255));
}

// Process user keyboard input
void processKeyInput(Mat frame){
	int key = (unsigned char)waitKey(10);
		
	if(key >= '0' && key <= '9'){
		int i;
		bool found = false;
		registerObject(frame, key - '0');
		cout<<"Object registered"<<endl;
		for(i = 0; i < objects.size() - 1; i++)
			if(objects[i].id == key - '0'){
				found = true;
				break;
			}
		if(!found)
			objectIds++;
	}
	if(key == 'd'){
		detecting = !detecting;
		cout<<"Detecting: "<<detecting<<endl;
	}
	if(key == 27){
		exitProgram();
	}
	if(key == '-'){
		detectionThreshold -= step;
		cout<<"threshold "<<detectionThreshold<<endl;
	}
	if(key == '='){
		detectionThreshold += step;
		cout<<"threshold "<<detectionThreshold<<endl;
	}
	if(key == 'n'){
		printf("ceva\n");
		networkSwitch();
	}
	if(key >= 190 && key <= 201){
		switch(key){
			case 190:	//F1 = SURF
				detectionMethod = DET_SURF;
				break;
			case 191:	//F1 = SIFT
				detectionMethod = DET_SIFT;
				break;
			case 192:	//F1 = FAST
				detectionMethod = DET_FAST;
				break;
		}
		changeMethod(detectionMethod);
		
		// Once changing method, already computed features are discarded
		emptyObjectPool();
	}
}

int main(){
	Mat videoOutput, resultOutput;
	vector<RefObject>::iterator result;
	int frameCounter = 0, misses = 0;
		
	VideoCapture cap(0); // open the default camera
    if(!cap.isOpened()) {  // check if we succeeded
		cerr<<"Camera not fount"<<endl;
        return -1;
	}
        
	init();
    
    for(;;)
    {
		
        Mat frame;

		// Get new frame from desired source
        if(networkEnabled)
			frame = getFrame();
		else
			cap >> frame;
			
        frameCounter++;
        
        // Process user input
        processKeyInput(frame);
                		
		if(detecting && objects.size() > 0){
			bool found = false;
			vector<RefObject>::iterator it, result;
			int count, maxCount = -1;
			RefObject scene = extractFeatures(frame);
			
			for(it = objects.begin(); it != objects.end(); it++){
				
				if((count = objectInScene(*it, scene)) > 0){
					
					if(maxCount < count){
						maxCount = count;
						result = it;
					}
				}
			}
		
			if(maxCount == -1){
				window[frameCounter % WINDOW_SIZE] = -1;
			} else {
				window[frameCounter % WINDOW_SIZE] = result->id;
			}
		
			char res = decide(window);
			
			if(networkEnabled)
				sendResult(res);
			
			if(res == 126){
				cout<<"Not Found\n";
				resultOutput = frame;
				
				putText (resultOutput,"Object Not Detected", cvPoint(200,400), FONT_HERSHEY_SIMPLEX, 1, cvScalar(0,0,255), 2);

				
			} else {
				char text[100];
				sprintf(text, "Detected Object: %d", res);
				
				for(int i = 0; i < objects.size(); i++)
					if(objects[i].id == res){
						resultOutput = objects[i].image.clone();
						break;
					}
					
				putText (resultOutput, text, cvPoint(200,400), FONT_HERSHEY_SIMPLEX, 1, cvScalar(0,255,0), 2);
				showBoundingBox(videoOutput, maxCount);
			}
			
			drawKeypoints(frame, scene.keyPoints, videoOutput);
			
		} else if(!detecting){
			videoOutput = frame;
			resultOutput = frame;
			
			putText (resultOutput,"Detection: off", cvPoint(20,25), FONT_HERSHEY_SIMPLEX, 1, cvScalar(0,0,255), 2);
			
			if(networkEnabled)
				sendResult((char) 127);
		}
		
		if(detecting){
			putText (videoOutput,"Detection: on", cvPoint(20,25), FONT_HERSHEY_SIMPLEX, 1, cvScalar(255,0,0), 2);
		}
		
		putText (videoOutput, getDetectionName(), cvPoint(300,25), FONT_HERSHEY_SIMPLEX, 0.75f, cvScalar(255,0,0), 2);
		
		char text[100];
		sprintf(text, "%d objects in %d images", objectIds, (int) objects.size());
		
		putText (videoOutput, text, cvPoint(20, videoOutput.rows - 20), FONT_HERSHEY_SIMPLEX, 0.75f, cvScalar(255,255,0), 2);
		
        imshow("Video", videoOutput);
        imshow("Result", resultOutput);
    }
	
	return 0;
}
