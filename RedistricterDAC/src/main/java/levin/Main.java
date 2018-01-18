package levin;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

import levin.geom.utils.MultiPolygonFlatener;
import levin.kdtree.DistanceFunction;
import levin.kdtree.SquareEuclideanDistanceFunction;
import levin.preprocess.ReadPreProcess;
import levin.printout.ErrorLog;
import levin.printout.Logger;
import levin.printout.Messenger;

public class Main {

	public static boolean DEBUG = false;
	private static String STATE = "hi";
	public static boolean IS_BLOCK = true;
	private static boolean SWAPS = true;
	private static boolean CONTIG = true;
	private static String MAX_FUNCTION = "pop-contig";
	private static String REF_FUNCTION = "line";//"point" or "line"
	private static String BLOCK_STRING = "block";
	private static int TOTAL_POPULATION = 0;
	private static long START_TIME = System.currentTimeMillis();
	private static String LABEL = "DEFAULT-LABEL";
	private static int k =10;
	
	public static String DOC_ROOT = "/Users/Harry/Desktop/Archive/data";
	static String dataFilePath = "/TEST/";
	static String outputDocRoot = "/Users/Harry/Desktop/Archive/results";
	static String PRE_PROCESS_FILE = "hi-block-preprocess.txt";
	private static String csvFilePath = STATE + ".csv";
	/**
	 * This is the marker for units that were manually added to make the whole state contiguous. 
	 * They should always have population=0
	 */
	private static double[][] defaultSearchPoints;
	
	private static HashMap<String, Unit> UNIT_MAP = new HashMap<String, Unit>();
	
	public static void main(String[] args) {
		if(args.length >0 && !validateParams(args)){
			ErrorLog.log("Error with one or more parameters");
		}
		readParams(args);
		
		District stateWideDistrict = readPreProcess();
		TOTAL_POPULATION = stateWideDistrict.getDistrictPopulation();
		defaultSearchPoints = getDefaultSearchPoints(stateWideDistrict.getGeometry());
		DistrictList finalDistricts = divideAndConquer(k, stateWideDistrict);
		double durationInSeconds = (System.currentTimeMillis() - START_TIME)/1000.0;
		printResults(finalDistricts, "final-districts", durationInSeconds);
		printBlockAssignmentList(UNIT_MAP.values(), finalDistricts);
		Messenger.log("DONE :-)");
	}
	
	/**
	 * This program takes 8 parameters that can be configured to override the default values that
	 * are listed in the global variables.
	 * @param args
	 * args[0] = state (ex: "hi")
	 *   interprets a previously written text file for input into an algorithm
	 * args[1] = k (ex: 2)
	 * args[3] = (file path) - root directory of input files (ex: "/Users/Harry/Desktop/Archive/data")
	 * args[4] = (file path) - root directory of output files (ex: "/Users/Harry/Desktop/Archive/results")
	 * args[5] = ("true"|"false") - true if debug mode is on; false otherwise
	 * args[6] = ("true"|"false") - true if the data is census blocks; false for census tracts
	 * args[7] = two letter state ID (ex: "hi" for Hawaii)
	 * args[8] = ("true"|"false") - true if to run swap component; false to turn off
	 * args[9] = ("pop"|"contig") - pop to minimize pop deviation; contig to maximize compactness
	 * args[10] = String text to add to output files to uniquely identify test runs
	 */
	private static void readParams(String args[]) {
		if(args.length == 11) {
			STATE = args[0];
			k = Integer.parseInt(args[1]);
			DOC_ROOT = args[2];
			dataFilePath = args[3];
			outputDocRoot = args[4];
			DEBUG = args[5].equals("true");
			IS_BLOCK = args[6].equals("block");
			SWAPS = args[7].equals("true");
			CONTIG = args[8].equals("true");
			MAX_FUNCTION = args[9];
			PRE_PROCESS_FILE = args[0] + "-" + args[6] + "-preprocess.txt";

			LABEL = args[10];
			
			//derived values
			csvFilePath = STATE + "-csv.csv";
			Logger.setDebugFlag(DEBUG);
			START_TIME = System.currentTimeMillis();
		}else {
			System.out.println("WARNING: using defaults params");
		}
		
		if(IS_BLOCK) {
			BLOCK_STRING = "block";
		}else {
			BLOCK_STRING = "tract";
		}
	}
	
