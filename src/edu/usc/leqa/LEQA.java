/*
 * 
 * Copyright (C) 2014 Mohammad Javad Dousti and Massoud Pedram, SPORT lab,
 * University of Southern California. All rights reserved.
 * 
 * Please refer to the LICENSE file for terms of use.
 * 
*/
package edu.usc.leqa;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.jgrapht.alg.BellmanFordShortestPath;
import org.jgrapht.alg.NeighborIndex;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.Multigraph;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;
import org.jgrapht.traverse.TopologicalOrderIterator;

import edu.usc.qspr.layout.Layout;
import edu.usc.qspr.qasm.QASM;
import edu.usc.qspr.qasm.Vertex;

public class LEQA {
	public enum Scheduling{
		ASAP, ALAP
	}

	HashMap<String, Integer> Ncritical=new HashMap<String, Integer>();

	public static long leqa(QASM qasm, Layout layout, double speed){

		//layout area
		int A=layout.getLayoutSize().height/2*layout.getLayoutSize().width/2;
		if (RuntimeConfig.VERBOSE){
			System.out.println("Layout area: "+A);
		}

		String[] qubitList=qasm.getQubitList();
		//number of qubits
		int Q=qubitList.length;
		if (RuntimeConfig.VERBOSE){
			System.out.println("# of Qubits: "+Q);
			System.out.println("# of gates: "+(qasm.getCommandsList().size()-2));
		}
		if (Q>A){
			System.err.println("Layout is not big enough. Try to enlarge it to cover "+Q+" qubits.");
			System.exit(-1);
		}

		//building the interaction graph (IG)
		Multigraph<String, DefaultEdge> IG=buildIG(qasm, Q);

		//Calculating M
		int[] M = calcM1(Q, IG, qubitList);

		//Calculating the area of a tile
		double B = calcTileArea(Q, qubitList, M, IG);
		int a= (int) Math.ceil(Math.sqrt(A));
		int b= (int) Math.ceil(Math.sqrt(B));

		if (b>=a){
			System.err.println("b>=a");
			System.exit(-1);
		}


		//Estimate the expected average time to 
		double d1_avg=calc_d1_avg(qubitList, IG, M, speed);
		
		//calculating delay array
		double d[] =calcD(Q, d1_avg);

		double P[][]=calcP(a, b);

		double[] E_i = calcE_i(a, Q, P);

		double L_avg_CNOT = calcL_avg_CNOT(Q, d, E_i, A);


		qasm.adjustWeights(2*layout.getOpDelay("move"),  L_avg_CNOT);

		//Finding the critical path and calculating Ncritical
		HashMap<String, Integer> Ncritical=LEQA.findLongestPathFast(qasm);

		long overAllDelay=calcOverallDelay(L_avg_CNOT, layout, Ncritical);

		return overAllDelay;
	}

	private static long calcOverallDelay(double L_avg_CNOT, Layout layout, HashMap<String, Integer> Ncritical) {
		double delay=0;
		double L_avg_others=2*layout.getOpDelay("move");
		Entry<String, Integer> temp;

		for (Iterator<Entry<String, Integer>> iterator= Ncritical.entrySet().iterator(); iterator.hasNext();) {
			temp=iterator.next();
			if (temp.getKey().compareToIgnoreCase("CNOT")==0){
				delay+=temp.getValue()*(layout.getOpDelay(temp.getKey())+L_avg_CNOT);
			}else{
				delay+=temp.getValue()*(layout.getOpDelay(temp.getKey())+L_avg_others);
			}

		}

		return (long)Math.ceil(delay);
	}

	private static double calcL_avg_CNOT(int Q, double[] d, double []E_i, int A) {
		double value=0;
		//double totalE_i=0;
		for (int i = 1; i <= Q; i++) {
			value+=(d[i]*E_i[i]);
//			totalE_i+=E_i[i];
		}
		value/=E_i[Q+1];

		if (RuntimeConfig.DEBUG){
			System.out.println("L_avg_CNOT= "+value);
		}

		return value;
	}

