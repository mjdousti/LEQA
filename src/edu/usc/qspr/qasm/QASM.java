/*
 * 
 * Copyright (C) 2014 Mohammad Javad Dousti and Massoud Pedram, SPORT lab,
 * University of Southern California. All rights reserved.
 * 
 * Please refer to the LICENSE file for terms of use.
 * 
*/
package edu.usc.qspr.qasm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import edu.usc.qspr.layout.Layout;

public class QASM {
	private Map<String, Integer> dependencyList=new Hashtable<String, Integer>();
	private int commandNo=0;
	List<Vertex> commands=new ArrayList<Vertex>();
	private SimpleDirectedWeightedGraph<Vertex, DefaultWeightedEdge> QODG;
	private Vertex startVertex, endVertex;
	private Layout layout;
	long totalWeight=0;

	public QASM(Layout l){
		layout=l;
		QODG = new SimpleDirectedWeightedGraph<Vertex, DefaultWeightedEdge>(DefaultWeightedEdge.class);
	}
	
	public void addSentinels(){
		//adding "start" node in graph
		commands.add(0, new Vertex("start", 0, (String) null));
		QODG.addVertex(commands.get(0));
		startVertex=commands.get(0);

		//adding "end" node in graph
		commands.add(new Vertex("end", commandNo+1, (String) null));
		QODG.addVertex(commands.get(commandNo+1));
		endVertex=commands.get(commandNo+1);

		Vertex tempVertex;
		DefaultWeightedEdge tempEdge;
		
		for (Iterator<Vertex> iterator = QODG.vertexSet().iterator(); iterator.hasNext();) {
			tempVertex=iterator.next();
			if (tempVertex==startVertex || tempVertex==endVertex)
				continue;
			else if (QODG.inDegreeOf(tempVertex)==0 || 
					(tempVertex.getOperandsNumber()==2 && QODG.inDegreeOf(tempVertex)==1 && tempVertex.getOpNoInCommon(getFather(tempVertex))==1) ){	//the node has no parents and hence should be connected to Start
				tempEdge=QODG.addEdge(startVertex, tempVertex);
				QODG.setEdgeWeight(tempEdge, layout.getOpDelay(tempVertex.getCommand()));
			}else if (QODG.outDegreeOf(tempVertex)==0 || 
					(tempVertex.getOperandsNumber()==2 && QODG.outDegreeOf(tempVertex)==1 && tempVertex.getOpNoInCommon(getChild(tempVertex))==1)){	//the node has no children and hence should be connected to End
				tempEdge=QODG.addEdge(tempVertex, endVertex);
				QODG.setEdgeWeight(tempEdge, 1);
			}
			tempVertex.setCommandNo(tempVertex.getCommandNo()+1);
				
		}
	}
	
	private Vertex getFather(Vertex v){
		DefaultWeightedEdge dwe=QODG.incomingEdgesOf(v).iterator().next();
		return QODG.getEdgeSource(dwe);
	}
	private Vertex getChild(Vertex v){
		DefaultWeightedEdge dwe=QODG.outgoingEdgesOf(v).iterator().next();
		return QODG.getEdgeTarget(dwe);
	}
	public void adjustWeights(double LG, double LCNOT){
		DefaultWeightedEdge dwe;
                int cnotDelay=layout.getOpDelay("cnot")+(int)LCNOT;
		for (Iterator<DefaultWeightedEdge> iterator = QODG.edgeSet().iterator(); iterator.hasNext();) {
			dwe=iterator.next();
			if (QODG.getEdgeTarget(dwe).getOperandsNumber()==2)
				QODG.setEdgeWeight(dwe, cnotDelay);
			else
				QODG.setEdgeWeight(dwe, QODG.getEdgeWeight(dwe)+LG);
		}
	}
	
	public Vertex getStartVertex(){
		return startVertex;
	}
	
	public Vertex getEndVertex(){
		return endVertex;
	}
	
	
	public void reverseCommandsOrder(){
		Collections.reverse(commands);
		for (int i = 0; i < commands.size(); i++) {
			commands.get(i).setCommandNo(i);
		}
	}
	
	public SimpleDirectedWeightedGraph<Vertex, DefaultWeightedEdge> getDFG(){
		return QODG;
	}
	
	public List<Vertex> getCommandsList(){
		return commands;		
	}
	
