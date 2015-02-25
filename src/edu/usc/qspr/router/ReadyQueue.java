/*
 * 
 * Copyright (C) 2014 Mohammad Javad Dousti and Massoud Pedram, SPORT lab,
 * University of Southern California. All rights reserved.
 * 
 * Please refer to the LICENSE file for terms of use.
 * 
*/
package edu.usc.qspr.router;

import java.util.Map.Entry;
import java.util.*;

import edu.usc.qspr.qasm.Vertex;


// TODO: Auto-generated Javadoc
/**
 * This class maintains a the instructions available for issuing. It identifies the commands on the same qubit(s) and
 * provides necessary data structures to remove instructions based on their priority. It also provides the insert 
 * functionality.  
 */
public class ReadyQueue{
	ArrayList <HashSet<String>> keys=new ArrayList<HashSet<String>>();
	ArrayList<PriorityQueue<Vertex>> values=new ArrayList<PriorityQueue<Vertex>>();
	ArrayList<Vertex> issuedCmd=new ArrayList<Vertex>();
	 
	 
	/** The waiting queue. */
	private WaitingQueue wq;
	
	
	public PriorityQueue<Vertex> get(String qubit){
		for (int i = 0; i < keys.size(); i++) {
			if (keys.get(i).contains(qubit))
				return values.get(i);		
		}	
		return null;
	}
	
	public boolean containsKey(HashSet<String> hs){
		for (int i = 0; i < keys.size(); i++) {
			//TODO: It might be better to check it element by element
			if (keys.get(i)==hs)
				return true;		
		}	
		return false;
	}
	
	public boolean containsKey(String qubit){
		for (int i = 0; i < keys.size(); i++) {
			if (keys.get(i).contains(qubit))
				return true;		
		}	
		return false;
	}
	
	private int findInstruction(Vertex v){
		int temp;
		for (int i = 0; i < v.getOperandsNumber(); i++) {
			temp=findSet(v.getOperand(i));
			if(temp!=-1){
				if (values.contains(v))
					return temp;
				else
					return -1;
			}
		}
		return -1;

	}

	
	private int findSet(String qubit){
		for (int i = 0; i < keys.size(); i++) {
			if (keys.get(i).contains(qubit))
				return i;		
		}	
		return -1;
	}
	
	public PriorityQueue<Vertex> remove(HashSet<String> hs){
		for (int i = 0; i < keys.size(); i++) {
			//TODO: It might be better to check it element by element
			if (keys.get(i)==hs){
				return remove(i);
			}
		}	
		return null;
	}
	
	public PriorityQueue<Vertex> remove(int i){
		keys.remove(i);
		issuedCmd.remove(i);
		return values.remove(i);
	}
	
	
	
	/**
	 * Checks to see the queue still contains the given qubit.
	 *
	 * @param pq the given priority queue to be searched
	 * @param qubit the to be searched
	 * @return true, if the priority queue contains the qubit
	 */
	private boolean queueContains(PriorityQueue<Vertex> pq, String qubit){
		for (Iterator<Vertex> iterator = pq.iterator(); iterator.hasNext();) {
			if (iterator.next().hasOperand(qubit))
				return true;		
		}		
		return false;
	}


	/**
	 * Removes the given instruction from priority queue and still maintaining the properties of
	 * ready queue.
	 *
	 * @param pq  the given priority queue to be which the instruction is will be removed from
	 * @param v the instruction to remove
	 * @return true, if the given priority queue contains the instruction and the instrction is 
	 * deleted successfully
	 */
	private boolean removeInstruction(HashSet<String> hs, PriorityQueue<Vertex> pq, Vertex v){
		if (pq.remove(v)==false)
			return false;
		for (int i = 0; i < v.getOperandsNumber(); i++) {
			if (!queueContains(pq, v.getOperand(i)))
				hs.remove(v.getOperand(i));				
		}		
		return true;
	}
	
	/**
	 * Instantiates a new ready queue. It give a waiting queue which will receive new instructions from
	 * when the currently running instructions are finished.
	 * @param waitingQueue the waiting queue which will receive new instructions from  
	 */
	public ReadyQueue(WaitingQueue waitingQueue) {
		this.wq=waitingQueue;
		addAll(wq.wakeUpAll());
	}

	/**
	 * Returns the next available instructions for issuing.
	 *
	 * @return the next available instructions
	 */
	public Set<Vertex> getNext(){
		HashSet<Vertex> highPriorityCmds=new HashSet<Vertex>();
		Vertex temp;
		for (int i = 0; i < keys.size(); i++) {
			temp=values.get(i).remove();
			issuedCmd.set(i, temp);
			highPriorityCmds.add(temp);
		}	

		return highPriorityCmds;
	}
	
	
	
