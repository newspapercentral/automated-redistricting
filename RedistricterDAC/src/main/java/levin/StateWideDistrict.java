package levin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;

public class StateWideDistrict extends District {

	private int FIPS;
	private String ID;
	private String DOC_ROOT;

	public StateWideDistrict(String stateId, String doc_root){
		super();
		ID = stateId;
		FIPS = CompactnessCalculator.getFIPS(stateId);
		DOC_ROOT = doc_root;
		geometry = readStateGeometry();
	}
	
	@Override
	public void add(Unit u){
		members.add(u);
		population += u.getPopulation();
		//We don't need to add unit geom because we know state geometry from another datasource
		//addGeometry(u.getGeometry());//merge population units
	}

	private Geometry readStateGeometry() {
		Geometry stateGeom = null;
		try {
			  //Read CSV data	
			  //DistrictList.run(DISTRICT_NUMBER, CSV_FILE_PATH);
		   	  
			  //Read in census block shapefile
			  File file = new File(DOC_ROOT + "/tl_2010_" + CompactnessCalculator.getFIPSString(ID) + "_state10/tl_2010_" + CompactnessCalculator.getFIPSString(ID) + "_state10.shp");
			  Map connect = new HashMap();
			  connect.put("url", file.toURL());
			  
			  DataStore dataStore = DataStoreFinder.getDataStore(connect);
			  String[] typeNames = dataStore.getTypeNames();
			  String typeName = typeNames[0];

			  FeatureSource featureSource = dataStore.getFeatureSource(typeName);
			  FeatureCollection collection = featureSource.getFeatures();
			  FeatureIterator iterator = collection.features();
			  System.out.println("Collection Size:" + collection.size());
			  //Note:Should only have one feature (i.e. the state-wide geometry)
			  if (iterator.hasNext()) {
			      SimpleFeature feature = (SimpleFeature) iterator.next();
			      MultiPolygon multiPolygon = (MultiPolygon) feature.getDefaultGeometry();
			      stateGeom = multiPolygon;
		      }
			  	
			  	
			    iterator.close();
			    dataStore.dispose();
			} catch (Throwable e) {
				System.err.println(e.getMessage());
				e.printStackTrace();
				System.exit(0);
			}
		return stateGeom;
	}


	
}
