package levin.preprocess;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;

import levin.DistrictList;
import levin.StateWideDistrict;
import levin.Unit;
import levin.UnitGroup;
import levin.kdtree.DistanceFunction;
import levin.kdtree.KdTree;
import levin.kdtree.NearestNeighborIterator;
import levin.kdtree.SquareEuclideanDistanceFunction;
import levin.printout.Logger;
import levin.printout.Messenger;

/** 
 * Read and process shape files for tracts and blocks
 * @author Harry
 *
 */
public class Read {

	private static String SHAPE_FILE;
	private static String POP_FILE;
	
	private static int POPULATION;
	private static int UNIT_COUNTER;
	private static String DOC_ROOT;
	protected static String DATA_PATH;
	private static int MAX_NEIGHBORS;
	private static HashMap<String, Unit> UNIT_MAP = new HashMap<String, Unit>();
	
	/**
	 * Census attribute name for census block IDs
	 */
	private static final String CENSUS_BLOCK_ID_ATTR = "BLOCKID10";
	/**
	 * Census attribute name for census tract IDs
	 */
	private static final String CENSUS_TRACT_ID_ATTR = "GEOID10";
	/**
	 * Census attribute name for census block population
	 * Note: census tract population is stored in a separate file
	 */
	private static final String CENSUS_POP_ATTR = "POP10";
	private static ArrayList<Unit> UNITS;
	private static boolean IS_BLOCK;
	
	private static int EDITED_UNITS;

	
	/**
	 * Configures reader path and census unit type
	 * @param doc_root (file path) - root directory of input files and output (ex: "/Users/Harry/Desktop/Archive/data")
	 * @param dataFilePath (file path) - directory of shape and data files (ex; "/TEST_TRACT/")
	 * @param shapeFile (file name) - name of shape file (ex: "TEST.shp")
	 * @param popFile (file name) - name of census tract data file
	 * @param isBlock ("true"|"false") - true if the data is census blocks; false for census tracts
	 * O(1)
	 */
	public Read(String doc_root , String dataFilePath, String shapeFile, String popFile, boolean isBlock, int max_neighbors){
		Logger.setDebugFlag(false);
		DOC_ROOT = doc_root;
		DATA_PATH = doc_root + dataFilePath;
		SHAPE_FILE = doc_root + dataFilePath + shapeFile;
		POP_FILE = doc_root + dataFilePath + popFile;
		UNIT_COUNTER = 0;
		POPULATION = 0;	
		EDITED_UNITS=0;
		UNITS = new ArrayList<Unit>();
		IS_BLOCK = isBlock;
		MAX_NEIGHBORS = max_neighbors;
		
		cleanUnits(readRawData());
		
		SummaryStatistics populationStat = new SummaryStatistics();
		for(Unit u: UNIT_MAP.values()){
			populationStat.addValue(u.getPopulation());
		}
		
		Messenger.log("EDITED: " + EDITED_UNITS + " units");
		Messenger.log("CLEANEDAverage Unit Population=" + populationStat .getMean());
		Messenger.log("CLEANEDStdev Unit Population=" + populationStat.getStandardDeviation());
		Messenger.log("CLEANEDMax Unit=" + populationStat.getMax());
		Messenger.log("CLEANEDMin Unit=" + populationStat.getMin());

	}
	
	public HashMap<String, Unit> getUnits(){
		return UNIT_MAP;
	}
	/**
	 * 
	 * @param stateId (ex: "hi")
	 * @return
	 * O(n) - stateWideDistrictList is O(1) because it sets integer/string values and reads in one 
	 * state wide geometry feature
	 * O(n^2) in worst case - practically O(n) see notes below
	 */
	public DistrictList getDistrictList(String stateId){
		DistrictList stateWideDistrictList = new DistrictList(1, stateId, DOC_ROOT);		
		for(Unit u: UNITS){
			((StateWideDistrict)stateWideDistrictList.getDistrict(0)).add(u, true);
		}
		Logger.log("Returning state wide district");
		Geometry stateGeometry = stateWideDistrictList.getDistrict(0).  getGeometry();
		if(stateGeometry.getNumGeometries() > 1){
			//Ex: Hawaii is not contiguous and requires modifications to the shape file
			Logger.log("State is a multipolygon, hope you made changes\n" + stateWideDistrictList.getDistrict(0).getGeometry().toText());
		}
		return stateWideDistrictList;
	}
	