	private static District readPreProcess() {
		ReadPreProcess r = new ReadPreProcess(DOC_ROOT, dataFilePath, PRE_PROCESS_FILE);
		ArrayList<Unit> units = r.readFile();
		District stateWideDistrict = new StateWideDistrict(STATE, DOC_ROOT);
		for (Unit u : units) {
			UNIT_MAP.put(u.getId(), u);
			stateWideDistrict.add(u);
		}
		return stateWideDistrict;
	}
	
	private static DistrictList divideAndConquer(int numDistrictsLeft, District d){
		Messenger.log("Districts Left: " + numDistrictsLeft + " , totalPop= " + d.getDistrictPopulation() + ", idealPop= " +  d.getDistrictPopulation()/numDistrictsLeft);
		if(numDistrictsLeft == 4) {
			System.out.println("here");
		}
		if(numDistrictsLeft == 1){
			//base case 1
			return new  DistrictList(d);
		}else if(numDistrictsLeft == 2){
			//base case 2
			return runWithSearchPoints(d, d.getDistrictPopulation()/2, false);
		}else if (numDistrictsLeft%2 != 0){
			//recursion
			DistrictList oddRecursionList = runWithSearchPoints(d, d.getDistrictPopulation()/numDistrictsLeft, false);
			DistrictList left = divideAndConquer(1, oddRecursionList.getDistrict(0));
			DistrictList right = divideAndConquer(numDistrictsLeft-1, oddRecursionList.getDistrict(1) );
			return merge(left.getDistrictList(), right.getDistrictList());
		}else if (numDistrictsLeft%2 == 0){
			//recursion
			DistrictList evenRecursionList = runWithSearchPoints(d, d.getDistrictPopulation()/2, false);
			DistrictList left =  divideAndConquer(numDistrictsLeft/2, evenRecursionList.getDistrict(0));
			DistrictList right = divideAndConquer(numDistrictsLeft/2, evenRecursionList.getDistrict(1) );
			return merge(left.getDistrictList(), right.getDistrictList());
		}else{
			ErrorLog.log("Invalid value for numDistrictsLeft: " + numDistrictsLeft);
			return null;
		}
	}
	
	private static DistrictList merge (District[] left, District[] right){
		   int leftLen = left.length;
		   int rightLen = right.length;
		   District[] merged= new District[leftLen+rightLen];
		   System.arraycopy(left, 0, merged, 0, leftLen);
		   System.arraycopy(right, 0, merged, leftLen, rightLen);
		   return new DistrictList(merged);

	}
	
	/**
	 * Return true if the district plan has a lower score than current best score
	 * @param districts
	 * @param bestScore
	 * @param idealPop
	 * @return
	 */
	private static boolean isBestDistrict(DistrictList districts, double bestScore, int idealPop) {
		double score = Double.MAX_VALUE;
		if(MAX_FUNCTION.equals("pop")) {
			score = Math.abs(districts.getDeviationPercentage(idealPop));
		}else if(MAX_FUNCTION.equals("contig")) {
			score = Math.abs(districts.getSimplePlanCompactnessScore());

		}else if(MAX_FUNCTION.equals("pop-contig")) {
			double pop_score = Math.abs(districts.getDeviationPercentage(idealPop));
			double contig_score = Math.abs(districts.getSimplePlanCompactnessScore());
			score = 0.5 * pop_score + 0.5 * contig_score;
		} else{
			ErrorLog.log("invalid MAX_FUNCTION flag. Must be 'pop', 'pop-contig' or 'contig'. Found: " + MAX_FUNCTION);
		}
		return (bestScore > score);
	}
	
