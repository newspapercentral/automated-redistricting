package levin;

import levin.printout.ErrorLog;
import levin.printout.Logger;

import java.util.ArrayList;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

public class UnitGroup extends Unit{

	private int numUnits;
	
	public UnitGroup(String _id, Point cen, int pop, Geometry geometry, ArrayList<String> neighbors) {
		super(_id, cen, pop, geometry);
		for(String neigh: neighbors) {
			super.addNeighbor(neigh);
		}
		numUnits = 1;
	}
	
	public void addUnit(Unit u){
		if(u.getId().length() < 5){
			Logger.log("trying to add bad unit " + u.getId() + "to UnitGroup");
		}
		super.neighbors = combineNeighbors(u.getNeighbors(), u.getId());
		super.id += "," + u.getId();
		super.population += u.getPopulation();
		super.geom = super.geom.union(u.getGeometry());
		super.centroid = super.getGeometry().getCentroid();
		this.numUnits ++;
	}
	
	public void addUnitList(ArrayList<Unit> units) {
		for(Unit u: units) {
			addUnit(u);
		}
	}
	
	private ArrayList<String> combineNeighbors(ArrayList<String> newNeighbors, String newId) {
		ArrayList<String> result = new ArrayList<String>(this.neighbors);
		for(String neigh : newNeighbors) {
			if(!super.neighbors.contains(neigh)) {
				result.add(neigh);
			}
		}
		
		result.remove(newId);
		result.remove(this.getId());
		
		return result;
	}
	
	

}
