/*
 * 
 * Copyright (C) 2014 Mohammad Javad Dousti and Massoud Pedram, SPORT lab,
 * University of Southern California. All rights reserved.
 * 
 * Please refer to the LICENSE file for terms of use.
 * 
*/
package edu.usc.qspr.router;

import edu.usc.leqa.RuntimeConfig;

import java.awt.Dimension;
import java.util.*;
import java.util.Map.Entry;

import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.DefaultEdge;

import edu.usc.qspr.layout.ChannelEdge;
import edu.usc.qspr.layout.Interaction;
import edu.usc.qspr.layout.Junction;
import edu.usc.qspr.layout.Layout;
import edu.usc.qspr.layout.Qubit;
import edu.usc.qspr.layout.Well;
import edu.usc.qspr.qasm.Vertex;
/**
 * @author Mohammad Javad
 *
 */
public class EventDrivenSimulator {
	private long simTime=0;


	Layout layout;
	WaitingQueue waitQueue=new WaitingQueue();
	List<Vertex> busyInsts=new ArrayList<Vertex>();
	PriorityQueue<Path> issueQueue=new PriorityQueue<Path>();;
	ReadyQueue readyQueue;
	List<Vertex> commands;
	
	
	public EventDrivenSimulator(Layout layout, List<Vertex> cmds) {
		this.layout=layout;
		commands=cmds;
	}
	
	
	/**
	 * Schedules the instructions in the commands list. It clears the <i>waitQueue</i>, <i>readyQueue</i>, and <i>issueQueue</i>. 
	 * Then it reschedules instructions in the <i>waitQueue</i> and <i>readyQeue</i>. 
	 */
	public void schedule(){
		waitQueue.clear();
		issueQueue.clear();
		
		for (Vertex v : commands) {
			if (v.isSentinel())
				continue;
			for (int i = 0; i < v.getOperandsNumber(); i++) {
				v.setReadyStatus(i, false);
				if (!waitQueue.containsKey(v.getOperand(i))){
					waitQueue.put(v.getOperand(i), new LinkedList<Vertex>());
				}
				waitQueue.get(v.getOperand(i)).add(v);
			}
		}
	
		issueQueue.clear();
		readyQueue=new ReadyQueue(waitQueue);

		if (RuntimeConfig.VERBOSE){
			System.out.println("Scheduling is done successfully!");	
		}
			
		if (RuntimeConfig.DEBUG)
		{
			System.out.println(waitQueue.toString());
			System.out.println(readyQueue.toString());
		}
	}
	


	/**
	 * Prints the issued queue in a human readible format.
	 */
	public void printIssuedQueue(){
		for (Iterator<Path> iterator = issueQueue.iterator(); iterator.hasNext();) {
			System.out.println(iterator.next().getFullCommand());
			
		}
	}
	
	
	/**
	 * Computes the Manhattan distance between two points.
	 *
	 * @param a first given point
	 * @param b second given point
	 * @return the computed distance
	 */
	public static int distance(Dimension a, Dimension b){
		return Math.abs(a.height-a.height)+Math.abs(a.width-b.width);
	}

	
	/**
	 * Find path.
	 *
	 * @param src the location of the src interaction well
	 * @param dst the location of the dst interaction well
	 * @return the list of channels which should be passed to reach from src to dst
	 */
	private ArrayList<Well> findPath(Dimension src, Dimension dst){
		ArrayList<Well> list=new ArrayList<Well>();
		ArrayList<ChannelEdge> temp;
		Well startJunction, endJunction;
		
		if (Math.abs(src.height-dst.height)==2 && Math.abs(src.width-dst.width)==2){
			//adding the start channel
			list.add(layout.getNearestChannel(src, dst));

			list.add(layout.getWell((src.height+dst.height)/2, (src.width+dst.width)/2));
			
			//adding the end channel
			list.add(layout.getNearestChannel(dst, src));
		}
		else if ((Math.abs(src.height-dst.height)==2 && src.width==dst.width) 
			||	(Math.abs(src.width-dst.width)==2 && src.height==dst.height)){
			list=new ArrayList<Well>();
			list.add(layout.getWell((src.height+dst.height)/2, (src.width+dst.width)/2));
		}else{
			//adding the start channel
			list.add(layout.getNearestChannel(src, dst));

			startJunction=layout.getNearestJunction(src, dst);
			endJunction = layout.getNearestJunction(dst, src);
			temp = (ArrayList<ChannelEdge>) DijkstraShortestPath.findPathBetween(layout.getGraph(), startJunction, endJunction);

			//adding the first junction
			list.add(startJunction);
			
			for (int i = 0; i < temp.size(); i++) {
				list.add(layout.getWell(temp.get(i).getLocation()));
				if (temp.get(i).getV1()==list.get(list.size()-2)){
					list.add(temp.get(i).getV2());
				}else{
					list.add(temp.get(i).getV1());
				}
			}

			//adding the end channel
			list.add(layout.getNearestChannel(dst, src));

		}

		//Adding the destination to the list
		list.add(layout.getWell(dst));
		
		return list;		
	}
	
