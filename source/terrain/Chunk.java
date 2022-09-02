package terrain;

import static org.lwjgl.opengl.GL15C.GL_STATIC_DRAW;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntFunction;

import org.joml.Vector2f;
import org.joml.Vector2i;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.EdgeShape;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.Shape;
import com.badlogic.gdx.utils.Array;

import opengl.VAO;

public class Chunk {
	public int ID;
	public int x;
	public int y;
	public int[] blocks;

	float[] pos;
	VAO debug;
	Body lines;

	public Chunk(int x, int y, int[] blocks, int id) {
		this.x = x;
		this.y = y;
		this.blocks = blocks;
		this.ID = id;
	}

	public Block getBlock(int x, int y) {
		int id = this.blocks[x + y * World.blocksPerChunk];

		return Block.blockFromID(id);
	}

	public void recomputeCollisionMesh(World world) {
		float[] positions = new float[400];
		int index = 0;

		for (int l = 0; l < terrain.World.blocksPerChunk; l++) {
			Block prev = world.getBlock(new Vector2i(//
					x * terrain.World.blocksPerChunk - 1, //
					y * terrain.World.blocksPerChunk + l));//
			for (int k = 0; k < terrain.World.blocksPerChunk; k++) {
				Block block = getBlock(k, l);
				if (prev.isCollideable != block.isCollideable) {
					float coordsx = x * terrain.World.blocksPerChunk + k;
					float coordsy = y * terrain.World.blocksPerChunk + l;

					positions[index++] = coordsx;
					positions[index++] = coordsy;
					positions[index++] = coordsx;
					positions[index++] = coordsy + 1;
				}
				prev = block;
			}
		}

		for (int k = 0; k < terrain.World.blocksPerChunk; k++) {
			Block prev = world
					.getBlock(new Vector2i(x * terrain.World.blocksPerChunk + k, y * terrain.World.blocksPerChunk - 1));
			for (int l = 0; l < terrain.World.blocksPerChunk; l++) {
				Block block = getBlock(k, l);
				if (prev.isCollideable != block.isCollideable) {
					float coordsx = x * terrain.World.blocksPerChunk + k;
					float coordsy = y * terrain.World.blocksPerChunk + l;

					positions[index++] = coordsx;
					positions[index++] = coordsy;
					positions[index++] = coordsx + 1;
					positions[index++] = coordsy;
				}
				prev = block;
			}
		}
		pos = Arrays.copyOf(positions, index + 1);
//		debug = new VAO();
//		debug.bind();
//		debug.createFloatAttribute(0, pos, 2, 0, GL_STATIC_DRAW);
//		debug.unbind();

		reconstructMesh(world.physics);

//		createBody(world.physics);
	}

//	private void createBody(Physics physics) {
//		if (segments.isEmpty()) {
//			lines = null;
//			return;
//		}
//		BodyDef def = new BodyDef();
//		def.type = BodyType.StaticBody;
//		lines = physics.box2d.createBody(def);
//		for (int i = 0; i < segments.size() / 2; i++) {
//			EdgeShape shape = new EdgeShape();
//			shape.set(segments.get(2 * i + 0).x, //
//					segments.get(2 * i + 0).y, //
//					segments.get(2 * i + 1).x, //
//					segments.get(2 * i + 1).y);
//			lines.createFixture(shape, 1f);
//		}
//	}

	public void reconstructMesh(Physics physics) {
		debug = new VAO();
		debug.bind();
		debug.createFloatAttribute(0, pos, 2, 0, GL_STATIC_DRAW);
		debug.unbind();
		
		if (lines == null) {
			BodyDef def = new BodyDef();
			def.type = BodyType.StaticBody;
			lines = physics.box2d.createBody(def);
		}

		// modify already existing fixtures instead of recreate them
		Array<Fixture> fixtures = lines.getFixtureList();

		if (pos.length / 4 > fixtures.size) {
			// we will have to add new fixtures
			Array<Shape> added = new Array<>();
			for (int i = 0; i < pos.length / 4; i++) {
				if (i > fixtures.size - 1) {
					EdgeShape shape = new EdgeShape();
					shape.set(pos[4 * i + 0], //
							pos[4 * i + 1], //
							pos[4 * i + 2], //
							pos[4 * i + 3]);//

					added.add(shape);
				} else {
					EdgeShape shape = (EdgeShape) fixtures.get(i).getShape();
					shape.set(pos[4 * i + 0], //
							pos[4 * i + 1], //
							pos[4 * i + 2], //
							pos[4 * i + 3]);//
				}
			}
			added.forEach(s -> lines.createFixture(s, 1f));
			System.out.println(added.size + " added fixtures");
		} else if (pos.length / 4 < fixtures.size) {
			int i = 0;
			for (; i < pos.length / 4; i++) {
				EdgeShape shape = (EdgeShape) fixtures.get(i).getShape();

				shape.set(pos[4 * i + 0], //
						pos[4 * i + 1], //
						pos[4 * i + 2], //
						pos[4 * i + 3]);//
			}
			for (; i < fixtures.size; i++) {
				lines.destroyFixture(fixtures.get(i));
			}
		}
	}
}
