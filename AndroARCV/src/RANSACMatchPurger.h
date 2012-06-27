/*
 * RANSACMatchPurger.h
 *
 *  Created on: Jun 27, 2012
 *      Author: alex
 */

#ifndef RANSACMATCHPURGER_H_
#define RANSACMATCHPURGER_H_

#include "MatchPurger.h"

class RANSACMatchPurger: public MatchPurger {
public:
	RANSACMatchPurger();
	virtual ~RANSACMatchPurger();

	vector<DMatch> purgeMatches(const vector<DMatch>& matches,
			const Features& query_features,
			const Features& train_features);

	vector<vector<DMatch> > purgeMatches(const vector<vector<DMatch> >& matches,
			const Features& query_features,
			const Features& train_features) {
		return matches;
	}

	string getName() const {
			return "RANSACMatchPurger";
		}

private:
	static const double RANSAC_DISTANCE = 3;
	static const double RANSAC_CONFIDENCE = 0.99;
};

#endif /* RANSACMATCHPURGER_H_ */