	/**
	 * Give a the best route between the operands of instruction v.
	 *
	 * @param v the given instruction
	 * @param simTime the simulation time
	 * @return list of channelEdges participating in the computed path. Returns null if no route could be found. 
	 */
	private List<Path> router(Vertex v, long simTime){
		boolean newInteractionReserved=false;
		//stores paths for each qubit to their destinations
		LinkedList<Path> pathList=new LinkedList<Path>();		
		LinkedList<Dimension> src=new LinkedList<Dimension>();
		LinkedList<Dimension> dst=new LinkedList<Dimension>();
		Dimension temp1, temp2;
		//This value is set only if one qubit will be moved (both in 1 and 2-qubit instructions
		Qubit qubit = null;
		src.add(layout.getQubit(v.getOperand(0)).getPosition());
		
		
		if (v.getOperandsNumber()==2){
			dst.add(layout.getQubit(v.getOperand(1)).getPosition());
			//A candidate destination is located between the src and dst; This will not be allocated yet
			if (layout.getNearestFreeInteraction(new Dimension((src.getFirst().width+dst.getFirst().width)/2, (src.getFirst().height+dst.getFirst().height)/2), false)!=null)
				temp1=layout.getNearestFreeInteraction(new Dimension((src.getFirst().width+dst.getFirst().width)/2, (src.getFirst().height+dst.getFirst().height)/2), false).getPosition();
			else
				temp1=null;
			
			//In the first two cases, it is decided to only move one qubit since the distance would be shorter. qubit is set in this case.
			if (((layout.getWell(dst.getFirst())).getQubitsNo()<2 
					&& (temp1==null || distance(src.getFirst(), dst.getFirst())<distance(src.getFirst(), temp1)+distance(temp1, dst.getFirst())))){
				//first qubit will move to the location of the second qubit
				qubit=layout.getQubit(v.getOperand(0));
			}else if ((layout.getWell(src.getFirst())).getQubitsNo()<2 
					&& (temp1==null || distance(src.getFirst(), dst.getFirst())<distance(src.getFirst(), temp1)+distance(temp1, dst.getFirst()))){
				//second qubit will move to the location of the first qubit
				temp2=dst.remove();
				dst.add(src.remove());
				src.add(temp2);
				qubit=layout.getQubit(v.getOperand(1));
			}else{
				temp2=dst.remove();
				src.add(temp2);
				dst.add(temp1);
				dst.add(temp1);				
				qubit=null;
				newInteractionReserved=true;
				layout.assignLastInteraction(temp1);				
			}
		}else{
			qubit=layout.getQubit(v.getOperand(0));
			//if q is the only qubit presents in the well, perform the interaction in that well.
			if  (((layout.getWell(src.getFirst()))).getQubitsNo()==1){
				dst=src;
			//otherwise, find another free interaction well for it 
			}else{
				newInteractionReserved=true;
				if (layout.getNearestFreeInteraction(src.getFirst(), false)!=null){
					dst.add(layout.getNearestFreeInteraction(src.getFirst(), true).getPosition());
				}else{
					//No interaction well is ready. Some qubits have scheduled to leave their source location to some destination, 
					//it should wait till they leave their current location					
					return null;
				}
			}
		}
		ArrayList<Well> path;
		if (RuntimeConfig.DEBUG)
			System.out.println("\nRouting: "+ v +" to destination: ("+dst.get(0).height+","+dst.get(0).width+")");
		//there is one pair of (src,dst) per operands of an operation; dst is the same among them
		for (int i = 0; i < dst.size(); i++) {
			//((Interaction)layout.getWell(dst.get(i))).addTargetQubit(v.getOperand(i));

			//source and destinations are on the same channel
			if (src.get(i)==dst.get(i)){
				path=new ArrayList<Well>();
			//source and destinations are on different channels and need to be routed
			}else{
				//finds the best path btn src and dst
				path = findPath(src.get(i), dst.get(i));
				
				//return null if any channel on the path is "busy"
				//this path will be recalculated later
				for (int k = 0; k < path.size(); k++) {
					if (path.get(k).getExpectedQubits()==Integer.MAX_VALUE){
						for (int j = 0; j < pathList.size(); j++) {
							freeChannels(pathList.get(j).getPath());
						}
						if (newInteractionReserved)
							layout.free(dst.get(0), null);							
						return null;
					}
				}
				//TODO mark the fist and the last channels as busy as well
				//mark all the channels on the path as busy
				busyChannels(path);

			}
			if (qubit==null)
				pathList.add(new Path(path, dst.get(i), layout.getQubit(v.getOperand(i)), v, simTime, layout));
			else{
				pathList.add(new Path(path, dst.get(i), qubit, v, simTime, layout));

				if (RuntimeConfig.DEBUG)
				{
					System.out.println(pathList.getLast());
				}
			}
		}
		
		return pathList;
	}
	
	
	/**
	 * Free the channels only one degree. 
	 *
	 * @param list the list of channelEdges participating in the path
	 */
	private void freeChannels(List<Well> list){
		Well c;
		for (Iterator<Well> iterator = list.iterator(); iterator.hasNext();) {
			c=iterator.next();
			freeChannel(c);
		}
	}
	