	public void addAll(int index, Collection<? extends Vertex> v){
		for (Iterator<? extends Vertex> iterator = v.iterator(); iterator.hasNext();) {
			add(index, iterator.next());			
		}
	}
	
	
	/**
	 * Adds the specified instructions to the ready queue.
	 *
	 * @param c the element to be added to the queue.
	 */
	public void addAll(Collection<? extends Vertex> c){
		for (Iterator<? extends Vertex> iterator = c.iterator(); iterator.hasNext();) {
			add(iterator.next());			
		}
	}

	public void add (int index, Vertex v){
		for (int i = 0; i < v.getOperandsNumber(); i++) {
				keys.get(index).add(v.getOperand(i));
		}
		values.get(index).add(v);
	}
	
	/**
	 * Adds the specified instruction to the ready queue.
	 *
	 * @param v element to be added to the queue.
	 */
	public void add(Vertex v){
		//If ready queue contains the operands of v, add it to the corresponding priority queue
		int temp;
		for (int i = 0; i < v.getOperandsNumber(); i++) {
			temp=findSet(v.getOperand(i));
			if (temp!=-1){
				for (int j = 0; j < v.getOperandsNumber(); j++) {
					keys.get(temp).add(v.getOperand(j));
				}
				values.get(temp).add(v);
				return;
			}
		}

		//If the queue doesn't contain the operands of v, make a new entry for it
		HashSet<String> hs=new HashSet<String>();
		for (int i = 0; i < v.getOperandsNumber(); i++) {
			hs.add(v.getOperand(i));
		}
		PriorityQueue<Vertex> vertices=new PriorityQueue<Vertex>();
		vertices.add(v);

		keys.add(hs);
		values.add(vertices);
		issuedCmd.add(null);
	}
	

	/**
	 * Gets the next available with the highest priority instruction which has at least one qubit in common with the given finished instruction.
	 *
	 * @param v the finished instruction.
	 * @return the next available instruction with the highest priority.
	 */
	public LinkedList<Vertex> getNext(Vertex v){
		int temp;
		LinkedList<Vertex> wakenUp;
		LinkedList<Vertex> readyInsts=new LinkedList<Vertex>();
		HashSet<Integer> readySets=new HashSet<Integer>();
		int qubitGroupNo;
		Vertex cmd;
		
		temp=findSet(v.getOperand(0));
		
		//If there's no other instructions in the value queue, remove the entry
		if (values.get(temp).isEmpty()){
			remove(temp);
			for (int i = 0; i < v.getOperandsNumber(); i++) {
				wakenUp=wq.wakeUp(v.getOperand(i));
				if (!wakenUp.isEmpty()){
					qubitGroupNo=keys.size();
					addAll(wakenUp);
					//In order not to awake previous waiting groups
					if (keys.size()>qubitGroupNo)
						readySets.add(new Integer(findSet(v.getOperand(i))));

				}
			}
		}else{
			//TODO: check the incoming is equal to the issued
			readySets.add(temp);
			for (int i = 0; i < v.getOperandsNumber(); i++) {
				if (!queueContains(values.get(temp), v.getOperand(i))){
					//no others uses qubit "v.getOperand(i)" in this queue
					//try to wake up more instructions which are using "v.getOperand(i)"
					wakenUp=wq.wakeUp(v.getOperand(i));
					//no instructions are ready for "v.getOperand(i)", so remove "v.getOperand(i)" from the keys
					keys.get(temp).remove(v.getOperand(i));
					if (!wakenUp.isEmpty()){
						qubitGroupNo=keys.size();
						addAll(wakenUp);
						//In order not to awake previous waiting groups
						if (keys.size()>qubitGroupNo)
							readySets.add(findSet(v.getOperand(i)));
					}
				}
			}
		}
		
		int i;
		for (Iterator<Integer> iterator = readySets.iterator(); iterator.hasNext();) {
			//(issuedCmd.get(readySets.get(i).intValue())==null || issuedCmd.get(readySets.get(i).intValue())==v)){
			i= (Integer) iterator.next().intValue();
			cmd=values.get(i).remove();
			readyInsts.add(cmd);
			issuedCmd.set(i , cmd);
			
		}
	
		
		return readyInsts;
	}
	
	public void clear(){
		keys.clear();
		values.clear();
	}
	
	public String toString(){
		String output=new String();
		output+=System.getProperty("line.separator");
		
		PriorityQueue<Vertex> vertices;
		//int keys=0;
		
		for (int i = 0; i < keys.size(); i++) {
//			keys++;
			HashSet<String> qubits=keys.get(i);
			for (Iterator<String> iterator = qubits.iterator(); iterator.hasNext();) {
				output+=iterator.next();
				if (iterator.hasNext())
					output+=", ";
				else
					output+=": ";
			}
			
			vertices=values.get(i);
			for (Iterator<Vertex> iterator = vertices.iterator(); iterator.hasNext();) {
				output+="("+iterator.next()+")";
				if (iterator.hasNext())
					output+=", ";
			}
			output+=System.getProperty("line.separator");
		}
		
//		output+=keys;
		return output;
	}

	public boolean isEmpty() {
		if (keys.size()==0)
			return true;
		else
			return false;
	}
}
