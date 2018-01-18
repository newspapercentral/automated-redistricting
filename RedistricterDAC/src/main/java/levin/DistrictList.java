package levin;

import java.util.ArrayList;

import levin.printout.Logger;

public class DistrictList {
	private int k;
	private District[] districtList;
	
	public DistrictList (District d){
		k = 1;
		districtList = new District[k];
		districtList[0] = d;
	}
	
	public DistrictList(District[] districts){
		k = districts.length;
		districtList = districts;
	}
	
	public DistrictList (int k){
		this.k = k;
		districtList = new District[k];
		for(int i=0; i<k; i++){
			districtList[i] = new District();
		}
	}
	
	public DistrictList(int k, String stateId, String doc_root){
		this.k = k;
		districtList = new District[k];
		if(k==1){
			districtList[0] = new StateWideDistrict(stateId, doc_root);
		}else{
			System.err.println("Invalid use of this constructor. Statewide districts can only have one district.");
			System.exit(0);
		}
	}
	
	public District getDistrict(int index){
		return districtList[index];
	}
	
	public int getDeviation(int idealPop){
		int largestDiff=0;
		for(District d : this.districtList){
			int diff = d.getDistrictPopulation() - idealPop;
			if(largestDiff<Math.abs(diff)){
				largestDiff = Math.abs(diff);
			}
		}
		return largestDiff;
	}
	
	public double getDeviationPercentage(int idealPop){	
		return ((double)this.getDeviation(idealPop)/(double)idealPop)*100;
	}
	
	private int getTotalPopulation(){
		int result = 0;
		for(District d: districtList){
			result += d.getDistrictPopulation();
		}
		return result;
	}
	
	public int getFirstDistrictDev(int idealPop){
		return Math.abs(districtList[0].getDistrictPopulation() - idealPop);

	}
	
	public String csvOutput(){
		String output = "";
		for(int i=0; i<districtList.length; i++){
			for(Unit u: districtList[i].getMembers()){
				String id = u.getId();
				output += id + "," + i + "\n";
			}
		}
		return output;
	}
	
	/**Only works for two districts because otherwise we have blocking problem
	 * 
	 * @param u
	 */
	public void swap(Unit u, boolean validate){
		if(this.districtList.length ==2){
			if(districtList[0].contains(u)){
				Logger.log("d0");
				Logger.log("Swapping: " + u.getId());
				Logger.log(districtList[0].getGeometry().toText());
				Logger.log("Unit geometry");
				Logger.log(u.getGeometry().toText());
				districtList[0].remove(u);
				districtList[1].add(u);
				Logger.log("Swapped " + u.getId());
				if(Main.DEBUG && (districtList[0].getGeometry().toText().contains("MULTIPOLYGON") ||
						districtList[1].getGeometry().toText().contains("MULTIPOLYGON"))){
					System.out.println("Swapped " + u.getId() + " and made districts non-contig");
				}
			}else if(districtList[1].contains(u)){
				Logger.log("d1");
				Logger.log("Swapping " + u.getId());
				Logger.log(districtList[1].getGeometry().toText());
				Logger.log("Unit geometry");
				Logger.log(u.getGeometry().toText());
				districtList[1].remove(u);
				districtList[0].add(u);
				Logger.log("Swapped " + u.getId());
			}else{
				System.err.println("Error: swapping district that is unassigned");
				System.exit(0);
			}
		}
	}
	
//	public void unswap(Unit u, int from, int to){
//		if(Main.DEBUG){
//			System.out.println("Unswapping: " + u.getId());
//		}
//		districtList[from].remove(u);
//		districtList[to].add(u);
//	}
	
//	public boolean validateSwap(District d){
//		boolean result = true;
//		if(d.getGeometry().toText().contains("MULTIPOLYGON")){
//			result = false;
//		}
//		return result;
//	}
	
	public int getLength(){
		return this.districtList.length;
	}
	
	public District[] getDistrictList(){
		return this.districtList;
	}
	
	public double getSimplePlanCompactnessScore(){
		double worstScore = 0.0;
		for(District d: this.districtList){
			if(d.getSimpleCompactnessScore() > worstScore) {
				worstScore = d.getSimpleCompactnessScore();
			}
		}
		return worstScore;
	}
	
	@Override
	public String toString(){
		String result = "District Index;Population;Number of Counties;Geometry";
		int index=1;
		
		for(District d: this.districtList){
			//result+= "\nFINALDistrict:" + index + " " + d.getDistrictPopulation() + "pop," + d.getNumCounties() + " counties";
			result += "\n";
			result+= index + ";" + 
					d.getDistrictPopulation() + ";"  +
					d.getNumCounties() + ";\n" +
					d.getGeometry();
			index ++;
		}
		return result;
	}
}
