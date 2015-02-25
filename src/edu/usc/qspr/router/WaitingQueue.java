/*
 * 
 * Copyright (C) 2014 Mohammad Javad Dousti and Massoud Pedram, SPORT lab,
 * University of Southern California. All rights reserved.
 * 
 * Please refer to the LICENSE file for terms of use.
 * 
*/
package edu.usc.qspr.router;

import java.util.*;
import java.util.Map.Entry;

import edu.usc.qspr.qasm.Vertex;

// TODO: Auto-generated Javadoc
/**
 * The Class WaitingQueue.
 */
public class WaitingQueue extends HashMap<String, LinkedList<Vertex>>{

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 6511965634224816452L;

	
	
	/**
	 * Wakes up instructions which are waiting for the given qubit. Two cases may happen:<BR> 
	 * 	<ol><li>The first instruction which changes the state of qubit may be waken up.</li>
	 *  <li>The first several instructions which just use qubit as the control qubit may be waken up.</li></ol> 
	 * If all the other operands of the waiting instruction becomes ready, it will be returned. 
	 * 
	 * @param qubit the qubit which becomes free
	 * @return the list of ready instructions
	 */
	public LinkedList<Vertex> wakeUp(String qubit){
		LinkedList<Vertex> temp=get(qubit);
		LinkedList<Vertex> readyList=new LinkedList<Vertex>();
		Vertex v;
		
		//at least one instruction should be removed from the list.
		int independentInsts=0;
		int i=0;
		if (temp!=null && !temp.isEmpty()){
			//if the current one and the next one is not target, increase the independentInsts 
			while(independentInsts<temp.size() && temp.get(independentInsts).isReady())
				independentInsts++;

			if (independentInsts<temp.size() && temp.get(independentInsts).isTarget(qubit)){
				independentInsts++;
			}else{
				while(independentInsts<temp.size() && !temp.get(independentInsts).isTarget(qubit)){
					independentInsts++;
				}
			}

			do{
				independentInsts--;
				v=temp.get(i);

				if (v.isReady()){
					//TODO
//					v.addPriority(temp.size()- independentInsts);
					temp.remove(i);
					i--;
				}else{
					v.setReadyStatus(qubit);
					if (v.isReady()){
						temp.remove(i);
						i--;
						v.addPriority(temp.size()- independentInsts);
						readyList.add(v);
					}
//					else
//						break;
				}
				i++;
			}while(independentInsts>0);
		}

		
		return readyList;
	}
	
	public HashSet<Vertex> wakeUpAll(){
		HashSet<Vertex> readyList=new HashSet<Vertex>();
		String qubit;
		
		for (Iterator<Entry<String, LinkedList<Vertex>>> iterator = entrySet().iterator(); iterator.hasNext();) {
			qubit=iterator.next().getKey();
			readyList.addAll(wakeUp(qubit));
		}
		
		return readyList;
	}
	
	/* (non-Javadoc)
	 * @see java.util.AbstractMap#toString()
	 */
	public String toString(){
		String output=new String();
		Iterator<Entry<String, LinkedList<Vertex>>> it = entrySet().iterator();
		List<Vertex> temp;
		Entry<String, LinkedList<Vertex>> k;
	    while (it.hasNext()) {
	    	k=it.next();
	    	temp=k.getValue();
	    	output+=k.getKey()+":\t";
	    	for (int i = 0; i < temp.size(); i++) {
				if (i ==temp.size()-1)
					output+=temp.get(i);
				else
					output+=temp.get(i)+" | ";
			}
	    	output+=System.getProperty("line.separator");
	    }
	    return output;
	}
}
