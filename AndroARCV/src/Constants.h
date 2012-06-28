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
	static const double CONFIDENCE_THRESHOLD = 0.8;
	static const char* TEST_FOLDER;
#ifdef TESTING
	static const bool DEBUG = false;
#else
	static const bool DEBUG = true;
#endif

	static const double MAX_MATCHES_FOR_BEST_CONFIDENCE = 15;
};


#endif /* CONSTANTS_H_ */
