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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import edu.usc.qspr.layout.Interaction;
import edu.usc.qspr.layout.Layout;
import edu.usc.qspr.layout.Well;
import edu.usc.qspr.qasm.QASM;
import edu.usc.qspr.qasm.Vertex;


public class InitialPlacer {
	private static QASM qasm;
	private static Layout layout;
	private static Random randomGenerator;
	
	
	public static enum Heuristic{
		Center, Random, ShuffledCenter
	}
	
	public static void place(QASM q, Layout l, Heuristic heuristic) {
		randomGenerator=new Random();
		layout=l;
		qasm=q;
		if (layout.getInteractionEmptyList().size()<qasm.getQubitList().length){
			System.err.println("Layout is not big enought to handle this QSPR. Try to enlarge it to cover "+qasm.getQubitList().length+" qubits.");
			System.exit(-1);
		}
		assignQubits(heuristic);
		if (RuntimeConfig.PRINT_QUBIT){
			System.out.println("Initial Placement:");
			for (int i = 0; i < q.getQubitList().length; i++) {
				System.out.println(q.getQubitList()[i]+" @ "+layout.getQubit(q.getQubitList()[i]).getPosition().height+","+layout.getQubit(q.getQubitList()[i]).getPosition().width);
			}
		}
		if (RuntimeConfig.VERBOSE)
			System.out.println("Qubit placement completed successfully!");
	}
	
	private static void assignQubits (Heuristic heuristic){
		Dimension place=new Dimension();
		Dimension size=layout.getLayoutSize();
		
		String[] qubits=qasm.getQubitList();

		switch (heuristic){
		case Center:
			place.height = size.height/2;
			place.width  = size.width/2;
			layout.sortInteractionWells(place, false, qasm.getQubitList().length);
			if (RuntimeConfig.PLACEMENT)
				System.out.println("Qubit count: "+qubits.length);
			for (int i = 0; i < qubits.length; i++) {
				layout.assignNewQubit(qubits[i], layout.getNearestFreeInteraction(true).getPosition());
			}		
			break;
		case ShuffledCenter:
			place.height = size.height/2;
			place.width  = size.width/2;
			layout.sortInteractionWells(place, true, qasm.getQubitList().length);
			for (int i = 0; i < qubits.length; i++) {
				layout.assignNewQubit(qubits[i], layout.getNearestFreeInteraction(true).getPosition());
			}		
			break;
		case Random:
			for (int i = 0; i < qubits.length; i++) {
				place.width=randomGenerator.nextInt(size.width);
				place.height=randomGenerator.nextInt(size.height);
				layout.sortInteractionWells(place, false,  qasm.getQubitList().length);
				layout.assignNewQubit(qubits[i], layout.getNearestFreeInteraction(true).getPosition());
			}		
			break;
		}
	}
	
}
