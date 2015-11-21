package levin;

import java.util.ArrayList;

import com.vividsolutions.jts.geom.Geometry;

public class DistrictValidation {

	private ArrayList<String> errorMessages;
	
	private static final String NON_CONTIG_FLAG ="Geometry is not contiguous: \n GEOMETRY";
	private static final String UNASSIGNED_UNITS="There are unassigned units: \n UNITS";
	private static final String DEVIATION_OUT_OF_BOUNDS = "Deviation out of bounds. Expecting LIMIT, but got ACTUAL_DEV";
	private static final String POPULATION_CORRUPTED = "Population data corrupted: ASSIGNED_POP is assigned, but TOTAL_POP is expected";
	
	public DistrictValidation(){
		this.errorMessages = new ArrayList<String>();
	}
	
	public ArrayList<String> getErrorMessages(){
		return this.errorMessages;
	}
	
	public boolean hasSuccessCode(){
		return this.errorMessages.size() == 0;
	}
	
	public void setNonContiguousFlag(Geometry invalidGeom){
		errorMessages.add(NON_CONTIG_FLAG.replaceAll("GEOMETRY", invalidGeom.toText()));
	}
	
	public void setUnassignedUnitsFlag(ArrayList<Unit> unassignedUnits){
		String unitIds = "";
		for(Unit u: unassignedUnits){
			unitIds+= u.getId() + ", ";
		}
		//TODO fix this later
		//unitIds = unitIds.substring(0, unitIds.length()-2);//cut off last comma
		errorMessages.add(UNASSIGNED_UNITS.replaceAll("UNITS", unitIds));
	}
	
	public void setDeviationOutOfBoundsFlag(double expectedDeviation, double actualDeviation){
		errorMessages.add(DEVIATION_OUT_OF_BOUNDS.replaceAll("LIMIT", Double.toString(expectedDeviation))
				.replaceAll("ACTUAL_DEVIATION", Double.toString(actualDeviation)));
	}
	
	public void setPopulationCorruptedFlag(int expectedPop, int actualPop){
		errorMessages.add(POPULATION_CORRUPTED.replaceAll("ASSIGNED_POP", Integer.toString(expectedPop))
				.replaceAll("TOTAL_POP", Integer.toString(actualPop)));
	}
	@Override
	public String toString(){
		return "Error Messages: " + this.errorMessages.toString();
	}

}
