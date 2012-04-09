/*
 * Socket.cpp
 *
 * Author: alex.m.damian@gmail.com
 */

#include <netinet/in.h>
#include <sys/types.h>
#include <sys/socket.h>

#ifndef SOCKET_H_
#define SOCKET_H_

class Socket {
public:
	Socket(int port);
	Socket(int port, int socket_descriptor);
	virtual ~Socket();

	bool initSocket();
	Socket* acceptConnections();
	void closeSocket();

	int getSocketDescriptor() {
		return socket_descriptor;
	}

private:
	int port;
	int socket_descriptor;

	// Internals
	struct sockaddr_in sin;
	struct sockaddr_in pin;
};

#endif /* SOCKET_H_ */