	private void freeChannel(Well well){
		if (well==null || layout.isIntraction(well.getPosition()))
			return;
		well.decExp();
		ChannelEdge ce=null;
		Dimension pos = well.getPosition();
		if (pos.width==0 || pos.height==0 ||  ((pos.width %2) ==1 && (pos.height %2)==1))//there is no edge in the margins or junctions should be considered 
			return;
		else if (pos.height%2 ==0) //vertical channel
			ce = layout.getGraph().getEdge(layout.getWell(pos.height-1, pos.width), layout.getWell(pos.height+1, pos.width));
		else //horizontal channel
			ce = layout.getGraph().getEdge(layout.getWell(pos.height, pos.width-1), layout.getWell(pos.height, pos.width+1));
		
		layout.getGraph().setEdgeWeight(ce, layout.getGraph().getEdgeWeight(ce)-1);
	}

	/**
	 * Busy the channels only one degree. 
	 *
	 * @param  list the list of channelEdges participating in the path
	 */
	private void busyChannels(List<Well> list){
		Well c;
		for (Iterator<Well> iterator = list.iterator(); iterator.hasNext();) {
			c=iterator.next();
			busyChannel(c);
		}
	}

	private void busyChannel(Well well){
		if (well==null || layout.isIntraction(well.getPosition()))
			return;
		well.incExp();
		ChannelEdge ce=null;
		Dimension pos = well.getPosition();
		if (pos.width==0 || pos.height==0 || ((pos.width %2) ==1 && (pos.height %2)==1))//there is no edge in the margins or junctions should be considered 
			return;
		else if (pos.height%2 ==0) //vertical channel
			ce = layout.getGraph().getEdge(layout.getWell(pos.height-1, pos.width), layout.getWell(pos.height+1, pos.width));
		else //horizontal channel
			ce = layout.getGraph().getEdge(layout.getWell(pos.height, pos.width-1), layout.getWell(pos.height, pos.width+1));
		
		layout.getGraph().setEdgeWeight(ce, layout.getGraph().getEdgeWeight(ce)+1);
	}
	
