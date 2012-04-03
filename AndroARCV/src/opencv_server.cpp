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

using namespace androar;

#define PORT 6667
// TODO(alex): fix this hard coding

int main(int argc, char** argv) {
	Socket server_socket(PORT);
	server_socket.initSocket();
	Socket java_client = server_socket.acceptConnections();

	// Just read messages from the java server
	while (true) {
		Image image_to_parse = Communication::getImageMessage(java_client);
		std::cerr << image_to_parse.DebugString();
	}
  
  return 0;
}
