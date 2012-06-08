/*
 * GeometryMatchPurger.cpp
 *
 *  Created on: Jun 8, 2012
 *      Author: alex.m.damian@gmail.com
 */

#include "GeometryMatchPurger.h"
#include <iostream>

using std::pair;

GeometryMatchPurger::GeometryMatchPurger() {}

GeometryMatchPurger::~GeometryMatchPurger() {}

vector<DMatch> GeometryMatchPurger::purgeMatches(const vector<DMatch>& matches,
		const Features& query_features,
		const Features& train_features) {
	std::cout << "Purging features using the GeometryMatchPurger" << std::endl;
	vector<DMatch> good_matches;
	vector<pair<double, int> > slopes;
	int num_inf = 0, num = 0;
	double mean = 0, total_without_inf = 0;
	for (unsigned int i = 0; i < matches.size(); ++i) {
		unsigned int query_idx = matches[i].queryIdx;
		unsigned int train_idx = matches[i].trainIdx;
		if (query_idx < 0 || query_idx >= query_features.key_points.size() ||
				train_idx < 0 || train_idx >= train_features.key_points.size()) {
			continue;
		}
		Point2f query_point = query_features.key_points[query_idx].pt;
		Point2f original_point = train_features.key_points[train_idx].pt;
		if (query_point == original_point) {
			good_matches.push_back(matches[i]);
			continue;
		}
		double slope;
		if (query_point.x == original_point.x) {
			slope = (query_point.y < original_point.y) ? INT_MAX : INT_MIN;
			num_inf += (query_point.y < original_point.y) ? 1 : -1;
		} else {
			slope = 1. * (query_point.y - original_point.y) / (query_point.x - original_point.x);
			total_without_inf += slope;
		}
		slopes.push_back(std::make_pair(slope, i));
		++num;
	}
	if (num == 0) {
		return good_matches;
	}
	// Compute mean
	mean = 1. * (total_without_inf / num) + 1. * num_inf / num * INT_MAX;
	// Compute STD
	double std = 0;
	for (unsigned int i = 0; i < slopes.size(); ++i) {
		std += (mean - slopes[i].first) * (mean - slopes[i].first);
	}
	std = sqrt(std / num);
	for (unsigned int i = 0; i < slopes.size(); ++i) {
		if (slopes[i].first >= mean - std && slopes[i].first <= mean + std) {
			good_matches.push_back(matches[slopes[i].second]);
		}
	}
	return good_matches;
}