	/**
	 * 
	 * @param shapeFiles
	 * @return
	 */
	public ArrayList<Unit> readRawDataList(ArrayList<String> shapeFiles){
		ArrayList<Unit> allUnits = new ArrayList<Unit>();
		for(String file: shapeFiles){
			SHAPE_FILE=file;
			ArrayList<Unit> newUnits = readRawData();
			allUnits.addAll(newUnits);
		}
		return allUnits;
	}
	
	/**
	 * Reads each feature in shape file and processes it into a levin.unit class
	 * @return ArrayList<Unit> units - list of all of the units
	 * O(n)
	 */
	public ArrayList<Unit> readRawData(){
		ArrayList<Unit> unitList = new ArrayList<Unit>();
	    ArrayList<String> uniqueCounties = new ArrayList<String>();
	    SummaryStatistics populationStat = new SummaryStatistics();
		try {	   	  
		  //Read in census block shapefile
		  File file = new File(SHAPE_FILE);
		  Map<String, URL> connect = new HashMap<String, URL>();
		  connect.put("url", file.toURI().toURL());
		  
		  DataStore dataStore = DataStoreFinder.getDataStore(connect);
		  String[] typeNames = dataStore.getTypeNames();
		  String typeName = typeNames[0];

		  @SuppressWarnings("rawtypes")
		  FeatureSource featureSource = dataStore.getFeatureSource(typeName);
		  @SuppressWarnings("rawtypes")
		  FeatureCollection collection = featureSource.getFeatures();
		  @SuppressWarnings("rawtypes")
		  FeatureIterator iterator = collection.features();
		  System.out.println("Collection Size:" + collection.size());
	      UNIT_COUNTER = collection.size();
	      
	      //Load population data for tracts
	      HashMap<String, Integer> tractData = null;
	      if(!IS_BLOCK){
	    	  tractData = getPopData();
	      }
	      
		  while (iterator.hasNext()) {
		      SimpleFeature feature = (SimpleFeature) iterator.next();
		      int population;
		      String blockId;
		      
		      if(IS_BLOCK){
			      blockId = feature.getAttribute(CENSUS_BLOCK_ID_ATTR).toString();
			      population = Integer.parseInt(feature.getAttribute(CENSUS_POP_ATTR).toString());
		      }else{
			    	  blockId = feature.getAttribute(CENSUS_TRACT_ID_ATTR).toString();
			    	  population = getTractPop(tractData, blockId);
		      }
		      
		      POPULATION += population;
		      MultiPolygon multiPolygon = (MultiPolygon) feature.getDefaultGeometry();		      
		      Point centroid = multiPolygon.getCentroid();
		      Unit u = new Unit(blockId, centroid, population, multiPolygon);
		      populationStat.addValue(population);
		      unitList.add(u);
		      UNIT_MAP.put(u.getId(), u);
		      
		      if(blockId.length() >=5){
		    	  String county = u.getId().substring(2, 5);
	    				if(!uniqueCounties.contains(county)){
	    					uniqueCounties.add(county);
	    				}
		      }
		  }
		  	
		  	
		  iterator.close();
		  dataStore.dispose();
		} catch (Throwable e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
			System.exit(0);
		}
		Messenger.log("Average Unit Population=" + populationStat.getMean());
		Messenger.log("Stdev Unit Population=" + populationStat.getStandardDeviation());
		Messenger.log("Max Unit=" + populationStat.getMax());
		Messenger.log("Min Unit=" + populationStat.getMin());
		Messenger.log("NumCounties=" + uniqueCounties.size());
		return unitList;
	}
	

