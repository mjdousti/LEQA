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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import edu.usc.qspr.layout.Layout.Types;


public class Well {
	protected Dimension location;
	private Types type;
	protected List<Qubit> qubitList=new ArrayList<Qubit>(RuntimeConfig.CHANNEL_CAP);
	protected int qubitNo;
	private boolean used=false;

	
	private int expectQubit=0;
	
	public void resetExp(){
		expectQubit=0;
	}
	
	public void incExp(){
		if (expectQubit+1==RuntimeConfig.CHANNEL_CAP)
			expectQubit=Integer.MAX_VALUE;
		else
			expectQubit++;
	}
	
	public void decExp(){
		if (expectQubit==RuntimeConfig.CHANNEL_CAP)
			expectQubit=RuntimeConfig.CHANNEL_CAP-1;
		else
			expectQubit--;
	}
	
	public int getExpectedQubits(){
		return expectQubit;
	}
	/**
	 * 
	 * @return total number of current stalled and futur stalled qubits
	 */
	
	public Well(Dimension m, Types t) {
		location=new Dimension(m);
		type=t;
		qubitNo=0;
	}


	public void setUsed(boolean status){
		used=status;
		if (status==false){
			qubitNo=0;
			qubitList.clear();
		}
	}
	
	public boolean isUsed(){
		return used;
	}
		
	
	public Dimension getPosition(){
		return location;
	}


	
	public Types getType(){
		return type;
	}
	
	public boolean isOccupied(){
		return qubitNo>0;
		//return qubitList[0]!=null;
	}
	
	
	public void addQubits(Qubit q){
		setUsed(true);
		if (qubitNo==RuntimeConfig.CHANNEL_CAP){
			System.err.println("Not enough space at the well!");
			System.exit(-1);
		}
		qubitList.add(q);
		qubitNo++;		
	}

	public boolean removeQubit(Qubit q){
//		System.out.println("Qubit "+q +" is removed from "+getPosition()+ " remaining qubits: "+qubitNo);
		if (qubitList.remove(q)){
			qubitNo--;
			return true;
		}else
			return false;
	}
	
	public int getQubitsNo(){
		return qubitNo;
	}
	
	public List<Qubit> getQubitSet(){
		return qubitList;
	}
	
	public void printQubitSet(){
		System.out.println("Qubit List @("+getPosition().height+","+getPosition().width+")");
		for (Qubit q : qubitList) {
			System.out.println(q.getName());
		}
	}
	
	
	@Override
	public String toString() {
		String output=new String();
		switch (type) {
		case Basic:
			output+="Basic";
			break;
		case Interaction:
			output+="Interaction";
			break;
		}
		output+="("+location.height+"x"+location.width+")";
		return output;
	}
}