	private static double[] calcD(int Q, double d1_avg) {
		double []d=new double[Q+1];
		for (int i = 0; i <= Q; i++) {
			if (i<=RuntimeConfig.CHANNEL_CAP){
				d[i]=d1_avg;
			}else{
				d[i]=(1+i)*d1_avg/RuntimeConfig.CHANNEL_CAP;
			}
//			System.out.println("d["+i+"]= "+d[i]);
		}

		return d;
	}

	private static double[] calcE_i(int a, int Q, double P[][]) {
		double []E_i=new double [Q+2];
                //storing the sum E_i[1]..E_i[Q]
                E_i[Q+1]=0;
		double m, n;
		long []QchooseI=new long[Q+1];
		QchooseI[0]=1;
		for (int i = 0; i <= Q; i++) {
			E_i[i]=0;
		}
		for (int i = 0; i <= Q; i++) {
			for (int x = 0; x < a; x++) {
				for (int y = 0; y < a; y++) {
					m=Math.pow(P[x][y], i);
					n=Math.pow((1-P[x][y]), Q-i);
					//This condition is added to avoid getting NaN from binomial function.
					if (m>0 && n>0){
						E_i[i]+=Math.pow(P[x][y], i) * Math.pow((1-P[x][y]), Q-i);
					}
				}
			}
			E_i[i]*=QchooseI[i];
                        E_i[Q+1]+=E_i[i];
			if (i>Q/2 && i<Q){
				QchooseI[i+1]=QchooseI[Q-i-1];
                        }else if (i<Q){
				QchooseI[i+1] =  (QchooseI[i] * Q-i) / (i+1);
				if (QchooseI[i+1]<QchooseI[i] || QchooseI[i+1]<0){ //an overflow happened	
					i=Q-i;
					QchooseI[Q-i+1]=-1;
					continue;
				}
			}
			//System.out.println("E_i["+i+"]: "+E_i[i]);			
		}
                E_i[Q+1]-=E_i[0];
		return E_i;
	}

	private static double[][] calcP(int a, int b) {
		double[][]P=new double[a][a];
		int fixMin=Math.min(a-b+1, b);
		int xMin;
		int denominator=((a-b+1) * (a-b+1));

		for (int x = 1; x <= a; x++) {
			xMin=Math.min(Math.min(x, a-x+1),fixMin);
			for (int y = 1; y <=a; y++) {
				P[x-1][y-1]=(xMin * Math.min(Math.min(y, a-y+1),fixMin));
				P[x-1][y-1]/= denominator;
				//System.out.println("P["+x+"]["+y+"]:\t"+P[x-1][y-1]);
			}
		}

		return P;
	}

	public static double f(){
		return 1;
	}
	public static double g(){
		return 1;
	}

	public static double calc_d1_avg(String[] qublitList, Multigraph<String, DefaultEdge> IG, int[] M, double v){
		double[] d1=new double [qublitList.length];
		double d1_avg=0;
		double totalD=0;

		double p, k, E_H;

		for (int i = 0; i < qublitList.length; i++) {
			//			p=Math.sqrt(IG.degreeOf(qublitList[i]));
			p=Math.sqrt(M[i]+1);
			k=M[i]+1;
			//Shor Method
			//E_H = Math.sqrt(2*p*(p+1)*k)*f() + 4*p *g();

			//Wikipedia Method (lower-bound)
			//E_H = p*(0.7080*Math.sqrt(k)+0.522);

			//Wikipedia Method (mean)
//			E_H = p*(0.713*Math.sqrt(k)+1.143);
			E_H = p*(0.713*Math.sqrt(k)+0.641)*(((double)M[i]-1)/(M[i]));
			//E_H = p*(p+1);

			d1[i]= E_H / (v*M[i]);

			d1_avg+=d1[i]*IG.degreeOf(qublitList[i]);
			totalD+=IG.degreeOf(qublitList[i]);
		}
		d1_avg/= totalD;
		if (RuntimeConfig.VERBOSE){
			System.out.println("d1_avg="+d1_avg);
		}
		return d1_avg;
	}
	public static HashMap<String, Integer> preOrderDFS(QASM qasm){
		return preOrderDFS(qasm.getDFG(), qasm.getStartVertex());
	}
	public static HashMap<String, Integer> preOrderDFS(SimpleDirectedWeightedGraph<Vertex, DefaultWeightedEdge> DFG, Vertex node){
		HashMap<String, Integer> Ntemp1=new HashMap<String, Integer>();
		HashMap<String, Integer> Ntemp2=new HashMap<String, Integer>();

		if (node.getName().compareToIgnoreCase("end")==0){
			//Can be added for debugging; Note that it will curropt the N_g^critical value
			//Ntemp1.put("critical", new Integer(1));
			return Ntemp1;
		}
		for (Iterator<DefaultWeightedEdge> iterator = DFG.outgoingEdgesOf(node).iterator(); iterator.hasNext();) {
			Ntemp1=preOrderDFS(DFG, DFG.getEdgeTarget(iterator.next()));
			Iterator<Entry<String, Integer>> it = Ntemp1.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<String, Integer> pairs = (Map.Entry<String, Integer>)it.next();
				if(!Ntemp2.containsKey(pairs.getKey()) || Ntemp2.get(pairs.getKey())<pairs.getValue()){
					Ntemp2.put(pairs.getKey(), pairs.getValue());
				}
				//System.out.println(pairs.getKey() + " = " + pairs.getValue());
				it.remove(); // avoids a ConcurrentModificationException
			}
		}
		if (!node.isSentinel()){
			if (Ntemp2.containsKey(node.getCommand()))
				Ntemp2.put(node.getCommand(), Ntemp2.get(node.getCommand())+1);	
			else
				Ntemp2.put(node.getCommand(), 1);
		}

