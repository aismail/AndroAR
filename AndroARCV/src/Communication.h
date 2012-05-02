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
using google::protobuf::Message;

class Communication {
public:
	static OpenCVRequest getImageMessage(Socket& socket);
	static void sendEmptyMessage(Socket& socket);
	static void sendReplyMessage(Socket& socket, void* message, int length);
	static void sendMessage(Socket& socket, const Message& message);

private:
	Communication();
	virtual ~Communication();
	static void getSocketMessage(Socket& socket, void* buffer, int length);
	static void sendSocketMessage(Socket& socket, void* buffer, int length);
};

#endif /* COMMUNICATION_H_ */
