/*
 * 
 * Copyright (C) 2014 Mohammad Javad Dousti and Massoud Pedram, SPORT lab,
 * University of Southern California. All rights reserved.
 * 
 * Please refer to the LICENSE file for terms of use.
 * 
*/
package edu.usc.qspr;

import edu.usc.qspr.layout.Layout;
import edu.usc.qspr.qasm.QASM;
import edu.usc.qspr.router.EventDrivenSimulator;
import edu.usc.qspr.router.InitialPlacer;
import edu.usc.qspr.router.InitialPlacer.Heuristic;

public class QSPR {
	public static void qspr(EventDrivenSimulator eds, Layout layout, QASM qasm, int time){
		eds=new EventDrivenSimulator(layout, qasm.getCommandsList());


		//nMVBF(time);
		//    	MVBF(time);
	}
	
	public static long center(EventDrivenSimulator eds, Layout layout, QASM qasm){
		eds=new EventDrivenSimulator(layout, qasm.getCommandsList());
		InitialPlacer.place(qasm, layout, Heuristic.Center);
		eds.schedule();
		long actualResult=eds.simluate();
		return actualResult;
		//System.out.println(System.getProperty("line.separator")+"------------------------------------"
		//		+System.getProperty("line.separator")+"Actual Result:\t"+actualResult+" \u00B5sec");
	}

	public static int nMVBF(int time){
		double smallest=-1;
		boolean worse1, worse2, worse3;
		int totalIterations=0;
		int i=0;

		/*while (System.currentTimeMillis()-start<time){
			worse1=worse2=worse3=false;
			if (i==0){
				InitialPlacer.place(qasm, layout, Heuristic.Center);
				i++;
			}else 
				InitialPlacer.place(qasm, layout, Heuristic.ShuffledCenter);
			do{
				totalIterations++;
				eds.schedule();
				actualResult=eds.simluate();

				if (smallest==-1 ||smallest>actualResult){
					smallest=actualResult;
					worse1=worse2=worse3=false;
				}else{
					if (worse2==true){
						worse3=true;
					}else if (worse1==true){
						worse2=true;
						worse3=false;
					}else{
						worse1=true;
						worse2=false;
						worse3=false;
					}
				}
				layout.clear();
				qasm.reverseCommandsOrder();
			}while(worse3!=true);
			layout.clean();
		}
		System.out.println(System.getProperty("line.separator")+"MVBF results: "+smallest+" \u00B5sec" + " with total iterations: "+totalIterations);

		 */
		return totalIterations;
	}
}