	private static DistrictList runWithSearchPoints(District d, int idealPop, boolean maxOptimize){
		double bestScore=Double.MAX_VALUE;
		DistrictList bestDistricts = null;
		int index=0;
		int bestIndex=0;
		for(int i = 0; i< defaultSearchPoints.length; i++){
			DistrictList districts = redistrict(d, idealPop, i, maxOptimize);
			Messenger.log("\tSimpleCompactness:" + districts.getSimplePlanCompactnessScore());
			Messenger.log("\tPop0:" + districts.getDistrict(0).getDistrictPopulation() +
					"Pop1:" + districts.getDistrict(1).getDistrictPopulation());
			Messenger.log("\t" + districts.getDistrict(0).getGeometry().toText());
			Messenger.log("\t" + districts.getDistrict(1).getGeometry().toText());
			if(isBestDistrict(districts, bestScore, idealPop) && validateDistrictList(districts, idealPop, d).hasSuccessCode() ) {
				bestDistricts = districts;
				bestScore = Math.abs(districts.getDeviationPercentage(idealPop));
				bestIndex=index;
			}
			index++;
		}

		Logger.log("-----------Best-------------" + bestIndex);
		Messenger.log("\tSimpleCompactness:" + bestDistricts.getSimplePlanCompactnessScore());
		Messenger.log("\tBest0:" + bestDistricts.getDistrict(0).getDistrictPopulation() + " \n\t Best1: "
				+ bestDistricts.getDistrict(1).getDistrictPopulation());
		Logger.log("DIFF: " + (bestDistricts.getDistrict(0).getDistrictPopulation() - bestDistricts.getDistrict(1).getDistrictPopulation()));
		Messenger.log("\t" + bestDistricts.getDistrict(0).getGeometry().toText());
		Messenger.log("\t" + bestDistricts.getDistrict(1).getGeometry().toText());

		/*
		 * For speed we will run a light version of the optimize in which we only
		 * swap one level of adjacent units. If we can't get the optimal deviation
		 * then we will run the full version of optimize, which can swap an infinite
		 * number of units until the population levels are equal. 
		 */
//		if(!maxOptimize && bestDistricts.getFirstDistrictDev(idealPop) > 1){
//			Messenger.log("********turning on maxOptimize");
//			bestDistricts = runWithSearchPoints(d, idealPop, true);
//		}
		
		return bestDistricts;
	}
	
	private static Unit getClosestUnit(int refPointIndex, DistanceFunction d, ArrayList<Unit> units, ArrayList<String> ids) {
		Unit result = null;
		double distance = Integer.MAX_VALUE;
		//for first unit find the closest one
		if(units != null) {
			for(Unit u: units) {
				double[] centroid = { u.centroid.getX(), u.centroid.getY()};
				double[] seed = setRefPointForDistanceCalc(refPointIndex, centroid);
				double dis = d.distance(seed, centroid);
				if(dis < distance) {
					result = u;
					distance = dis;
				}
			}
		} 
		//Similar code if we're looking up id
		else if(ids != null) {
			for(String id: ids) {
				Unit u = UNIT_MAP.get(id);
				double[] centroid = { u.centroid.getX(), u.centroid.getY()};
				double[] seed = setRefPointForDistanceCalc(refPointIndex, centroid);
				double dis = d.distance(seed, centroid);
				if(dis < distance) {
					result = u;
					distance = dis;
				}
			}
		}
		return result;
	}
	
	/**
	 * Return reference point that will calculate closest to a line of closest to a point based on config param
	 * @param index
	 * @param centroid
	 * @return
	 */
	private static double[] setRefPointForDistanceCalc (int index, double[] centroid) {
		double [] result = {-1.0,-1.0};
		if(REF_FUNCTION.equals("line")) {
			if(index == 0 || index == 3) {//lines y=#number
				result[0] = centroid[0];
				result[1] = defaultSearchPoints[index][1];
			}else if(index==1 || index ==2) {//lines x=#number
				result[0] = defaultSearchPoints[index][0];
				result[1] = centroid[1];
			}else {
				ErrorLog.log("invalid reference point index:" + index);
			}
		}else if (REF_FUNCTION.equals("point")) {
			result = defaultSearchPoints[index];
		}else {
			ErrorLog.log("invalid ref_function:" + REF_FUNCTION);			
		}
		return result;
	}
	
	private static void addToNeighbors(Unit u, DistrictList d, ArrayList<String> neighbors, District oldDistrict) {
		for(String id: u.getNeighbors()) {
			boolean is_member = false;
			for(Unit member: d.getDistrict(0).getMembers()) {
				if(member.getId().equals(id)) {
					is_member = true;
				}
			}
			for(Unit member: d.getDistrict(1).getMembers()) {
				if(member.getId().equals(id)) {
					is_member = true;
				}
			}
			if(UNIT_MAP.get(id) == null) {
				ErrorLog.log("Invalid id: " + id + " for unit: " + u.getId());
			}
			if(!is_member && !neighbors.contains(id) && oldDistrict.contains(UNIT_MAP.get(id))) {
				neighbors.add(id);
			}
		}
	}
	
