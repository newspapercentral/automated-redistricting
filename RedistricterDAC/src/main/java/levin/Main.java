package levin;


import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

import levin.geom.utils.MultiPolygonFlatener;
import levin.kdtree.DistanceFunction;
import levin.kdtree.KdTree;
import levin.kdtree.NearestNeighborIterator;
import levin.kdtree.SquareEuclideanDistanceFunction;
import levin.preprocess.PreProcess;
import levin.printout.ErrorLog;
import levin.printout.Logger;
import levin.printout.Messenger;

public class Main {

	public static boolean DEBUG;
	private static String STATE;
	public static boolean IS_BLOCK;
	private static boolean SWAPS;
	private static boolean CONTIG = true;
	private static String MAX_FUNCTION;
	private static int k;
	
	public static String DOC_ROOT;
	private static String dataFilePath;
	private static String shapeFilePath;
	private static String popFilePath;
	private static String csvFilePath;
	/**
	 * This is the marker for units that were manually added to make the whole state contiguous. 
	 * They should always have population=0
	 */
	public static String ADDED_UNIT_ID = "13";
	private static double[][] defaultSearchPoints;
	
	private static HashMap<String, Unit> UNIT_MAP = new HashMap<String, Unit>();
	
	public static void main(String[] args) {
		Messenger.log("Processing with " + args.length + " args");
		if(args.length > 3){
			STATE = args[0];
			k = Integer.parseInt(args[1]);
			DOC_ROOT = args[2];
			DEBUG = args[3].equals("true");
			Logger.setDebugFlag(DEBUG);
			IS_BLOCK = args[4].equals("block");
			SWAPS = args[5].equals("true");
			if(args.length > 7){
				CONTIG = args[6].equals("true");
				MAX_FUNCTION = args[7];
			}
			if(IS_BLOCK){
				dataFilePath = "/tabblock2010_" + CompactnessCalculator.getFIPSString(STATE) + "_pophu/";
				shapeFilePath = "tabblock2010_" + CompactnessCalculator.getFIPSString(STATE) + "_pophu.shp";
				popFilePath = "";
			}else{
				dataFilePath = "/tl_2010_" + CompactnessCalculator.getFIPSString(STATE) + "_tract10/";
				shapeFilePath = "tl_2010_" + CompactnessCalculator.getFIPSString(STATE) + "_tract10.shp";
				popFilePath = "tract-pop.txt";

			}
			csvFilePath = STATE + "-csv.csv";
			Messenger.log("STATE=" + STATE + ", k=" + k + " , DOC_ROOT=" + DOC_ROOT + " , DEBUG=" + DEBUG
					+ "SWAPS= " + SWAPS
					+ "dataFilePath= " + dataFilePath
					+ "shapeFilePath= " + shapeFilePath
					+ "popFilePath= " + popFilePath
					+ "IS_BLOCK" + IS_BLOCK);
	
		}else{
			Logger.log("USSAGE: RedistricterDAC STATE #districts dataFilePath DEBUG [block/tract] SWAPS[true/false]");
			Logger.log("Example: RedistricterDAC nh 2 /Users/Harry/Desktop/tabblock2010_33_pophu/tabblock2010_33_pophu.shp false block true");
			Logger.log("Running with default values");
			STATE = "hi";
			k = 2;
			DOC_ROOT = "/Users/Harry/Desktop/Archive/data";
			DEBUG = false;
			Logger.setDebugFlag(DEBUG);
			IS_BLOCK=true;
			SWAPS = true;
			dataFilePath = "/TEST/";
			shapeFilePath = "TEST.shp";
			popFilePath = "tract-pop.txt";
			csvFilePath = STATE + "-csv.csv";
			Messenger.log("STATE=" + STATE + ", k=" + k + " , DOC_ROOT=" + DOC_ROOT 
					+ " , DEBUG=" + DEBUG
					+ " , IS_BLOCK = " + IS_BLOCK
					+ " , SWAPS= " + SWAPS);
			
		}
		if(!validateRequirements()){
			//ErrorLog.log("One or more expected data files does not exist");
		}
		District stateWideDistrict = readPreProcess();
		defaultSearchPoints = getDefaultSearchPoints(stateWideDistrict.getGeometry());
		DistrictList finalDistricts = divideAndConquer(k, stateWideDistrict);
		Messenger.log("-----------------FINAL DISTRICTS---------------------");
		double devPercentage = finalDistricts.getDeviationPercentage(stateWideDistrict.getDistrictPopulation()/k);
		Messenger.log("FINAL Deviation Percentage:" + finalDistricts.getDeviation(stateWideDistrict.getDistrictPopulation()/k) + " people=" + devPercentage + "%");
		//TODO
		//Write.write(STATE + "-shapedata.csv", finalDistricts.toString());
		CompactnessCalculator calculator = new CompactnessCalculator(DOC_ROOT, finalDistricts, STATE);
		Messenger.log(calculator.toString());
//TODO 
		//		String[] blockAssignmentData = printBlockAssignmentList(r.getRawUnits(), finalDistricts);
		//TODO
		//Write.write(blockAssignmentData[0], blockAssignmentData[1]);
		Messenger.log("DONE :-)");
	}
	
