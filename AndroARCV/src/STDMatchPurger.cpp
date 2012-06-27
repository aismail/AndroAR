/*
 * STDMatchPurger.cpp
 *
 *  Created on: Jun 8, 2012
 *      Author: alex
 */

#include "STDMatchPurger.h"

#include <iostream>
#include "Constants.h"

STDMatchPurger::STDMatchPurger() {
}

STDMatchPurger::~STDMatchPurger() {
}

namespace {
	void computeMinAndMaxThreshold(const vector<DMatch>& matches, double* min_threshold,
			double* max_threshold) {
		double min_dist = 10000, max_dist = 0, mean_dist = 0, std = 0, dist;
		int total = 0;
		// MIN, MAX, MEAN
		for (unsigned int i = 0; i < matches.size(); ++i) {
			dist = matches[i].distance;
			if (isnan(dist) || isnan(-dist)) {
				--total;
				continue;
			}
			if (dist < min_dist) {
				min_dist = dist;
			}
			if (dist > max_dist) {
				max_dist = dist;
			}
			mean_dist += dist;
			++total;
		}
		if (total != 0) {
			mean_dist /= (total);
		} else {
			mean_dist = 0;
		}
		// STANDARD DEVIATION
		for (unsigned int i = 0; i < matches.size(); ++i) {
			dist = matches[i].distance;
			if (isnan(dist) || isnan(-dist)) {
				continue;
			}
			std += (dist - mean_dist) * (dist - mean_dist);
		}
		if (total != 0) {
			std /= total;
		} else {
			std = 0;
		}
		std = sqrt(std);
		// MIN AND MAX THRESHOLDS
		*min_threshold = mean_dist - STDMatchPurger::STD_THRESHOLD * std;
		*max_threshold = mean_dist + STDMatchPurger::STD_THRESHOLD * std;
	}

} //anonymous namespace

vector<DMatch> STDMatchPurger::purgeMatches(const vector<DMatch>& matches,
		const Features& query_features,
		const Features& train_features) {
	double min_threshold = 0, max_threshold = 0;
	computeMinAndMaxThreshold(matches, &min_threshold, &max_threshold);

	// Find the number of good matches
	vector<DMatch> good_matches;
	for (unsigned int i = 0; i < matches.size(); ++i) {
		if ((matches[i].distance >= min_threshold) && (matches[i].distance <= max_threshold)) {
			good_matches.push_back(matches[i]);
		}
	}
	std::cout << "[STDMatchPurger] Purged " << (100 - 100. * good_matches.size() / matches.size())
			<< "% features." << std::endl;
	return good_matches;
}

vector<vector<DMatch> > STDMatchPurger::purgeMatches(
		const vector<vector<DMatch> >& matches,
		const Features& query_features,
		const Features& train_features) {
	// We don't have support for this in the STDMatchPurger;
	return matches;
}

