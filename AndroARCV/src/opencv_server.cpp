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
	Features current_features = ObjectClassifier::computeFeatureDescriptor(*image);
	for (int i = 0; i < image->possible_present_objects_size(); ++i) {
		const PossibleObject& possible_object = image->possible_present_objects(i);
		ObjectBoundingBox box;
		double confidence = classifier.matchObject(current_features, possible_object, box);
		if (confidence >= Constants::CONFIDENCE_THRESHOLD) {
			DetectedObject detected_object;
			detected_object.set_object_type(DetectedObject::BUILDING);
			detected_object.mutable_bounding_box()->CopyFrom(box);
			detected_object.set_id(possible_object.id());
			ObjectMetadata metadata;
			metadata.set_description("Found by OPENCV");
			metadata.set_name(possible_object.id());
			detected_object.mutable_metadata()->CopyFrom(metadata);
			image->add_detected_objects()->CopyFrom(detected_object);
		}
	}
	image->clear_possible_present_objects();
}

MultipleOpenCVFeatures getAllOpenCVFeatures(ObjectClassifier& classifier, const Image& image) {
	MultipleOpenCVFeatures ret;
	OpenCVFeatures* opencv_features;
	// Compute the features for the image. For the big image, we won't set the object_id field
	opencv_features = ret.add_features();
	Features features = classifier.computeFeatureDescriptor(image);
	ObjectClassifier::parseToOpenCVFeatures(features, opencv_features);
	// Compute the features for all the cropped images (objects) in this image
	for (int i = 0; i < image.detected_objects_size(); ++i) {
		const DetectedObject& detected_object = image.detected_objects(i);
		String cropped_image = detected_object.cropped_image();
		// We should also set the object_id field to the id of the detected object
		opencv_features = ret.add_features();
		features = classifier.computeFeatureDescriptor(cropped_image);
		ObjectClassifier::parseToOpenCVFeatures(features, opencv_features);
		opencv_features->set_object_id(detected_object.id());
	}
	return ret;
}

int main(int argc, char** argv) {

	ObjectClassifier classifier;

	Socket server_socket(Constants::PORT);
	server_socket.initSocket();
	Socket* java_client = server_socket.acceptConnections();

	// Just read messages from the java server
	while (true) {
		OpenCVRequest request = Communication::getImageMessage(*java_client);
		if (request.request_type() == OpenCVRequest::STORE) {
			// Compute the features for this image and its objects and send it back
			MultipleOpenCVFeatures all_opencv_features =
					getAllOpenCVFeatures(classifier, request.image_contents());
			// Send them back
			Communication::sendMessage(*java_client, all_opencv_features);
		} else if (request.request_type() == OpenCVRequest::QUERY) {
			// Process the possible_present_objects repeated field and return a new image with
			// the newly set detected objects, if any.
			Image* image = request.mutable_image_contents();
			processImage(image, classifier);
			// Send the new image back to the client
			Communication::sendMessage(*java_client, *image);
		}
	}

	delete java_client;
  
  return 0;
}
