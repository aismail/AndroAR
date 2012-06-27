/*
 * STDMatchPurger.h
 *
 *  Created on: Jun 8, 2012
 *      Author: alex
 */

#ifndef STDMATCHPURGER_H_
#define STDMATCHPURGER_H_

#include "MatchPurger.h"

class STDMatchPurger: public MatchPurger {
public:
	STDMatchPurger();
	virtual ~STDMatchPurger();

	vector<DMatch> purgeMatches(const vector<DMatch>& matches,
			const Features& query_features,
			const Features& train_features);

	vector<vector<DMatch> > purgeMatches(const vector<vector<DMatch> >& matches,
			const Features& query_features,
			const Features& train_features);

	static const double STD_THRESHOLD = 0.5;
};

#endif /* STDMATCHPURGER_H_ */
