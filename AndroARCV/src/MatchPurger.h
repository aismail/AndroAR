/*
 * MatchPurger.h
 *
 *  Created on: Jun 8, 2012
 *      Author: alex.m.damian@gmail.com
 */

#ifndef MATCHPURGER_H_
#define MATCHPURGER_H_

#include <iostream>
#include <vector>
#include "Common.h"
#include "opencv2/features2d/features2d.hpp"


using namespace cv;
using std::vector;

class MatchPurger {
public:
	MatchPurger() {};
	virtual ~MatchPurger() {};

	virtual vector<DMatch> purgeMatches(const vector<DMatch>& matches,
			const Features& query_features,
			const Features& train_features) = 0;

	virtual vector<vector<DMatch> > purgeMatches(const vector<vector<DMatch> >& matches,
			const Features& query_features,
			const Features& train_features) = 0;

	virtual void printStatistics(int size_after, int size_before) {
		std::cout << "[" << getName() << "] Purged " << 100 * (1 - 1. * size_after / size_before)
				<< "% (" << (size_before - size_after) << "/" << size_before << ") features."
				<< std::endl;
	}

	virtual string getName() const {
		return "MatchPurger";
	}
};

#endif /* MATCHPURGER_H_ */
