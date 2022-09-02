package terrain;

import static org.lwjgl.opengl.GL15C.GL_STATIC_DRAW;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.joml.RoundingMode;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.lwjgl.opengl.GL46C;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.EdgeShape;
import com.badlogic.gdx.physics.box2d.World;

import engine.Game;
import opengl.Shader;
import opengl.VAO;

/**
 * manages physics simulations using box2d
 */
public class Physics {
	World box2d;// box2d world
	terrain.World world;

	public final static Vector2 gravity = new Vector2(0, -10);

	/**
	 * used in renderDebug
	 */
	private Shader debugShader;

	public Physics(terrain.World w) {
		box2d = new World(gravity, true);
		this.world = w;
		debugShader = new Shader("shaders/physicsDebug.vs", "shaders/physicsDebug.fs");
		debugShader.finishInit();
		debugShader.init_uniforms(List.of("zoom", "cameraPos", "screenSize"));
	}

	/**
	 * compute all collision lines from scratch
	 */
	public void computeGround() {
		long start = System.currentTimeMillis();
		world.chunks.forEach(c->c.recomputeCollisionMesh(world));
		System.out.println("compute ground took " + (System.currentTimeMillis() - start) + " ms");
	}

	/**
	 * perform a step in the simulation
	 */
	public void step() {

	}

	public void renderDebug(Game game) {
		debugShader.start();
		debugShader.loadFloat("zoom", game.getZoom());
		debugShader.loadVec2("cameraPos", game.cameraPos());
		debugShader.loadVec2("screenSize", game.screenSize);

		Vector2i minChunkCoords = new Vector2i().set(//
				new Vector2f(-game.screenSize.x / 2.0f, -game.screenSize.y / 2.0f)//
						.div(terrain.World.blockPixelHeight)//
						.mul(game.getZoom())//
						.add(game.cameraPos())//
						.div(terrain.World.blocksPerChunk), //
				RoundingMode.FLOOR).max(new Vector2i(0));

		Vector2i maxChunkCoords = new Vector2i().set(//
				new Vector2f(+game.screenSize.x / 2.0f, +game.screenSize.y / 2.0f)//
						.div(terrain.World.blockPixelHeight)//
						.mul(game.getZoom())//
						.add(game.cameraPos())//
						.div(terrain.World.blocksPerChunk), //
				RoundingMode.CEILING).min(new Vector2i(terrain.World.chunksPerWorld - 1));

		for (int y = minChunkCoords.y; y <= maxChunkCoords.y; y++) {
			for (int x = minChunkCoords.x; x <= maxChunkCoords.x; x++) {
				Chunk c = world.getChunk(new Vector2i(x, y));

				if (c.debug == null)
					continue;
				c.debug.bind();
				c.debug.bindAttribute(0);
				GL46C.glDrawArrays(GL46C.GL_LINES, 0, c.debug.getAttributeVBOs().get(0).getDataLength() / 8);
				c.debug.unbindAttribute(0);
				c.debug.unbind();

			}
		}

		// const vec2 onScreenPixelCoords = gl_FragCoord.xy - screenSize / 2.0;
		// const vec2 worldCoords = onScreenPixelCoords / blockPixelHeight * zoom
		// + cameraPos;

		// const ivec2 blockGlobalCoords = ivec2(floor(worldCoords));
		// const vec2 pixelLocalCoords = (worldCoords - blockGlobalCoords);

		// const ivec2 chunkCoords = ivec2(floor(worldCoords / blocksPerChunk));

		debugShader.stop();
	}

}
