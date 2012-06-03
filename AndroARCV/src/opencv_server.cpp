/*
 * Author: alex.m.damian@gmail.com
 */
#include <stdio.h>
#include <string.h>
#include <string>
#include <iostream>
#include <sys/stat.h>
#include <sys/time.h>
#include <sys/types.h>
#include <sys/socket.h>

#include "comm.pb.h"
#include "Communication.h"
#include "Constants.h"
#include "opencv_test.h"
#include "opencv2/core/core.hpp"
#include "opencv2/highgui/highgui.hpp"
#include "ObjectClassifier.h"
#include "Socket.h"

using namespace androar;
using namespace cv;
using namespace std;

const char* Constants::TEST_FOLDER =  "src/test/";

#define MILLISEC(s, e) ((e.tv_sec - s.tv_sec) * 1000 + (e.tv_usec - s.tv_usec) / 1000)

int main(int argc, char** argv) {

	// Test injection
	if (argc > 1 && strcmp(argv[1], "--test") == 0) {
		string filename = (argc > 2) ?
				argv[2] :
				string(Constants::TEST_FOLDER).append("features_test_input.txt");
		struct timeval start_time, end_time;
		gettimeofday(&start_time, NULL);
		run_tests(filename);
		gettimeofday(&end_time, NULL);
		cout << "Testing took " << MILLISEC(start_time, end_time) << " milliseconds." << endl;
		return 0;
	}

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
			cout << "Processing query for image " << image->image().image_hash() << endl;
			struct timeval start_time, end_time;
			gettimeofday(&start_time, NULL);
			classifier.processImage(image);
			gettimeofday(&end_time, NULL);
			cout << "Finished processing query for image " << image->image().image_hash() <<
					" of size " << image->image().image_contents().size() << " bytes." << endl;
			cout << "Processing took " << MILLISEC(start_time, end_time) << " milliseconds." << endl;
			// We don't need to send the image back
			if (Constants::DEBUG == false) {
				image->mutable_image()->set_image_contents("");
			}
			// Send the new image back to the client
			Communication::sendMessage(*java_client, *image);
		}
	}

	delete java_client;
  
  return 0;
}
