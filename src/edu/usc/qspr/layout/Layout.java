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
import java.util.*;
import java.util.Map.Entry;
import org.jgrapht.graph.*;


public class Layout {	
	private Dimension dim=new Dimension();
	private Well [][]fabric;
	private Map<String, Operation>supportedOps=new HashMap<String, Operation>();

	private Map<String, Vector<Operation>> wellInsts = new HashMap<String, Vector<Operation>>();


	//May it's better to use a MiniMap
	private Map<String, Qubit> qubits=new HashMap<String, Qubit>();
	private ArrayList<Well> interactionWellEmptyList=new ArrayList<Well>();

	private SimpleWeightedGraph<Well, ChannelEdge> layoutGraph;

	public enum Types{
		Basic, Interaction, Junction
	}


	public void addInstToWell(String wellName, String inst){
		wellInsts.get(wellName.toLowerCase()).add(supportedOps.get(inst.toLowerCase()));
	}
	public void addNewOperation(Operation op){
		supportedOps.put(op.getName().toLowerCase(), op);
	}

	public void free(Dimension x, Qubit qubit) {
		getWell(x).removeQubit(qubit);
		if (getWellType(x)==Types.Interaction && getWell(x).getQubitsNo()==0){
			interactionWellEmptyList.add(getWell(x));
			if (RuntimeConfig.DEBUG){
				System.out.println("\n"+qubit.getName()+" freed the interaction well ("+x.height+","+x.width+")!");
			}
		}
	}
	//
	//
	public void occupy(Dimension x, Qubit q){
		getWell(x).addQubits(q);
	}


	public void sortInteractionWells( Dimension a, boolean shuffle, int qubitCount){
		Collections.sort(interactionWellEmptyList, new NearestWell(a));
		if (shuffle){
			ArrayList<Well> shuffleList=new ArrayList<Well>();
			for (int i=0;i<qubitCount;i++){
				if (RuntimeConfig.DEBUG)
					System.out.println("Interaction "+interactionWellEmptyList.get(0).getPosition().height+","+interactionWellEmptyList.get(0).getPosition().width+" is reserved.");
				shuffleList.add(interactionWellEmptyList.remove(0));
			}
			//do{
			Collections.shuffle(shuffleList, new Random(System.nanoTime()));
			//}while(repeated(shuffleList));
			//explored.add(shuffleList);
			for (int i=0;i<qubitCount;i++){
				interactionWellEmptyList.add(i, shuffleList.get(i));
			}
		}
	}


