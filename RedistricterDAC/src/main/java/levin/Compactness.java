package levin;

import java.util.ArrayList;
import java.util.List;

import levin.circle.SECCircle;
import levin.circle.SECPoint;
import levin.circle.Smallestenclosingcircle;
import levin.compactnessScore.HaversineDistance;

import com.dreizak.miniball.highdim.Miniball;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

/**
 * http://www.newswithnumbers.com/2010/02/04/gerrymandering-and-the-2010-census/#Detail
 * @author Harry
 *
 */
public class Compactness {

	private District district;
	
	private double area;
	private double perimeter;
	private Geometry convexHull;
	
	private double smallestCircleArea;
	private double smallestCirclePerim;
	
	
	public Compactness(District d){
		this.district = d;
		Geometry districtGeom = d.getRealGeometry();
		this.area = districtGeom.getArea();
		this.perimeter = districtGeom.getLength();
		this.convexHull = districtGeom.convexHull();
//		double radius = getDistrictSmallestEnclosingCircleRadius(d);
//		this.smallestCircleArea = (Math.pow(radius,2) * Math.PI);
//		this.smallestCirclePerim = (radius*2 * Math.PI);
		
	}
	
	public double getConvexHullMeasure(){
		double result = (this.district.getGeometry().getArea() /this.convexHull.getArea());
		if(!validate(result)){
			System.err.println("Result out of bounds: " + result);
			System.exit(0);
		}
		return result;
	}
	
	public double getReockMeasure(){
		
		double result = (this.district.getGeometry().getArea() /this.smallestCircleArea);
		if(!validate(result)){
			System.err.println("Result out of bounds: " + result);
			System.exit(0);
		}
		return result;
	}
	
	public double getPolsbyPopperMeasure(){
		double districtPerim = this.district.getGeometry().getLength();
		double radius = districtPerim/(2*Math.PI);
		double circleArea = Math.pow(radius,2) * Math.PI;
		double result = (this.district.getGeometry().getArea() /circleArea);
		if(!validate(result)){
			System.err.println("Result out of bounds: " + result);
			System.exit(0);
		}
		return result;
	}
	
	
	public double getModifiedSchwartzberg(){
		double districtArea = this.district.getGeometry().getArea();
		double radius = Math.sqrt(districtArea/Math.PI);
		double circlePerim = radius * 2 * Math.PI;
		double result = (circlePerim/this.district.getGeometry().getLength());
		if(!validate(result)){
			System.err.println("Result out of bounds: " + result);
			System.exit(0);
		}
		return result;
	}
	private boolean validate(double result){
		return (result >=0 && result <=1);
	}
	
	public double getDistrictSmallestEnclosingCircleRadius(District d){
		//all the coordinates in the Congressional kml file
		List<SECPoint> points  = getPointsForGeometry(d.getGeometry());
		Smallestenclosingcircle circle = new Smallestenclosingcircle();
		SECCircle b = circle.makeSECCircle(points);
		double radius = b.r;//radius
		System.out.println("radius= " + radius);
		SECPoint centerPoint = b.c;
		double[] center = {centerPoint.x , centerPoint.y};
		System.out.println("center= " + center[0] + " , " + center[1]);
		//On map the coordinates are flipped
		double[] coordinates = {center[1], center[0]};
		HaversineDistance h = new HaversineDistance(center[0] , center[0] - radius , center[1], center[1]);
		double actualRadius = h.getDistance();
		return actualRadius;
	}
	
	public List<SECPoint> getPointsForGeometry(Geometry geom){
		Coordinate[] coordinates = geom.getCoordinates();
		System.out.println("Num Coordinates: " + coordinates.length);
		List<SECPoint> points = new ArrayList<SECPoint>();
		for(Coordinate c : coordinates){
			SECPoint p = new SECPoint(c.x, c.y);
			points.add(p);
		}
		return points;
	}
	
	

}
