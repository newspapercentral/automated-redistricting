package levin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;

public class CompactnessCalculator {

	private static String dataFilePath = "";
	private static final String FIPS_ATTR = "STATEFP";
	private DistrictList district;
	private int FIPS;
	
	public SummaryStatistics statOurConvexHull;
	public SummaryStatistics statOurReock;
	public SummaryStatistics statOurPolsbyPopper;
	public SummaryStatistics statOurModifiedSchwartzberg;
	public SummaryStatistics statCongConvexHull;
//	private SummaryStatistics statCongReock = new SummaryStatistics();
	public SummaryStatistics statCongPolsbyPopper = new SummaryStatistics();
	public SummaryStatistics statCongModifiedSchwartzberg = new SummaryStatistics();
	
	public CompactnessCalculator(String docroot, DistrictList districts, String stateId){
		dataFilePath = docroot + "/2012Congress/2012Congress.shp";
		this.district = districts;
		this.FIPS = getFIPS(stateId);
		statOurConvexHull = new SummaryStatistics();
//		statOurReock = new SummaryStatistics();
		statOurPolsbyPopper = new SummaryStatistics();
		statOurModifiedSchwartzberg = new SummaryStatistics();
		for(District ourDistrict: districts.getDistrictList()){
			Compactness congCompactness = new Compactness(ourDistrict);
			statOurConvexHull.addValue(congCompactness.getConvexHullMeasure());
//			statOurReock.addValue(congCompactness.getReockMeasure());
			statOurPolsbyPopper.addValue(congCompactness.getPolsbyPopperMeasure());
			statOurModifiedSchwartzberg.addValue(congCompactness.getModifiedSchwartzberg());
			
		}
		
		ArrayList<District> congDistricts = read();
		statCongConvexHull = new SummaryStatistics();
//		statCongReock = new SummaryStatistics();
		statCongPolsbyPopper = new SummaryStatistics();
		statCongModifiedSchwartzberg = new SummaryStatistics();
		for(District actualCongDistrict: congDistricts){
			Compactness congCompactness = new Compactness(actualCongDistrict);
			statCongConvexHull.addValue(congCompactness.getConvexHullMeasure());
//			statCongReock.addValue(congCompactness.getReockMeasure());
			statCongPolsbyPopper.addValue(congCompactness.getPolsbyPopperMeasure());
			statCongModifiedSchwartzberg.addValue(congCompactness.getModifiedSchwartzberg());
		}
	}
	
	private ArrayList<District> read() {
	    ArrayList<District> districtList = new ArrayList<District>();
		try {
		  //Read CSV data	
		  //DistrictList.run(DISTRICT_NUMBER, CSV_FILE_PATH);
	   	  
		  //Read in census block shapefile
		  File file = new File(dataFilePath);
		  Map connect = new HashMap();
		  connect.put("url", file.toURL());
		  
		  DataStore dataStore = DataStoreFinder.getDataStore(connect);
		  String[] typeNames = dataStore.getTypeNames();
		  String typeName = typeNames[0];

		  FeatureSource featureSource = dataStore.getFeatureSource(typeName);
		  FeatureCollection collection = featureSource.getFeatures();
		  FeatureIterator iterator = collection.features();		  
		
		  while (iterator.hasNext()) {
			  District d = new District(1);
		      SimpleFeature feature = (SimpleFeature) iterator.next();
		      int fips = Integer.parseInt(feature.getAttribute(FIPS_ATTR).toString());
		      if(fips == this.FIPS){
		    	  MultiPolygon multiPolygon = (MultiPolygon) feature.getDefaultGeometry();
			      Point centroid = multiPolygon.getCentroid();
			      Unit u = new Unit(String.valueOf(fips), centroid , 0, multiPolygon);
			      d.add(u);
			      districtList.add(d);
		      }
		    }
		  	
		  	
		    iterator.close();
		    dataStore.dispose();
		} catch (Throwable e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
			System.exit(0);
		}
		
		return districtList;
	}
	
	public static String getFIPSString(String state){
		int FIPS = getFIPS(state);
		String result = String.valueOf(FIPS);
		if(FIPS < 10){
			result = "0" + String.valueOf(FIPS);
		}
		return result;
	}
	
