package levin;

import java.util.ArrayList;

import org.geotools.geometry.jts.FactoryFinder;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;

public class District {
	protected ArrayList<Unit> members;
	protected ArrayList<Unit> skippedUnits;
	protected Geometry geometry;
	protected int population;
	protected ArrayList<Geometry> geomList;
	
	public District(){
		population = 0;
		geometry = null;
		members = new ArrayList<Unit>();
		skippedUnits = new ArrayList<Unit>();
		geomList = new ArrayList<Geometry>();
	}
	
	public void add(Unit u){
		members.add(u);
		population += u.getPopulation();
		addGeometryCollection(u.getGeometry());
		//addGeometry(u.getGeometry());//merge population units
	}
	
	public void remove(Unit u){
		
		if(this.members.contains(u)){
			members.remove(u);
			population -= u.getPopulation();
			this.geometry = geometry.difference(u.getGeometry());
			
		}else{
			System.err.println("Trying to remove unit that's not in this district");
			System.exit(0);
		}
	}
	
	public boolean contains(Unit u){
		return members.contains(u);
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
	
//	private void addGeometry(Geometry geometry) {
//		if(geometry == null){
//			throw new NullPointerException();
//		}else if(this.geometry == null){
//			this.geometry = geometry;
//		}else{
//			this.geometry = this.geometry.union(geometry);
//		}
//	}

	public ArrayList<Unit> getSkippedUnits(){
		return this.skippedUnits;
	}

//	public void addContiguousUnit(Unit nextUnit){
//		this.skippedUnits.add(nextUnit);
//		ArrayList<Unit> removeFromSkippedUnits = new ArrayList<Unit>();
//		for(Unit u: this.skippedUnits){
//			Geometry nextUnitGeom = u.getGeometry();
//			//If geom is empty, we don't have any units so add one
//			if(this.geometry == null || 
//					this.geometry.touches(nextUnitGeom) || this.geometry.within(nextUnitGeom)){
//				add(u);
//				removeFromSkippedUnits.add(u);
//			}else{
//				//System.out.println("BREAK Point");
//			}
//		}
//		for(Unit u: removeFromSkippedUnits){
//			if(this.skippedUnits.contains(u)){
//				this.skippedUnits.remove(u);
//			}
//		}
//	}
}
