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

vector<vector<DMatch> > GeometryMatchPurger::purgeMatches(
		const vector<vector<DMatch> >& matches,
		const Features& query_features,
		const Features& train_features) {
	// We don't have support for this in the GeometryMatchPurger;
	return matches;
}

vector<DMatch> GeometryMatchPurger::purgeMatches(const vector<DMatch>& matches,
		const Features& query_features,
		const Features& train_features) {
	vector<DMatch> good_matches;
	vector<pair<double, int> > slopes;
	double mean = 0, total = 0;
	double min = 1000, max = -1000;
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
		slope = 1. * (query_point.y - original_point.y) /
				(QUERY_IMAGE_AVERAGE_SIZE - query_point.x + original_point.x);
		if (min > slope) {
			min = slope;
		}
		if (max < slope) {
			max = slope;
		}
		total += slope;
		slopes.push_back(std::make_pair(slope, i));
	}
	if (slopes.size() == 0) {
		std::cout << "[GeometryMatchPurger] Purged 0% features." << std::endl;
		return good_matches;
	}
	// Compute mean
	mean = 1. * total / slopes.size();
	// Compute STD
	double std = 0;
	for (unsigned int i = 0; i < slopes.size(); ++i) {
		std += (mean - slopes[i].first) * (mean - slopes[i].first);
	}
	std = sqrt(std / slopes.size());
	std::cout << "[GeometryMatchPurger] Mean is " << mean << " std is " << std
			<< " min is " << min << " max is " << max << std::endl;
	for (unsigned int i = 0; i < slopes.size(); ++i) {
		if (slopes[i].first >= mean - GeometryMatchPurger::STD_THRESHOLD * std &&
				slopes[i].first <= mean + GeometryMatchPurger::STD_THRESHOLD * std) {
			good_matches.push_back(matches[slopes[i].second]);
		}
	}
	std::cout << "[GeometryMatchPurger] Purged " << (100. * good_matches.size() / matches.size())
				<< "% features." << std::endl;
	return good_matches;
}
