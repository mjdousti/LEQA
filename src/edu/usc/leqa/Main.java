/*
 * 
 * Copyright (C) 2014 Mohammad Javad Dousti and Massoud Pedram, SPORT lab,
 * University of Southern California. All rights reserved.
 * 
 * Please refer to the LICENSE file for terms of use.
 * 
*/
package edu.usc.leqa;

import java.io.File;
import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;

import edu.usc.qspr.QSPR;
import edu.usc.qspr.layout.Layout;
import edu.usc.qspr.parser.LayoutParser;
import edu.usc.qspr.parser.qasm.QASMParser;
import edu.usc.qspr.parser.tfc.TFCParser;
import edu.usc.qspr.qasm.QASM;
import edu.usc.qspr.router.EventDrivenSimulator;


/**
 * The Class Main.
 */
public class Main {	
	static String lineSeparator = System.getProperty("line.separator");
	static Layout layout;
	static QASM qasm;
	static EventDrivenSimulator eds;
	static double parseRuntime, leqaRuntime, QSPRRuntime;
	static double speed;

	
	/** The output file addr. */
	private static String inputFileAddr, fabricFileAddr, techFileAddr;
	
	/**
	 * Parses the inputs.
	 *
	 * @param args the args
	 */
	@SuppressWarnings("static-access")
	public static void parseInputs(String [] args){
		Options options=new Options();
		
		options.addOption(OptionBuilder.withLongOpt("input")
				.withDescription( "QASM/TFC input file (QASM is preferred)" )
				.isRequired()
				.hasArg()
				.withArgName("file")
				.create("i"));

		options.addOption(OptionBuilder.withLongOpt("fabric")
				.withDescription( "Fabric specification file" )
				.isRequired()
				.hasArg()
				.withArgName("file")
				.create("f"));
		
		options.addOption(OptionBuilder.withLongOpt("technology")
				.withDescription( "Technology file" )
				.isRequired()
				.hasArg()
				.withArgName("file")
				.create("t"));		

		options.addOption(OptionBuilder.withLongOpt("speed")
                .withDescription("Qubit movement speed")
                .isRequired()
                .hasArg()
                .withArgName("num")
                .create("s"));

		options.addOption(OptionBuilder.withLongOpt("skip")
				.withDescription("Skip invocation of QSPR")
				.create("j"));

		
		options.addOption(OptionBuilder.withLongOpt("verbose")
				.withDescription( "Verbosely prints the quantum operations" )
				.create("v"));

		options.addOption(OptionBuilder.withLongOpt("debug")
				.withDescription( "Print debugging info" )
				.create("d"));
		
		options.addOption(OptionBuilder.withLongOpt("help")
				.withDescription( "Print this help menu" )
				.create("h"));

		CommandLineParser parser=new GnuParser();
		CommandLine cmd=null;
		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e){
			System.err.println(e.getMessage());
			// automatically generate the help statement
			HelpFormatter formatter = new HelpFormatter();
			formatter.setWidth(80);
			formatter.printHelp( "leqa", "LEQA estimates the latency of a given QASM/TFC " +
					"mapped to a given PMD fabric.", options,"", true);
			
			System.exit(-1);
		}

		
		inputFileAddr=cmd.getOptionValue("input");
		if (!new File(inputFileAddr).exists()){
			System.err.println("Input file "+inputFileAddr+" does not exist.");
			System.exit(-1);
		}else if (!new File(inputFileAddr).setReadable(true)){
			System.err.println("Input file "+inputFileAddr+" does not have read permission.");
			System.exit(-1);
		}
		
		fabricFileAddr=cmd.getOptionValue("fabric");
		if (!new File(fabricFileAddr).exists()){
			System.err.println("Fabric file "+fabricFileAddr+" does not exist.");
			System.exit(-1);
		}else if (!new File(fabricFileAddr).setReadable(true)){
            System.err.println("Fabric file "+inputFileAddr+" does not have read permission.");
			System.exit(-1);
        }
		
		techFileAddr=cmd.getOptionValue("technology");
		if (!new File(techFileAddr).exists()){
			System.err.println("Technology file "+techFileAddr+" does not exist.");
			System.exit(-1);
		}else if (!new File(techFileAddr).setReadable(true)){
            System.err.println("Technology file "+inputFileAddr+" does not have read permission.");
			System.exit(-1);
        }
		
