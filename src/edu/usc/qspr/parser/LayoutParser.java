/*
 * 
 * Copyright (C) 2014 Mohammad Javad Dousti and Massoud Pedram, SPORT lab,
 * University of Southern California. All rights reserved.
 * 
 * Please refer to the LICENSE file for terms of use.
 * 
*/

package edu.usc.qspr.parser;

import java.awt.Dimension;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import edu.usc.qspr.layout.Layout;
import edu.usc.qspr.layout.Operation;


public class LayoutParser {
	
	public static Layout parse(String defAddr, String layoutAddr){
		Layout layout=new Layout();
		//parsing the definitions file
		if (!defsParser(layout, defAddr))
			return null;
		//parsing the layout file
		if (!layoutParser(layout, layoutAddr))
			return null;
		return layout;
	}
	
	public static boolean defsParser(final Layout layout, String addr){
		try {

			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();

			DefaultHandler handler = new DefaultHandler() {
				boolean definitions=false;
				boolean ISA=false;
				boolean wells=false;
				boolean inst=false;

				//ISA
				boolean name = false;
				boolean delay = false;
				boolean error = false;

				//wells			
				boolean well = false;
				boolean instructions = false;
				boolean i=false;

				String instName, wellName;
				double errorRate; 
				int delayValue;


				public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
					if (qName.equalsIgnoreCase("definitions")){
						definitions=true;
					}else if (qName.equalsIgnoreCase("ISA")){
						ISA = true;
					}else if (qName.equalsIgnoreCase("wells")){
						wells = true;
					}
					//ISA
					else if (qName.equalsIgnoreCase("inst") && definitions && ISA){
						inst = true;
					}else if (qName.equalsIgnoreCase("name") && definitions && ISA && inst){
						name = true;
					}else if (qName.equalsIgnoreCase("delay") && definitions && ISA && inst) {
						delay = true;
					}else if (qName.equalsIgnoreCase("error") && definitions && ISA && inst) {
						error = true;
					}

					//Wells
					else if (qName.equalsIgnoreCase("well") && definitions && wells){
						well = true;
					}else if (qName.equalsIgnoreCase("name") && definitions && wells && well) {
						name = true;
					}else if (qName.equalsIgnoreCase("instructions") && definitions && wells && well) {
						instructions = true;
					}else if (qName.equalsIgnoreCase("i") && definitions && wells && well && instructions) {
						i = true;
					}else{
						throw new SAXException("Incorrect XML Format: "+qName);
					}
				}

				public void endElement(String uri, String localName, String qName) throws SAXException {
					if (qName.equalsIgnoreCase("definitions")){
						definitions=false;
					}else if (qName.equalsIgnoreCase("ISA")){
						ISA = false;
					}else if (qName.equalsIgnoreCase("wells")){
						wells = false;
					}
					//ISA
					else if (qName.equalsIgnoreCase("inst") && definitions && ISA){
						inst = false;
					}else if (qName.equalsIgnoreCase("name") && definitions && ISA && inst){
						name = false;
					}else if (qName.equalsIgnoreCase("delay") && definitions && ISA && inst) {
						delay = false;
					}else if (qName.equalsIgnoreCase("error") && definitions && ISA && inst) {
						error = false;
					}

					//Wells
					else if (qName.equalsIgnoreCase("well") && definitions && wells){
						well = false;
					}else if (qName.equalsIgnoreCase("name") && definitions && wells && well) {
						name = false;
					}else if (qName.equalsIgnoreCase("instructions") && definitions && wells && well) {
						instructions = false;
					}else if (qName.equalsIgnoreCase("i") && definitions && wells && well && instructions) {
						i = false;
					}else{
						throw new SAXException("Incorrect XML Format: "+qName);
					}
				}

				public void characters(char ch[], int start, int length) throws SAXException {
					//ISA
					if (name && ISA) {
						//System.out.println("ISA::NAME: " + new String(ch, start, length));
						instName=new String(ch, start, length);					
					}else if (delay) {
						//System.out.println("Delay: " + new String(ch, start, length));
						delayValue=Integer.parseInt(new String(ch, start, length));
					}else if (error){
						//System.out.println("Error: " + new String(ch, start, length));
						errorRate=Double.parseDouble(new String(ch, start, length));

						layout.addNewOperation(new Operation(instName, errorRate, delayValue));
					}

					//wells
					else if (name && wells) {
						wellName=new String(ch, start, length);
					}else if (i) {
						instName= new String(ch, start, length);
						layout.addInstToWell(wellName, instName);
					}//skipping over white spaces
					else if (new String(ch, start, length).trim().length()!=0){
						throw new SAXException("Incorrect XML Format: "+ new String(ch, start, length) + length);
					}
				}

			};

			File file = new File(addr);
			InputStream inputStream= new FileInputStream(file);
			Reader reader = new InputStreamReader(inputStream,"UTF-8");

			InputSource is = new InputSource(reader);
			is.setEncoding("UTF-8");

			saxParser.parse(is, handler);
		}catch(FileNotFoundException e){
			System.err.println("Layout definition file not found!");
			return false;
		}catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	public static boolean layoutParser(final Layout layout, String addr){
		try {

			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();

			DefaultHandler handler = new DefaultHandler() {
				boolean gridConfig=false;
				boolean numXPatterns=false;
				boolean numYPatterns=false;
				boolean pattern=false;
				boolean rows = false;
				boolean cols = false;
				boolean row = false;
				boolean col = false;

				Dimension fabricSize=new Dimension();
				Dimension tileSize=new Dimension();
				Layout.Types [][]tile;

				int i,j;

				public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
					if (qName.equalsIgnoreCase("gridConfig")){
						gridConfig=true;
					}else if (qName.equalsIgnoreCase("numXPatterns") && gridConfig){
						numXPatterns = true;
					}else if (qName.equalsIgnoreCase("numYPatterns") && gridConfig){
						numYPatterns = true;
					}else if (qName.equalsIgnoreCase("pattern") && gridConfig){
						pattern = true;
					}else if (qName.equalsIgnoreCase("rows") && pattern && gridConfig){
						rows = true;
					}else if (qName.equalsIgnoreCase("cols") && pattern && gridConfig){
						cols = true;
					}else if (qName.equalsIgnoreCase("row") && pattern && gridConfig){
						j=0;
						i++;
						row = true;
					}else if (qName.equalsIgnoreCase("col") && row && pattern && gridConfig){
						col = true;
					}else{
						throw new SAXException("Incorrect XML Format: "+qName);
					}
				}

				public void endElement(String uri, String localName, String qName) throws SAXException {
					if (qName.equalsIgnoreCase("gridConfig")){
						gridConfig=false;
					}else if (qName.equalsIgnoreCase("numXPatterns") && gridConfig){
						numXPatterns = false;
					}else if (qName.equalsIgnoreCase("numYPatterns") && gridConfig){
						numYPatterns = false;
					}else if (qName.equalsIgnoreCase("pattern") && gridConfig){
						pattern = false;
						//pattern is generated
						layout.initFabric(fabricSize, tileSize, tile);
					}else if (qName.equalsIgnoreCase("rows") && pattern && gridConfig){
						rows = false;
					}else if (qName.equalsIgnoreCase("cols") && pattern && gridConfig){
						cols = false;
					}else if (qName.equalsIgnoreCase("row") && pattern && gridConfig){
						row = false;
					}else if (qName.equalsIgnoreCase("col") && row && pattern && gridConfig){
						col = false;
					}else{
						throw new SAXException("Incorrect XML Format: "+qName);
					}
				}

				public void characters(char ch[], int start, int length) throws SAXException {
					if (numXPatterns) {
						fabricSize.width=Integer.parseInt(new String(ch, start, length));
						//System.out.println(new String(ch, start, length));

					}else if (numYPatterns){
						fabricSize.height=Integer.parseInt(new String(ch, start, length));
						//System.out.println(new String(ch, start, length));

					}else if (rows){
						tileSize.height=Integer.parseInt(new String(ch, start, length));
						//System.out.println(new String(ch, start, length));

					}else if (cols) {
						tileSize.width=Integer.parseInt(new String(ch, start, length));

						//initializing fabric tile
						tile=new Layout.Types[tileSize.height][tileSize.width];
						//System.out.println(new String(ch, start, length));
						i=-1;
					}else if (row){
						if (col){
							String temp=new String(ch, start, length);
							if (temp.equalsIgnoreCase("basic")){
								tile[i][j]=Layout.Types.Basic;
							}else if (temp.equalsIgnoreCase("junction")){
								tile[i][j]=Layout.Types.Junction;
							}else if (temp.equalsIgnoreCase("interaction")){
								tile[i][j]=Layout.Types.Interaction;
							}else{
								throw new SAXException("Incorrect XML Format: undefined well type: "+temp);
							}
							j++;

						}
						//System.out.println(new String(ch, start, length));
					}//skipping over white spaces
					else if (new String(ch, start, length).trim().length()!=0){
						throw new SAXException("Incorrect XML Format: "+ new String(ch, start, length) + length);
					}
				}

			};


			File file = new File(addr);
			InputStream inputStream= new FileInputStream(file);
			Reader reader = new InputStreamReader(inputStream,"UTF-8");

			InputSource is = new InputSource(reader);
			is.setEncoding("UTF-8");

			saxParser.parse(is, handler);
		}catch(FileNotFoundException e){
			System.err.println("Layout definition file not found!");
			return false;
		}catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}


}