	public int getNumUnits(){
		return this.UNIT_COUNTER;
	}

	public static int getPopulation() {
		return POPULATION;
	}
	
	public static int getEditedCount() {
		return EDITED_UNITS;
	}
	
	/**
	 * Reads in population data from STATE-pop-data.txt
	 * @return
	 */
	//Code From: http://stackoverflow.com/questions/4716503/best-way-to-read-a-text-file
	public static HashMap<String, Integer> getPopData(){
		    try {
		    	 	Messenger.log("Attempting to read file " + POP_FILE  );
				BufferedReader br = new BufferedReader(new FileReader(POP_FILE ));

		        StringBuilder sb = new StringBuilder();
		        String line = br.readLine();

		        while (line != null) {
		            sb.append(line);
		            sb.append("\n");
		            line = br.readLine();
		        }
		        String everything = sb.toString();
		        br.close();
		        return readPop(everything);
		    } catch (IOException e) {
				e.printStackTrace();
				return null;
			}
	}

	public static HashMap<String, Integer> readPop(String data){
		HashMap<String , Integer> result = new HashMap<String, Integer>();
		String[] line = data.split("\n");
		for(String s: line){
			String[] fields = s.split(",");
			int pop = Integer.parseInt(fields[13]);
			String id = fields[12];
			result.put(id, pop);
		}
		System.out.println("Found " + result.size() + " entries");
		return result;
	}
	
	private Integer getTractPop(HashMap<String, Integer> tractData, String id){
		Integer population = null;
		try{
			population = tractData.get(id);
			if (population == null){
				throw new NullPointerException();
			}
		}catch(NullPointerException e){
			System.err.println("Error: could not find data for unit " + id + "null returned. Exiting...");
			System.exit(0);
		}
		return population;
	}
	
	/**
	 * Check the geometry of each unit to see if it has multiple polygons. Merge appropriate units
	 * to make each unit a simple polygon.
	 * 
	 * O(n^2) because of find neighbors
	 * @param units
	 * @return new list of units
	 */
	private void cleanUnits(ArrayList<Unit> units){		
		findNeighbors(units);		
		for(Unit u: units){
			if(UNIT_MAP.get(u.getId())!= null && u.getGeometry().getNumGeometries() > 1){
				mergeMultiPolygons(u);
			}
		}		
	}
	
	private void mergeMultiPolygons(Unit multi){
		
		boolean hasChanged = false;
		//simple case, one unit fixes it (99% of cases)
		for(String neigh: multi.getNeighbors()) {
			String multiId = multi.getId();
			
			if(multi.getGeometry().union(UNIT_MAP.get(neigh).getGeometry()).getNumGeometries() == 1) {
				Messenger.log("Merging: " + multiId + " and " + neigh);
				UnitGroup combinedUnit = new UnitGroup(multiId, multi.getCentroid(), multi.getPopulation(), multi.getGeometry(), multi.getNeighbors());
				combinedUnit.addUnit(UNIT_MAP.get(neigh));
				UNIT_MAP.remove(neigh);
				UNIT_MAP.remove(multiId);
				UNIT_MAP.put(combinedUnit.getId(), combinedUnit);
				Logger.log("Fixing Multi-neighbors for "+ multiId);
				updateNeighbors(multiId, neigh, combinedUnit, new ArrayList<String>());
				EDITED_UNITS+=2;
				hasChanged = true;
				break;
			}
		}
		//complex case: add all neighbors and try again
		if(!hasChanged) {
			Messenger.log("Merging all neighbors for unit: " + multi.getId());
			UnitGroup combinedUnit = new UnitGroup(multi.getId(), multi.getCentroid(), multi.getPopulation(), multi.getGeometry(), multi.getNeighbors());
			UNIT_MAP.remove(multi.getId());
			ArrayList<Unit> neighbors = new ArrayList<Unit>();
			ArrayList<String> ids = multi.getNeighbors();
			EDITED_UNITS++;
			
			for(String neigh: ids) {
				neighbors.add(UNIT_MAP.get(neigh));
				UNIT_MAP.remove(neigh);
				EDITED_UNITS++;
			}
			combinedUnit.addUnitList(neighbors);
			UNIT_MAP.put(combinedUnit.getId(), combinedUnit);
			updateNeighborsList(multi.getId(), ids , combinedUnit);
			//recursively call merge until it's fixed
			if(combinedUnit.getGeometry().getNumGeometries() > 1) {
				mergeMultiPolygons(multi);
			}
		}

	}
	
