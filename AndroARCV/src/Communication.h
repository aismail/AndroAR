/*
 * Communication.h
 *
 *  Created on: Mar 27, 2012
 *      Author: alex
 */

#ifndef COMMUNICATION_H_
#define COMMUNICATION_H_

#include "comm.pb.h"
#include "Socket.h"

using namespace androar;

class Communication {
public:
	static OpenCVRequest getImageMessage(Socket& socket);

private:
	Communication();
	virtual ~Communication();
	static void getSocketMessage(Socket& socket, void* buffer, int length);
	static void sendSocketMessage(Socket& socket, void* buffer, int length);
};

#endif /* COMMUNICATION_H_ */
