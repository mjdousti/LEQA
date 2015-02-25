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

import javax.swing.plaf.basic.BasicArrowButton;

import edu.usc.qspr.layout.ChannelEdge;
import edu.usc.qspr.layout.Layout;
import edu.usc.qspr.layout.Qubit;
import edu.usc.qspr.layout.Well;
import edu.usc.qspr.layout.Layout.Types;
import edu.usc.qspr.qasm.Vertex;

public class Path implements Comparable<Path>{
	private ArrayList<Well> path;
	private Dimension src, dst;
	private Qubit qubit;
	private Vertex cmd;
	private long delay;
	private Layout layout;
	//	private int state;
	private Dimension indicator=null;
	private boolean finished;
	private boolean executionFinished;
	private int Trouting=0;


	public Path(ArrayList<Well> p, Dimension dst, Qubit q, Vertex command, long simTime, Layout l) {
		layout=l;
		qubit=q;
		this.dst=dst;
		this.src=qubit.getPosition();

		path=p;
		cmd=command;
		delay=simTime+updateDelay();
		finished=false;
		executionFinished=false;
	}

	public Dimension getDestination(){
		return dst;
	}

	public ArrayList<Well> getPath(){
		return path;
	}


	private long updateDelay() {
		Dimension x=qubit.getPosition();
		switch (layout.getWellType(x)){
		case Basic:
			Trouting+=layout.getOpDelay("Move");
			return delay+=layout.getOpDelay("Move");
		case Junction:
			Trouting+=layout.getOpDelay("Move");
			return delay+=layout.getOpDelay("Move");
		case Interaction:
			if (x.equals(dst)){
				if ((layout.getWell(x)).getQubitsNo() == cmd.getOperandsNumber()){
					return delay+=layout.getOpDelay(cmd.getName());
				}else{
					if (RuntimeConfig.DEBUG)
						System.out.println("Jumped!");
					finished=true;
					return delay=0;
				}
			}else{
				Trouting+=layout.getOpDelay("Move");
				return delay+=layout.getOpDelay("Move");
			}
		}
		return delay;
	}

	public long getDelay(){
		return delay;
	}

	public Well nextMove(){
		Dimension current = qubit.getPosition();
		Well removedPath=null;
		finished=false;
		switch (layout.getWellType(current)){
		case Basic:
			removedPath=path.remove(0);
			qubit.move(removedPath.getPosition());

			updateDelay();
			break;
		case Junction:
			removedPath=path.remove(0);
			qubit.move(removedPath.getPosition());
			
			updateDelay();
			break;		
		case Interaction:
			//Reached at the destination
			if (qubit.getPosition().equals(dst)){
				if ((layout.getWell(qubit.getPosition())).getQubitsNo() == cmd.getOperandsNumber()){
					//TODO: Should be moved to the layout. Layout is responsible of doing quantum operations 
					if(RuntimeConfig.VERBOSE){
						System.out.print("'"+getFullCommand()+"'"+" @("+qubit.getPosition().height+","+qubit.getPosition().width+") ");
						if (RuntimeConfig.Trouting)
							System.out.print("Trouting="+Trouting);
						else
							System.out.print("\t");
						if (RuntimeConfig.Tcongestion)
							System.out.print(", "+"Tcongestion="+cmd.getTcongestion()+ "\t\t");
						else
							System.out.print("\t");
						//						System.out.print("*************************");
					}
					executionFinished=true;
				}//This check is not needed! Just to be safe!
				else if ((layout.getWell(current)).getQubitsNo() > cmd.getOperandsNumber()){
					System.out.println("Fatal error in routing! More qubits than needed were routed to ("+current.height+","+current.width+").");
					System.exit(-1);
				}
				finished=true;
				return removedPath;
			}//Just started the journey
			else{
				removedPath=path.remove(0);
				qubit.move(removedPath.getPosition());
				
				updateDelay();
			}		
		}
		return removedPath;
	}

	public boolean isFinished(){
		return finished;
	}

	public boolean isExecutionFinished(){
		return executionFinished;
	}

	public String getCommand(){
		return cmd.getCommand();
	}

	public String getFullCommand(){
		String s=cmd.getCommand()+" "+cmd.getOperand(0);
		for (int i = 1; i < cmd.getOperandsNumber(); i++) {
			s+=", "+cmd.getOperand(i);
		}
		return s;
	}


	public Dimension getQubitPosition(){
		return qubit.getPosition();
	}

	public Qubit getQubit(){
		return qubit;
	}

	public int getOperandsNumber(){
		return cmd.getOperandsNumber();
	}

	public String getOperand(int index){
		return cmd.getOperand(index);
	}

	@Override
	public int compareTo(Path o) {
		if (getDelay()<o.getDelay())
			return -1;
		else if (getDelay()>o.getDelay())
			return 1;
		else{
			return cmd.compareTo(o.getVertex());
		}
	}

	public Vertex getVertex(){
		return cmd;
	}

	public String toString(){
		String output=new String();

		for (int i = 0; i < path.size(); i++) {
			output+=i+": "+ path.get(i)
					+System.getProperty("line.separator");
		}
		return output;
	}
}
