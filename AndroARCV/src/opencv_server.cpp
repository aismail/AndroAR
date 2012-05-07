/*
 * Author: alex.m.damian@gmail.com
 */
#include <stdio.h>
#include <string.h>
#include <string>
#include <iostream>
#include <sys/types.h>
#include <sys/socket.h>

#include "comm.pb.h"
#include "Communication.h"
#include "Constants.h"
#include "opencv2/core/core.hpp"
#include "opencv2/highgui/highgui.hpp"
#include "ObjectClassifier.h"
#include "Socket.h"

using namespace androar;
using namespace cv;
using namespace std;

int main(int argc, char** argv) {

	ObjectClassifier classifier;

	Socket server_socket(Constants::PORT);
	server_socket.initSocket();
	Socket* java_client = server_socket.acceptConnections();

	// Just read messages from the java server
	while (true) {
		OpenCVRequest request = Communication::getRequestMessage(*java_client);
		if (request.request_type() == OpenCVRequest::STORE) {
			// Compute the features for this image and its objects and send it back
			MultipleOpenCVFeatures all_opencv_features =
					classifier.getAllOpenCVFeatures(request.image_contents());
			// Send them back
			Communication::sendMessage(*java_client, all_opencv_features);
		} else if (request.request_type() == OpenCVRequest::QUERY) {
			// Process the possible_present_objects repeated field and return a new image with
			// the newly set detected objects, if any.
			Image* image = request.mutable_image_contents();
			classifier.processImage(image);
			// Send the new image back to the client
			Communication::sendMessage(*java_client, *image);
		}
	}

	delete java_client;
  
  return 0;
}
