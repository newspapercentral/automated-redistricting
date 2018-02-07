package levin.preprocess;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;

import com.vividsolutions.jts.geom.Point;

import levin.District;
import levin.Unit;
import levin.kdtree.KdTree;
/**
 * This class reads and writes shape files for census tracts and census blocks. The output file
 * creates a population unit graph that can be used in any redistricting algorithm. The processed
 * file cleans the population units of discontiguous shapes and calculates each unit's neighbors. 
 *
 * @author Harry
 *
 */
public class PreProcess {
	//Default values
	static String ACTION = "write";
	static String DOC_ROOT = "/Users/Harry/Desktop/Archive/data";
	static String dataFilePath = "/TEST/";
	static String shapeFilePath = "TEST.shp";
	static String popFilePath = "tract-pop.txt";
	static String PRE_PROCESS_FILE = "preprocess.txt";
	static boolean IS_BLOCK=true;
	static String STATE = "hi";
	static int MAX_NEIGHBOR = 100;
	
	static String BLOCK_STRING = "null";

	public PreProcess (String doc_root, String data_file, String shape_file, String pop_file, 
			String pre_process_file, boolean is_block, String state, int max_neighbor) {
		DOC_ROOT = doc_root;
		dataFilePath = data_file;
		shapeFilePath = shape_file;
		popFilePath = pop_file;
		PRE_PROCESS_FILE = pre_process_file;
		IS_BLOCK = is_block;
		STATE = state;
		MAX_NEIGHBOR = 100;
	}
	
	public static void main(String[] args) {
		readParams(args);
		if(ACTION.equalsIgnoreCase("read")) {
			ArrayList<Unit> units = readPreProcess();
			System.out.println(units.get(0).toString());
		}else if(ACTION.equalsIgnoreCase("write")) {
			writePreProcess();
		}else {
			System.out.println("Invalid Action:" + ACTION);
			System.exit(0);
		}
	}
	
	/**
	 * This program takes 8 parameters that can be configured to override the default values that
	 * are listed in the global variables.
	 * @param args
	 * args[0] = ("write"|"read") - write writes a shape file to a preprocessed text file, read
	 *   interprets a previously written text file for input into an algorithm
	 * args[1] = (file path) - root directory of input files and output (ex: "/Users/Harry/Desktop/Archive/data")
	 * args[2] = (file path) - directory of shape and data files (ex; "/TEST_TRACT/")
	 * args[3] = (file name) - name of shape file (ex: "TEST.shp")
	 * args[4] = (file name) - name of census tract data file
	 *   Note: while census blocks have population figures listed in the shape file, census tracts
	 *   use a separate file
	 * args[5] = (file name) - name of output file (ex: "preprocess.txt")
	 *   Note: STATE and ("block"|"tract") will be prepended
	 * args[6] = ("true"|"false") - true if the data is census blocks; false for census tracts
	 * args[7] = two letter state ID (ex: "hi" for Hawaii)
	 */
	private static void readParams(String args[]) {
		if(args.length == 9) {
			ACTION = args[0];
			DOC_ROOT = args[1];
			dataFilePath = args[2];
			shapeFilePath = args[3];
			popFilePath = args[4];
			PRE_PROCESS_FILE = args[5];
			IS_BLOCK = args[6].equals("true");
			STATE = args[7];
			MAX_NEIGHBOR = Integer.parseInt(args[8]);
		}else {
			System.out.println("WARNING: using defaults params");
		}
		
		if(IS_BLOCK) {
			BLOCK_STRING = "block";
		}else {
			BLOCK_STRING = "tract";
		}
	}
	
	/**
	 * Reads previously processed file. The output can be used in any type of redistricting algorithm.
	 * @return list of population units using the levin.unit class
	 */
	public static ArrayList<Unit> readPreProcess() {
		ReadPreProcess r = new ReadPreProcess(DOC_ROOT, dataFilePath, PRE_PROCESS_FILE);
		return r.readFile();
	}
	
	/**
	 * Reads data from parameters and write processed data out to a text file.
	 */
	public static void writePreProcess() {
		Read r = new Read(DOC_ROOT, dataFilePath, shapeFilePath, popFilePath, IS_BLOCK, MAX_NEIGHBOR);
		HashMap<String, Unit> units = r.getUnits();
		BufferedWriter writer = null;
	    try {
	    		//create file ex:/Users/Harry/Desktop/Archive/data/TEST_TRACT/hi-tract-preprocess.txt
	    		File logFile = new File(DOC_ROOT + dataFilePath + STATE + "-" + BLOCK_STRING + "-" +  PRE_PROCESS_FILE);
	    		System.out.println(logFile.getCanonicalPath());
	
	    		writer = new BufferedWriter(new FileWriter(logFile));
	    		int totalPop = 0;
	    		int numUnits = 0;
	    		for(Unit u: units.values()) {
	    			writer.write(u.toString() + "\n");
	    			numUnits ++;
	    			totalPop += u.getPopulation();
	    		}
	    		System.out.println("Num Units: " + numUnits);
	    		System.out.println("totalPop: " + totalPop);
	    } catch (Exception e) {
	        e.printStackTrace();
	    } finally {
	        try {
	            // Close the writer regardless
	            writer.close();
	        } catch (Exception e) {
		        e.printStackTrace();
	        }
	    }
	}
}
