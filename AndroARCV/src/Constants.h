/*
 * Constants.h
 *
 *  Created on: Apr 5, 2012
 *      Author: alex.m.damian@gmail.com
 */

#ifndef CONSTANTS_H_
#define CONSTANTS_H_

#define MILLISEC(s, e) ((e.tv_sec - s.tv_sec) * 1000 + (e.tv_usec - s.tv_usec) / 1000)

class Constants {
private:
	Constants() {}
	virtual ~Constants() {}

public:
	static const int PORT = 6667;
	static const double CONFIDENCE_THRESHOLD = 0.8;
	static const char* TEST_FOLDER;
	static const bool DEBUG = true;

	static const double MAX_MATCHES_FOR_BEST_CONFIDENCE = 10;
};


#endif /* CONSTANTS_H_ */
