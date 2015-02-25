/*
 * 
 * Copyright (C) 2014 Mohammad Javad Dousti and Massoud Pedram, SPORT lab,
 * University of Southern California. All rights reserved.
 * 
 * Please refer to the LICENSE file for terms of use.
 * 
*/
package edu.usc.qspr.layout;

import java.util.Comparator;

public class TTLComparator implements Comparator<Interval> {

	@Override
    public int compare(Interval x, Interval y)
    {
		if (x.getTTL()<y.getTTL())
			return -1;
		else if (x.getTTL()==y.getTTL())
			return 0;
		else
			return 1;
    }
}