	private static void addUnitToDistrict(Unit u, DistrictList districts, int index, ArrayList<String> neighbors, ArrayList<String> other, District oldDistrict) {
		int otherIndex = (index == 0)? 1: 0;
		if(districts.getDistrict(otherIndex).contains(u)) {
			ErrorLog.log("trying to assign unit twice!: " + u.getId());
		}
		districts.getDistrict(index).add(u);
		neighbors.remove(u.getId());//need to track added units and not add them back to neighbors
		other.remove(u.getId());//need to track added units and not add them back to neighbors
		addToNeighbors(u, districts, neighbors, oldDistrict);
	}
		
	private static DistrictList redistrict(District district, int idealPop, int refPointIndex, boolean optimizeMax){

		Messenger.log("");
		Messenger.log("\tUsing searchPoint: " + defaultSearchPoints[refPointIndex][0] + " , " + defaultSearchPoints[refPointIndex][1] );
		Messenger.log("\tidealPop=" + idealPop); 
		DistrictList districts = new DistrictList(2);
		DistanceFunction d = new SquareEuclideanDistanceFunction();
		ArrayList<String> neighborsD0 = new ArrayList<String>();
		ArrayList<String> neighborsD1 = new ArrayList<String>();

		//add first unit
		Unit next = getClosestUnit(refPointIndex, d, district.getMembers(), null);
		addUnitToDistrict(next, districts, 0, neighborsD0, neighborsD1, district);
		boolean flipped_districts = false;
		int districtPop = districts.getDistrict(0).getDistrictPopulation();

		while(Math.abs(districtPop - idealPop) >= Math.abs(districtPop + next.getPopulation() - idealPop)){			
			ArrayList<String> neighbors = (!flipped_districts && neighborsD0.size() > 0 ) ? neighborsD0: neighborsD1;
			next = getClosestUnit(refPointIndex, d, null, neighbors);
			districtPop = districts.getDistrict(0).getDistrictPopulation();
				addUnitToDistrict(next, districts, 0, neighborsD0, neighborsD1, district);
		}
		//Assign anything left over that we missed to the other district
		 assignSkippedDistricts(districts, district);
		//Fix non-contiguity if we can
		if(CONTIG){
			Logger.log("MultiPolygonFlatener process starting");
			MultiPolygonFlatener mpf = new MultiPolygonFlatener(districts);
			Logger.log("made changes: " + mpf.hasChanged());
			if(mpf.hasChanged()){
				districts = mpf.getNewDistrictList();
			}
			Logger.log("Flattener Test: " );
			Logger.log(String.valueOf(districts.getDistrict(0).getGeometry().toText().contains("MULTIPOLYGON")));
			Logger.log(String.valueOf(districts.getDistrict(0).getGeometry().toText().contains("MULTIPOLYGON")));
			Logger.log(districts.getDistrict(0).getGeometry().toText());
			Logger.log(districts.getDistrict(1).getGeometry().toText());
			Logger.log("Deviation: " + districts.getDeviation(idealPop));
		}
		if(SWAPS){
			int lastDeviation = Integer.MAX_VALUE;
			Logger.log("starting optimize loop");
			while(districts.getFirstDistrictDev(idealPop) > 1 && districts.getFirstDistrictDev(idealPop) < lastDeviation){
				Logger.log(districts.getFirstDistrictDev(idealPop) +  "> 1 && " +  "<" + lastDeviation);
				lastDeviation = districts.getFirstDistrictDev(idealPop);
				optimizePopulation(districts, idealPop);
				
				/*
				 * Only run optimize once unless we set flag for maxOptimize
				 */
				if(!optimizeMax){
					break;
				}
			}
			Logger.log("end optimize loop");
		}
		double devPercentage = districts.getDeviationPercentage(idealPop);
		Messenger.log("\tDeviation: " + districts.getDeviation(idealPop) + " people = " + ((double)Math.round(devPercentage * 1000)/1000) + "%");
		
		Logger.log(String.valueOf(districts.getDistrict(1).getSkippedUnits().size()));
		Logger.log(districts.getDistrict(1).getGeometry().toText());
		Logger.log(districts.getDistrict(0).getGeometry().toText());
		Logger.log("Is multipolygon: " + districts.getDistrict(0).getGeometry().toText().contains("MULTIPOLYGON"));
		Logger.log("---------OUTPUTING-------");
		Logger.log(districts.getDistrict(0).getDistrictPopulation() + " \n"
				+ districts.getDistrict(1).getDistrictPopulation());
		Logger.log("DIFF: " + (districts.getDistrict(0).getDistrictPopulation() - districts.getDistrict(1).getDistrictPopulation()));
		return districts;
	}
	
