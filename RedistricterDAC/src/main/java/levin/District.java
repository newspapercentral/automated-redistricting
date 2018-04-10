package levin;

import java.util.ArrayList;

import org.geotools.geometry.jts.FactoryFinder;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;

import levin.printout.ErrorLog;

public class District {
	protected ArrayList<Unit> members;
	protected ArrayList<Unit> skippedUnits;
	protected Geometry geometry;
	protected int population;
	protected ArrayList<Geometry> geomList;
	protected int id;
	
	public District(int _id){
		id = _id;
		population = 0;
		geometry = null;
		members = new ArrayList<Unit>();
		skippedUnits = new ArrayList<Unit>();
		geomList = new ArrayList<Geometry>();
	}
	
	public void add(Unit u, boolean updateAssignment){
		if(!this.contains(u)) {
			members.add(u);
			population += u.getPopulation();
			addGeometryCollection(u.getGeometry());
			u.setDistrictAssignment(this.id);
			if(updateAssignment) {
				Main.UNIT_MAP.get(u.getId()).setDistrictAssignment(this.id);
			}

		}else {
			System.err.println("Trying to add unit that is already a memeber");
			System.exit(0);		}
	}
	
	public void remove(Unit u, boolean updateAssignment){
		
		if(this.members.contains(u)){
			members.remove(u);
			population -= u.getPopulation();
			this.geometry = geometry.difference(u.getGeometry());
			u.setDistrictAssignment(-1);
			if(updateAssignment) {
				Main.UNIT_MAP.get(u.getId()).setDistrictAssignment(-1);
			}
			
		}else{
			System.err.println("Trying to remove unit that's not in this district");
			System.exit(0);
		}
	}
	
	public boolean contains(Unit u){
		boolean result = false;
		for(Unit mem: members) {
			if(mem.getId().equals(u.getId())) {
				result = true;
			}
		}
		return result;
	}
	
	public boolean contains(String s){
		boolean result = false;
		for(Unit mem: members) {
			if(mem.getId().equals(s)) {
				result = true;
			}
		}
		return result;
	}
	
	public int getDistrictPopulation(){
		return population;
	}
	
	public ArrayList<Unit> getMembers(){
		return this.members;
	}


	public Geometry getGeometry() {
		if(this.geometry == null){
			GeometryFactory factory = new GeometryFactory();
	
		     // note the following geometry collection may be invalid (say with overlapping polygons)
		     GeometryCollection geometryCollection =
		          (GeometryCollection) factory.buildGeometry( this.geomList );
	
		     this.geometry = geometryCollection.union();
		}
	     return this.geometry;
	}

	private void addGeometryCollection(Geometry geometry){
		this.geomList.add(geometry);
		if(this.geometry!= null){
			this.geometry = this.geometry.union(geometry);
		}
	}
	

	public ArrayList<Unit> getSkippedUnits(){
		return this.skippedUnits;
	}


	

	
	public double getSimpleCompactnessScore() {
			//Modified Schwartzberg Measure
			double districtArea = this.getGeometry().getArea();
			double radius = Math.sqrt(districtArea/Math.PI);
			double circlePerim = radius * 2 * Math.PI;
			double result = (circlePerim/this.getGeometry().getLength());
			return result;
	}
<<<<<<< master
	
	public int getNumCounties(){
		ArrayList<String> uniqueCounties = new ArrayList<String>();
		for(Unit u: this.members){
			String county = u.getId().substring(2, 5);
			if(!uniqueCounties.contains(county)){
				uniqueCounties.add(county);
			}
		}
		return uniqueCounties.size();
	}
	
	public int getId() {
		return this.id;
	}
=======
>>>>>>> 36d778b Final code for DAC
}