		//Ntemp2.put("critical", Ntemp2.get("critical")+1);

		return Ntemp2;
	}

	public static HashMap<String, Integer> findLongestPathFast(QASM qasm){
		Vertex start=qasm.getStartVertex();
		Vertex end=qasm.getEndVertex();
		SimpleDirectedWeightedGraph<Vertex, DefaultWeightedEdge> DFG=qasm.getDFG();				

		TopologicalOrderIterator<Vertex, DefaultWeightedEdge> toi = new TopologicalOrderIterator<Vertex, DefaultWeightedEdge>(DFG);

		//initialize single-source
		Vertex u,v;
		DefaultWeightedEdge edge;

//		for (Iterator<Vertex> iterator = DFG.vertexSet().iterator(); iterator.hasNext();) {
//			u=iterator.next();
//			u.d=Integer.MIN_VALUE;
//			u.pi=null;
//		}
		start.d=0;
                int newDistance;
		for (; toi.hasNext();) {
			u=toi.next();

			for (Iterator<DefaultWeightedEdge> iterator = DFG.outgoingEdgesOf(u).iterator(); iterator.hasNext();) {
				edge=iterator.next();
				v=DFG.getEdgeTarget(edge);
				//RELAXing
                                newDistance=(int)(u.d + DFG.getEdgeWeight(edge));
				if (v.d < newDistance){
					v.d=newDistance;
					v.pi=u;
				}
			}
		}
		if (RuntimeConfig.VERBOSE){
			System.out.println("Total critical path delay: "+(end.d-1));
		}

		HashMap<String, Integer> Ncritical=new HashMap<String, Integer>();
		u=end.pi;
                Integer temp;
		while (u!=start){
			//			System.out.println(u);
                        temp=Ncritical.get(u.getCommand());
			if (temp!=null){
				Ncritical.put(u.getCommand(), temp+1);
                        }else{
				Ncritical.put(u.getCommand(), +1);
                        }
//			if (u.pi==null){
//				System.out.println(u);
//			}
			u=u.pi;
		}
//		if (RuntimeConfig.DEBUG){
			for (Iterator<Entry<String,Integer>> iterator = Ncritical.entrySet().iterator(); iterator.hasNext();) {
				Entry<String,Integer> entry=iterator.next();
//				System.out.println("Ncritical_"+entry.getKey()+": "+entry.getValue());			
			}
//		}

		return Ncritical;
	}
	public static HashMap<String, Integer> findLongestPath(QASM qasm){
		Vertex start=qasm.getStartVertex();
		Vertex end=qasm.getEndVertex();
		SimpleDirectedWeightedGraph<Vertex, DefaultWeightedEdge> DFG=qasm.getDFG();				
		List<DefaultWeightedEdge>longestPath= BellmanFordShortestPath.findPathBetween(DFG, start, end);
		double totalWeight=0;

		Vertex temp=start;
		HashMap<String, Integer> Ncritical=new HashMap<String, Integer>();

		for (Iterator<DefaultWeightedEdge> iterator = longestPath.iterator(); iterator.hasNext();) {
			DefaultWeightedEdge dwe = iterator.next();
			totalWeight+=DFG.getEdgeWeight(dwe);
			if (DFG.getEdgeTarget(dwe)==temp)
				temp=DFG.getEdgeSource(dwe);
			else
				temp=DFG.getEdgeTarget(dwe);

			if (temp.getCommand().equalsIgnoreCase("end"))
				break;

			if (Ncritical.containsKey(temp.getCommand()))
				Ncritical.put(temp.getCommand(), Ncritical.get(temp.getCommand())+1);
			else
				Ncritical.put(temp.getCommand(), +1);
		}
		System.out.println("Total delay: "+totalWeight);

		//		if (RuntimeConfig.DEBUG){
		for (Iterator<Entry<String,Integer>> iterator = Ncritical.entrySet().iterator(); iterator.hasNext();) {
			Entry<String,Integer> entry=iterator.next();
			System.out.println("Ncritical_"+entry.getKey()+": "+entry.getValue());			
		}
		//		}
		return Ncritical;
	}

	private static double calcTileArea(int Q, String[] qublitList, int[] M, Multigraph<String, DefaultEdge> IG){
		double B=0;
		int totalDegree=0;
		for (int i=0;i <Q; i++) {
			B+=IG.degreeOf(qublitList[i])*(M[i]+1);
			totalDegree+=IG.degreeOf(qublitList[i]);
		}
		B/=totalDegree;
		if (RuntimeConfig.VERBOSE){
			System.out.println("B= "+B);
		}
		return B;		
	}
	private static int[]  calcM1(int Q, Multigraph<String, DefaultEdge> IG, String[] qublitList){
		int[]  M=new int[Q];
		Set<String> temp0=new HashSet<String>();
		ArrayList<String> temp1=new ArrayList<String>();
		
		NeighborIndex<String, DefaultEdge> ni= new NeighborIndex<String, DefaultEdge>(IG);
		
		for (int i=0; i<Q; i++) {
			//Calculating M1
//			for (Iterator<DefaultEdge> it = IG.edgesOf(qublitList[i]).iterator();it.hasNext();){
//				DefaultEdge edge=it.next();
//				if (IG.getEdgeSource(edge).compareTo(qublitList[i])==0){
//					temp1.add(IG.getEdgeTarget(edge));
//				}else{
//					temp1.add(IG.getEdgeSource(edge));
//				}
//			}
//			temp0.addAll(temp1);
//			temp1.clear();
//			M[i]=temp0.size();
//			temp0.clear();
//			temp1.clear();
			M[i]=ni.neighborsOf(qublitList[i]).size();
		}
		return M;
	}
	private static Multigraph<String, DefaultEdge> buildIG(QASM qasm, int Q) {
		Multigraph<String, DefaultEdge> IG=new Multigraph<String, DefaultEdge>(DefaultEdge.class);
		//adding qubits to the IG
		for (int i = 0; i < Q; i++) {
			IG.addVertex(qasm.getQubitList()[i]);			
		}
		//adding edges to the IG
		for (Iterator<Vertex> iterator = qasm.getCommandsList().iterator(); iterator.hasNext();) {
			Vertex v=iterator.next();
			if (v.getOperandsNumber()==2){
				IG.addEdge(v.getOperand(0), v.getOperand(1));
			}
		}
		return IG;
	}
	
	
	private static Multigraph<String, DefaultEdge> buildIG1(QASM qasm, int Q) {
		Multigraph<String, DefaultEdge> IG=new Multigraph<String, DefaultEdge>(DefaultEdge.class);
		int [] degQ=new int[Q];
		//adding qubits to the IG
		for (int i = 0; i < Q; i++) {
			degQ[Q]=0;			
		}
		
		//adding edges to the IG
		for (Iterator<Vertex> iterator = qasm.getCommandsList().iterator(); iterator.hasNext();) {
			Vertex v=iterator.next();
			if (v.getOperandsNumber()==2){
				IG.addEdge(v.getOperand(0), v.getOperand(1));
			}
		}
		return IG;
	}
}
