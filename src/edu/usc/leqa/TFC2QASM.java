/*
 * 
 * Copyright (C) 2014 Mohammad Javad Dousti and Massoud Pedram, SPORT lab,
 * University of Southern California. All rights reserved.
 * 
 * Please refer to the LICENSE file for terms of use.
 * 
*/
package edu.usc.leqa;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Iterator;


public class TFC2QASM {
	ArrayList<String> qubitList=new ArrayList<String>();
	//	ArrayList<String> commandList=new ArrayList<String>();
	int ancillaCount=-1;
	FileWriter fw1;
	BufferedWriter bw1;

	public void openFile(String outAddr) {
		try{
			fw1=new FileWriter(outAddr+".tmp1");
			bw1=new BufferedWriter(fw1);
		}catch (Exception e){
			System.err.println("Error: " + e.getMessage());
		}
	}

	public void closeFile(String outAddr){
		FileWriter fw2;
		BufferedWriter bw2;
		try{
			//closing the temp file
			bw1.close();
			fw1.close();

			fw2=new FileWriter(outAddr+".tmp2");
			bw2=new BufferedWriter(fw2);

			for (Iterator<String> iterator = qubitList.iterator(); iterator.hasNext();) {
				bw2.write("qubit "+iterator.next()+"\r\n");
			}
			bw2.write("\r\n \r\n");

			bw2.close();
			fw2.close();

			//Concatenating two files
			FileInputStream fis1=new FileInputStream(outAddr+".tmp1");
			FileInputStream fis2=new FileInputStream(outAddr+".tmp2");
			FileOutputStream fos=new FileOutputStream(outAddr);
			FileChannel source1 = fis1.getChannel();
			FileChannel source2 = fis2.getChannel();
			FileChannel destination = fos.getChannel();

			destination.transferFrom(source2, 0, source2.size());
			destination.transferFrom(source1, source2.size(), source1.size());
			

			destination.close();
			source1.close();
			source2.close();
			fos.close();
			fis1.close();
			fis2.close();

			//deleting the .tmp file
			File f=new File(outAddr+".tmp1");
			f.delete();
			f=new File(outAddr+".tmp2");
			f.delete();
		}catch (Exception e){
			System.err.println("Error: " + e.getMessage());
		}
	}

	public void defineQubit(String defType, ArrayList<String> qubits){
		String qubit;
		if (defType.compareToIgnoreCase(".v")==0){
			for (Iterator<String> iterator = qubits.iterator(); iterator.hasNext();) {
				qubit="q"+iterator.next();
				if (qubitList.contains(qubit)){
					System.err.println("Qubit "+qubit+" is redefined.");
					System.exit(-1);
				}else
					qubitList.add(qubit);
			}
		}
	}

	public void addComplexCommand(String cmd, ArrayList<String> qubits){
		try{
			String command;
			switch (qubits.size()) {
			case 1:
				command="x q"+qubits.get(0);
				bw1.write(command+"\r\n");
				//commandList.add(command);
				return;
			case 2:
				if (cmd.compareToIgnoreCase("f2")==0){ //swap gate
					command="CNOT q"+qubits.get(0)+", q"+qubits.get(1);
					bw1.write(command+"\r\n");
					//commandList.add(command);
					command="CNOT q"+qubits.get(1)+", q"+qubits.get(0);
					bw1.write(command+"\r\n");
					//commandList.add(command);
					command="CNOT q"+qubits.get(0)+", q"+qubits.get(1);
					bw1.write(command+"\r\n");
					//commandList.add(command);
				}else{//cnot gate
					command="CNOT q"+qubits.get(0)+", q"+qubits.get(1);
					bw1.write(command+"\r\n");
					//commandList.add(command);
				}
				return;
			case 3:
				if (cmd.compareToIgnoreCase("f3")==0){ //fredkin gate
					synthToffoli("q"+qubits.get(0), "q"+qubits.get(1), "q"+qubits.get(2));
					synthToffoli("q"+qubits.get(0), "q"+qubits.get(2), "q"+qubits.get(1));
					synthToffoli("q"+qubits.get(0), "q"+qubits.get(1), "q"+qubits.get(2));
				}else{ //toffoli gate
					synthToffoli("q"+qubits.get(0), "q"+qubits.get(1), "q"+qubits.get(2));
				}
				break;
			default:
				if (cmd.charAt(0)=='f' || cmd.charAt(0)=='F' ){
					System.err.println("The input circuit has multiple-control SWAP which is not supported yet.");
					System.exit(-1);
				}
				synthMultiInputToffoli(qubits);
				break;
			}
		}catch(Exception e){
			System.err.println("Error: " + e.getMessage());
		}
	}

	private void synthMultiInputToffoli(ArrayList<String> qubits){
		//adding new ancillas; recording the index of the first ancillas
		int index=ancillaCount+1;
		for(int i=ancillaCount+1; i<= ancillaCount+qubits.size()-3; i++){
			qubitList.add("ancilla"+i);
		}
		if (ancillaCount==-1)
			ancillaCount+=qubits.size()-2;
		else
			ancillaCount+=qubits.size()-3;

		//first toffoli
		synthToffoli("q"+qubits.get(0), "q"+qubits.get(1), "ancilla"+index);
		//down ladded toffolis
		for (int i=2;i<=qubits.size()-3;i++){
			synthToffoli("q"+qubits.get(i), "ancilla"+(index), "ancilla"+(index+1));
			index++;
		}
		//the most bottom toffoli
		synthToffoli("q"+qubits.get(qubits.size()-2), "ancilla"+index, "q"+qubits.get(qubits.size()-1));
		//up ladded toffoli
		for (int i=qubits.size()-3;i>=2;i--){
			index--;
			synthToffoli("q"+qubits.get(i), "ancilla"+(index), "ancilla"+(index+1));
		}
		//last toffoli
		synthToffoli("q"+qubits.get(0), "q"+qubits.get(1), "ancilla"+index);	
	}

	private void synthToffoli(String control1, String control2, String target) {
		//		ArrayList<String> commands=new ArrayList<String>();
		//		commands.add("toffoli "+control1+", "+control2+", "+target);
		try{
			bw1.write("H "+target+"\r\n");
			bw1.write("CNOT "+control2+", "+target+"\r\n");
			bw1.write("T_dag "+target+"\r\n");
			bw1.write("CNOT "+control1+", "+target+"\r\n");
			bw1.write("T "+target+"\r\n");
			bw1.write("CNOT "+control2+", "+target+"\r\n");
			bw1.write("T_dag "+target+"\r\n");
			bw1.write("CNOT "+control1+", "+target+"\r\n");
			bw1.write("T "+target+"\r\n");
			bw1.write("T "+control2+"\r\n");
			bw1.write("H "+target+"\r\n");
			bw1.write("CNOT "+control1+", "+control2+"\r\n");
			bw1.write("T "+control1+"\r\n");
			bw1.write("T_dag "+control2+"\r\n");
			bw1.write("CNOT "+control1+", "+control2+"\r\n");
		}catch (Exception e){
			System.err.println("Error: " + e.getMessage());
		}

	}

	public void addSimpleCommand(String cmd,String q0){
		String command;
		command="X q"+q0;
		try{
			bw1.write(command+"\r\n");
		}catch(Exception e){
			System.err.println("Error: " + e.getMessage());
			//commandList.add(command);
		}
	}


}
