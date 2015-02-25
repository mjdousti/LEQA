/*
 * 
 * Copyright (C) 2014 Mohammad Javad Dousti and Massoud Pedram, SPORT lab,
 * University of Southern California. All rights reserved.
 * 
 * Please refer to the LICENSE file for terms of use.
 * 
*/
package edu.usc.qspr.layout;


public class Interval{
	private int TTR, TTL, weight; 
	private Qubit qubit;
	public Interval(int ttr, int ttl, int w, Qubit q) {
		TTL=ttl;
		TTR=ttr;
		weight=w;
		qubit=q;
	}
	
	public boolean isInside(int time){
		if (time>=TTR && time <=TTL){
			return true;
		}else{
			return false;
		}
			
	}
	
	public Qubit getQubit(){
		return qubit;
	}
	
	public int getTTL(){
		return TTL;
	}

	public int getTTR(){
		return TTR;
	}
	
	public int getWeight(){
		return weight;
	}
	
	public int compareToTTR(Interval o) {
		if (TTR<o.getTTR())
			return -1;
		else if (TTR==o.getTTR())
			return 0;
		else
			return 1;
	}
}
