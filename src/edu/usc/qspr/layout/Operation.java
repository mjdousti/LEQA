/*
 * 
 * Copyright (C) 2014 Mohammad Javad Dousti and Massoud Pedram, SPORT lab,
 * University of Southern California. All rights reserved.
 * 
 * Please refer to the LICENSE file for terms of use.
 * 
*/
package edu.usc.qspr.layout;

public class Operation {
	private String name;
	private double error;
	private int delay;
	
	public Operation(String s, double e, int d){
		name=new String(s);
		error=e;
		delay=d;
	}
	
	public String getName(){
		return name;
	}
	
	public int getDelay(){
		return delay;
	}	
}