	public boolean assignNewQubit(String name, Dimension initPos){
		if (RuntimeConfig.PLACEMENT)
			System.out.println("Qubit "+name+" is placed @ "+initPos.height+"x"+initPos.width+".");

		Qubit temp=new Qubit(name, initPos, this);
		qubits.put(name, temp);

		//Just to be extra cautious
		if (isIntraction(initPos)){
			occupy(initPos, temp);
		}

		return true;
	}
	public boolean assignLastInteractionWell(Dimension x) {
		boolean temp;
		if (RuntimeConfig.DEBUG)
			System.out.println("Interaction ("+x.height+","+x.width+") is reserved.");
		temp=interactionWellEmptyList.remove(getWell(x));
		if (temp){
			if (RuntimeConfig.DEBUG)
				System.out.println("Interaction ("+x.height+","+x.width+") is reserved.");
			return true;
		}else
			return false;

	}
	//retains all the traps to the empty trap list
	public void clean(){
		//Clearing the usage statistics for channels
		ChannelEdge c;
		for (Iterator<ChannelEdge> iterator = layoutGraph.edgeSet().iterator(); iterator.hasNext();) {
			c=iterator.next();
			c.setUsed(false);
		}

		//Clearing the usage statistics for junctions and traps
		for (int i = 0; i < dim.height; i++) {
			for (int j = 0; j < dim.width; j++) {
				switch(fabric[i][j].getType()){
				case Interaction:
					fabric[i][j].setUsed(false);
					//((Interaction)fabric[i][j]).clearProgressList();
					if (!interactionWellEmptyList.contains(fabric[i][j]))
						interactionWellEmptyList.add(fabric[i][j]);
					break;
				case Basic:
					fabric[i][j].setUsed(false);
				}
			}
		}
		qubits.clear();
	}
	//retains all the traps to the empty trap list but the ones which are occupied
	public void clear(){
		//Clearing the usage statistics for channels
		ChannelEdge c;
		for (Iterator<ChannelEdge> iterator = layoutGraph.edgeSet().iterator(); iterator.hasNext();) {
			c=iterator.next();
			c.setUsed(false);
		}

		//Clearing the usage statistics for junctions and traps
		for (int i = 0; i < dim.height; i++) {
			for (int j = 0; j < dim.width; j++) {
				switch(fabric[i][j].getType()){
				case Interaction:
					fabric[i][j].setUsed(false);
					//((Interaction)fabric[i][j]).clearProgressList();
					if (!interactionWellEmptyList.contains(fabric[i][j]))
						interactionWellEmptyList.add(fabric[i][j]);
					break;
				case Basic:
					fabric[i][j].setUsed(false);
				}
			}
		}

		//		for
		Qubit q;
		Dimension current;
		for (Iterator<Entry<String, Qubit>> iterator = qubits.entrySet().iterator(); iterator.hasNext();) {
			q=iterator.next().getValue();
			current=q.getPosition();
			sortInteractionWells(current, false, 0);
			q.setPosition(getNearestFreeInteraction(false).getPosition());
			occupy(getNearestFreeInteraction(false).getPosition(), q);
			if (RuntimeConfig.PRINT_QUBIT){
				System.out.println(q.getName()+" @"+q.getPosition().height+","+q.getPosition().width);
			}
		}
		//		interactionWellEmptyList
		HashSet<Well> test=new HashSet<Well>(interactionWellEmptyList);
		if (test.size()!=interactionWellEmptyList.size()){
			System.out.println(":((((((((((");
			System.exit(-1);
		}

	}
	public void assignLastInteraction(Dimension x) {
		if (RuntimeConfig.DEBUG)
			System.out.println("Interaction ("+x.height+","+x.width+") is reserved.");
		if (!interactionWellEmptyList.remove(0).getPosition().equals(x)){
			System.out.println("ERRRR");
			System.exit(-1);
		}
	}
	//---------------------Initialization functions-----------------------
	public Layout(){
		wellInsts.put(new String ("basic"), new Vector<Operation>());
		wellInsts.put(new String ("creation"), new Vector<Operation>());
		wellInsts.put(new String ("interaction"), new Vector<Operation>());
	}
	public void initFabric(Dimension fabricSize, Dimension tileSize, Types[][] tile){
		Dimension temp=new Dimension();
		dim=new Dimension(fabricSize);
		int i,j,k,l;
		dim.height=(fabricSize.height)*(tileSize.height);
		dim.width=(fabricSize.width)*(tileSize.width);

		fabric=new Well[dim.height][dim.width];

		for (i = 0; i < fabricSize.height; i++) {
			for (j = 0; j < fabricSize.width; j++) {
				for (k = 0; k < tileSize.height; k++) {
					for (l = 0; l < tileSize.width; l++) {
						temp.height=i*(tileSize.height) + k;
						temp.width=j*(tileSize.width) + l;
						if (tile[k][l]==Types.Interaction)
							fabric[temp.height][temp.width]=new Interaction(temp, tile[k][l]);
						else
							fabric[temp.height][temp.width]=new Well(temp, tile[k][l]);
					}
				}
			}
		}

		makeGraph();
		if (RuntimeConfig.DEBUG){
			printLayoutGraph();
			printFabric();
		}
	}
	public void makeGraph(){
		layoutGraph = new SimpleWeightedGraph<Well, ChannelEdge>(new ClassBasedEdgeFactory<Well, ChannelEdge>(ChannelEdge.class)); 
		Well prev;
		ChannelEdge channelEdge;
		int cost=0;

		for (int i = 0; i < dim.height; i++) {
			prev=null;
			for (int j = 0; j < dim.width; j++) {
				if (fabric[i][j].getType()==Types.Junction){
					layoutGraph.addVertex(fabric[i][j]);
					//Looking for any edge to the left node
					if (prev!=null){
						channelEdge=new ChannelEdge(prev, fabric[i][j], cost);
						layoutGraph.addEdge(prev, fabric[i][j], channelEdge);	
						layoutGraph.setEdgeWeight(channelEdge, cost);
					}
					//Looking for any edge to the upper node
					if (i>0){
						cost=0;
						for (int k = i-1; k >= 0; k--) {
							if (layoutGraph.containsVertex(fabric[k][j])){
								channelEdge=new ChannelEdge(fabric[k][j], fabric[i][j], cost);
								layoutGraph.addEdge(fabric[k][j], fabric[i][j], channelEdge);
								layoutGraph.setEdgeWeight(channelEdge, cost);
								break;
							}
							cost++;
						}
					}
					prev=fabric[i][j];
					cost=0;
				}else if (fabric[i][j].getType()==Types.Interaction){
					interactionWellEmptyList.add(fabric[i][j]);
				}
			}

		}
	}
	//--------------------------Diagnosis functions-----------------------
	public void printFreeIneractionWells(){
		for (int i = 0; i < interactionWellEmptyList.size(); i++) {
			System.out.println(interactionWellEmptyList.get(i).getPosition().height+","+interactionWellEmptyList.get(i).getPosition().width);
		}
	}
	public void printLayoutGraph(){
		ChannelEdge ce;
		for (Iterator<ChannelEdge> iterator = layoutGraph.edgeSet().iterator(); iterator.hasNext(); ) {
			ce=iterator.next();
			System.out.println(ce + "   cost: "+ ce.getCost(0) + ", weight: "+layoutGraph.getEdgeWeight(ce));
		}
	}
	/**
	 * Prints the parsed fabric.
	 */
	public void printFabric(){
		int i, j;
		System.out.println("Fabric ("+dim.height+"x"+dim.width+"):");
		for (i = 0; i < dim.height; i++) {
			for (j = 0; j < dim.width; j++) {
				if (fabric[i][j].getType()==Types.Basic)
					System.out.print("B");
				else if (fabric[i][j].getType()==Types.Junction)
					System.out.print("J");
				else if (fabric[i][j].getType()==Types.Interaction)
					System.out.print("I");
				else
					System.out.print("X");
			}
			System.out.println();
		}

	}
	//	
	public void printQubitPlaces(){
		System.out.println("Qubit places:");
		Set<Entry<String, Qubit>> qSet =qubits.entrySet();
		for (Iterator<Entry<String, Qubit>> iterator = qSet.iterator(); iterator.hasNext();) {
			Entry<String, Qubit> entry = iterator.next();
			System.out.println(entry.getKey()+" is @("+entry.getValue().getPosition().height+","+entry.getValue().getPosition().width+")");

		}
	}
	
