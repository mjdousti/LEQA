/*
 * 
 * Copyright (C) 2014 Mohammad Javad Dousti and Massoud Pedram, SPORT lab,
 * University of Southern California. All rights reserved.
 * 
 * Please refer to the LICENSE file for terms of use.
 * 
*/
package edu.usc.qspr.router;

import java.awt.Dimension;
import java.util.ArrayList;

import edu.usc.qspr.layout.ChannelEdge;
import edu.usc.qspr.layout.Layout;
import edu.usc.qspr.layout.Qubit;
import edu.usc.qspr.qasm.Vertex;

public class BasePath  implements Comparable<BasePath>{
	private Vertex cmd;
	private long delay;
	private Layout layout;

	public BasePath(Vertex command, long simTime, Layout layout) {
		cmd=command;
//		delay=simTime+layout.getMoveDelay()+layout.getOpDelay(command.getName());
		delay=simTime+layout.getOpDelay(command.getName());
	}
	
	public Vertex getVertex(){
		return cmd;
	}
	
	public long getDelay(){
		return delay;
	}
	
	@Override
	public int compareTo(BasePath o) {
		if (getDelay()<o.getDelay())
			return -1;
		else if (getDelay()>o.getDelay())
			return 1;
		else{
			return cmd.compareTo(o.getVertex());
		}
	}
}
