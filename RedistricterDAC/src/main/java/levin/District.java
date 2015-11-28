package levin;

import java.util.ArrayList;

import org.geotools.geometry.jts.FactoryFinder;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;

public class District {
	protected ArrayList<Unit> members;
	protected ArrayList<Unit> actualMembers;
	protected ArrayList<Unit> skippedUnits;
	protected Geometry geometry;
	protected int population;
	protected ArrayList<Geometry> geomList;
	protected ArrayList<Geometry> actualGeomList;
	
	public District(){
		population = 0;
		geometry = null;
		members = new ArrayList<Unit>();
		actualMembers = new ArrayList<Unit>();
		skippedUnits = new ArrayList<Unit>();
		geomList = new ArrayList<Geometry>();
		actualGeomList = new ArrayList<Geometry>();
	}
	
	public void add(Unit u){
		members.add(u);
		population += u.getPopulation();
		addGeometryCollection(u.getGeometry());
		
		if(u.getId().length() > 5){
			this.actualMembers.add(u);
			this.actualGeomList.add(u.getGeometry());
		}
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
	

	public ArrayList<Unit> getSkippedUnits(){
		return this.skippedUnits;
	}

	public Geometry getRealGeometry(){
		Geometry result = null;
		if(this.actualGeomList.size() > 0){
			GeometryFactory factory = new GeometryFactory();
	
		     // note the following geometry collection may be invalid (say with overlapping polygons)
		     GeometryCollection geometryCollection =
		          (GeometryCollection) factory.buildGeometry( this.actualGeomList );
	
		     result = geometryCollection.union();
		}else{
			result = getGeometry();
		}
	     return result;
	}
	
	public ArrayList<Unit> getActualMembers(){
		return this.actualMembers;
	}
}
