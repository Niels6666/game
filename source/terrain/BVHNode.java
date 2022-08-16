package terrain;

import java.util.Set;

public class BVHNode<T> {

	final AABB box;
	BVHNode<T> parent;
	BVHNode<T> child1;
	BVHNode<T> child2;
	final boolean isLeaf;
	float cost;
	int index = 0;

	private final T handle;

	/**
	 * Builds a new node which can be placed in the {@link BroadPhaseTree} subsequently.
	 * 
	 * @param box
	 * @param handle
	 */
    public BVHNode(AABB box, T handle) {
        this.box = box;
		this.handle = handle;
		this.isLeaf = true;
	}

	/**
	 * This constructor is called by the {@link BroadPhaseTree} to create interior nodes.
	 * 
	 * @param parent
	 * @param box
	 */
	BVHNode(BVHNode<T> parent, AABB box) {
		this.parent = parent;
		this.box = box;
		this.handle = null;
		this.isLeaf = false;
	}

	/**
	 * @param box
	 * @param dest
	 */
	void boxTest(AABB box, Set<T> dest) {

		if (!AABB.intersect(this.box, box)) {
			return;
		}

		if (isLeaf) {
			dest.add(this.handle);
		} else {
			this.child1.boxTest(box, dest);
			this.child2.boxTest(box, dest);
		}
	}

	public AABB getBox() {
		return box;
	}

	public T getHandle() {
		return handle;
	}

	@Override
	public String toString() {
		return (isLeaf ? "Leaf " + handle : "Node " + super.toString());
	}

}
