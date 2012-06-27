/*
 * KNNMatchPurger.h
 *
 *  Created on: Jun 27, 2012
 *      Author: alex
 */

#ifndef KNNMATCHPURGER_H_
#define KNNMATCHPURGER_H_

#include "MatchPurger.h"

class KNNMatchPurger: public MatchPurger {
public:
	KNNMatchPurger();
	virtual ~KNNMatchPurger();

	vector<DMatch> purgeMatches(const vector<DMatch>& matches,
			const Features& query_features,
			const Features& train_features);

	vector<vector<DMatch> > purgeMatches(const vector<vector<DMatch> >& matches,
			const Features& query_features,
			const Features& train_features);

	string getName() const {
		return "KNNMatchPurger";
	}

private:
	static const double NEIGHBOR_RATIO_THRESHOLD = 0.8;
};

#endif /* KNNMATCHPURGER_H_ */
