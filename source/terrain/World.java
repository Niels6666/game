package terrain;

import static org.lwjgl.opengl.GL15C.GL_STATIC_DRAW;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.joml.Matrix4f;
import org.joml.RoundingMode;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL46C;
import org.lwjgl.system.MemoryStack;

import display.Color;
import display.Debug;
import engine.Game;
import engine.Window;
import opengl.FBO;
import opengl.OpenGLSurface;
import opengl.QueryBuffer;
import opengl.Shader;
import opengl.Texture;
import opengl.TextureAtlas;
import opengl.VAO;
import opengl.VBO;

public class World {
	public static final int blockPixelHeight = 20;
	public static final int blocksPerChunk = 20;
	public static final int chunksPerWorld = 128;
	public static final Vector2f WorldOrigin = new Vector2f(chunksPerWorld / 2.0f * blocksPerChunk);
	public HashMap<Integer, Light> lights = new HashMap<>();
	public List<Entity> entities = new ArrayList<>();

	int chunkIDs[] = new int[chunksPerWorld * chunksPerWorld];
	List<Chunk> chunks = new ArrayList<>();
	VBO chunkBuffer;
	VBO worldBuffer;
	boolean updateWorldBuffer = false;
	Set<Chunk> chunksToUpdate = new HashSet<>();

	int bgChunkIDs[] = new int[chunksPerWorld * chunksPerWorld];
	List<Chunk> bgChunks = new ArrayList<>();
	VBO bgChunkBuffer;
	VBO bgWorldBuffer;

	float[] altitudes = new float[chunksPerWorld * blocksPerChunk];
	VBO altitudesBuffer;

	VAO blockQuad;
	Shader worldShader;
	Shader entityShader;
	Shader postProcessShader;
	Shader quadShader;
	Shader downScale;
	Shader upScale;
	TextureAtlas blockAtlas;
	TextureAtlas blockGlowAtlas;

	FBO fbo;
	public List<OpenGLSurface> bloomCascade;
	// public Texture colorTexture;
	public Texture postProcessTex;
	QueryBuffer queries;

	String savePath;

	public int frames = 0;
	public int totalFrames = 0;
	double averageMS = 0;
	List<Long> times = new ArrayList<>();

	// day-night cycle
	final Vector4f day = new Vector4f(0.5f, 0.8f, 1f, 1f);
	final Vector4f dusk = new Vector4f(0.8f, 0.3f, 0.1f, 1f);
	final Vector4f night = new Vector4f(0.005f, 0.005f, 0.01f, 1f);
	final Vector4f dawn = new Vector4f(1f, 0.6f, 0.6f, 1f);
	final Vector4f daySun = new Vector4f(1f, 0.9f, 0.9f, 1f);
	final Vector4f dawnSun = new Vector4f(1f, 1f, 0.7f, 1f);

	float[] states = new float[] { // from -1.0f to 1.0f
			-1.0f, -0.7f, // night
			-0.7f, -0.4f, // dawn
			-0.4f, 0.4f, // day
			0.4f, 0.7f, // dusk
			0.7f, 1.0f // night
	};

	final Vector4f[] changes = new Vector4f[] { //
			night, dawn, // -1.0f, -0.2f
			dawn, day, // -0.2f, 0.0f
			day, day,
			day, dusk, // 0.0f, 0.8f
			dusk, night // 0.8f, 1.0f
	};

	float state = 0.0f;
	Vector4f sky = new Vector4f(night);
	Vector4f sun = new Vector4f(daySun);
	/**
	 * the sun position at noon
	 */
	Vector2f sunPos = new Vector2f(WorldOrigin.x - 130, 40);

	Player player;

