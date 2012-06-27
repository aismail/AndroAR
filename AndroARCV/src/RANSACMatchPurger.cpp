/*
 * RANSACMatchPurger.cpp
 *
 *  Created on: Jun 27, 2012
 *      Author: alex
 */
#include <iostream>

#include "RANSACMatchPurger.h"
#include "opencv2/calib3d/calib3d.hpp"

using namespace cv;

RANSACMatchPurger::RANSACMatchPurger() {}

RANSACMatchPurger::~RANSACMatchPurger() {}

vector<DMatch> RANSACMatchPurger::purgeMatches(
		const vector<DMatch>& matches,
		const Features& query_features,
		const Features& train_features) {
	vector<DMatch> good_matches;
	if (matches.size() < 8) {
		// RANSAC needs >= 8 matches
		return good_matches;
	}

	vector<Point2f> query_points, train_points;
	for (unsigned int i = 0; i < matches.size(); ++i) {
		unsigned int query_idx = matches[i].queryIdx;
		unsigned int train_idx = matches[i].trainIdx;
		if (query_idx < 0 || query_idx >= query_features.key_points.size() ||
				train_idx < 0 || train_idx >= train_features.key_points.size()) {
			continue;
		}
		Point2f query_point = query_features.key_points[query_idx].pt;
		Point2f train_point = train_features.key_points[train_idx].pt;

		query_points.push_back(query_point);
		train_points.push_back(train_point);
	}
	// Compute the fundamental matrix
	std::vector<uchar> status(query_points.size(), 0);
	// status[i] = 0, when the point/match is an outlier
	//             1, when the point/match is an inlier
	Mat fundamental_mat = findFundamentalMat(query_points, train_points, status, FM_RANSAC);
	for (unsigned int i = 0; i < status.size(); ++i) {
		if (status[i] == 1) {
			//inlier
			good_matches.push_back(matches[i]);
		}
	}
	std::cout << "[RANSACMatchPurger] Purged " << (100 - 100. * good_matches.size() / matches.size())
					<< "% (" << good_matches.size() << "/" << matches.size() << ") features."
					<< std::endl;

	return good_matches;
}

