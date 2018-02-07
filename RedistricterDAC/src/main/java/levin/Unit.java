package levin;

import java.util.ArrayList;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class Unit{
	protected String id;
	protected int population;
	protected Point centroid;
	protected Geometry geom;
	protected int districtAssignment;
	protected ArrayList<String> neighbors;

	public Unit(String _id, Point cen, int pop, Geometry geometry){
		id = _id;
		centroid = cen;
		population = pop;
		geom = geometry;
		districtAssignment=-1;
		neighbors = new ArrayList<String>();
	}
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public int getPopulation() {
		return population;
	}
	public void setPopulation(int population) {
		this.population = population;
	}
	
	public void mergeUnit(Unit u) {
		this.id += "," + u.getId();
		this.population += u.getPopulation();
	}

	public Point getCentroid() {
		return centroid;
	}

	public void setCentroid(Point centroid) {
		this.centroid = centroid;
	}
	
	public Geometry getGeometry(){
		return this.geom;
	}
	
	public void setGeometry(Geometry geometry){
		this.geom = geometry;
	}
	
	public int getNumUnits(){
		return this.id.split(",").length;
	}
	
	public void setDistrictAssignment(int districtNum){
		this.districtAssignment = districtNum;
	}
	
	public int getDistrictAssignment(){
		return this.districtAssignment;
	}
	
	public ArrayList<String> getNeighbors(){
		return this.neighbors;
	}
	
	public void replaceNeighbor(String unitID, UnitGroup combined) {
		this.neighbors.remove(unitID);
		this.neighbors.remove(combined.getId());
		this.neighbors.add(combined.getId());
	}
	
	public void removeNeighbor(String unitId) {
		this.neighbors.remove(unitId);
	}
	
	public void addNeighbor(String s){
		this.neighbors.add(s);
	}
	
	private String neighborsToString() {
		String result = "";
		for(String s: this.neighbors) {
			result += s + ";";
		}
		return result;
	}

	@Override
	public String toString(){
		//"|" is the delimiter because commas are used in coordinates
		return id + "|" + population + "|" + centroid.toString() + "|" + 
				neighborsToString() + "|" + getGeometry().toText();
	}
	
	
}
