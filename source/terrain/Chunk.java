package terrain;

public class Chunk {
	public int ID;
	public int x;
	public int y;
	public int[] blocks;
	
	public Chunk(int x, int y, int[] blocks, int id) {
		this.x = x;
		this.y = y;
		this.blocks = blocks;
		this.ID = id;
	}
//
//	public HashMap<Point, Integer> createCoordinates() {
//		HashMap<Point, Integer> points = new HashMap<>(blocks.length);
//		for (int i = CHUNK_SIZE; i <= blocks.length; i += CHUNK_SIZE) {
//			byte[] slice = Arrays.copyOfRange(blocks, i - CHUNK_SIZE, i);
//			for (int j = 0; j < slice.length; j++) {
//				int x = this.x + j * Block.BLOCK_SIZE;
//				int y = this.y + i - CHUNK_SIZE;
//				points.put(new Point(x, y), Integer.valueOf(i - CHUNK_SIZE + j));
//			}
//		}
//		return points;
//	}
//
//	public ArrayList<Point> search(byte texture) {
//		ArrayList<Point> res = new ArrayList<>();
//		HashMap<Point, Integer> coo = createCoordinates();
//		coo.forEach((p, i) -> {
//			if (blocks[i] == texture) {
//				res.add(p);
//			}
//		});
//		return res;
//	}
//
//	public ArrayList<Point> search(byte texture, HashMap<Point, Integer> coo) {
//		ArrayList<Point> res = new ArrayList<>();
//		coo.forEach((p, i) -> {
//			if (blocks[i] == texture) {
//				res.add(p);
//			}
//		});
//		return res;
//	}
//
//	public static Point top(Point p, Set<Point> forTests) {
//		Point top = new Point(p.x, p.y + Block.BLOCK_SIZE);
//		if (forTests.contains(top)) {
//			return top;
//		}
//		return null;
//	}
//
//	public static Point bottom(Point p, Set<Point> forTests) {
//		Point bottom = new Point(p.x, p.y - Block.BLOCK_SIZE);
//		if (forTests.contains(bottom)) {
//			return bottom;
//		}
//		return null;
//	}
//
//	public static Point left(Point p, Set<Point> forTests) {
//		Point left = new Point(p.x - Block.BLOCK_SIZE, p.y);
//		if (forTests.contains(left)) {
//			return left;
//		}
//		return null;
//	}
//
//	public static Point right(Point p, Set<Point> forTests) {
//		Point right = new Point(p.x + Block.BLOCK_SIZE, p.y);
//		if (forTests.contains(right)) {
//			return right;
//		}
//		return null;
//	}
//
//	public static ArrayList<Line2D> makeCollisionLines(ArrayList<Chunk> concerned) {
//		ArrayList<Line2D> res = new ArrayList<>();
//		ArrayList<Point> air = new ArrayList<>();
//
//		concerned.forEach((c) -> {
//			air.addAll(c.search((byte) 0));
//		});
//
//		if (air.isEmpty()) {
//			return res;
//		}
//
//		// faster
//		Set<Point> forTests = new HashSet<>(air);
//
//		air.forEach((a) -> {
//			Rectangle rect = new Rectangle(a.x, a.y, Block.BLOCK_SIZE, Block.BLOCK_SIZE);
//
//			if (top(a, forTests) == null) {
//				res.add(new Line2D.Double(rect.getX(), rect.getMaxY(), rect.getMaxX(), rect.getMaxY()));
//			}
//
//			if (bottom(a, forTests) == null) {
//				res.add(new Line2D.Double(rect.getX(), rect.getMinY(), rect.getMaxX(), rect.getMinY()));
//			}
//
//			if (left(a, forTests) == null) {
//				res.add(new Line2D.Double(rect.getX(), rect.getY(), rect.getX(), rect.getMaxY()));
//			}
//
//			if (right(a, forTests) == null) {
//				res.add(new Line2D.Double(rect.getMaxX(), rect.getY(), rect.getMaxX(), rect.getMaxY()));
//			}
//		});
//		return res;
//	}
}
