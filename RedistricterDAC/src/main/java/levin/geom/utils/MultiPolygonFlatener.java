package levin.geom.utils;

import java.util.ArrayList;

import levin.District;
import levin.DistrictList;
import levin.Main;
import levin.Unit;
import levin.printout.Logger;

import com.vividsolutions.jts.geom.Geometry;

public class MultiPolygonFlatener {

	private DistrictList districts;
	private boolean hasChanged;
	
	public MultiPolygonFlatener(DistrictList districts){
		this.districts = districts;
		this.hasChanged = false;
		
		Logger.log("district len=" + districts.getDistrictList().length);
		boolean district0hasMulti = districts.getDistrict(0).getGeometry().toText().contains("MULTIPOLYGON");
		boolean district1hasMulti = districts.getDistrict(1).getGeometry().toText().contains("MULTIPOLYGON");
		Logger.log("0 has multi: " + district0hasMulti + " , 1 has multi: " + district1hasMulti);
		Logger.log("0 pop: " + districts.getDistrict(0).getDistrictPopulation() + " , 1 pop: " + districts.getDistrict(1).getDistrictPopulation() );
		Logger.log(districts.getDistrict(0).getGeometry().toText());
		Logger.log(districts.getDistrict(1).getGeometry().toText());
		if(districts.getDistrictList().length ==2){
			if(districts.getDistrict(0).getGeometry().toText().contains("MULTIPOLYGON")) {
				hasChanged = true;
				flatten(districts.getDistrict(0));
			}
			
			if(	districts.getDistrict(1).getGeometry().toText().contains("MULTIPOLYGON")){
				hasChanged = true;
				flatten(districts.getDistrict(1));
			}
		}
	}
	
	private ArrayList<Geometry> findBadPieces(Geometry geom){
		ArrayList<Geometry> badPieces = new ArrayList<Geometry>();
		int index = getMostCoordinatesIndex(geom);
		if(Main.DEBUG){
			System.out.println("goodPieceIndex: " + index);
		}
		for(int i=0; i<geom.getNumGeometries(); i++){
			if(i != index){
				badPieces.add(geom.getGeometryN(i));
			}
		}
		return badPieces;
	}
	
	private int getMostCoordinatesIndex(Geometry geom){
		int maxCoordinates =0;
		int maxCoordIndex = 0;
		for(int i=0; i<geom.getNumGeometries(); i++){
			int numCoord = geom.getGeometryN(i).getCoordinates().length;
			if(numCoord>maxCoordinates){
				maxCoordIndex=i;
				maxCoordinates = numCoord;
			}
		}
		return maxCoordIndex;
	}
	
	private void flatten(District d){
		ArrayList<Unit> swapUnits = new ArrayList<Unit>();
		for(Geometry geom : findBadPieces(d.getGeometry())){
			ArrayList<Unit> members = d.getMembers();
			for(Unit u: members){
				if(geom.covers(u.getGeometry())){
					swapUnits.add(u);
				}
			}
		}
		Logger.log("Swapping " + swapUnits.size() + " units");
		for(Unit u: swapUnits){
			districts.swap(u, false);
		}
	}
	
	public boolean hasChanged(){
		return this.hasChanged;
	}
	
	public DistrictList getNewDistrictList(){
		return this.districts;
	}

}