		speed = Double.parseDouble(cmd.getOptionValue("speed"));
	
        if (cmd.hasOption("skip")){
            RuntimeConfig.SKIP=true;
        }else{
            RuntimeConfig.SKIP=false;
        }
		
		if (cmd.hasOption("debug")){
			RuntimeConfig.DEBUG=true;
		}else{
			RuntimeConfig.DEBUG=false;
		}

		if (cmd.hasOption("verbose")){
			RuntimeConfig.VERBOSE=true;
		}else{
			RuntimeConfig.VERBOSE=false;
		}
}
		
	
	
	/**
	 * The main method for LEQA.
	 *
	 * @param args the command line arguments
	 * @throws IOException 
	 */
	public static void main(String [] args) throws IOException{
		long start, actual=0, estimated=0;
				
//		args = "-f ../sample_inputs/fabric.xml-t ../sample_inputs/tech.xml -i ../sample_inputs/tfc/8bitadder.tfc".split("\\s");
		parseInputs(args);
		// -s 0.001 
		
		/*
		 * Parsing inputs: fabric, tech & qasm files
		 * LEQA uses parsers of QSPR
		 */
		System.out.println("Parsing technology and fabric files...");
		start = System.currentTimeMillis();
		layout=LayoutParser.parse(techFileAddr, fabricFileAddr);
		
		/* Converting TFC to QASM if TFC is provided. */
		String inputExtension = inputFileAddr.substring(inputFileAddr.lastIndexOf('.')+1);
		if (inputExtension.compareToIgnoreCase("TFC")==0){
			System.out.println("TFC file is provided. Converting to QASM...");
			String qasmFileAddr=inputFileAddr.substring(0,inputFileAddr.lastIndexOf('.'))+".qasm";
			
			if (TFCParser.parse(inputFileAddr, qasmFileAddr)==false){
				System.err.println("Failed to convert "+inputFileAddr+" to QASM format.");
				System.exit(-1);
			}				
			
			inputFileAddr = qasmFileAddr;
		}else if (inputExtension.compareToIgnoreCase("QASM")!=0){
			System.err.println("Extension "+inputExtension + " is not supported! Only qasm and tfc are supported.");
			System.exit(-1);
		}
		
		/* Parsing the QASM file */
		System.out.println("Parsing QASM file...");
		qasm= QASMParser.QASMParser(inputFileAddr, layout);
		
		if (qasm==null || layout==null)
			System.exit(-1);
		parseRuntime=(System.currentTimeMillis() - start)/1000.0;
		
		/*
		 * Invoking LEQA for estimating the latency
		 */
		System.out.println("Invoking LEQA...");
		start = System.currentTimeMillis();
		estimated=LEQA.leqa(qasm, layout, speed);
		leqaRuntime=(System.currentTimeMillis() - start)/1000.0;

		/*
		 * Invoking HL-QSPR for calculating the actual latency 
		 * This is for comparison only. It can be commented out
		 */
		if (! RuntimeConfig.SKIP){
			System.out.println("Invoking QSPR...");
			start=System.currentTimeMillis();
 			actual=QSPR.center(eds, layout, qasm);
			QSPRRuntime=(System.currentTimeMillis() - start)/1000.0;
		}
		
		/*
		 * Printing the results
		 */
		int separatorLength = 30;
		System.out.println();
		System.out.println(StringUtils.center("Results", separatorLength));
		System.out.println(StringUtils.repeat('-', separatorLength));
		
		System.out.println("Estimated value:\t"+estimated);
		if (!RuntimeConfig.SKIP){
			System.out.println("Actual value:\t\t"+actual);
			System.out.printf("Error:\t\t\t%.2f%%"+lineSeparator ,(Math.abs(estimated-actual)*100.0/actual));
		}
		System.out.println(StringUtils.repeat('-', separatorLength));
		
		System.out.println("Parsing overhead:\t"+ parseRuntime+"s");
		System.out.println("LEQA runtime:\t\t"+ leqaRuntime+"s");
        if (!RuntimeConfig.SKIP){
	    	System.out.println("QSPR time:\t\t"+QSPRRuntime+"s");
    		System.out.printf("Speed up:\t\t%.2f" + lineSeparator,QSPRRuntime/leqaRuntime);
		}
	}
}