	public static void assignSkippedDistricts(DistrictList districts, District oldDistrict){
		for(Unit u: oldDistrict.getMembers()) {
			if(!districts.getDistrict(0).contains(u) && !districts.getDistrict(1).contains(u)) {
				districts.getDistrict(1).add(u);
			}
		}
	
	}
	
	private static void optimizePopulation(DistrictList districts, int idealPop) {
		Logger.log("starting optimize pop");
		Unit bestSwap = null;
		ArrayList<Unit> swappablesD1 = getSwappabe(districts.getDistrict(1), districts.getDistrict(0));
		ArrayList<Unit> swappablesD2 = getSwappabe(districts.getDistrict(0), districts.getDistrict(1));
		while(swappablesD1.size() > 0 && swappablesD2.size() > 0){
			Logger.log("sizes: " + swappablesD1.size() + " && "  + swappablesD2.size() );
			if(districts.getDistrict(0).getDistrictPopulation() > idealPop){
				//negative dev because we need to remove population
				int currentDev = idealPop - districts.getDistrict(0).getDistrictPopulation();
				Logger.log("0 > ideal: currentDev = " + currentDev);
				bestSwap = getBestSwappable(swappablesD1, currentDev, districts.getDistrict(1), districts.getDistrict(0));
			}else if (districts.getDistrict(0).getDistrictPopulation() < idealPop){
				int currentDev = districts.getDistrict(0).getDistrictPopulation() - idealPop;
				Logger.log("1 > ideal: currentDev = " + currentDev);
				bestSwap = getBestSwappable(swappablesD2, currentDev, districts.getDistrict(0), districts.getDistrict(1));
			}else{
				Messenger.log("\tPERFECT DIVISION! :-)");
				break;
			}
			
			if(bestSwap != null){
				Logger.log("bestSwap= " + bestSwap.getId());
				districts.swap(bestSwap, true);
			}else{
				//we didn't find any good units to swap so we're done
				break;
			}
		}
	}
	
	private static ArrayList<Unit> getSwappabe(District from, District to){
		ArrayList<Unit> result = new ArrayList<Unit>();
		for(Unit u: to.getMembers()){
			//Check deviation is still valid, receiving district is still contiguous and from district is still contiguous
			if(u.getGeometry().touches(from.getGeometry())
					&& !u.getGeometry().union(from.getGeometry()).toText().contains("MULTIPOLYGON")
					&& !to.getGeometry().difference(u.getGeometry()).toText().contains("MULTIPOLYGON")){
				//Last case eliminates single point contiguity
				result.add(u);
			}
		}
		return result;
	}
	
	private static Unit getBestSwappable(ArrayList<Unit> swappables, int currentDev, District from, District to){
		Unit bestUnit = null;
		int bestDeviation = currentDev;
		for(Unit u: swappables){
			int newDev = currentDev + u.getPopulation();
			if(Math.abs(newDev) < Math.abs(bestDeviation)
					&& !u.getGeometry().union(from.getGeometry()).toText().contains("MULTIPOLYGON")
					&& !to.getGeometry().difference(u.getGeometry()).toText().contains("MULTIPOLYGON")){
				bestUnit = u;
				bestDeviation = newDev;
			}
		}
		swappables.remove(bestUnit);
		return bestUnit;
	}

