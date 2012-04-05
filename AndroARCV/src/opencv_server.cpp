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
		Image image_to_parse = Communication::getImageMessage(*java_client);
		std::cerr << image_to_parse.DebugString();
	}

	delete java_client;
  
  return 0;
}
