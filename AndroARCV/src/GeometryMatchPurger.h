/*
 * GeometryMatchPurger.h
 *
 *  Created on: Jun 8, 2012
 *      Author: alex
 */

#ifndef GEOMETRYMATCHPURGER_H_
#define GEOMETRYMATCHPURGER_H_

#include "MatchPurger.h"

class GeometryMatchPurger: public MatchPurger {
public:
	GeometryMatchPurger();
	virtual ~GeometryMatchPurger();

	vector<DMatch> purgeMatches(const vector<DMatch>& matches,
				const Features& query_features,
				const Features& train_features);

	vector<vector<DMatch> > purgeMatches(const vector<vector<DMatch> >& matches,
			const Features& query_features,
			const Features& train_features);

	string getName() const {
		return "GeometryMatchPurger";
	}

private:
	static const double STD_THRESHOLD = 0.5;
	static const int QUERY_IMAGE_AVERAGE_SIZE = 800;

};

#endif /* GEOMETRYMATCHPURGER_H_ */
