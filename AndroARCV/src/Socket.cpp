/*
 * Socket.cpp
 *
 * Author: alex.m.damian@gmail.com
 */

#include "Socket.h"

#include <stdio.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netdb.h>
#include <string.h>
#include <unistd.h>

Socket::Socket(int port) : port(port), socket_descriptor(-1) {
}

Socket::Socket(int port, int socket_descriptor) :
		port(port), socket_descriptor(socket_descriptor) {}

Socket::~Socket() {
}

bool Socket::initSocket() {
	int sd_current, cc, fromlen, tolen;

	if ((socket_descriptor = socket(AF_INET, SOCK_STREAM, 0)) == -1) {
		perror("Unable to create socket");
		return false;
	}

	memset(&sin, 0, sizeof(sin));
	sin.sin_family = AF_INET;
	sin.sin_addr.s_addr = INADDR_ANY;
	sin.sin_port = htons(port);

	// Bind the socket to the port number
	if (bind(socket_descriptor, (struct sockaddr *) &sin, sizeof(sin)) == -1) {
		perror("Unable to bind the socket to the port number");
		return false;
	}
	return true;
}

Socket* Socket::acceptConnections() {
	if (listen(socket_descriptor, 5) == -1) {
		perror("Cannot listen on the socket");
		return false;
	}
	int addrlen = sizeof(pin);
	int sd_current = accept(
			socket_descriptor, (struct sockaddr *) &pin, (socklen_t *) &addrlen);
	if (sd_current == -1) {
		perror("Cannot accept client socket");
		return NULL;
	}
	return new Socket(pin.sin_port, sd_current);
}

void Socket::closeSocket() {
	close(socket_descriptor);
	return;
}
