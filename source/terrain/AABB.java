package terrain;

import org.joml.Vector2f;

public class AABB {

	public float minX;
	public float minY;
	public float maxX;
	public float maxY;
	private float surfaceArea = 0;

	/**
	 * Initializes the box with min = max = vec2(0, 0);
	 */
	public AABB() {
		set(new Vector2f(), 0f);
	}

	/**
	 * Builds an AABB from a center point and an half-extent along each axis. <br>
	 * Its size will be 2*radius along each axis.
	 * 
	 * @param center The center position of the AABB
	 * @param radius The extent of the AABB
	 */
	public AABB(Vector2f center, float radius) {
		set(center, radius);
	}

	/**
	 * Sets this AABB from a center point and an extent along each axis. <br>
	 * Its size will be 2*radius along each axis.
	 * 
	 * @param center The center position of the AABB
	 * @param radius The extent of the AABB
	 */
	public void set(Vector2f center, float radius) {
		minX = center.x - radius;
		minY = center.y - radius;
		maxX = center.x + radius;
		maxY = center.y + radius;
	}

	/**
	 * Sets this AABB from another one.
	 * 
	 * @param box
	 */
	public void set(AABB box) {
		minX = box.minX;
		minY = box.minY;
		maxX = box.maxX;
		maxY = box.maxY;
	}

	/**
	 * Checks if this contains other entirely but not strictly (the sides can have
	 * equal values). <br>
	 * Note: {@code this.contains(this)} is always true.
	 * 
	 * @param other
	 * @return true if other is inside this.
	 */
	public boolean contains(AABB other) {
		if (minX <= other.minX && maxX >= other.maxX) {
			if (minY <= other.minY && maxY >= other.maxY) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Computes the union of two AABBs.
	 * 
	 * @param left
	 * @param right
	 * @return The smallest AABB containing both left and right
	 */
	public static AABB union(AABB left, AABB right) {
		AABB dest = new AABB();
		union(left, right, dest);
		return dest;
	}

	static float min(float a, float b) {
		return a < b ? a : b;
	}

	static float max(float a, float b) {
		return a > b ? a : b;
	}

	/**
	 * Computes the union of two AABBs and puts the result in dest.
	 * 
	 * @param left
	 * @param right
	 * @param dest  The smallest AABB containing both left and right
	 */
	public static void union(AABB left, AABB right, AABB dest) {
		dest.minX = min(left.minX, right.minX);
		dest.minY = min(left.minY, right.minY);
		dest.maxX = max(left.maxX, right.maxX);
		dest.maxY = max(left.maxY, right.maxY);
		dest.computeSurfaceArea();
	}

	/**
	 * Checks if left and right have a non-empty intersection using strict
	 * comparisons.
	 * 
	 * @param left
	 * @param right
	 * @return
	 */
	public static boolean intersect(AABB left, AABB right) {
		if (left.maxX > right.minX && left.minX < right.maxX) {
			if (left.maxY > right.minY && left.minY < right.maxY) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @return the surface area of this AABB
	 */
	float getSurfaceArea() {
		return surfaceArea;
	}

	/**
	 * Updates the surface area of this AABB
	 * 
	 * @return the new surface area
	 */
	public float computeSurfaceArea() {
		float dx = maxX - minX;
		float dy = maxY - minY;
		surfaceArea = dx + dy;
		return surfaceArea;
	}

	@Override
	public String toString() {
		return "AABB[ (" + minX + ", " + minY + ") | (" + maxX + ", " + maxY + ") ]";
	}

}
