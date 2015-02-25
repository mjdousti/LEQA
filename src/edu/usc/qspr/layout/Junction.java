/*
 * 
 * Copyright (C) 2014 Mohammad Javad Dousti and Massoud Pedram, SPORT lab,
 * University of Southern California. All rights reserved.
 * 
 * Please refer to the LICENSE file for terms of use.
 * 
*/
package edu.usc.qspr.layout;

import java.awt.Dimension;

import edu.usc.qspr.layout.Layout.Types;

public class Junction extends Well{

//	private Channel[] directChannels;
//	public Junction(Dimension m, Dimension n, int step, Types t, Channel []c) {
//		super(m, n, step, t);
//		directChannels=c;
//	}
	

	public Junction(Dimension m, Types t, Direction d) {
		super(m, t);
		direction=d;
	}

	enum Direction{
		Horizontal, Vertical, Old
	}
	
	private Direction direction;
	
//	public Channel[] getChannels(){
//		return directChannels;
//	}

	public Direction getDirection(){
		return direction;
	}
}
