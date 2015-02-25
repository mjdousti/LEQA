/*
 * 
 * Copyright (C) 2014 Mohammad Javad Dousti and Massoud Pedram, SPORT lab,
 * University of Southern California. All rights reserved.
 * 
 * Please refer to the LICENSE file for terms of use.
 * 
*/
package edu.usc.qspr.layout;

import edu.usc.leqa.RuntimeConfig;

import java.awt.Dimension;

import edu.usc.qspr.layout.Layout.Types;


public class Qubit {
	private boolean sleep=false;
	private String name;
	private Dimension pos;
	private Layout layout;
	private boolean readyToGo=false;
	private int interactions=0;

	public void setSleep(){
		sleep=true;
	}
	
	public void incInteractions(){
		interactions++;
	}
	
	public int getInteractions(){
		return interactions;
	}	
	
	public void resetSleep(){
		sleep=false;
	}
	
	public boolean isSleep(){
		return sleep;
	}
	
	public boolean getReady(){
		return readyToGo;
	}
	
	public void setReady(boolean rtg){
		readyToGo=rtg;
	}
	
	
	public Qubit(String s, Dimension d, Layout l){
		name=new String(s);
		pos=new Dimension(d);
		layout=l;
	}
	
	public String getName(){
		return name;
	}
	
	public Dimension getPosition(){
		return pos;
	}

	public void move(Dimension dst){
		Dimension prevPosition=new Dimension(pos);
		pos.setSize(dst);
		
		layout.free(prevPosition, this);
		layout.occupy(pos, this);
		if(RuntimeConfig.VERBOSE){
			System.out.print("Move "+getName()+" ("+prevPosition.height+","+prevPosition.width+")->(");
			System.out.print(getPosition().height+","+getPosition().width+") ");
		}
	}
	@Override
	public String toString() {
		return getName();
	}
	
	@Override
	public boolean equals(Object obj) {
		return obj.equals(name);
	}
	
	//	Might be needed in the future
	public void setPosition(Dimension d){
		pos.height=d.height;
		pos.width=d.width;
	}


}