	public long baseSimulate(){
		PriorityQueue<BasePath> issueQueue=new PriorityQueue<BasePath>();;
		Set<Vertex> initList=readyQueue.getNext();
		BasePath temp0;
		simTime=0;

		
		for (Iterator<Vertex> iterator = initList.iterator(); iterator.hasNext();) {
			Vertex v=iterator.next();
			issueQueue.add(new BasePath(v, simTime, layout));			
		}

		while (!readyQueue.isEmpty() || !issueQueue.isEmpty()){
			simTime=issueQueue.peek().getDelay();
			if(RuntimeConfig.TIME)
				System.out.print(System.getProperty("line.separator")+"SimTime:"+simTime+"\t");
			else if (RuntimeConfig.VERBOSE)
				System.out.print(System.getProperty("line.separator"));
			
			do{
				temp0=issueQueue.remove();
				if(RuntimeConfig.VERBOSE)
					System.out.print(temp0.getVertex()+"\t");
				LinkedList<Vertex> vTemp=readyQueue.getNext(temp0.getVertex());

				for (int i = 0; i < vTemp.size(); i++) {
					issueQueue.add(new BasePath(vTemp.get(i), simTime, layout));
				}
			}while(!issueQueue.isEmpty() && simTime==issueQueue.peek().getDelay());
		}
		return simTime;
	}

	
	/**
	 * Event driven simulator.
	 *
	 * @return the length of simulation in &microsec
	 */
	public long simluate(){
		Well freedChannel;
		Path temp0;
		simTime=0;
		List<Path> paths;
		
		PriorityQueue<Vertex> initList=new PriorityQueue<Vertex>(readyQueue.getNext());
		
		for (Iterator<Vertex> iterator = initList.iterator(); iterator.hasNext();) {
			Vertex v=iterator.next();
			paths=router(v, simTime);
			
			if (paths!=null)
			{
				v.issue(simTime);
				issueQueue.addAll(paths);
			}else{
				busyInsts.add(v);
				v.addToQueue(simTime);
			}
		}
		
		
		while (!readyQueue.isEmpty() || !issueQueue.isEmpty()){
			//For debugging
			if (issueQueue.isEmpty() ){
				System.out.println(System.getProperty("line.separator")+"Fatal Error: No more instruction to issue!");
//				System.out.println(readyQueue);
//				System.out.println(waitQueue);
				System.exit(-1);
			}
			
			simTime=issueQueue.peek().getDelay();
			if(RuntimeConfig.TIME)
				System.out.print(System.getProperty("line.separator")+"SimTime:"+simTime+"\t");
			else if (RuntimeConfig.VERBOSE)
				System.out.print(System.getProperty("line.separator"));

			do{
				temp0=issueQueue.remove();
				freedChannel= temp0.nextMove();
				freeChannel(freedChannel);
				if (freedChannel!=null && !busyInsts.isEmpty()){
					for (int i = 0; i < busyInsts.size(); i++) {
						Vertex bi=busyInsts.get(i);
						paths=router(bi, simTime);
						if (paths!=null){
							busyInsts.remove(i);
							bi.removeFromQueue(simTime);
							bi.issue(simTime);
							issueQueue.addAll(paths);
							i--;
						}
					}
				}
				if (!temp0.isFinished()){
					issueQueue.add(temp0);
				}else if (temp0.isExecutionFinished()){
					temp0.getVertex().finish(simTime);
					LinkedList<Vertex> vTemp=readyQueue.getNext(temp0.getVertex());

					for (int i = 0; i < vTemp.size(); i++) {
						paths=router(vTemp.get(i), simTime);
						if (paths!=null)
						{
							vTemp.get(i).issue(simTime);
							issueQueue.addAll(paths);
						}else{
							busyInsts.add(vTemp.get(i));
							vTemp.get(i).addToQueue(simTime);
//								System.out.println(":((---");
//								System.out.println(Thread.currentThread().getStackTrace());
//								System.exit(-1);
						}
					}
				}
			}while(!issueQueue.isEmpty() && simTime==issueQueue.peek().getDelay());
		}
		
		if (RuntimeConfig.DEBUG){
			//retrieving statistics
			System.out.println("---------------STATS-------------");
			HashMap<String, Long> commandsDelay=new HashMap<String, Long>();
			HashMap<String, Integer> commandsCounts=new HashMap<String, Integer>();
			
			long maxCNOT=0;
			for (Iterator<Vertex> iterator = commands.iterator(); iterator.hasNext();) {
				Vertex v=iterator.next();
				if (v.isSentinel())
					continue;
				String command=v.getCommand();
				if (commandsDelay.containsKey(command)){
					commandsCounts.put(command, commandsCounts.get(command).intValue()+1);
					commandsDelay.put(command, commandsDelay.get(command).intValue()+v.getTotalTime());
				}else{
					commandsCounts.put(command, 1);
					commandsDelay.put(command, v.getTotalTime());
				}
				if (command.compareToIgnoreCase("cnot")==0 && v.getTotalTime()>maxCNOT)
					maxCNOT=v.getTotalTime();
	//			if (command.compareToIgnoreCase("cnot")==0)
	//				System.out.println(v.getTotalTime());
			}
			
			for (Iterator<Entry<String, Long>> iterator = commandsDelay.entrySet().iterator(); iterator.hasNext();) {
				Entry<String, Long> entry=iterator.next();
				System.out.println("Delay "+entry.getKey()+": "+ (((double)commandsDelay.get(entry.getKey()))/commandsCounts.get(entry.getKey()) - layout.getOpDelay(entry.getKey())));
			}
			System.out.println("Max CNOT: "+maxCNOT);
		}
		if (RuntimeConfig.VERBOSE){//Adding an empty line at the end of output trace
			System.out.println();
		}
		return simTime;
	}



	

}