	private static DistrictValidation validateDistrictList(DistrictList districts, int idealPop, District originalDistrict){
		DistrictValidation valid = new DistrictValidation();
		int totalPop = originalDistrict.getDistrictPopulation();
		int numUnits = originalDistrict.getMembers().size();
		
		int totalUnitsAssigned=0;
		int totalPopulationAssigned=0;
		ArrayList<Unit> allMembers = new ArrayList<Unit>();
		for(District d: districts.getDistrictList()){
			validateDistrict(d, idealPop, valid);
			totalUnitsAssigned += d.getMembers().size();
			totalPopulationAssigned += d.getDistrictPopulation();
			allMembers.addAll(d.getMembers());
		}
		
		if(totalPop != totalPopulationAssigned){
			int originalSize = districts.getDistrict(0).getMembers().size();
			districts.getDistrict(0).getMembers().removeAll(districts.getDistrict(1).getMembers());
			int newSize = districts.getDistrict(0).getMembers().size();
//			if(originalSize != newSize) {
//				System.out.println("SIZES NOT SAME");
//				System.exit(0);
//			}
////			for(Unit u1: districts.getDistrict(0).getMembers()) {
////				System.out.println(u1.getId());
////
////			}
////			for(Unit u2: districts.getDistrict(1).getMembers()) {
////				System.out.println("now #2");
////				System.out.println(u2.getId());
////
////			}
			valid.setPopulationCorruptedFlag(totalPop, totalPopulationAssigned);
		}
		
		if(numUnits != totalUnitsAssigned){
			ArrayList<Unit> missingUnits = new ArrayList<Unit>();
			for(Unit u1: originalDistrict.getMembers()) {
				boolean contains = false;
				for(Unit u2: allMembers) {
					if(u1.getId() == u2.getId()) {
						contains =  true;
					}
				}
				
				if(!contains) {
					missingUnits.add(u1);
				}
			}
			valid.setUnassignedUnitsFlag(missingUnits);
		}
		Messenger.log(valid.toString());
		return valid;
	}
	
	private static void validateDistrict(District d, int idealPop, DistrictValidation valid){
		//Contiguity test
		if(d.getGeometry().toText().contains("MULTIPOLYGON")){
			valid.setNonContiguousFlag(d.getGeometry());
		}
		
		//TODO deviation test
	}
	
	private static double[][]getDefaultSearchPoints(Geometry stateWideGeom){
		double[][] result = new double[4][2];
		
		Coordinate[] coords = stateWideGeom.getEnvelope().getCoordinates();
		//The first and last points are the same so ignore the last point
		for(int i=0; i<coords.length-1; i++){
			Coordinate c = coords[i];
			result[i][0] = c.y;
			result[i][1] = c.x;
		}
		return result;
	}
	
	private static boolean validateParams(String[] args){
		boolean result = true;
		String validStates = "al,ak,az,ar,ca,co,ct,de,fl,ga,hi,id,il,in,io,ks,ky,la,me,md,ma,mi,mn,ms,mo,mt,ne,nv,nh,nj,nm,ny,nc,nd,oh,ok,or,pa,ri,sc,sd,tn,tx,ut,vt,va,wa,wv,wi,wy";
		//Validate state
		if(!validStates.contains(args[0])) {
			result = false;
			Messenger.log("Invalid state:" + args[0] + "; Not in list:" + validStates);
		}
		//Validate k (positive integer)
		try {
			int kINT = Integer.parseInt(args[1]);
			if(kINT <=0) {
				result = false;
				Messenger.log("Invalid k:" + args[1] + "; must be greater than 0");
			}
		}catch(NumberFormatException e) {
			Messenger.log("Invalid k:" + args[1] + "; must be an integer");
			result = false;
		}

		if(!args[5].equals("true") && !args[5].equals("false")) {
			result = false;
			Messenger.log("Invalid DEBUG:" + args[5] + "; must be boolean");
		}
		if(!args[6].equals("block") && !args[6].equals("tract")) {
			result = false;
			Messenger.log("Invalid IS_BLOCK:" + args[6] + "; must be 'block' or 'tract'");
		}
		if(!args[7].equals("true") && !args[7].equals("false")) {
			result = false;
			Messenger.log("Invalid SWAPS:" + args[7] + "; must be boolean");
		}
		if(!args[8].equals("true") && !args[8].equals("false")) {
			result = false;
			Messenger.log("Invalid CONTIG:" + args[8] + "; must be boolean");
		}
		if(!args[9].equals("pop") && !args[9].equals("contig") && !args[9].equals("pop-contig")) {
			result = false;
			Messenger.log("Invalid MAX_FUNCTION:" + args[9] + "; must be 'pop' or 'contig'");
		}
		
		String fips = CompactnessCalculator.getFIPSString(args[0]);
		File stateShapeFile = new File(args[2] + "/tl_2010_" + fips + "_state10/tl_2010_" + fips+ "_state10.shp");
		File actualDistrictsShapeFile = new File(args[2] + "/2012Congress/2012Congress.shp");
		File preProcessFile = new File(args[2] + args[3] + args[0] + "-" + args[6] + "-preprocess.txt");
		File outputFie = new File(args[4]);
		
		if(!stateShapeFile.exists()) {
			result = false;
			Messenger.log("State Shape File doesn't exist:" + stateShapeFile.getAbsolutePath());

		}
		if(!actualDistrictsShapeFile.exists()) {
			result = false;
			Messenger.log("Actual District Shape File doesn't exist:" + actualDistrictsShapeFile.getAbsolutePath());

		}
		if(!preProcessFile.exists()) {
			result = false;
			Messenger.log("Pre Process File doesn't exist:" + preProcessFile.getAbsolutePath());

		}
		if(!outputFie.exists()) {
			result = false;
			Messenger.log("Output Directory doesn't exist:" + outputFie.getAbsolutePath());

		}
		
		return result;
	}
	
