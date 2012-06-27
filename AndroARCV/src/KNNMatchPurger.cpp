/*
 * KNNMatchPurger.cpp
 *
 *  Created on: Jun 27, 2012
 *      Author: alex.m.damian@gmail.com
 */
#include <iostream>

#include "KNNMatchPurger.h"

KNNMatchPurger::KNNMatchPurger() {}

KNNMatchPurger::~KNNMatchPurger() {}

vector<DMatch> KNNMatchPurger::purgeMatches(
		const vector<DMatch>& matches,
		const Features& query_features,
		const Features& train_features) {
	return matches;
}

vector<vector<DMatch> > KNNMatchPurger::purgeMatches(
		const vector<vector<DMatch> >& matches,
		const Features& query_features,
		const Features& train_features) {
	vector<vector<DMatch> > good_matches;
	for (unsigned int i = 0; i < matches.size(); ++i) {
		if (matches[i].size() < 2) {
			//TODO(alex) Remove the match? Or keep it?
			continue;
		}
		if (matches[i][0].distance < matches[i][1].distance * NEIGHBOR_RATIO_THRESHOLD) {
			good_matches.push_back(matches[i]);
		}
	}
	printStatistics(good_matches.size(), matches.size());
	return good_matches;
}


