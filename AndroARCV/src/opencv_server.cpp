/*
 * Socket.cpp
 *
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

void processImage(Image* image, ObjectClassifier& classifier) {
	image->clear_detected_objects();
	const char* image_contents = image->image().image_contents().data();
	int image_contents_size = image->image().image_contents().size();
	Mat image_mat = imdecode(vector<char>(image_contents, image_contents + image_contents_size), 0);
	for (int i = 0; i < image->possible_present_objects_size(); ++i) {
		string possible_object_id = image->possible_present_objects(i);
		ObjectBoundingBox box;
		double confidence = classifier.matchObject(image_mat, possible_object_id, box);
		if (confidence >= Constants::CONFIDENCE_THRESHOLD) {
			DetectedObject detected_object;
			detected_object.set_object_type(DetectedObject::BUILDING);
			detected_object.mutable_bounding_box()->CopyFrom(box);
			detected_object.set_id(possible_object_id);
			ObjectMetadata metadata;
			metadata.set_description("Found by OPENCV");
			metadata.set_name(possible_object_id);
			detected_object.mutable_metadata()->CopyFrom(metadata);
			image->add_detected_objects()->CopyFrom(detected_object);
		}
	}
	image->clear_possible_present_objects();
}


int main(int argc, char** argv) {

	ObjectClassifier classifier;
	classifier.train();

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
			processImage(image, classifier);
			// Send the new image back to the client
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