	public void printFabricQubitPlaces(){
		System.out.println("Fabric qubits:");
		for (int i = 0; i < fabric.length; i++) {
			for (int j = 0; j < fabric[i].length; j++) {
				if (fabric[i][j].getType()==Types.Interaction)
					fabric[i][j].printQubitSet();
			}
		}
	}
	//-------------------------Getters-----------------------------
	public Operation getOperation(String s){
		return supportedOps.get(s.toLowerCase());
	}
	public Well getNearestFreeInteraction(Dimension a, boolean assign){
		int count=0;
		Well temp=null;
		if (interactionWellEmptyList.size()==0){
			if (RuntimeConfig.DEBUG){
				//not necessarily an error
				System.out.println("No new interaction well!");
				//System.exit(-1);
			}
			return null;
		}
		Collections.sort(interactionWellEmptyList, new NearestWell(a));
		if (assign){
//			System.out.println("BEFORE:----");
//			printFreeIneractionWells();
			temp= interactionWellEmptyList.remove(0);
			if (RuntimeConfig.DEBUG)
				System.out.println("Interaction ("+temp.getPosition().height+","+temp.getPosition().width+") is reserved.");
//			System.out.println("AFTER:----");
//			printFreeIneractionWells();
//			printQubitPlaces();
//			printFabricQubitPlaces();
			return temp;
		}else
			return interactionWellEmptyList.get(0);
	}
	public Qubit getQubit(String s){
		return qubits.get(s);		
	}
	public int getOpDelay(String s){
		if (!supportedOps.containsKey(s.toLowerCase()))
			System.out.println(s);
		return supportedOps.get(s.toLowerCase()).getDelay();
	}
	public Types getWellType(int height, int width){
		return fabric[height][width].getType();
	}
	//	
	public Types getWellType(Dimension a){
		return getWellType(a.height, a.width);
	}
	public Well getWell(Dimension d){
		return fabric[d.height][d.width];
	}
	public Well getWell(int height, int width){
		return fabric[height][width];
	}
	public Dimension getLayoutSize(){
		//returns a new instance of Dimension to keep data of layout safe
		return new Dimension(dim);
	}
	public SimpleWeightedGraph<Well, ChannelEdge> getGraph(){
		return layoutGraph;
	}
	public ArrayList<Well> getInteractionEmptyList(){
		return interactionWellEmptyList;
	}
	public Well getNearestFreeInteraction(boolean assign){
		if (interactionWellEmptyList.size()==0){
			System.err.println("Sever QSPR error: No new interaction well!");
			System.exit(-1);
		}
		//		Collections.sort(creationWellEmptyList, new NearestWell(a));
		if (assign){
			if (RuntimeConfig.DEBUG)
				System.out.println("Interaction "+interactionWellEmptyList.get(0).getPosition().height+","+interactionWellEmptyList.get(0).getPosition().width+" is reserved.");
			return interactionWellEmptyList.remove(0);
		}
		else
			return interactionWellEmptyList.get(0);
	}
	public ChannelEdge getChannelEdge(Dimension src, Dimension dst){
		return getChannelEdge(new Dimension((src.width+dst.width)/2, (src.height+dst.height)/2));
	}
	public ChannelEdge getChannelEdge(Dimension src){
		if (src.height%2 ==0) //vertical channel
			return layoutGraph.getEdge(fabric[src.height-1][src.width], fabric[src.height+1][src.width]);
		else //horizontal channel
			return layoutGraph.getEdge(fabric[src.height][src.width-1], fabric[src.height][src.width+1]);
	}
	public ChannelEdge getNearestChannelEdge(Dimension a, Dimension b){
		boolean isDown=false, isLeft=false;
		
		if (a.height>b.height)
			isDown=true;
		if (a.width>b.width)
			isLeft=true;
		
		if (isDown && isLeft)
			return getChannelEdge(new Dimension(a.width, a.height-1));
		else if (isDown && !isLeft)
			return getChannelEdge(new Dimension(a.width, a.height-1));
		else if (!isDown && isLeft)
			return getChannelEdge(new Dimension(a.width, a.height+1));
		else if (!isDown && !isLeft)
			return getChannelEdge(new Dimension(a.width, a.height+1));
			
		return null;
	}
	public Well getNearestChannel(Dimension a, Dimension b){
		boolean isDown=false, isRight=false;
		
		if (a.height>b.height)
			isDown=true;
		if (a.width>b.width)
			isRight=true;
		
		if (isDown && isRight)
			return fabric[a.height-1][a.width];
		else if (isDown && !isRight)
			return fabric[a.height-1][a.width];
		else if (!isDown && isRight)
			return fabric[a.height+1][a.width];
		else if (!isDown && !isRight)
			return fabric[a.height+1][a.width];
			
		return null;
	}
	public Dimension getNearestChannelLocation(Dimension a, Dimension b){
		boolean isDown=false, isLeft=false;
		
		if (a.height>b.height)
			isDown=true;
		if (a.width>b.width)
			isLeft=true;
		
		if (isDown && isLeft)
			new Dimension(a.width, a.height-1);
		else if (isDown && !isLeft)
			new Dimension(a.width, a.height-1);
		else if (!isDown && isLeft)
			new Dimension(a.width, a.height+1);
		else if (!isDown && !isLeft)
			new Dimension(a.width, a.height+1);
			
		return null;
	}
	public Well getNearestJunction(Dimension a, Dimension b) {
		boolean isDown=false, isLeft=false;
		
		if (a.height>b.height)
			isDown=true;
		if (a.width>b.width)
			isLeft=true;
		
		if (isDown && isLeft)
			return fabric[a.height-1][a.width-1];
		else if (isDown && !isLeft)
			return fabric[a.height-1][a.width+1];
		else if (!isDown && isLeft)
			return fabric[a.height+1][a.width-1];
		else if (!isDown && !isLeft)
			return fabric[a.height+1][a.width+1];
			
		return null;
	}
	//-------------------------Test functions-----------------------------
	public boolean isOperationSupported(String s){
		return supportedOps.containsKey(s.toLowerCase());
	}
	public boolean isNode(Dimension x){
		if (layoutGraph.containsVertex(getWell(x)))
			return true;
		else
			return false;
	}
	public boolean isIntraction(Dimension x){
		return isIntraction(x.height, x.width);
	}
	public boolean isIntraction(int height, int width){
		if (fabric[height][width].getType()==Types.Interaction)
			return true;
		else
			return false;
	}
	public boolean isJunction(Dimension x){
		return isJunction(x.height, x.width);
	}
	public boolean isJunction(int height, int width){
		if (fabric[height][width].getType()==Types.Junction)
			return true;
		else
			return false;
	}
	public boolean isBasic(int height, int width){
		if (fabric[height][width].getType()==Types.Basic)
			return true;
		else
			return false;
	}
	public boolean isBasic(Dimension x){
		return isBasic(x.height, x.width);
	}
	public boolean isInteractionWellFree(Dimension x){
		if (isIntraction(x)==false) //x well is not an interaction.
			return false;
		if (interactionWellEmptyList.contains(fabric[x.height][x.width]))// || ((Interaction)fabric[x.height][x.width]).isInProgress() )
			return true;
		else
			return false;
	}
	//--------------------------------------------------------------------
}
