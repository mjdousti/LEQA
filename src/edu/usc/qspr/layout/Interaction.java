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
import java.util.List;
import java.util.PriorityQueue;

import edu.usc.qspr.layout.Layout.Types;
import edu.usc.qspr.qasm.Vertex;

public class Interaction extends Well{
	private ArrayList<String> targetQubit=new ArrayList<String>(2);
	
	public Interaction(Dimension m, Types t) {
		super(m, t);
	}

	public void addTargetQubit(String q){
		if (targetQubit.contains(q)){
			System.err.println("Qubit is already added");
		}else			
			targetQubit.add(q);
	}
	public boolean removeTargetQubit(String q){
		if (targetQubit.contains(q)){
			return targetQubit.remove(q);
		}else			
			return false;
	}
	
 	public Qubit getOtherQubit(String q){
		for (Qubit qubit : qubitList) {
			if (!qubit.getName().equals(q))
				return qubit;
		}
		return null;
	}
	
 	
 	public boolean allQubitsArrived(Vertex v){
 		boolean found;
 		
 		for (int i = 0; i < v.getOperandsNumber(); i++) {
 			found=false;
 	 		for (int j = 0; j < qubitNo; j++) {
 	 			if (qubitList.get(j)==null){
 	 				System.out.println("Qubit#: "+qubitNo);
 	 				System.exit(-1);
 	 			}else if (qubitList.get(j).getName().equals(v.getOperand(i))){
 	 				found=true;
 	 				break;
 	 			}
 			}
 	 		if (!found)
 	 			return false;
		}
 		return true;
 	}
 	

	
}