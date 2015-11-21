package levin.circle;
//http://www.nayuki.io/res/smallest-enclosing-circle/smallestenclosingcircle.java
/* 
 * Smallest enclosing circle
 * 
 * Copyright (c) 2014 Project Nayuki
 * http://www.nayuki.io/page/smallest-enclosing-circle
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program (see COPYING.txt).
 * If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;


public class Smallestenclosingcircle {
	
	/* 
	 * Returns the smallest circle that encloses all the given points. Runs in expected O(n) time, randomized.
	 * Note: If 0 points are given, null is returned. If 1 point is given, a circle of radius 0 is returned.
	 */
	// Initially: No boundary points known
	public static SECCircle makeSECCircle(List<SECPoint> points) {
		// Clone list to preserve the caller's data, randomize order
		List<SECPoint> shuffled = new ArrayList<SECPoint>(points);
		Collections.shuffle(shuffled, new Random());
		
		// Progressively add points to circle or recompute circle
		SECCircle c = null;
		for (int i = 0; i < shuffled.size(); i++) {
			SECPoint p = shuffled.get(i);
			if (c == null || !c.contains(p))
				c = makeSECCircleOneSECPoint(shuffled.subList(0, i + 1), p);
		}
		return c;
	}
	
	
	// One boundary point known
	private static SECCircle makeSECCircleOneSECPoint(List<SECPoint> points, SECPoint p) {
		SECCircle c = new SECCircle(p, 0);
		for (int i = 0; i < points.size(); i++) {
			SECPoint q = points.get(i);
			if (!c.contains(q)) {
				if (c.r == 0)
					c = makeDiameter(p, q);
				else
					c = makeSECCircleTwoSECPoints(points.subList(0, i + 1), p, q);
			}
		}
		return c;
	}
	
	
	// Two boundary points known
	private static SECCircle makeSECCircleTwoSECPoints(List<SECPoint> points, SECPoint p, SECPoint q) {
		SECCircle temp = makeDiameter(p, q);
		if (temp.contains(points))
			return temp;
		
		SECCircle left = null;
		SECCircle right = null;
		for (SECPoint r : points) {  // Form a circumcircle with each point
			SECPoint pq = q.subtract(p);
			double cross = pq.cross(r.subtract(p));
			SECCircle c = makeCircumcircle(p, q, r);
			if (c == null)
				continue;
			else if (cross > 0 && (left == null || pq.cross(c.c.subtract(p)) > pq.cross(left.c.subtract(p))))
				left = c;
			else if (cross < 0 && (right == null || pq.cross(c.c.subtract(p)) < pq.cross(right.c.subtract(p))))
				right = c;
		}
		return right == null || left != null && left.r <= right.r ? left : right;
	}
	
	
	static SECCircle makeDiameter(SECPoint a, SECPoint b) {
		return new SECCircle(new SECPoint((a.x + b.x)/ 2, (a.y + b.y) / 2), a.distance(b) / 2);
	}
	
	
	static SECCircle makeCircumcircle(SECPoint a, SECPoint b, SECPoint c) {
		// Mathematical algorithm from Wikipedia: Circumscribed circle
		double d = (a.x * (b.y - c.y) + b.x * (c.y - a.y) + c.x * (a.y - b.y)) * 2;
		if (d == 0)
			return null;
		double x = (a.norm() * (b.y - c.y) + b.norm() * (c.y - a.y) + c.norm() * (a.y - b.y)) / d;
		double y = (a.norm() * (c.x - b.x) + b.norm() * (a.x - c.x) + c.norm() * (b.x - a.x)) / d;
		SECPoint p = new SECPoint(x, y);
		return new SECCircle(p, p.distance(a));
	}
	
}