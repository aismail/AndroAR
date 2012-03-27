/*
 * Socket.cpp
 *
 * Author: alex.m.damian@gmail.com
 */

#ifndef SOCKET_H_
#define SOCKET_H_

namespace comm_androar_cv {

class Socket {
public:
	Socket(int port);
	Socket(int port, int socket_descriptor);
	virtual ~Socket();

	bool initSocket();
	Socket acceptConnections();
	void closeSocket();

private:
	int port;
	int socket_descriptor;

	// Internals
	struct sockaddr_in sin;
	struct sockaddr_in pin;
};

} /* namespace comm_androar_cv */
#endif /* SOCKET_H_ */