	public static int getFIPS(String state){
		if(state.toLowerCase().equals("nh")){
			return 33;
		} else if (state.toLowerCase().equals("ne")){
			return 31;
		} else if (state.toLowerCase().equals("pa")){
			return 42;
		}
		else if (state.toLowerCase().equals("al")){
			return 1;
		}
		else if (state.toLowerCase().equals("az")){
			return 4;
		}
		else if (state.toLowerCase().equals("ar")){
			return 5;
		}
		else if (state.toLowerCase().equals("ca")){
			return 6;
		}
		else if (state.toLowerCase().equals("co")){
			return 8;
		}
		else if (state.toLowerCase().equals("ct")){
			return 9;
		}
		else if (state.toLowerCase().equals("fl")){
			return 12;
		}
		else if (state.toLowerCase().equals("ga")){
			return 13;
		}
		else if (state.toLowerCase().equals("hi")){
			return 15;
		}
		else if (state.toLowerCase().equals("id")){
			return 16;
		}
		else if (state.toLowerCase().equals("il")){
			return 17;
		}
		else if (state.toLowerCase().equals("in")){
			return 18;
		}
		else if (state.toLowerCase().equals("ia")){
			return 19;
		}else if (state.toLowerCase().equals("ks")){
			return 20;
		}else if (state.toLowerCase().equals("ky")){
			return 21;
		}else if (state.toLowerCase().equals("la")){
			return 22;
		}else if (state.toLowerCase().equals("me")){
			return 23;
		}else if (state.toLowerCase().equals("md")){
			return 24;
		}else if (state.toLowerCase().equals("ma")){
			return 25;
		}else if (state.toLowerCase().equals("mi")){
			return 26;
		}else if (state.toLowerCase().equals("mn")){
			return 27;
		}else if (state.toLowerCase().equals("ms")){
			return 28;
		}else if (state.toLowerCase().equals("mo")){
			return 29;
		}else if (state.toLowerCase().equals("nv")){
			return 32;
		}else if (state.toLowerCase().equals("nj")){
			return 34;
		}else if (state.toLowerCase().equals("nm")){
			return 35;
		}else if (state.toLowerCase().equals("ny")){
			return 36;
		}else if (state.toLowerCase().equals("nc")){
			return 37;
		}else if (state.toLowerCase().equals("oh")){
			return 39;
		}else if (state.toLowerCase().equals("ok")){
			return 40;
		}else if (state.toLowerCase().equals("or")){
			return 41;
		}else if (state.toLowerCase().equals("ri")){
			return 44;
		}else if (state.toLowerCase().equals("sc")){
			return 45;
		}else if (state.toLowerCase().equals("tn")){
			return 47;
		}else if (state.toLowerCase().equals("tx")){
			return 48;
		}else if (state.toLowerCase().equals("ut")){
			return 49;
		}else if (state.toLowerCase().equals("va")){
			return 51;
		}else if (state.toLowerCase().equals("wa")){
			return 53;
		}else if (state.toLowerCase().equals("wv")){
			return 54;
		}else if (state.toLowerCase().equals("wi")){
			return 55;
		}else{
			return 0;
		}
	}
	
	public double getAverageScore(){
		return ((this.statOurConvexHull.getMean() + this.statOurReock.getMean() + this.statOurPolsbyPopper.getMean()
				+ this.statOurModifiedSchwartzberg.getMean())/4.0);
	}
	
	@Override
	public String toString(){
		String result="";
		result +="-------------------Compactness-------------------\n";
		result +="------Ours-------------------------Existing------\n";
		result +="ConvexHullOursMean=" + this.statOurConvexHull.getMean() + "<>" + this.statCongConvexHull.getMean() + "\n";
		result +="ConvexHullOursSdev=" + this.statOurConvexHull.getStandardDeviation() + "<>" + this.statCongConvexHull.getStandardDeviation() + "\n\n";
		
//		result +=this.statOurReock.getMean() + "----vs----" + this.statCongReock.getMean() + "\n";
//		result +=this.statOurReock.getStandardDeviation() + "----vs----" + this.statCongReock.getStandardDeviation() + "\n\n";

		result +="PolsbyPopperOursMean=" + this.statOurPolsbyPopper.getMean() + "<>" + this.statCongPolsbyPopper.getMean() + "\n";
		result +="PolsbyPopperOursSdev=" + this.statOurPolsbyPopper.getStandardDeviation() + "<>" + this.statCongPolsbyPopper.getStandardDeviation() + "\n\n";

		result +="ModifiedSchwartzbergOursMean=" + this.statOurModifiedSchwartzberg.getMean() + "<>" + this.statCongModifiedSchwartzberg.getMean() + "\n";
		result +="ModifiedSchwartzbergOursSdev=" + this.statOurModifiedSchwartzberg.getStandardDeviation() + "<>" + this.statCongModifiedSchwartzberg.getStandardDeviation() + "\n\n";

		return result;

	}

}
