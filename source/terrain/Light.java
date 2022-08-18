package terrain;

import static org.lwjgl.opengl.GL15C.GL_STATIC_DRAW;

import java.awt.Color;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Random;
import java.util.Random;

import org.joml.Vector2f;
import org.lwjgl.opengl.GL46C;

import engine.Game;
import opengl.Shader;
import opengl.VAO;
import opengl.VBO;

public class Light {
	public Vector2f pos;
	public float radius;
	public Vector2f direction;
	public float angle;
	public Color color;

	private BVHNode<Light> node;

	private static BVH<Light> bvh = new BVH<>();
	public static VBO BVHBuffer;
	private static VAO debugQuad;
	private static Shader debugShader;

	private static boolean needsUpdate = true;

	public static void init() {
		debugQuad = new VAO();
		debugQuad.bind();
		float[] positions = { -1f, -1f, 1f, -1f, 1f, 1f, -1f, 1f, -1f, -1f };
		debugQuad.createFloatAttribute(0, positions, 2, 0, GL_STATIC_DRAW);
		// debugQuad.createIndexBuffer(new int[] { 0, 1, 2, 2, 1, 3 });
		debugQuad.unbind();

		debugShader = new Shader("shaders/debug.vs", "shaders/debug.fs");
		debugShader.finishInit();
		debugShader.init_uniforms(
				List.of("zoom", "cameraPos", "screenSize", "lightPosition", "boxDimensionz", "lightDepth", "isLeaf"));
	}

	public static void renderDebug(Game game) {
		if (debugShader == null)
			init();
		debugShader.start();
		debugShader.loadFloat("zoom", game.getZoom());
		debugShader.loadVec2("cameraPos", game.cameraPos());
		debugShader.loadVec2("screenSize", game.screenSize);

		debugQuad.bind();
		debugQuad.bindAttribute(0);
		bvh.exploreBVH(100, (node, depth) -> {
			debugShader.loadFloat("lightDepth", 1f - (depth * 10f) / 100);
			if (node.getHandle() != null) {
				debugShader.loadInt("isLeaf", 1);
				debugShader.loadVec2("lightPosition", node.getHandle().pos);
				debugShader.loadVec2("boxDimensionz", new Vector2f(node.getHandle().radius));
			} else {
				debugShader.loadInt("isLeaf", 0);
				AABB box = node.getBox();
				Vector2f pos = new Vector2f((box.maxX + box.minX) / 2f, (box.maxY + box.minY) / 2f);
				debugShader.loadVec2("lightPosition", pos);
				Vector2f dimz = new Vector2f((box.maxX - box.minX) / 2f, (box.maxY - box.minY) / 2f);
				debugShader.loadVec2("boxDimensionz", dimz);
			}
//			Vector2f pos = new Vector2f(World.chunksPerWorld / 2.0f * World.blocksPerChunk);
//			debugShader.loadVec2("lightPosition", pos);
//			debugShader.loadVec2("boxDimensionz", new Vector2f(World.chunksPerWorld * World.blocksPerChunk / 2f));
			GL46C.glDrawArrays(GL46C.GL_LINE_STRIP, 0, 5);
		}, true);

		debugQuad.unbindAttribute(0);
		debugQuad.unbind();
		debugShader.stop();
	}

	public Light(Vector2f pos, float radius) {
		this.pos = pos;
		this.radius = radius;
		this.direction = new Vector2f(1, 0);
		this.angle = (float) (Math.PI*2);
		this.color = new Color(255, 255, 125);

		this.node = new BVHNode<Light>(new AABB(pos, radius), this);
		putInBVH();
	}

	public Light(Vector2f pos, float radius, float angle, Vector2f direction, Color color) {
		this.pos = pos;
		this.radius = radius;
		this.direction = direction;
		this.angle = angle;
		this.color = color;

		this.node = new BVHNode<Light>(new AABB(pos, radius), this);
		putInBVH();
	}

	public void putInBVH() {
		bvh.add(node);
		needsUpdate = true;
	}

	/**
	 * Doit être appelé à chaque frame si la lumière est déplacée ou bien change de
	 * rayon.
	 */
	public void update() {
		node.box.set(pos, radius);
		bvh.update(node);
		needsUpdate = true;
	}

	public void delete() {
		bvh.remove(node);
		needsUpdate = true;
	}

	public static void createBVHBuffer() {
		if (!needsUpdate)
			return;

		needsUpdate = false;
		int structSize = 48;
		int nodeCount = bvh.leavesCount * 2 - 1;

		if (bvh.leavesCount == 0) {
			ByteBuffer buff = ByteBuffer.allocate(1 * structSize);
			buff.putFloat(0);
			buff.putFloat(0);
			buff.putFloat(0);
			buff.putFloat(0);
			
			buff.putInt(-1); // child1
			buff.putInt(-1); // child2
			buff.putFloat(0); // radius
			buff.putInt(0); // color
			
			buff.putFloat(0); // direction.x
			buff.putFloat(0); // direction.y
			buff.putFloat(0); // angle
			buff.putInt(0); // padding
			

			BVHBuffer = new VBO(GL46C.GL_SHADER_STORAGE_BUFFER);
			BVHBuffer.bind();
			BVHBuffer.storeData(buff, GL46C.GL_READ_ONLY);
			BVHBuffer.unbind();
			return;
		}

		ByteBuffer buff = ByteBuffer.allocate(nodeCount * structSize);

		bvh.exploreBVH(1000, (n, d) -> {
			n.index = buff.position() / structSize;

			if (n.parent != null) {
				if (n == n.parent.child1) {
					int pos = n.parent.index * structSize + 16;
					buff.putInt(pos, n.index);
				} else {
					int pos = n.parent.index * structSize + 20;
					buff.putInt(pos, n.index);
				}
			}

			buff.putFloat(n.box.minX);
			buff.putFloat(n.box.minY);

			buff.putFloat(n.box.maxX);
			buff.putFloat(n.box.maxY);
			if (n.isLeaf) {
				buff.putInt(-1); // child1
				buff.putInt(-1); // child2
				buff.putFloat(n.getHandle().radius); // radius

				int u = 0;
				u |= n.getHandle().color.getRed() << 0;
				u |= n.getHandle().color.getGreen() << 8;
				u |= n.getHandle().color.getBlue() << 16;

				buff.putInt(u); // color
				
				buff.putFloat(n.getHandle().direction.x); // direction.x
				buff.putFloat(n.getHandle().direction.y); // direction.y
				buff.putFloat((float) Math.cos(n.getHandle().angle*0.5)); // angle
				buff.putInt(0); // padding
			} else {
				buff.putInt(-1); // child1
				buff.putInt(-1); // child2
				buff.putFloat(0); // radius
				buff.putInt(0); // color
				buff.putFloat(0); // direction.x
				buff.putFloat(0); // direction.y
				buff.putFloat(0); // angle
				buff.putInt(0); // padding
			}
		}, false);

		int[] array = new int[buff.capacity() / 4];
		for (int i = 0; i < array.length; i++) {
			array[i] = buff.getInt(i * 4);
		}

		if (BVHBuffer != null) {
			BVHBuffer.delete();
		}
		BVHBuffer = new VBO(GL46C.GL_SHADER_STORAGE_BUFFER);
		BVHBuffer.bind();
		BVHBuffer.storeData(array, GL46C.GL_STATIC_DRAW);
		BVHBuffer.unbind();
	}

}