	private static District readPreProcess() {
		String blockString = (IS_BLOCK) ? "block" : "tract"; 
		String preprocessFile = STATE + "-" + blockString + "-preprocess.txt";
		PreProcess pp = new PreProcess(DOC_ROOT, dataFilePath, shapeFilePath, popFilePath, 
				preprocessFile, IS_BLOCK, STATE);	
		ArrayList<Unit> units = pp.readPreProcess();
		District stateWideDistrict = new StateWideDistrict(STATE, DOC_ROOT);
		for (Unit u : units) {
			UNIT_MAP.put(u.getId(), u);
			stateWideDistrict.add(u);
		}
		return stateWideDistrict;
	}
	
	private static DistrictList divideAndConquer(int numDistrictsLeft, District d){
		Messenger.log("Districts Left: " + numDistrictsLeft + " , totalPop= " + d.getDistrictPopulation() + ", idealPop= " +  d.getDistrictPopulation()/numDistrictsLeft);
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
	
	private static DistrictList runWithSearchPoints(District d, int idealPop, boolean maxOptimize){
		int bestDeviation=Integer.MAX_VALUE;
		DistrictList bestDistricts = null;
		int index=0;
		int bestIndex=0;
		for(double[] searchPoint: defaultSearchPoints){
			Logger.log("Calling redistrict");
			DistrictList districts = redistrict(d, idealPop, searchPoint, maxOptimize);
			Logger.log("return from redistrict");
			Messenger.log("\tPop0:" + districts.getDistrict(0).getDistrictPopulation() +
					"Pop1:" + districts.getDistrict(1).getDistrictPopulation());
			if(Math.abs(districts.getDistrict(0).getDistrictPopulation() - idealPop) < bestDeviation && 
					validateDistrictList(districts, idealPop, d.getMembers().size(), d.getDistrictPopulation()).hasSuccessCode()){
				bestDistricts = districts;
				bestDeviation = Math.abs(districts.getDistrict(0).getDistrictPopulation() - idealPop);
				bestIndex=index;
			}else if(districts.getDeviation(idealPop)==bestDeviation && 
					validateDistrictList(districts, idealPop, d.getMembers().size(), d.getDistrictPopulation()).hasSuccessCode()){
				//If two district sets have the same deviation, pick the one with the 
				//smallest area + perimeter
				Logger.log("tie means check compactness");
				double proposedScore = districts.getAverageSimpleCompactnessScore();
				double bestScore = bestDistricts.getAverageSimpleCompactnessScore();
				
				proposedScore = districts.getAverageSimpleCompactnessScore();
				if(proposedScore < bestScore){
					bestDistricts = districts;
					bestDeviation = districts.getDeviation(idealPop);
					bestIndex=index;
				}
				
				
			}
			index++;
		}

		Logger.log("-----------Best-------------" + bestIndex);
		Logger.log(bestDistricts.getDistrict(1).getGeometry().toText());
		Logger.log(bestDistricts.getDistrict(0).getGeometry().toText());
		Messenger.log("\tBest0:" + bestDistricts.getDistrict(0).getDistrictPopulation() + " \n\t Best1: "
				+ bestDistricts.getDistrict(1).getDistrictPopulation());
		Logger.log("DIFF: " + (bestDistricts.getDistrict(0).getDistrictPopulation() - bestDistricts.getDistrict(1).getDistrictPopulation()));
		
		/*
		 * For speed we will run a light version of the optimize in which we only
		 * swap one level of adjacent units. If we can't get the optimal deviation
		 * then we will run the full version of optimize, which can swap an infinite
		 * number of units until the population levels are equal. 
		 */
		if(!maxOptimize && bestDistricts.getFirstDistrictDev(idealPop) > 1){
			Messenger.log("********turning on maxOptimize");
			bestDistricts = runWithSearchPoints(d, idealPop, true);
		}
		
		return bestDistricts;
	}
	
	private static Unit getClosestUnit(double[] seed, DistanceFunction d, ArrayList<Unit> units, ArrayList<String> ids) {
		Unit result = null;
		double distance = Integer.MAX_VALUE;
		//for first unit find the closest one
		if(units != null) {
			for(Unit u: units) {
				double[] centroid = { u.centroid.getX(), u.centroid.getY()};
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
				double dis = d.distance(seed, centroid);
				if(dis < distance) {
					result = u;
					distance = dis;
				}
			}
		}
		return result;
	}
	
	private static void addToNeighbors(Unit u, DistrictList d, ArrayList<String> neighbors) {
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
			if(!is_member && !neighbors.contains(id)) {
				neighbors.add(id);
			}
		}
	}
		
