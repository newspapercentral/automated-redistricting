package levin.preprocess;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

import levin.Unit;

/**
 * Parses previously written file and returns list of population units (levin.unit class)
 * @author Harry
 *
 */
public class ReadPreProcess{

	private static String DAT_FILE;
	private static String DOC_ROOT;
	private static String dataFilePath;
	
	public ReadPreProcess(String doc_root, String _dataFilePath, String datFile) {
		DAT_FILE = datFile;
		DOC_ROOT = doc_root;
		dataFilePath = _dataFilePath;
	}
	
	public ArrayList<Unit> readFile() {
		FileReader in = null;
		BufferedReader br = null;
	    ArrayList<Unit> units = new ArrayList<Unit>();

		try {
			//Read in data file
		    in = new FileReader(DOC_ROOT + dataFilePath + "/" + DAT_FILE);
		    br = new BufferedReader(in);
		    
		    String line = br.readLine();//getFristLine
			while (line != null) {
				Unit u = initializeUnit(line);
				units.add(u);
			    line = br.readLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}finally {
		    try {
		    		//close reader and buffer regardless
		    		br.close();
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return units;
	}
	
	private Unit initializeUnit(String line) {
		Point centroid = null;
		Geometry geometry = null;
	    WKTReader wkt = new WKTReader();

		String[] data = line.split("\\|");//"|" is the delimeter of the data
		String id = data[0];
		int population = Integer.parseInt(data[1]);
		String[] neighbors = data[3].split(";");//";" is the delimeter for neighbors
		try {
			centroid = (Point) wkt.read(data[2]);
			geometry = (Geometry) wkt.read(data[4]);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		Unit u = new Unit(id, centroid, population, geometry);
		for(String neigh: neighbors) {
			u.addNeighbor(neigh);
		}
		return u;
	}

}
