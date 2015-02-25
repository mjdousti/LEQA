/*
 * 
 * Copyright (C) 2014 Mohammad Javad Dousti and Massoud Pedram, SPORT lab,
 * University of Southern California. All rights reserved.
 * 
 * Please refer to the LICENSE file for terms of use.
 * 
*/
package edu.usc.qspr.router;


import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import edu.usc.qspr.layout.ChannelEdge;
import edu.usc.qspr.layout.Junction;
import edu.usc.qspr.layout.Layout;
import edu.usc.qspr.layout.Well;
import edu.usc.qspr.qasm.Vertex;

public class PathFinder {
//	Map<String, LinkedList<Vertex>> waitList=new HashMap<String, LinkedList<Vertex>>();

	
	public PathFinder(Layout layout, DirectedGraph<Vertex, DefaultEdge> dfg) {
		SimpleWeightedGraph<Well, ChannelEdge> graph=layout.getGraph();
//		for (Command cmd : readyQueue) {
//			//TODO: Generalize for more than 2-operand commands
//			if (cmd.getOperandsNumber()==2){
//				Square 
//				int x0=cmd.getOperand(0).getPosition().height;
//				int y0=cmd.getOperand(0).getPosition().width;
//				int x1=cmd.getOperand(1).getPosition().height;
//				int y1=cmd.getOperand(1).getPosition().width;
//				DijkstraShortestPath.findPathBetween(graph, layout.getNearestJunction(x0, y0), layout.getNearestJunction(x1, y1));
//			}
//				
//		}
	
	}
}
