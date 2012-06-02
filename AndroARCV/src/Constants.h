/*
 * Constants.h
 *
 *  Created on: Apr 5, 2012
 *      Author: alex.m.damian@gmail.com
 */

#ifndef CONSTANTS_H_
#define CONSTANTS_H_

class Constants {
private:
	Constants() {}
	virtual ~Constants() {}

public:
	static const int PORT = 6667;
	static const double FEATURE_VECTORS_THRESHOLD_DISTANCE = 0.5;
	static const double CONFIDENCE_THRESHOLD = 0.8;
	static const char* TEST_FOLDER;
	static const bool DEBUG = true;
};


#endif /* CONSTANTS_H_ */
