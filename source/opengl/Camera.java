package opengl;

import org.joml.Vector2f;

import terrain.World;

public class Camera {
	public float zoom = 1f;
	public Vector2f pos = new Vector2f(World.chunksPerWorld / 2.0f * World.blocksPerChunk);

	public Camera() {
	}

	/**
	 * @param mult if > 1 : zoom in else zoom out
	 */
	public void zoom(float mult) {
		zoom *= mult;
	}

	public void moveH(float x) {
		pos.add(x * zoom, 0);
	}

	public void moveV(float y) {
		pos.add(0, y * zoom);
	}

	public void move(float x, float y) {
		pos.add(new Vector2f(x, y).mul(zoom));
	}

	public void setLoc(Vector2f v) {
		pos.set(v.x, v.y);
	}

	/**
	 * reset zoom to 1 and pos to origin
	 */
	public void reset() {
		zoom = 1f;
		pos.set(World.chunksPerWorld / 2.0f * World.blocksPerChunk);
	}
}
