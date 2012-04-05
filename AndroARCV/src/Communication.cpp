/*
 * Communication.cpp
 *
 *  Created on: Mar 27, 2012
 *      Author: alex.m.damian@gmail.com
 */

#include "Communication.h"

#include "image_features.pb.h"
#include "Socket.h"

#include <iostream>

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

Image Communication::getImageMessage(Socket& socket) {
	int message_size;

	// Read the length of the message
	getSocketMessage(socket, &message_size, sizeof(message_size));
	message_size = ntohl(message_size);
	// Read entire message, as byte array
	void* message_byte_array = malloc(message_size * sizeof(char));
	getSocketMessage(socket, message_byte_array, message_size);
	// Parse it into a valid Image protocol buffer
	Image image;
	image.ParseFromArray(message_byte_array, message_size);
	free(message_byte_array);
	return image;
}
