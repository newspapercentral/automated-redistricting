package levin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import levin.printout.ErrorLog;
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
import com.vividsolutions.jts.geom.Polygon;

public class Read {

	private static String SHAPE_FILE;
	private static String POP_FILE;
	
	//private static ArrayList<Unit> units;
	private static int POPULATION;
	private int UNIT_COUNTER;
	private static String DOC_ROOT;
	
	private static final String CENSUS_BLOCK_ID_ATTR = "BLOCKID10";
	private static final String CENSUS_TRACT_ID_ATTR = "GEOID10";
	private static final String CENSUS_POP_ATTR = "POP10";
	
	private static int EDITED_UNITS;

	
	
	public Read(String doc_root , String dataFilePath, String shapeFile, String popFile){
		DOC_ROOT = doc_root;
		SHAPE_FILE = doc_root + dataFilePath + shapeFile;
		POP_FILE = doc_root + dataFilePath + popFile;
		UNIT_COUNTER = 0;
		POPULATION = 0;	
		EDITED_UNITS=0;
		
	}
	
	public DistrictList getDistrictList(String stateId){
		DistrictList stateWideDistrictList = new DistrictList(1, stateId, DOC_ROOT);
		ArrayList<Unit> rawUnits = read();
		//Optimization: take this out and rely on post processing to fix this
		//ArrayList<Unit> processedUnits = unitGroupProcessing(rawUnits);
		Logger.log("Finished reading now making state wide district");
		
		for(Unit u: rawUnits){
			((StateWideDistrict)stateWideDistrictList.getDistrict(0)).add(u);
		}
		Logger.log("Returning state wide district");
		Geometry stateGeometry = stateWideDistrictList.getDistrict(0).getGeometry();
		if(stateGeometry.getNumGeometries() > 1){
			for(Unit u: stateWideDistrictList.getDistrict(0).getMembers()){
				if(u.getId().equals("13")){
					Logger.log("found 13 in memberList");
				}
			}
			Logger.log("State is a multipolygon, hope you made changes\n" + stateWideDistrictList.getDistrict(0).getGeometry().toText());
		}
		return stateWideDistrictList;
	}
	
	private ArrayList<Unit> read() {
	    ArrayList<Unit> unitList = new ArrayList<Unit>();
	    SummaryStatistics populationStat = new SummaryStatistics();
		try {	   	  
		  //Read in census block shapefile
		  File file = new File(SHAPE_FILE);
		  Map connect = new HashMap();
		  connect.put("url", file.toURL());
		  
		  DataStore dataStore = DataStoreFinder.getDataStore(connect);
		  String[] typeNames = dataStore.getTypeNames();
		  String typeName = typeNames[0];

		  FeatureSource featureSource = dataStore.getFeatureSource(typeName);
		  FeatureCollection collection = featureSource.getFeatures();
		  FeatureIterator iterator = collection.features();
		  System.out.println("Collection Size:" + collection.size());
	      UNIT_COUNTER = collection.size();
	      
	      //Load population data for tracts
	      HashMap<String, Integer> tractData = null;
	      if(!Main.IS_BLOCK){
	    	  tractData = getPopData();
	      }
	      
		  while (iterator.hasNext()) {
		      SimpleFeature feature = (SimpleFeature) iterator.next();
		      int population;
		      String blockId;

		      
		      if(Main.IS_BLOCK){
			      blockId = feature.getAttribute(CENSUS_BLOCK_ID_ATTR).toString();
			      population = Integer.parseInt(feature.getAttribute(CENSUS_POP_ATTR).toString());
		      }else{
		    	  blockId = feature.getAttribute(CENSUS_TRACT_ID_ATTR).toString();
		    	  population = getTractPop(tractData, blockId);
		      }
		      

		      
		      Read.POPULATION += population;
		      MultiPolygon multiPolygon = (MultiPolygon) feature.getDefaultGeometry();		      
		      Point centroid = multiPolygon.getCentroid();
		      Unit u = new Unit(blockId, centroid, population, multiPolygon);
		      populationStat.addValue(population);
		      unitList.add(u);
		      
		      if(blockId.length() <= 5){
		    	  Logger.log("gotBlock" + blockId);
		    	  Logger.log("With geom " + multiPolygon.toText());
		      }
		      
		    }
		  	
		  	
		    iterator.close();
		    dataStore.dispose();
		} catch (Throwable e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
			System.exit(0);
		}
		Messenger.log("Average Unit Population: " + populationStat.getMean());
		Messenger.log("Stdev Unit Population: " + populationStat.getStandardDeviation());
		return cleanUnits(unitList);
		//return unitList;
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
				// TODO Auto-generated catch block
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
	
	private ArrayList<Unit> cleanUnits(ArrayList<Unit> units){
		mergeMultiPolygonUnits(units);
		mergeNestedUnits(units);
		return units;
	}
	
	private ArrayList<Unit> mergeNestedUnits(ArrayList<Unit> units){
		ArrayList<Unit> removeUnits = new ArrayList<Unit>();
		ArrayList<Unit> addUnits = new ArrayList<Unit>();
		for(Unit u: units){

			boolean hasHoles = ((Polygon)u.getGeometry().union()).getNumInteriorRing() != 0;
			if(hasHoles && u.getId().length() > 2){
				EDITED_UNITS ++;
				Logger.log("Editing: " + u.getId());
				Logger.log("Edit Count: " + EDITED_UNITS);
				removeUnits.add(u);//has holes so remove and replace with new unit with no holes
				UnitGroup mergedUnits = new UnitGroup(u.getId(), u.getCentroid(), u.getPopulation(), u.getGeometry());
				ArrayList<Unit> insideUnits = findInsideUnits(u, units);
				removeUnits.addAll(insideUnits);//remove all inside units because it's replaced with new unit with no holes
				for(Unit innerUnit: insideUnits){
					mergedUnits.addUnit(innerUnit);
				}
				addUnits.add(mergedUnits);
			}
		}
		
		return updateUnitList(units, removeUnits, addUnits);
	}
	
	private ArrayList<Unit> findInsideUnits(Unit big, ArrayList<Unit> units){
		ArrayList<Unit> insideUnits = new ArrayList<Unit>();
		for(Unit u: units){
			if(!u.getId().equals(big.getId()) && u.getId().length() > 2  
					&& u.getId().substring(0, 11).equals(big.getId().substring(0, 11))// the first parts of the id should be similar denoting that they are in the same general vicinity
					&& big.getGeometry().within(u.getGeometry())){
				insideUnits.add(u);
			}
		}
		return insideUnits;
	}
	
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

}
