package levin.circle;
import java.util.Collection;


public class SECCircle {
	
	private static double EPSILON = 1e-12;
	
	
	public final SECPoint c;   // Center
	public final double r;  // Radius
	
	
	public SECCircle(SECPoint c, double r) {
		this.c = c;
		this.r = r;
	}
	
	
	public boolean contains(SECPoint p) {
		return c.distance(p) <= r + EPSILON;
	}
	
	
	public boolean contains(Collection<SECPoint> ps) {
		for (SECPoint p : ps) {
			if (!contains(p))
				return false;
		}
		return true;
	}
	
	
	public String toString() {
		return String.format("SECCircle(x=%g, y=%g, r=%g)", c.x, c.y, r);
	}
	
}
	
