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
import java.util.Iterator;
import java.util.PriorityQueue;

import org.jgrapht.graph.DefaultWeightedEdge;


// TODO: Auto-generated Javadoc
/**
 * The Class ChannelEdge.
 */
@SuppressWarnings("serial")
public class ChannelEdge extends DefaultWeightedEdge {
	private Well v1;
	private Well v2;
	private final int baseCost;
	//	private int currentCost;
	/** Specifies whether the channel is used or not. */
	private boolean isUsed;
	private PriorityQueue<Interval> TTR, TTL;

	/**
	 * Adds to the cost of channel for the given interval [<i>ttr</i>, <i>ttl</i>] for <i>qubit</i> by 1;
	 * No interaction will be done on the qubit in the channel.
	 *
	 * @param ttr the <i><b>T</b>ime <b>T</b>o <b>R</b>each</i> of a qubit to the channel 
	 * @param ttl the <i><b>T</b>ime <b>T</b>o <b>L</b>eave</i> of a qubit to the channel
	 * @param q the qubit object
	 */
	public void addCost(int ttr, int ttl, Qubit q){
		Interval temp=new Interval(ttr, ttl, 1, q);
		TTR.add(temp);
		TTL.add(temp);
	}

	/**
	 * Adds to the cost of channel for the given interval [<i>ttr</i>, <i>ttl</i>] for <i>qubit</i> by <i>cost</i>.
	 *
	 * @param ttr the <i><b>T</b>ime <b>T</b>o <b>R</b>each</i> of a qubit to the channel 
	 * @param ttl the <i><b>T</b>ime <b>T</b>o <b>L</b>eave</i> of a qubit to the channel
	 * @param q the qubit object
	 * @param cost the imposed by the qubit
	 */
	public void addCost(int ttr, int ttl, Qubit q, int cost){
		Interval temp=new Interval(ttr, ttl, cost, q);
		TTR.add(temp);
		TTL.add(temp);
	}

	/**
	 * Removes the interval (and the cost) associated with qubit <i>q</i>.
	 *
	 * @param q the qubit which is going to depart the channel
	 */
	public void removeCost(Qubit q){
		Interval temp;
		for (Iterator<Interval> iterator = TTR.iterator(); iterator.hasNext();) {
			temp=iterator.next();
			if (temp.getQubit()==q){
				TTR.remove(temp);
				TTL.remove(temp);
				break;
			}
		}
	}

	/**
	 * Gets the cost at time <i>t</i>.
	 *
	 * @param t the time
	 * @return the cost
	 */
	public int getCost(int t){
		Interval temp;
		int cost=0;

		//TODO: should be implemented as binary search later for speedup
		for (Iterator<Interval> iterator = TTR.iterator(); iterator.hasNext();) {
			temp=iterator.next();
			if (temp.isInside(t)){
				cost+=temp.getWeight();
			}
		}
		return cost;
	}
	
	int getPassingTime(int t0){
		return 0;
	}
	
	public double getWeight(){
		return baseCost;
	}


	/**
	 * Instantiates a new channel edge.
	 *
	 * @param j1 the j1
	 * @param j2 the j2
	 */
	public ChannelEdge(Well j1, Well j2, int cost) {
		v1=j1;
		v2=j2;
		
		TTR=new PriorityQueue<Interval>(RuntimeConfig.TTDefaultCap, new TTRComparator());
		TTL=new PriorityQueue<Interval>(RuntimeConfig.TTDefaultCap, new TTLComparator());
		baseCost=cost;
	}

	/**
	 * Gets the other vertex.
	 *
	 * @param v the v
	 * @return the other vertex
	 */
	public Well getOtherVertex(Well v) {
		if (v==v1)
			return v2;
		else if(v==v2)
			return v1;
		else{
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see org.jgrapht.graph.DefaultEdge#toString()
	 */
	public String toString(){
		String output=new String();
		output+="("+getV1().getPosition().height+","+getV1().getPosition().width+") <-> "
				+"("+ getV2().getPosition().height+","+getV2().getPosition().width+")";
		return output;
	}


	/**
	 * Sets the v1.
	 *
	 * @param j the new v1
	 */
	public void setV1(Well j){
		v1=j;
	}

	/**
	 * Sets the v2.
	 *
	 * @param j the new v2
	 */
	public void setV2(Well j){
		v2=j;
	}

	/**
	 * Gets the vertex 1.
	 *
	 * @return the vertex 1
	 */
	public Well getV1() {
		return v1;
	}

	/**
	 * Gets the vertex 2.
	 *
	 * @return the vertex 2
	 */
	public Well getV2() {
		return v2;
	}
	public Dimension getLocation(){
		Dimension x=new Dimension();
		x.height = (v1.getPosition().height + v2.getPosition().height)/2;
		x.width = (v1.getPosition().width + v2.getPosition().width)/2;
		return x;
	}

	/**
	 * Sets the used.
	 *
	 * @param status the new used
	 */
	public void setUsed(boolean status) {
		isUsed=status;
	}

	/**
	 * Checks if is used.
	 *
	 * @return true, if is used
	 */
	public boolean isUsed() {
		return isUsed;
	}

}
