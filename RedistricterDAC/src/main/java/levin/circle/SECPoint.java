package levin.circle;
public class SECPoint {
	
	public final double x;
	public final double y;
	
	
	public SECPoint(double x, double y) {
		this.x = x;
		this.y = y;
	}
	
	
	public SECPoint subtract(SECPoint p) {
		return new SECPoint(x - p.x, y - p.y);
	}
	
	
	public double distance(SECPoint p) {
		return Math.hypot(x - p.x, y - p.y);
	}
	
	
	// Signed area / determinant thing
	public double cross(SECPoint p) {
		return x * p.y - y * p.x;
	}
	
	
	// Magnitude squared
	public double norm() {
		return x * x + y * y;
	}
	
	
	public String toString() {
		return String.format("SECPoint(%g, %g)", x, y);
	}
}