	private static String printResults(DistrictList districts, String label, double durationInSecnds) {
		double devPercentage = districts.getDeviationPercentage(TOTAL_POPULATION/k);
				//Write.write(STATE + "-shapedata.csv", finalDistricts.toString());
		CompactnessCalculator calculator = new CompactnessCalculator(DOC_ROOT, districts, STATE);
		//Messenger.log(calculator.toString());
		
		//"block-swaps,hi,2,0.005%,0.8,1.0,0.8,1.0,0.8,1.0"
		Date d = new Date();
		String result = d.toGMTString() + "," + LABEL + "," + label + "," + STATE + "," + k + "," + durationInSecnds + "," +  districts.getDeviation(TOTAL_POPULATION/k) + "," + devPercentage + "," + 
				calculator.statOurConvexHull.getMean() + "," + calculator.statCongConvexHull.getStandardDeviation() + "," +
				calculator.statOurPolsbyPopper.getMean() + "," + calculator.statOurPolsbyPopper.getStandardDeviation() + "," +
				calculator.statOurModifiedSchwartzberg.getMean() + "," + calculator.statOurModifiedSchwartzberg.getStandardDeviation() ;
		Messenger.log("Duration:" + durationInSecnds + " seconds");
		Messenger.log(districts.toString());
		Messenger.log("-----------------FINAL DISTRICTS---------------------");
		Messenger.log("FINAL Deviation Percentage:" + districts.getDeviation(TOTAL_POPULATION/k) + " people=" + devPercentage + "%");
		Messenger.log("FINAL Compactness:" + districts.getSimplePlanCompactnessScore());
		Messenger.log(calculator.toString());

		FileWriter fw = null;
		BufferedWriter bw = null;
		PrintWriter out = null;
		try {
			fw = new FileWriter(outputDocRoot + "/RESULTS.csv", true);
			bw = new BufferedWriter(fw);
			out = new PrintWriter(bw);
			out.println(result);
		}catch(IOException e) {
			e.printStackTrace();
			System.exit(0);
		}finally {
			try {
				out.close();
				bw.close();
				fw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return result;
	}
	
	private static void printBlockAssignmentList(Collection<Unit> rawUnits, DistrictList districts){
		String fileName="BlockAssign_ST_" + CompactnessCalculator.getFIPSString(STATE) + "_" + STATE + "_" + BLOCK_STRING  + "_" + LABEL + "_CD.txt";
		int index=1;//start at 1
		
		FileWriter fw = null;
		BufferedWriter bw = null;
		PrintWriter out = null;
		try {
			fw = new FileWriter(outputDocRoot + "/" + fileName, true);
			bw = new BufferedWriter(fw);
			out = new PrintWriter(bw);
			
			for(District d : districts.getDistrictList()){
				for(Unit u: rawUnits){
					if(u.getDistrictAssignment() == -1 &&
							d.containsId(u.getId())){
						u.setDistrictAssignment(index);
					}
				}
				index++;
			}
			
			for(Unit u: rawUnits){
				//Pieces we've merged have to be separated again
				if(u.getId().contains(",")){
					for(String piece: u.getId().split(",")){
						if(u.getDistrictAssignment() <1 && u.getDistrictAssignment() > k) {
							ErrorLog.log("invalid district assignment: " + u.getId() + "->" + u.getDistrictAssignment());
						}
						out.write(piece + "," + u.getDistrictAssignment() + "/n");

					}
				}else{
					if(u.getDistrictAssignment() <1 && u.getDistrictAssignment() > k) {
						ErrorLog.log("invalid district assignment: " + u.getId() + "->" + u.getDistrictAssignment());
					}
					out.write(u.getId() + "," + u.getDistrictAssignment() + "/n");
				}
			}
		}catch(IOException e) {
			e.printStackTrace();
			System.exit(0);
		}finally {
			try {
				out.close();
				bw.close();
				fw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}