/*
 * Socket.cpp
 *
 * Author: alex.m.damian@gmail.com
 */
#include "Socket.h"

#include <stdio.h>
#include <string.h>
#include <iostream>
#include <sys/types.h>
#include <sys/socket.h>

#include "comm.pb.h"
#include "Communication.h"
#include "Constants.h"

using namespace androar;

int main(int argc, char** argv) {
	Socket server_socket(Constants::PORT);
	server_socket.initSocket();
	Socket* java_client = server_socket.acceptConnections();

	// Just read messages from the java server
	while (true) {
		OpenCVRequest request = Communication::getImageMessage(*java_client);
		if (request.request_type() == OpenCVRequest::STORE) {
			// Store it and send an empty message
			Communication::sendEmptyMessage(*java_client);
		} else if (request.request_type() == OpenCVRequest::QUERY) {
			// Process the possible_present_objects repeated field and return a new image with
			// the newly set detected objects, if any.

			Image* image = request.mutable_image_contents();
			image->add_possible_present_objects("JUST_A_TEST");
			// For now, we'll just send the same message back
			int serialized_size = image->ByteSize();
			char *serialized_pb = new char[serialized_size];
			image->SerializeToArray(serialized_pb, serialized_size);
			Communication::sendReplyMessage(*java_client, serialized_pb, serialized_size);
			delete[] serialized_pb;
		}
	}

	delete java_client;
  
  return 0;
}
