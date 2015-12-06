package levin;

import levin.printout.ErrorLog;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

public class UnitGroup extends Unit{

	private int numUnits;
	
	public UnitGroup(String _id, Point cen, int pop, Geometry geometry) {
		super(_id, cen, pop, geometry);
		numUnits = 1;
	}
	
	public void addUnit(Unit u){
		if(u.getId().length() < 5){
			ErrorLog.log("trying to add bad unit " + u.getId() + "to UnitGroup");
		}
		super.id += "," + u.getId();
		super.population += u.getPopulation();
		super.geom = super.geom.union(u.getGeometry());
		super.centroid = super.getGeometry().getCentroid();
		this.numUnits ++;
	}
	
	

}
