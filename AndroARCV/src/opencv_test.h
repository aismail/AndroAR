/*
 * Author: alex.m.damian@gmail.com
 */
#include <string>
#include <iostream>
#include <fstream>
#include <cstdio>

#include "image_features.pb.h"
#include "ObjectClassifier.h"

using namespace androar;
using namespace cv;
using namespace std;

Image run_tests(string filename, int* time_processing) {
	*time_processing = 0;
	ObjectClassifier classifier;

	map<string, vector<OpenCVFeatures> > all_objects;
	char str[1024];
	int N, M;
	ifstream* file_reader;

	FILE *fin = fopen(filename.c_str(), "rt");
	fscanf(fin, "%d %d", &N, &M);

	// Train
	for (int i = 0; i < N; ++i) {
		int num_objects;
		Image image;
		fscanf(fin, "%s %d", str, &num_objects);
		// Big image
		string big_image_filename = string(Constants::TEST_FOLDER).append(str);
		image.mutable_image()->set_image_hash(big_image_filename);
		file_reader = new ifstream(big_image_filename.c_str());
		string big_image_contents((std::istreambuf_iterator<char>(*file_reader)),
				std::istreambuf_iterator<char>());
		delete file_reader;
		image.mutable_image()->set_image_contents(big_image_contents);
		// Detected objects
		for (int obj = 0; obj < num_objects; ++obj) {
			fscanf(fin, "%s", str);
			string object_id(str);
			fscanf(fin, "%s", str);
			string cropped_image_filename = string(Constants::TEST_FOLDER).append(str);
			file_reader = new ifstream(cropped_image_filename.c_str());
			string cropped_image_contents((std::istreambuf_iterator<char>(*file_reader)),
					std::istreambuf_iterator<char>());
			delete file_reader;
			DetectedObject *detected_object = image.add_detected_objects();
			detected_object->set_object_type(DetectedObject::BUILDING);
			detected_object->set_id(object_id);
			detected_object->set_cropped_image(cropped_image_contents);
			detected_object->mutable_bounding_box()->set_bottom(100);
			detected_object->mutable_bounding_box()->set_left(10);
			detected_object->mutable_bounding_box()->set_right(100);
			detected_object->mutable_bounding_box()->set_top(10);
		}
		// Compute all features
		MultipleOpenCVFeatures features = classifier.getAllOpenCVFeatures(image);
		for (int feature_num = 0; feature_num < features.features_size(); ++feature_num) {
			const OpenCVFeatures& feature = features.features(feature_num);
			if (!feature.has_object_id()) {
				continue;
			}
			string id = feature.object_id();
			all_objects[id].push_back(feature);
		}
	}
	// Test
	// We'll reuse the same pb object, in which the repeated possible_present_objects
	// field remains the same and only the image_contents changes
	Image image_template;
	for (map<string, vector<OpenCVFeatures> >::iterator it = all_objects.begin();
			it != all_objects.end();
			++it) {
		PossibleObject* obj = image_template.add_possible_present_objects();
		obj->set_id(it->first);
		for (vector<OpenCVFeatures>::iterator v_it = it->second.begin();
				v_it != it->second.end();
				++v_it) {
			obj->add_features()->CopyFrom(*v_it);
		}
	}
	for (int i = 0; i < M; ++i) {
		int num_objects;
		vector<string> expected_result;
		fscanf(fin, "%s %d", str, &num_objects);
		string test_image_filename = string(Constants::TEST_FOLDER).append(str);
		file_reader = new ifstream(test_image_filename.c_str());
		string test_image_contents((std::istreambuf_iterator<char>(*file_reader)),
				std::istreambuf_iterator<char>());
		delete file_reader;
		Image image;
		image.CopyFrom(image_template);
		image.mutable_image()->set_image_hash(test_image_filename);
		image.mutable_image()->set_image_contents(test_image_contents);
		for (int obj = 0; obj < num_objects; ++obj) {
			fscanf(fin, "%s", str);
			expected_result.push_back(string(str));
		}
		struct timeval start_time, end_time;
		gettimeofday(&start_time, NULL);
		classifier.processImage(&image);
		gettimeofday(&end_time, NULL);
		*time_processing += MILLISEC(start_time, end_time);
		image.mutable_image()->clear_image_contents();
		image.clear_possible_present_objects();
		// Let's check if everything is ok
		cout << "********** TEST " << (i + 1) << " **********" << endl;
		cout << image.DebugString() << endl;
		//assert((int) expected_result.size() == image.detected_objects_size());
	}
	return image_template;
}