	public World(String savePath) throws Exception {
		this.savePath = savePath;
		Block.createBlocksInfo();
		blockAtlas = Block.atlas;
		blockGlowAtlas = Block.glowAtlas;

		Arrays.fill(chunkIDs, -1);

		try {
			IntBuffer blocks = loadBuffer(savePath + "/blocks.save").asIntBuffer();
			IntBuffer positions = loadBuffer(savePath + "/chunks.save").asIntBuffer();
			int bpc2 = blocksPerChunk * blocksPerChunk;
			for (int i = 0; i < positions.limit() / 2; i++) {
				int[] data = new int[bpc2];
				Chunk c = new Chunk(positions.get(2 * i), positions.get(2 * i + 1), data, i);
				for (int k = 0; k < bpc2; k++) {
					int ID = blocks.get(k + i * bpc2);
					data[k] = ID;

					if (Block.isLight(ID)) {
						// this block is a light
						Vector2f worldPos = new Vector2f(k % blocksPerChunk, k / blocksPerChunk)
								.add(c.x * blocksPerChunk, c.y * blocksPerChunk).add(0.5f, 0.5f);
						int index = (k % blocksPerChunk + c.x * blocksPerChunk)
								+ (k / blocksPerChunk + c.y * blocksPerChunk) * blocksPerChunk * chunksPerWorld;
						lights.put(index, new Light(worldPos, 15));
					}
				}
				chunks.add(c);
			}

		} catch (FileNotFoundException e3) {

			for (int j = 0; j < chunksPerWorld; j++) {
				for (int i = 0; i < chunksPerWorld; i++) {
					int[] data = new int[blocksPerChunk * blocksPerChunk];
					Arrays.fill(data, 1);
					Chunk c = new Chunk(i, j, data, chunks.size());
					chunks.add(c);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		int[] allBlocks = new int[chunks.size() * blocksPerChunk * blocksPerChunk];

		for (Chunk c : chunks) {
			chunkIDs[c.x + c.y * chunksPerWorld] = c.ID;
			System.arraycopy(c.blocks, 0, allBlocks, c.ID * blocksPerChunk * blocksPerChunk,
					blocksPerChunk * blocksPerChunk);
		}

		chunkBuffer = new VBO(GL46C.GL_SHADER_STORAGE_BUFFER);
		chunkBuffer.bind();
		chunkBuffer.storeData(allBlocks, GL46C.GL_STATIC_DRAW);
		chunkBuffer.unbind();

		worldBuffer = new VBO(GL46C.GL_SHADER_STORAGE_BUFFER);
		worldBuffer.bind();
		worldBuffer.storeData(chunkIDs, GL46C.GL_STATIC_DRAW);
		worldBuffer.unbind();

		////////////////
		// background //
		////////////////

		Arrays.fill(bgChunkIDs, -1);

		try {
			IntBuffer blocks = loadBuffer(savePath + "/BGblocks.save").asIntBuffer();
			IntBuffer positions = loadBuffer(savePath + "/BGchunks.save").asIntBuffer();
			int bpc2 = blocksPerChunk * blocksPerChunk;
			for (int i = 0; i < positions.limit() / 2; i++) {
				int[] data = new int[bpc2];
				Chunk c = new Chunk(positions.get(2 * i), positions.get(2 * i + 1), data, i);
				for (int k = 0; k < bpc2; k++) {
					int ID = blocks.get(k + i * bpc2);
					data[k] = ID;
				}
				bgChunks.add(c);
			}

		} catch (FileNotFoundException e3) {
			for (int j = 0; j < chunksPerWorld; j++) {
				for (int i = 0; i < chunksPerWorld; i++) {
					int[] data = new int[blocksPerChunk * blocksPerChunk];
					Arrays.fill(data, 1);
					Chunk c = new Chunk(i, j, data, bgChunks.size());
					bgChunks.add(c);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		int[] BGallBlocks = new int[bgChunks.size() * blocksPerChunk * blocksPerChunk];

		for (Chunk c : bgChunks) {
			bgChunkIDs[c.x + c.y * chunksPerWorld] = c.ID;
			System.arraycopy(c.blocks, 0, BGallBlocks, c.ID * blocksPerChunk * blocksPerChunk,
					blocksPerChunk * blocksPerChunk);
		}
		bgChunkBuffer = new VBO(GL46C.GL_SHADER_STORAGE_BUFFER);
		bgChunkBuffer.bind();
		bgChunkBuffer.storeData(BGallBlocks, GL46C.GL_STATIC_DRAW);
		bgChunkBuffer.unbind();

		bgWorldBuffer = new VBO(GL46C.GL_SHADER_STORAGE_BUFFER);
		bgWorldBuffer.bind();
		bgWorldBuffer.storeData(bgChunkIDs, GL46C.GL_STATIC_DRAW);
		bgWorldBuffer.unbind();

		///////////
		// altitudes
		///////////
		try {
			FloatBuffer coords = loadBuffer(savePath + "/altitudes.save").asFloatBuffer();
			for (int x = 0; x < coords.limit(); x++) {
				float y = coords.get(x);
				altitudes[x] = y;
			}
		} catch (FileNotFoundException e3) {
			Arrays.fill(altitudes, 0);
		} catch (Exception e) {
			e.printStackTrace();
		}

		altitudesBuffer = new VBO(GL46C.GL_SHADER_STORAGE_BUFFER);
		altitudesBuffer.bind();
		altitudesBuffer.storeData(altitudes, GL46C.GL_STATIC_DRAW);
		altitudesBuffer.unbind();

		/////////////
		// shaders //
		/////////////

		// worldShader = new Shader("shaders/blockrenderer.cs");
		worldShader = new Shader("shaders/blockrenderer.vs", "shaders/blockrenderer.fs");
		worldShader.finishInit();
		worldShader.init_uniforms(List.of(//
				"cameraPos", //
				"screenSize", //
				"zoom", //
				"time", //
				"surfaceAmbientLight", //
				"depthAmbientLight", //
				"sunColor", //
				"sunPos"));//

		entityShader = new Shader("shaders/entityRenderer.vs", "shaders/entityRenderer.fs");
		entityShader.finishInit();
		entityShader.init_uniforms(List.of(//
				"transform", //
				"zoom", //
				"cameraPos", //
				"screenSize", //
				"time", //
				"ambientLight"));//

		postProcessShader = new Shader("shaders/postpross.cs");
		postProcessShader.finishInit();
		postProcessShader.init_uniforms(List.of(//
				"exposure", //
				"glowPower", //
				"skyColor", //
				"depthColor", //
				"cameraPos", //
				"screenSize", //
				"zoom"));//

		downScale = new Shader("shaders/downScale.cs");
		downScale.finishInit();

		upScale = new Shader("shaders/upScale.cs");
		upScale.finishInit();
		upScale.init_uniforms(List.of("weight"));

		postProcessTex = new Texture(1, 1, GL46C.GL_RGBA8, GL46C.GL_NEAREST);

		quadShader = new Shader("shaders/quad.vs", "shaders/quad.fs");
		quadShader.finishInit();

		fbo = new FBO("WorldFBO", 0);
		fbo.bind();
		fbo.addTextureAttachment("COLOR", FBO.AttachmentFormat.RGBA);
		fbo.addTextureAttachment("LIGHT", FBO.AttachmentFormat.R11F_G11F_B10F);
		fbo.addTextureAttachment("GLOW", FBO.AttachmentFormat.RGBA);
		fbo.unbind();

		if (!fbo.finish()) {
			throw new Exception("Erreur lors de la création de WorldFBO");
		}

		bloomCascade = new ArrayList<>();

		blockQuad = new VAO();
		blockQuad.bind();
		float[] positions = { -1f, -1f, -1f, 1f, 1f, -1f, 1f, 1f };
		blockQuad.createFloatAttribute(0, positions, 2, 0, GL_STATIC_DRAW);
		blockQuad.unbind();

		queries = new QueryBuffer(GL46C.GL_TIME_ELAPSED);

		entities.add(player = new Player(new Vector2f(WorldOrigin.x, 0)));
	}

	private ByteBuffer loadBuffer(String path) throws IOException {
		BufferedInputStream bis = new BufferedInputStream(new FileInputStream(new File(path)));
		DataInputStream dis = new DataInputStream(bis);
		ByteBuffer ret = ByteBuffer.wrap(dis.readAllBytes());
		dis.close();
		return ret;
	}

	public Chunk getChunk(Vector2i blockCoords, Vector2i localBlockCoords) {
		if (blockCoords.x < 0 || blockCoords.y < 0) {
			return null;
		}
		Vector2i chunkCoords = new Vector2i(blockCoords).div(blocksPerChunk);
		localBlockCoords.set(blockCoords).sub(new Vector2i(chunkCoords).mul(blocksPerChunk));

		boolean insideWorld = chunkCoords.x >= 0 && chunkCoords.y >= 0 && chunkCoords.x < chunksPerWorld
				&& chunkCoords.y < chunksPerWorld;

		if (!insideWorld) {
			return null;
		}

		int chunk_id = chunkIDs[chunkCoords.x + chunkCoords.y * chunksPerWorld];
		Chunk c = null;
		if (chunk_id == -1) {
			chunk_id = chunks.size();
			int blockIDs[] = new int[blocksPerChunk * blocksPerChunk];
			Arrays.fill(blockIDs, 0);
			c = new Chunk(chunkCoords.x, chunkCoords.y, blockIDs, chunk_id);
			chunks.add(c);
			chunkIDs[chunkCoords.x + chunkCoords.y * chunksPerWorld] = chunk_id;
			updateWorldBuffer = true;
		} else {
			c = chunks.get(chunk_id);
		}
		return c;
	}

	public Block getBlock(Vector2f worldCoords) {
		Vector2i blockCoords = new Vector2i().set(worldCoords, RoundingMode.FLOOR);

		Vector2i localBlockCoords = new Vector2i();
		Chunk c = getChunk(blockCoords, localBlockCoords);

		if (c == null) {
			return Block.AIR;
		}

		int id = c.blocks[localBlockCoords.x + localBlockCoords.y * blocksPerChunk];

		return Block.blockFromID(id);
	}

	public void setBlock(Vector2i blockCoords, Block blockID) {
		Vector2i localBlockCoords = new Vector2i();
		Chunk c = getChunk(blockCoords, localBlockCoords);

		if (c == null) {
			return;
		}

		chunksToUpdate.add(c);

		int oldID = c.blocks[localBlockCoords.x + localBlockCoords.y * blocksPerChunk];
		int index = (localBlockCoords.x + c.x * blocksPerChunk)
				+ (localBlockCoords.y + c.y * blocksPerChunk) * blocksPerChunk * chunksPerWorld;
		if (Block.isLight(oldID)) {
			lights.remove(index).delete();
		}
		c.blocks[localBlockCoords.x + localBlockCoords.y * blocksPerChunk] = blockID.blockID;
		if (blockID.isLight()) {
			// this block is a light
			Vector2f worldPos = new Vector2f(localBlockCoords).add(c.x * blocksPerChunk, c.y * blocksPerChunk).add(0.5f,
					0.5f);
			lights.put(index, new Light(worldPos, 15));
		}
	}

	public void generate() {
		player.position.set(WorldOrigin.x, 100);
		WorldGen gen = new WorldGen();

		for (int j = 0; j < chunksPerWorld; j++) {
			for (int i = 0; i < chunksPerWorld; i++) {

				for (int y = 0; y < blocksPerChunk; y++) {
					for (int x = 0; x < blocksPerChunk; x++) {
						Vector2i coords = new Vector2i(i * blocksPerChunk + x, j * blocksPerChunk + y);
						Block block = gen.genBlock(coords.x, coords.y);
						setBlock(coords, block);
						// should be the top of the world
						if (block == Block.GRASS) {
							altitudes[coords.x] = coords.y;
						}
					}
				}

			}
		}

		for (int k = 0; k < 10; k++) {// iterate a few times
			// box filter on the altitudes
			float prev_value = 0;
			for (int i = 0; i < altitudes.length; i++) {
				float weight = 1.0f;
				float value = altitudes[i];
				if (i > 0) {
					weight += 1.0f;
					value += altitudes[i - 1];
				}
				if (i < altitudes.length - 1) {
					weight += 1.0f;
					value += altitudes[i + 1];
				}
				if (i > 0) {
					altitudes[i - 1] = prev_value;
				}
				prev_value = value / weight;
			}
			altitudes[altitudes.length - 1] = prev_value;
		}
		altitudesBuffer = new VBO(GL46C.GL_SHADER_STORAGE_BUFFER);
		altitudesBuffer.bind();
		altitudesBuffer.storeData(altitudes, GL46C.GL_STATIC_DRAW);
		altitudesBuffer.unbind();

		// background
		// need a new seed
		gen = new WorldGen(gen.worldSeed - 1723654);

		bgChunkIDs = new int[chunksPerWorld * chunksPerWorld];
		bgChunks = new ArrayList<>();

		for (int j = 0; j < chunksPerWorld; j++) {
			for (int i = 0; i < chunksPerWorld; i++) {
				int[] data = new int[blocksPerChunk * blocksPerChunk];
				int current = 0;
				for (int y = 0; y < blocksPerChunk; y++) {
					for (int x = 0; x < blocksPerChunk; x++) {
						Block bg = gen.genBackground(i * blocksPerChunk + x, j * blocksPerChunk + y);
						data[current] = bg.blockID;
						current++;
					}
				}
				Chunk c = new Chunk(i, j, data, bgChunks.size());
				bgChunks.add(c);
			}
		}

		int[] allBlocks = new int[bgChunks.size() * blocksPerChunk * blocksPerChunk];

		for (Chunk c : bgChunks) {
			bgChunkIDs[c.x + c.y * chunksPerWorld] = c.ID;
			System.arraycopy(c.blocks, 0, allBlocks, c.ID * blocksPerChunk * blocksPerChunk,
					blocksPerChunk * blocksPerChunk);
		}

		bgChunkBuffer = new VBO(GL46C.GL_SHADER_STORAGE_BUFFER);
		bgChunkBuffer.bind();
		bgChunkBuffer.storeData(allBlocks, GL46C.GL_STATIC_DRAW);
		bgChunkBuffer.unbind();

		bgWorldBuffer = new VBO(GL46C.GL_SHADER_STORAGE_BUFFER);
		bgWorldBuffer.bind();
		bgWorldBuffer.storeData(bgChunkIDs, GL46C.GL_STATIC_DRAW);
		bgWorldBuffer.unbind();
	}

	public void update(Game game, Window window) {
		Vector2f cameraPos = game.cameraPos();
		float zoom = game.getZoom();
		Vector2f screenSize = new Vector2f(window.getWidth(), window.getHeight());
		Vector2f mouseCoords = window.cursorPos();
		boolean isClicked = window.lmb() || window.rmb();
		Block blockID = window.lmb() ? Block.AIR : window.gui.playerGUI.getBlock();

		Vector2f mouseNDCCoords = new Vector2f(mouseCoords)//
				.div(screenSize)//
				.mul(new Vector2f(2.0f, -2.0f))//
				.add(-1.0f, 1.0f);

		Vector2f mouseTexCoords = new Vector2f(mouseNDCCoords)//
				.mul(0.5f, -0.5f).add(0.5f, 0.5f);

		Vector2f worldCoords = new Vector2f(mouseTexCoords)//
				.add(-0.5f, -0.5f)//
				.mul(screenSize)//
				.mul(zoom / blockPixelHeight)//
				.add(cameraPos);

		Vector2i mouseBlockCoords = new Vector2i().set(worldCoords, RoundingMode.FLOOR);
		// Vector2f blockLocalCoords = new Vector2f(worldCoords).sub(new
		// Vector2f(blockCoords)).mul(blockPixelHeight);

		int radius = 0;

		if (isClicked) {
			for (int j = -radius; j <= radius; j++) {
				for (int i = -radius; i <= radius; i++) {
					if (i * i + j * j > radius * radius) {
						continue;
					}

					Vector2i blockCoords = new Vector2i(mouseBlockCoords).add(i, j);
					setBlock(blockCoords, blockID);

				}
			}
		}

		for (var t : entities) {
			t.update(this, game, window);
		}
		window.gui.debug.gpuTime = averageMS;
	}

	int startTime() {
		int queryID = 0;
		try (MemoryStack stack = MemoryStack.stackPush()) {
			IntBuffer pID = stack.mallocInt(1);
			GL46C.glGenQueries(pID);
			queryID = pID.get(0);
		}
		GL46C.glBeginQuery(GL46C.GL_TIME_ELAPSED, queryID);
		return queryID;
	}

	long stopTime(int queryID) {
		GL46C.glEndQuery(GL46C.GL_TIME_ELAPSED);

		long time = 0;
		try (MemoryStack stack = MemoryStack.stackPush()) {
			LongBuffer p = stack.mallocLong(1);
			long pointer = stack.getPointerAddress();
			GL46C.glGetQueryObjecti64v(queryID, GL46C.GL_QUERY_RESULT, pointer);
			time = p.get(0);
		}
		GL46C.glDeleteQueries(queryID);

		return time;
	}

	public void render(Game game, Debug debug) {
		frames++;
		totalFrames++;

		int expectedWidth = (int) game.screenSize.x / debug.scaleFactor;
		int expectedHeight = (int) game.screenSize.y / debug.scaleFactor;

		if (debug.rebuildBloomCascades || fbo.width != expectedWidth || fbo.height != expectedHeight) {
			// resize the textures
			int width = expectedWidth;
			int height = expectedHeight;

			fbo.resize(width, height);
			fbo.getAttachment("GLOW").bindAsTexture(0, GL46C.GL_LINEAR, GL46C.GL_CLAMP_TO_EDGE);
			fbo.getAttachment("GLOW").unbindAsTexture(0);

			// colorTexture.delete();
			// colorTexture = new Texture(width, height, GL46C.GL_RGBA16F,
			// GL46C.GL_NEAREST);

			postProcessTex.delete();
			postProcessTex = new Texture(width, height, GL46C.GL_RGBA8, GL46C.GL_LINEAR);

			for (var t : bloomCascade) {
				((Texture) t).delete();
			}
			bloomCascade.clear();

			int bloomFormat = fbo.getAttachment("GLOW").getFormat().internalFormat;

			width /= 2;
			height /= 2;
			for (int i = 0; i < debug.bloomCascades; i++) {
				bloomCascade.add(new Texture(width, height, bloomFormat, GL46C.GL_LINEAR));
				width /= 2;
				height /= 2;

				if (width < 8 || height < 8) {
					break;
				}
			}

			System.out.println("Rebuilt textures !");
		}

		if (updateWorldBuffer) {
			// overwrite the buffer on the gpu with the updated data
			worldBuffer.bind();
			worldBuffer.storeData(chunkIDs, GL46C.GL_STATIC_DRAW);
			worldBuffer.unbind();
			updateWorldBuffer = false;
		}
		if (!chunksToUpdate.isEmpty()) {
			int chunkSize = blocksPerChunk * blocksPerChunk * 4;
			if (chunks.size() > chunkBuffer.getDataLength() / chunkSize) {
				System.out.println("Re-allocating chunkBuffer");
				// create a larger buffer
				VBO tmp = new VBO(GL46C.GL_SHADER_STORAGE_BUFFER);
				tmp.bind();
				tmp.reserveData(Math.max(chunkBuffer.getDataLength() * 2, chunks.size() * chunkSize), GL_STATIC_DRAW);
				tmp.unbind();

				chunkBuffer.delete();
				chunkBuffer = tmp;

				chunksToUpdate.clear();
				chunksToUpdate.addAll(chunks);
			}

			// copy the new chunks
			IntBuffer buff = GL46C.glMapNamedBuffer(chunkBuffer.getID(), GL46C.GL_WRITE_ONLY).asIntBuffer();
			for (Chunk c : chunksToUpdate) {
				for (int i = 0; i < c.blocks.length; i++) {
					buff.put(i + c.ID * c.blocks.length, c.blocks[i]);
				}
			}
			GL46C.glUnmapNamedBuffer(chunkBuffer.getID());
			chunksToUpdate.clear();
		}

		Light.createBVHBuffer();

		int queryID = startTime();

		// Draw the world

		fbo.bind();
		fbo.setViewport();
		fbo.bindColorAttachments(List.of("COLOR", "LIGHT", "GLOW"));
		fbo.clearColorAttachment("COLOR", new Vector4f(0, 0, 0, 0));
		fbo.clearColorAttachment("LIGHT", new Vector4f(0, 0, 0, 0));
		fbo.clearColorAttachment("GLOW", new Vector4f(0, 0, 0, 0));

		blockQuad.bind();
		blockQuad.bindAttribute(0);

		worldShader.start();
		blockAtlas.bindAsTexture(0);
		blockGlowAtlas.bindAsTexture(1);

		// foreground
		int storage = GL46C.GL_SHADER_STORAGE_BUFFER;
		GL46C.glBindBufferBase(storage, 0, worldBuffer.getID());
		GL46C.glBindBufferBase(storage, 1, chunkBuffer.getID());
		GL46C.glBindBufferBase(storage, 2, Light.BVHBuffer.getID());
		GL46C.glBindBufferBase(storage, 3, Block.blocksInfo.getID());

		// background
		GL46C.glBindBufferBase(storage, 4, bgWorldBuffer.getID());
		GL46C.glBindBufferBase(storage, 5, bgChunkBuffer.getID());

		// altitudes
		GL46C.glBindBufferBase(storage, 6, altitudesBuffer.getID());

		worldShader.loadFloat("zoom", game.getZoom() * debug.scaleFactor);
		worldShader.loadVec2("cameraPos", game.cameraPos());
		worldShader.loadVec2("screenSize", new Vector2f(expectedWidth, expectedHeight));
		worldShader.loadInt("time", totalFrames);
		Vector4f light = new Vector4f(1f - Math.abs(state));
		worldShader.loadVec4("surfaceAmbientLight", light);
		worldShader.loadVec4("depthAmbientLight", new Vector4f(0.076f));

		if (debug.timePaused) {
			state = debug.timeofday;
		} else {
			debug.timeofday = state += 2.0f / 60.0f / 60.0f;
		}
		for (int i = 0; i < states.length; i += 2) {
			float min = states[i];
			float max = states[i + 1];
			if (state < max) {
				Vector4f from = changes[i];
				Vector4f to = changes[i + 1];
				sky = new Vector4f(from).lerp(to, (state - min) / (max - min));
				break;
			}
		}
//
//		// night
//		if (state < -0.2f) {// state in [-1: -0.2f]
//			sky = new Vector4f(night).lerp(dawn, (state - -1.0f) / (-0.2f - -1.0f));
//			// prepare sun color
//			sun.set(dawnSun);
//		} else if (state < 0f) { // dawn
//			sky = new Vector4f(dawn).lerp(day, (state - -0.2f) / (+0.0f - -0.2f));
//		} else if (state < 0.8f) { // in [0, 0.8]
//			sky = new Vector4f(day).lerp(dusk, (state - 0.0f) / (+0.8f - 0.0f));
//			sun.set(daySun);
//		} else if (state < 1f) { // in [0.8 : 1.0f]
//			sky = new Vector4f(dusk).lerp(night, (state - 0.8f) / (+1.0f - 0.8f));
//		}

		if (state > 1.0f) {
			state = -1.0f;
		}
		worldShader.loadVec4("sunColor", sun);

		float teta = (float) ((state*0.5 + 1.5f) * Math.PI);
		float r = 1200;
		Vector2f pos = new Vector2f(WorldOrigin).add(r * (float)Math.cos(teta), r * (float)Math.sin(teta));
		worldShader.loadVec2("sunPos", pos);

		GL46C.glDrawArrays(GL46C.GL_TRIANGLE_STRIP, 0, 4);// draw fullscreen quad

		blockAtlas.unbindAsTexture(0);
		blockGlowAtlas.unbindAsTexture(1);

		GL46C.glBindBufferBase(storage, 0, 0);
		GL46C.glBindBufferBase(storage, 1, 0);
		GL46C.glBindBufferBase(storage, 2, 0);
		GL46C.glBindBufferBase(storage, 3, 0);

		GL46C.glBindBufferBase(storage, 4, 0);
		GL46C.glBindBufferBase(storage, 5, 0);
		GL46C.glBindBufferBase(storage, 6, 0);

		worldShader.stop();

		entityShader.start();
		entityShader.loadFloat("zoom", game.getZoom() * debug.scaleFactor);
		entityShader.loadVec2("cameraPos", game.cameraPos());
		entityShader.loadVec2("screenSize", new Vector2f(expectedWidth, expectedHeight));
		entityShader.loadInt("time", totalFrames);
		entityShader.loadVec4("ambientLight", light);
		GL46C.glBindBufferBase(GL46C.GL_SHADER_STORAGE_BUFFER, 2, Light.BVHBuffer.getID());

		Matrix4f worldToNDC = new Matrix4f();
		worldToNDC.m00(expectedWidth * game.getZoom() / blockPixelHeight);
		worldToNDC.m11(expectedHeight * game.getZoom() / blockPixelHeight);
		worldToNDC.m30(game.cameraPos().x);
		worldToNDC.m31(game.cameraPos().y);
		worldToNDC.invertAffine();

		for (Entity entity : entities) {

			Matrix4f T = new Matrix4f(worldToNDC).mul(entity.getTransform());
			entityShader.loadMat4("transform", T);

			entity.getTexture().bindAsTexture(0);
			entity.getGlowTexture().bindAsTexture(1);
			GL46C.glDrawArrays(GL46C.GL_TRIANGLE_STRIP, 0, 4);// draw quad
			entity.getTexture().unbindAsTexture(0);
			entity.getGlowTexture().unbindAsTexture(1);
		}

		GL46C.glBindBufferBase(GL46C.GL_SHADER_STORAGE_BUFFER, 2, 0);
		entityShader.stop();

		blockQuad.unbindAttribute(0);
		blockQuad.unbind();

		fbo.unbind();

		GL46C.glViewport(0, 0, (int) game.screenSize.x, (int) game.screenSize.y);

		int colorFormat = fbo.getAttachment("COLOR").getFormat().internalFormat;
		int lightFormat = fbo.getAttachment("LIGHT").getFormat().internalFormat;
		int bloomFormat = fbo.getAttachment("GLOW").getFormat().internalFormat;

		// Bloom effect
		if (debug.isBloomEnabled) {
			bloomCascade.add(0, fbo.getAttachment("GLOW"));

			downScale.start();
			for (int i = 0; i < bloomCascade.size() - 1; i++) {
				bloomCascade.get(i).bindAsTexture(0);
				GL46C.glBindImageTexture(0, bloomCascade.get(i + 1).getID(), 0, false, 0, GL46C.GL_WRITE_ONLY,
						bloomFormat);

				GL46C.glDispatchCompute((bloomCascade.get(i + 1).getWidth() + 15) / 16,
						(bloomCascade.get(i + 1).getHeight() + 15) / 16, 1);
				GL46C.glMemoryBarrier(GL46C.GL_ALL_BARRIER_BITS);
			}
			downScale.stop();

			upScale.start();
			upScale.loadFloat("weight", debug.bloomWeight);

			for (int i = bloomCascade.size() - 2; i >= 0; i--) {
				bloomCascade.get(i + 1).bindAsTexture(0);
				GL46C.glBindImageTexture(0, bloomCascade.get(i).getID(), 0, false, 0, GL46C.GL_READ_WRITE, bloomFormat);

				GL46C.glDispatchCompute((bloomCascade.get(i).getWidth() + 15) / 16,
						(bloomCascade.get(i).getHeight() + 15) / 16, 1);
				GL46C.glMemoryBarrier(GL46C.GL_ALL_BARRIER_BITS);
			}
			bloomCascade.get(0).unbindAsTexture(0);
			upScale.stop();

			bloomCascade.remove(0);
		}

		// Tone mapping

		GL46C.glBindImageTexture(0, fbo.getAttachment("COLOR").getID(), 0, false, 0, GL46C.GL_READ_ONLY, colorFormat);
		GL46C.glBindImageTexture(1, fbo.getAttachment("LIGHT").getID(), 0, false, 0, GL46C.GL_READ_ONLY, lightFormat);
		GL46C.glBindImageTexture(2, fbo.getAttachment("GLOW").getID(), 0, false, 0, GL46C.GL_READ_ONLY, bloomFormat);
		GL46C.glBindImageTexture(3, postProcessTex.id, 0, false, 0, GL46C.GL_WRITE_ONLY, GL46C.GL_RGBA8);

		postProcessShader.start();
		postProcessShader.loadFloat("exposure", debug.toneMappingExposure);
		postProcessShader.loadFloat("glowPower", debug.glowPower);
		postProcessShader.loadVec4("skyColor", sky);
		postProcessShader.loadVec4("depthColor", new Vector4f(sky).mul(0.1f));
		postProcessShader.loadFloat("zoom", game.getZoom() * debug.scaleFactor);
		postProcessShader.loadVec2("cameraPos", game.cameraPos());
		postProcessShader.loadVec2("screenSize", new Vector2f(expectedWidth, expectedHeight));
		GL46C.glBindBufferBase(storage, 6, altitudesBuffer.getID());

		GL46C.glDispatchCompute((postProcessTex.width + 15) / 16, (postProcessTex.height + 15) / 16, 1);
		GL46C.glMemoryBarrier(GL46C.GL_ALL_BARRIER_BITS);
		GL46C.glBindBufferBase(storage, 6, 0);
		postProcessShader.stop();

		GL46C.glBindImageTexture(0, 0, 0, false, 0, GL46C.GL_WRITE_ONLY, colorFormat);
		GL46C.glBindImageTexture(1, 0, 0, false, 0, GL46C.GL_WRITE_ONLY, lightFormat);
		GL46C.glBindImageTexture(2, 0, 0, false, 0, GL46C.GL_WRITE_ONLY, bloomFormat);
		GL46C.glBindImageTexture(3, 0, 0, false, 0, GL46C.GL_WRITE_ONLY, GL46C.GL_RGBA8);

		// copy the image to screen
		quadShader.start();
		blockQuad.bind();
		blockQuad.bindAttribute(0);
		postProcessTex.bindAsTexture(0);
		GL46C.glDrawArrays(GL46C.GL_TRIANGLE_STRIP, 0, 4);
		postProcessTex.unbindAsTexture(0);
		blockQuad.unbindAttribute(0);
		blockQuad.unbind();
		quadShader.stop();

		long elapsedTime = stopTime(queryID);

		times.add(elapsedTime);
		if (times.size() > 120) {
			times.remove(0);
		}
		averageMS = times.stream().mapToDouble(l -> (double) l * 1.0E-6).average().getAsDouble();

//		double averageMS = times.stream().mapToDouble(l -> (double) l * 1.0E-6).average().getAsDouble();
//		DecimalFormat format = new DecimalFormat("#.###");
//		System.out.println(format.format(averageMS) + " ms");
//
		Light.renderDebug(game);
	}

	public void save() {
		try {
			// foreground
			DataOutputStream chunksSave = new DataOutputStream(
					new BufferedOutputStream(new FileOutputStream(new File(savePath + "/chunks.save"))));
			DataOutputStream blocksSave = new DataOutputStream(
					new BufferedOutputStream(new FileOutputStream(new File(savePath + "/blocks.save"))));

			for (Chunk c : chunks) {
				for (int i = 0; i < c.blocks.length; i++) {
					blocksSave.writeInt(c.blocks[i]);
				}
				chunksSave.writeInt(c.x);
				chunksSave.writeInt(c.y);
			}
			chunksSave.close();
			blocksSave.close();

			// background
			DataOutputStream BGchunksSave = new DataOutputStream(
					new BufferedOutputStream(new FileOutputStream(new File(savePath + "/BGchunks.save"))));
			DataOutputStream BGblocksSave = new DataOutputStream(
					new BufferedOutputStream(new FileOutputStream(new File(savePath + "/BGblocks.save"))));

			for (Chunk c : bgChunks) {
				for (int i = 0; i < c.blocks.length; i++) {
					BGblocksSave.writeInt(c.blocks[i]);
				}
				BGchunksSave.writeInt(c.x);
				BGchunksSave.writeInt(c.y);
			}
			BGchunksSave.close();
			BGblocksSave.close();

			// terrain altitude
			DataOutputStream altitudeSave = new DataOutputStream(
					new BufferedOutputStream(new FileOutputStream(new File(savePath + "/altitudes.save"))));
			for (int x = 0; x < altitudes.length; x++) {
				float y = altitudes[x];
				altitudeSave.writeFloat(y);
			}
			altitudeSave.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
