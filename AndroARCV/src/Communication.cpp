/*
 * Communication.cpp
 *
 *  Created on: Mar 27, 2012
 *      Author: alex.m.damian@gmail.com
 */

#include "Communication.h"

#include "comm.pb.h"
#include "Socket.h"

#include <iostream>

using namespace std;
using google::protobuf::Message;

Communication::Communication() {}

Communication::~Communication() {}

void Communication::getSocketMessage(Socket& socket, void* buffer, int length) {
	int total_size_read = 0;
	do {
		int size_read = recv(socket.getSocketDescriptor(),
				(char*) buffer + total_size_read, length, 0);
		total_size_read += size_read;
	} while (total_size_read < length);
}

void Communication::sendSocketMessage(Socket& socket, void* buffer, int length) {
	int total_size_sent = 0;
	do {
		int size_sent = send(socket.getSocketDescriptor(),
				(char*) buffer + total_size_sent, length, 0);
		total_size_sent += size_sent;
	} while (total_size_sent < length);
}

OpenCVRequest Communication::getRequestMessage(Socket& socket) {
	int message_size;

	// Read the length of the message
	getSocketMessage(socket, &message_size, sizeof(message_size));
	message_size = ntohl(message_size);
	// Read entire message, as byte array
	void* message_byte_array = malloc(message_size * sizeof(char));
	getSocketMessage(socket, message_byte_array, message_size);
	// Parse it into a valid Image protocol buffer
	OpenCVRequest request;
	request.ParseFromArray(message_byte_array, message_size);
	free(message_byte_array);
	return request;
}

void Communication::sendReplyMessage(Socket& socket, void* message, int length) {
	// Send the length of the message
	int network_length = htonl(length);
	sendSocketMessage(socket, &network_length, sizeof(network_length));
	// Send entire message, as byte array
	sendSocketMessage(socket, message, length);
	return;
}

void Communication::sendEmptyMessage(Socket& socket) {
	int zero = htonl(0);
	sendSocketMessage(socket, &zero, sizeof(zero));
}

void Communication::sendMessage(Socket& socket, const Message& message) {
	int serialized_size = message.ByteSize();
	char *serialized_pb = new char[serialized_size];
	message.SerializeToArray(serialized_pb, serialized_size);
	sendReplyMessage(socket, serialized_pb, serialized_size);
	delete[] serialized_pb;
}
