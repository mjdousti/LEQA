/*
 * 
 * Copyright (C) 2014 Mohammad Javad Dousti and Massoud Pedram, SPORT lab,
 * University of Southern California. All rights reserved.
 * 
 * Please refer to the LICENSE file for terms of use.
 * 
*/
package edu.usc.qspr.qasm;

public class Command {
	private String name;
	private String[] operands=null;
	private boolean[] ready=null;
	
	private long issueTime, finishTime;
	
	public void issue(long t){
		issueTime=t;
	}
	
	public void finish(long t){
		finishTime=t;
	}
	
	public long getTotalTime(){
		return finishTime-issueTime;
	}
	
	public Command(String s, String ...ops) {
		name=s;
		operands=ops;
		
		if (ops!=null){
			ready=new boolean[ops.length];
			for (int i = 0; i < ops.length; i++) {
				ready[i]=false;
			}
		}
	}
	
	public String getName(){
		return name;
	}
	
	public int getOperandsNumber(){
		return operands==null? 0 : operands.length;
	}

	public String getOperand(int index){
		return operands[index];
	}

	public boolean getReadyStatus(int i){
		return ready[i];
	}
	
	public boolean isReady(){
		for (int i = 0; i < ready.length; i++) {
			if (ready[i]==false)
				return false;
		}
		return true;
	}

	
	public boolean setReadyStatus(int i, boolean status){
		if (i>=0 && i<operands.length){
			ready[i]=status;
			return true;
		}else
			return false;
	}
	
	public boolean setReadyStatus(String q){
		for (int i = 0; i < operands.length; i++) {
			if (operands[i].equals(q)){
				ready[i]=true;
				return true;
			}
		}
		return false;
	}
	
	public boolean isTarget(String q){
		if (operands[0].equals(q))
			return true;
		else
			return false;
	}
}
