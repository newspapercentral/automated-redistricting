package levin;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

public class Unit{
	private String id;
	private int population;
	private Point centroid;
	private Geometry geom;
	

	public Unit(String _id, Point cen, int pop, Geometry geometry){
		id = _id;
		centroid = cen;
		population = pop;
		geom = geometry;
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

	@Override
	public String toString(){
		return "[id:" + id + ", population:" + population + ", centroid" + centroid.toString() + "]";
	}
	
	
}
