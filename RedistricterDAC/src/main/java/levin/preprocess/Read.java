package levin.preprocess;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import levin.DistrictList;
import levin.StateWideDistrict;
import levin.Unit;
import levin.UnitGroup;
import levin.printout.Logger;
import levin.printout.Messenger;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;

/** 
 * Read and process shape files for tracts and blocks
 * @author Harry
 *
 */
public class Read {

	private static String SHAPE_FILE;
	private static String POP_FILE;
	
	private static int POPULATION;
	private int UNIT_COUNTER;
	private static String DOC_ROOT;
	protected static String DATA_PATH;
	
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
	public Read(String doc_root , String dataFilePath, String shapeFile, String popFile, boolean isBlock){
		DOC_ROOT = doc_root;
		DATA_PATH = doc_root + dataFilePath;
		SHAPE_FILE = doc_root + dataFilePath + shapeFile;
		POP_FILE = doc_root + dataFilePath + popFile;
		UNIT_COUNTER = 0;
		POPULATION = 0;	
		EDITED_UNITS=0;
		UNITS = new ArrayList<Unit>();
		IS_BLOCK = isBlock;
		
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
		UNITS = cleanUnits(readRawData());
		Logger.log("Finished reading now making state wide district");
		
		for(Unit u: UNITS){
			((StateWideDistrict)stateWideDistrictList.getDistrict(0)).add(u);
		}
		Logger.log("Returning state wide district");
		Geometry stateGeometry = stateWideDistrictList.getDistrict(0).getGeometry();
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
	
	/**
	 * Reads in population data from STATE-pop-data.txt
	 * @return
	 */
	//Code From: http://stackoverflow.com/questions/4716503/best-way-to-read-a-text-file
	public static HashMap<String, Integer> getPopData(){
		    try {
		    	 System.out.println("Attempting to read file " + POP_FILE  );
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
	private ArrayList<Unit> cleanUnits(ArrayList<Unit> units){
		int previousEditCount = -1;
		Logger.log("mergeMultiPolygonUnitsLoop: " + previousEditCount + "<" + EDITED_UNITS);
		while(previousEditCount < EDITED_UNITS){
			Logger.log("mergeMultiPolygonUnitsLoop: " + previousEditCount + "<" + EDITED_UNITS);
			previousEditCount = EDITED_UNITS;
			mergeMultiPolygonUnits(units);
		}
		//mergeNestedUnits(units);
		//this is not necessary because there will be other units that are valid that will break
		//compactness. We will need to do post processing and flip bad assignments
		
		SummaryStatistics populationStat = new SummaryStatistics();
		for(Unit u: units){
			populationStat.addValue(u.getPopulation());
		}
		
		Messenger.log("CLEANEDAverage Unit Population=" + populationStat .getMean());
		Messenger.log("CLEANEDStdev Unit Population=" + populationStat.getStandardDeviation());
		Messenger.log("CLEANEDMax Unit=" + populationStat.getMax());
		Messenger.log("CLEANEDMin Unit=" + populationStat.getMin());
		
		findNeighbors(units);
		
		return units;
	}
	/**
	 * O(n^2) in worst case, practically O(n) because there are only a handful of these units
	 * and only a few units for each one have to be merged
	 * @param units
	 * @return
	 */
	private ArrayList<Unit> mergeMultiPolygonUnits(ArrayList<Unit> units){
		ArrayList<Unit> addUnits = new ArrayList<Unit>();
		ArrayList<Unit> removeUnits = new ArrayList<Unit>();
		for(Unit u: units){
			if(u.getGeometry().getNumGeometries() > 1){
				EDITED_UNITS ++;
				Logger.log("Editing: " + u.getId());
				Logger.log("Edit Count: " + EDITED_UNITS);
				removeUnits.add(u);
				UnitGroup mergedUnits = new UnitGroup(u.getId(), u.getCentroid(), u.getPopulation(), u.getGeometry());
				for(Unit combineUnits: findInBetweenUnits(u, units)){
					mergedUnits.addUnit(combineUnits);
					removeUnits.add(combineUnits);
				}
				addUnits.add(mergedUnits);
			}
		}
		return updateUnitList(units, removeUnits, addUnits);
	}
	
	private ArrayList<Unit> findInBetweenUnits(Unit multi, ArrayList<Unit> units){
		ArrayList<Unit> mergeUnits = new ArrayList<Unit>();
		ArrayList<Unit> neighbors = new ArrayList<Unit>();
		for(Unit u: units){
			
			if(!u.getId().equals(multi.getId())
					&& multi.getGeometry().union(u.getGeometry()).getNumGeometries() == 1){
				mergeUnits.add(u);
				break;
			}else if(!u.getId().equals(multi.getId())
					&& multi.getGeometry().touches(u.getGeometry())){
				neighbors.add(u);
			}
		}
		return mergeUnits.size() >0 ? mergeUnits : neighbors;
	}
	
	private ArrayList<Unit> updateUnitList(ArrayList<Unit> units, ArrayList<Unit> removeUnits, ArrayList<Unit> addUnits){
		for(Unit u: removeUnits){
			units.remove(u);
		}
		units.addAll(addUnits);
		return units;
	}
	
	public ArrayList<Unit> getRawUnits(){
		return UNITS;
	}
	/** 
	 * O(n^2) look at every pair of units to see if they are neighbors
	 * @param units
	 */
	private void findNeighbors(ArrayList<Unit>units) {
		System.out.println("Starting neighbor calculation - FYI O(n^2)");
		for(Unit u1: units) {
			Geometry g1 = u1.getGeometry();
			for(Unit u2: units) {
				if(u1.getId() != u2.getId()) {
					Geometry g2 = u2.getGeometry();
					String inter = g1.intersection(g2).toText();
					if(!inter.contains("EMPTY") && !inter.contains("POINT")) {
						u1.addNeighbor(u2.getId());
					}
				}
			}
		}
	}

}