	public String[] getQubitList(){
		String[] qubits=new String[dependencyList.size()];
		Iterator<Entry<String, Integer>> it=dependencyList.entrySet().iterator();
		int i=0;
		while (it.hasNext()){
			qubits[i]=it.next().getKey();
			i++;
		}
		return qubits;
	}
	
	public void printDFG(){
		for (DefaultWeightedEdge e : QODG.edgeSet()) {
			System.out.println(e.toString()+" weight: " +QODG.getEdgeWeight(e));                    
		}
	}
	
	public void printCommands(){
		for (int i = 0; i < commands.size(); i++) {
			System.out.println(commands.get(i));
		}
	}
	
	public void printDependancyList(){
		for (Map.Entry<String, Integer> entry : dependencyList.entrySet())
		{
			System.out.println(entry.getKey());
		}
	}

	/****************************************************************************/
	// For helping parser
	@SafeVarargs
	public static <T> ArrayList<T> createArrayList(T ... elements) { 
		ArrayList<T> list = new ArrayList<T>();  
		for (T element : elements) { 
			list.add(element); 
		} 
		return list; 
	} 

	private void parseError(String token){
		System.err.println("Qubit `"+token+"` is not defined.");
		//TODO: convert to an exception with correct message
		System.exit(-1);
	}

	public void incCommandNo(){
		commandNo++;
	}

	public void defineQubit(String q){
		//		System.out.println(q);
		//Qubit definition
		//Adding the qubits in dependencyList
		if (dependencyList.containsKey(q)==true){
			System.err.println("Qubit "+q+" is already defined.");
			System.exit(-1);
		}
		dependencyList.put(q, new Integer (0));
		//To access the value of qubit
		//if (qubitValue!=null)
		//	System.out.println(qubitValue.image);
	}

	public void addOneOpInst(String cmd, String op){
		DefaultWeightedEdge dwe;
		//Reports error if the used qubit is not defined before
		if (dependencyList.containsKey(op)==false){
			parseError(op);
		}
		String[] temp={op};
		//Vertex(String c, int no, int p, Qubit ...ops)
		commands.add(new Vertex (cmd, commandNo, temp));
		//Adding new command in the graph
		QODG.addVertex(commands.get(commandNo));

		//Adding an edge to the node which depends on 
		if (dependencyList.get(op).intValue()>0){
			dwe=QODG.addEdge(commands.get(dependencyList.get(op).intValue()), commands.get(commandNo));
			QODG.setEdgeWeight(dwe, layout.getOpDelay(commands.get(commandNo).getName()));
		}
		//Changing the dependency of its operand to point to itself
		dependencyList.put(op, new Integer (commandNo));	
	}

	public void addTwoOpInst(String cmd, String op0, String op1){
		DefaultWeightedEdge dwe;
		if (op0.equals(op1)){
			System.err.println("Error: operands of a 2-qubit operator, i.e. `"+op0+"`, cannot be the same");
			//TODO: convert to an exception with correct message
			System.exit(-1);
		}
		//Reports error if the used qubits are not defined before
		if (dependencyList.containsKey(op0)==false){
			parseError(op0);
		} else if (dependencyList.containsKey(op1)==false){
			parseError(op1);
		}

		//Adding the command in commands list
		String[] temp={op0, op1};
		commands.add(new Vertex (cmd,commandNo, temp));

		//Adding new command in the graph
		QODG.addVertex(commands.get(commandNo));

		//Adding an edge to the node which depends on 
		if (dependencyList.get(op0).intValue()>0){
			dwe=QODG.addEdge(commands.get(dependencyList.get(op0).intValue()), commands.get(commandNo));
			QODG.setEdgeWeight(dwe, layout.getOpDelay(commands.get(commandNo).getName()));
		}
		//It is assumed that the target qubit does not make any dependency on another target (wrong assumption?!)
		//Also, plz note that the schedular may encounter problems if this section is uncommented
		//Because it is assuming that any node has only one parent 
		if (dependencyList.get(op1).intValue()>0){
			//adds an edge if it is not already added by the op0
			if (!QODG.containsEdge(commands.get(dependencyList.get(op1).intValue()), commands.get(commandNo))){
				dwe=QODG.addEdge(commands.get(dependencyList.get(op1).intValue()), commands.get(commandNo));
				QODG.setEdgeWeight(dwe, layout.getOpDelay(commands.get(commandNo).getName()));
			}
		}

		//Changing the dependency of its operand to point to itself
		dependencyList.put(op1, new Integer (commandNo));
		
		//NEW
		dependencyList.put(op0, new Integer (commandNo));
	}	

}