	private static DistrictList redistrict(District district, int idealPop, double[] searchPoint, boolean optimizeMax){

		Messenger.log("");
		Messenger.log("\tUsing searchPoint: " + searchPoint[0] + " , " + searchPoint[1] );
		Messenger.log("\tidealPop=" + idealPop); 
		DistrictList districts = new DistrictList(2);
		DistanceFunction d = new SquareEuclideanDistanceFunction();
		ArrayList<String> neighborsD0 = new ArrayList<String>();
		ArrayList<String> neighborsD1 = new ArrayList<String>();

		//add first unit
		Unit first = getClosestUnit(searchPoint, d, district.getMembers(), null);
		districts.getDistrict(0).add(first);
		neighborsD0.addAll(first.getNeighbors());
		boolean flipped_districts = false;

		while(districts.getDistrict(0).getMembers().size() + districts.getDistrict(1).getMembers().size() != district.getMembers().size()){			
			ArrayList<String> neighbors = (flipped_districts) ? neighborsD1: neighborsD0;
			Unit next = getClosestUnit(searchPoint, d, null, neighbors);
			//assign any remaining units for first district
			if(next == null && flipped_districts && neighborsD0.size()>0) {
				for(String s: neighborsD0) {
					districts.getDistrict(0).add(UNIT_MAP.get(s));
				}
				break;//done with this iteration
			}
			int districtPop = districts.getDistrict(0).getDistrictPopulation();
			if(!flipped_districts && Math.abs(districtPop - idealPop) >= Math.abs(districtPop + next.getPopulation() - idealPop) ){
				districts.getDistrict(0).add(next);
				neighborsD0.remove(next.getId());//need to track added units and not add them back to neighbors
				addToNeighbors(next, districts, neighborsD0);
				//TODO reset neighbors for d1 and see which ones are left over, assign these to d1
			}else{
				flipped_districts = true;
				//have to add this so it is assigned
				districts.getDistrict(1).add(next);
				neighborsD0.remove(next.getId());//need to track added units and not add them back to neighbors
				neighborsD1.remove(next.getId());//need to track added units and not add them back to neighbors
				addToNeighbors(next, districts, neighborsD1);

			}
		}
		//Assign anything left over that we missed
		districts.assignSkippedDistricts();
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

	private static DistrictValidation validateDistrictList(DistrictList districts, int idealPop, int numUnits, int totalPop){
		DistrictValidation valid = new DistrictValidation();
		
		int totalUnitsAssigned=0;
		int totalPopulationAssigned=0;
		for(District d: districts.getDistrictList()){
			validateDistrict(d, idealPop, valid);
			totalUnitsAssigned += d.getMembers().size();
			totalPopulationAssigned += d.getDistrictPopulation();
		}
		
		if(totalPop != totalPopulationAssigned){
			valid.setPopulationCorruptedFlag(totalPop, totalPopulationAssigned);
		}
		
		if(numUnits != totalUnitsAssigned){
			//TODO fix this later
			valid.setUnassignedUnitsFlag(new ArrayList<Unit>());
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
	
	private static KdTree<Unit> makeKdTree(ArrayList<Unit> units){
		KdTree<Unit> kd = new KdTree<Unit>(2);
		for(Unit u: units){
			Point centroid = u.getCentroid();
			double[] cen = {centroid.getY(), centroid.getX()};
		    kd.addPoint(cen, u);
		}
		return kd;
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
	
	private static boolean validateRequirements(){
		File stateShapeFile = new File(DOC_ROOT + "/tl_2010_" + CompactnessCalculator.getFIPSString(STATE) + "_state10/tl_2010_" + CompactnessCalculator.getFIPSString(STATE) + "_state10.shp");
		File actualDistrictsShapeFile = new File(DOC_ROOT + "/2012Congress/2012Congress.shp");
		File blockShapeFile = new File(DOC_ROOT + "/tabblock2010_" + CompactnessCalculator.getFIPSString(STATE) + "_pophu/tabblock2010_" + CompactnessCalculator.getFIPSString(STATE) + "_pophu.shp");
		
		Logger.log(stateShapeFile.getAbsolutePath() + " " + stateShapeFile.exists());
		Logger.log(actualDistrictsShapeFile.getAbsolutePath() + " " + actualDistrictsShapeFile.exists());
		Logger.log(blockShapeFile.getAbsolutePath() + " " + blockShapeFile.exists());
		
		return (stateShapeFile.exists() && actualDistrictsShapeFile.exists() && blockShapeFile.exists());
	}
	
	private static String[] printBlockAssignmentList(ArrayList<Unit> rawUnits, DistrictList districts){
		String fileName="BlockAssign_ST_" + CompactnessCalculator.getFIPSString(STATE) + "_" + STATE + "_CD.txt";
		String assignments = "";
		int index=1;//start at 1
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
					assignments += piece + "," + u.getDistrictAssignment() + "\n";
				}
			}else{
				assignments += u.getId() + "," + u.getDistrictAssignment() + "\n";
			}
		}
		
		return new String[] {fileName, assignments};
	}
}