	private void updateNeighborsList(String comb1, ArrayList<String> comb2s, UnitGroup combined) {
		for(String comb2: comb2s) {
			updateNeighbors(comb1, comb2, combined, comb2s);
		}
	}
	
	private void updateNeighbors(String comb1, String comb2, UnitGroup combined, ArrayList<String> comb2s) {
		ArrayList<String> neighbors = new ArrayList<String>();
		neighbors.addAll(combined.getNeighbors());
		for(String neigh: neighbors ) {
			if(neigh.equals(comb1) || neigh.equals(comb2) || comb2s.contains(neigh)) {
				combined.removeNeighbor(neigh);
			}else {
				Messenger.log("for " + neigh + " replacing: " + comb1 + " and " + comb2 + 
						" with " + combined.getId());
				Unit neighbor = UNIT_MAP.get(neigh);
				neighbor.replaceNeighbor(comb1, combined);
				neighbor.replaceNeighbor(comb2, combined);
			}
		}
	}
	
	public ArrayList<Unit> getRawUnits(){
		return UNITS;
	}
	/** 
	 * O(mn) look at every pair of units to see if they are neighbors
	 * @param units
	 */
	private void findNeighbors(ArrayList<Unit>units) {
		Messenger.log("makeMap(units);//O(n)");
		HashMap <Coordinate, String> map = makeMap(units);//O(n) to make map where n is # coordinates
		Messenger.log("parseMap(map);//O(n)");
		HashMap <String, Integer> countMap = parseMap(map);
		Messenger.log("Count pairs - O(m)");
		for(String pairs: countMap.keySet()) {
			//A->B and B->A is one point, so >2
			if(countMap.get(pairs)>2) {
				String[] data = pairs.split(":");
				String minUnit = data[0];
				String maxUnit = data[1];
				UNIT_MAP.get(minUnit).addNeighbor(maxUnit);
				UNIT_MAP.get(maxUnit).addNeighbor(minUnit);
			}
			
			
		}
	}
	
	
	private static HashMap<Coordinate, String> makeMap(ArrayList<Unit> units) {
		HashMap<Coordinate, String> map = new HashMap<Coordinate, String>();
		for(Unit u : units) {
			for (Coordinate c: u.getGeometry().getCoordinates()){
				String mapVal = map.get(c);
				if(mapVal != null && mapVal.contains(u.getId())) {
					//do nothing
				}else if(map.containsKey(c)) {
					String newValue = map.get(c) + "," + u.getId();
					map.remove(c);
					map.put(c, newValue);
				}else {
					map.put(c, u.getId());
				}
			}
		}
		return map;
	}

	private static HashMap<String, Integer> parseMap(HashMap<Coordinate, String> map) {
		HashMap<String, Integer> unitCountMap = new HashMap<String, Integer>();
		for(String units: map.values()) {
			String[] ids = units.split(",");
			if(ids.length >1) {
				for(String u1: ids) {
					for(String u2: ids) {
						if(!u1.equals(u2)) {
							String min = (u1.compareTo(u2) >= 0) ? u1 : u2;
							String max = (u1.compareTo(u2) >= 0) ? u2 : u1;
							String key = min + ":" + max;
							if(unitCountMap.containsKey(key)) {
								int count = unitCountMap.get(key) +1;
								unitCountMap.remove(key);
								unitCountMap.put(key, count);
							}else {
								unitCountMap.put(key, 1);
							}
						}
					}
				}
			}
		}
		return unitCountMap;
	}
}
