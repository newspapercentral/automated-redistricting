package levin.compactnessScore;

/**
 * This is the implementation Haversine Distance Algorithm between two places
 * @author ananth
 *  R = earth’s radius (mean radius = 6,371km)
    Δlat = lat2− lat1
    Δlong = long2− long1
    a = sin²(Δlat/2) + cos(lat1).cos(lat2).sin²(Δlong/2)
    c = 2.atan2(√a, √(1−a))
    d = R.c
 *
 */
 //Code From: http://bigdatanerd.wordpress.com/2011/11/03/java-implementation-of-haversine-formula-for-distance-calculation-between-two-points/
public class HaversineDistance {
	
	private double distance;
 
    /**
     * @param args
     * arg 1- latitude 1
     * arg 2 - latitude 2
     * arg 3 - longitude 1
     * arg 4 - longitude 2
     */
    public  HaversineDistance(Double lat1, Double lat2, Double lon1, Double lon2) {
        // TODO Auto-generated method stub
        final int R = 6371; // Radious of the earth
        Double latDistance = toRad(lat2-lat1);
        Double lonDistance = toRad(lon2-lon1);
        Double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) + 
                   Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * 
                   Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        Double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        this.distance = R * c;
         
        //System.out.println("The distance between two lat and long is::" + this.distance);
        
 
    }
    
    public Double getDistance(){
    	return this.distance;
    }
     
    private static Double toRad(Double value) {
        return value * Math.PI / 180;
    }
 
}