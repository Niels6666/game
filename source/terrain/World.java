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
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.joml.RoundingMode;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL46C;
import org.lwjgl.system.MemoryStack;

import display.Debug;
import engine.Game;
import opengl.FBO;
import opengl.FBO.FBOAttachment;
import opengl.OpenGLSurface;
import opengl.Query;
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

	int chunkIDs[] = new int[chunksPerWorld * chunksPerWorld];

	List<Chunk> chunks = new ArrayList<>();
	public HashMap<Integer, Light> lights = new HashMap<>();

	VBO chunkBuffer;
	VBO worldBuffer;
	

	boolean updateWorldBuffer = false;
	Set<Chunk> chunksToUpdate = new HashSet<>();

	VAO blockQuad;

	Shader worldShader; // compute
	Shader postProcessShader; // fragment
	Shader quadShader; // fragment
	Shader downScale;
	Shader upScale;
	TextureAtlas blockAtlas;
	TextureAtlas blockGlowAtlas;

	FBO fbo;
	
	public List<OpenGLSurface> bloomCascade;
	//public Texture colorTexture;

	public Texture postProcessTex;

	QueryBuffer queries;

	String savePath;

	public int frames = 0;
	public int totalFrames = 0;

	private long startTime = System.currentTimeMillis();
	
	List<Long> times = new ArrayList<>();

	public World(String savePath) throws Exception {
		this.savePath = savePath;
		BlockIDs.createBlocksInfo();
		blockAtlas = BlockIDs.atlas;
		blockGlowAtlas = BlockIDs.glowAtlas;

		Arrays.fill(chunkIDs, -1);

		try {
			DataInputStream dis2 = new DataInputStream(
					new BufferedInputStream(new FileInputStream(new File(savePath + "/blocks.save"))));
			IntBuffer blocks = ByteBuffer.wrap(dis2.readAllBytes()).asIntBuffer();

			DataInputStream dis = new DataInputStream(
					new BufferedInputStream(new FileInputStream(new File(savePath + "/chunks.save"))));
			IntBuffer positions = ByteBuffer.wrap(dis.readAllBytes()).asIntBuffer();
			for (int i = 0; i < positions.limit() / 2; i++) {
				int[] data = new int[blocksPerChunk * blocksPerChunk];
				Chunk c = new Chunk(positions.get(2 * i), positions.get(2 * i + 1), data, i);
				for (int k = 0; k < blocksPerChunk * blocksPerChunk; k++) {
					int ID = blocks.get(k + i * blocksPerChunk * blocksPerChunk);
					data[k] = ID;

					if (BlockIDs.isLight(ID)) {
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
			dis.close();
			dis2.close();

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

		//worldShader = new Shader("shaders/blockrenderer.cs");
		worldShader = new Shader("shaders/blockrenderer.vs", "shaders/blockrenderer.fs");
		worldShader.finishInit();
		worldShader.init_uniforms(List.of("zoom", "cameraPos", "screenSize", "time", "glowPower"));

		postProcessShader = new Shader("shaders/postpross.cs");
		postProcessShader.finishInit();
		postProcessShader.init_uniforms(List.of("exposure"));

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
		fbo.addTextureAttachment("COLOR", FBO.AttachmentFormat.RGBA16F);
		fbo.addTextureAttachment("LIGHT", FBO.AttachmentFormat.RGBA16F);
		fbo.unbind();

		if(!fbo.finish()){
			throw new Exception("Erreur lors de la création de WorldFBO");
		}

		bloomCascade = new ArrayList<>();
		//colorTexture = new Texture(1, 1, GL46C.GL_RGBA16F, GL46C.GL_NEAREST);

		blockQuad = new VAO();
		blockQuad.bind();
		float[] positions = { -1f, -1f, -1f, 1f, 1f, -1f, 1f, 1f };
		blockQuad.createFloatAttribute(0, positions, 2, 0, GL_STATIC_DRAW);
		blockQuad.unbind();

		queries = new QueryBuffer(GL46C.GL_TIME_ELAPSED);

	}

	public void update(Vector2f cameraPos, float zoom, Vector2f screenSize, Vector2f mouseCoords, boolean isClicked,
			BlockIDs blockID) {
		Vector2f mouseNDCCoords = new Vector2f(mouseCoords).div(screenSize).mul(new Vector2f(2.0f, -2.0f)).add(-1.0f,
				1.0f);
		Vector2f mouseTexCoords = new Vector2f(mouseNDCCoords).mul(0.5f, -0.5f).add(0.5f, 0.5f);
		Vector2f worldCoords = new Vector2f(mouseTexCoords).add(-0.5f, -0.5f).mul(screenSize)
				.mul(zoom / blockPixelHeight).add(cameraPos);

		Vector2i mouseBlockCoords = new Vector2i().set(worldCoords, RoundingMode.FLOOR);
		// Vector2f blockLocalCoords = new Vector2f(worldCoords).sub(new
		// Vector2f(blockCoords)).mul(blockPixelHeight);

		int radius = 0;

		for (int j = -radius; j <= radius; j++) {
			for (int i = -radius; i <= radius; i++) {
				if (i * i + j * j > radius * radius) {
					continue;
				}

				Vector2i blockCoords = new Vector2i(mouseBlockCoords).add(i, j);
				if (blockCoords.x < 0 || blockCoords.y < 0) {
					continue;
				}
				Vector2i chunkCoords = new Vector2i(blockCoords).div(blocksPerChunk);
				blockCoords.sub(new Vector2i(chunkCoords).mul(blocksPerChunk));

				boolean insideWorld = chunkCoords.x >= 0 && chunkCoords.y >= 0 && chunkCoords.x < chunksPerWorld
						&& chunkCoords.y < chunksPerWorld;

				if (insideWorld && isClicked) {

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
					chunksToUpdate.add(c);
					int oldID = c.blocks[blockCoords.x + blockCoords.y * blocksPerChunk];
					int index = (blockCoords.x + c.x * blocksPerChunk)
							+ (blockCoords.y + c.y * blocksPerChunk) * blocksPerChunk * chunksPerWorld;
					if (BlockIDs.isLight(oldID)) {
						lights.remove(index).delete();
					}
					c.blocks[blockCoords.x + blockCoords.y * blocksPerChunk] = blockID.blockID;
					if (blockID.isLight()) {
						// this block is a light
						Vector2f worldPos = new Vector2f(blockCoords).add(c.x * blocksPerChunk, c.y * blocksPerChunk)
								.add(0.5f, 0.5f);
						lights.put(index, new Light(worldPos, 15));
					}
				}

			}
		}
	}

//	public void visit(Vector2f cameraPos, float zoom, Vector2f screenSize, Vector2f mouseCoords, boolean isClicked) {
//		value = ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | ((b & 0xFF) << 0);
//	}

	int startTime(){
		int queryID = 0;
		try ( MemoryStack stack = MemoryStack.stackPush() ) {
			IntBuffer pID = stack.mallocInt(1);
			GL46C.glGenQueries(pID);
			queryID = pID.get(0);
		}
		GL46C.glBeginQuery(GL46C.GL_TIME_ELAPSED, queryID);
		return queryID;
	}

	long stopTime(int queryID){
 		GL46C.glEndQuery(GL46C.GL_TIME_ELAPSED);

		long time = 0;
		try ( MemoryStack stack = MemoryStack.stackPush() ) {
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

		if (debug.rebuildBloomCascades || 
				fbo.width != game.screenSize.x || 
				fbo.height != game.screenSize.y) {
			// resize the textures
			int width = (int) game.screenSize.x;
			int height = (int) game.screenSize.y;

			fbo.resize(width, height);
			fbo.getAttachment("LIGHT").bindAsTexture(0, GL46C.GL_LINEAR, GL46C.GL_CLAMP_TO_EDGE);
			fbo.getAttachment("LIGHT").unbindAsTexture(0);

			//colorTexture.delete();
			//colorTexture = new Texture(width, height, GL46C.GL_RGBA16F, GL46C.GL_NEAREST);

			postProcessTex.delete();
			postProcessTex = new Texture(width, height, GL46C.GL_RGBA8, GL46C.GL_NEAREST);


			for (var t : bloomCascade) {
				((Texture)t).delete();
			}
			bloomCascade.clear();

			width /= 2;
			height /= 2;
			for(int i=0; i<debug.bloomCascades; i++){
				bloomCascade.add(new Texture(width, height, GL46C.GL_RGBA16F, GL46C.GL_LINEAR));
				width /= 2;
				height /= 2;

				if(width < 8 || height < 8){
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
		fbo.bindColorAttachments(List.of("COLOR", "LIGHT"));
		fbo.clearColorAttachment("COLOR", new Vector4f(0, 0, 0, 0));
		fbo.clearColorAttachment("LIGHT", new Vector4f(0, 0, 0, 0));

		worldShader.start();
		blockAtlas.bindAsTexture(0);
		blockGlowAtlas.bindAsTexture(1);

		GL46C.glBindBufferBase(GL46C.GL_SHADER_STORAGE_BUFFER, 0, worldBuffer.getID());
		GL46C.glBindBufferBase(GL46C.GL_SHADER_STORAGE_BUFFER, 1, chunkBuffer.getID());
		GL46C.glBindBufferBase(GL46C.GL_SHADER_STORAGE_BUFFER, 2, Light.BVHBuffer.getID());
		GL46C.glBindBufferBase(GL46C.GL_SHADER_STORAGE_BUFFER, 3, BlockIDs.blocksInfo.getID());

		//GL46C.glBindImageTexture(0, colorTexture.id, 0, false, 0, GL46C.GL_WRITE_ONLY, GL46C.GL_RGBA16F);
		//GL46C.glBindImageTexture(1, bloomCascade.get(0).id, 0, false, 0, GL46C.GL_WRITE_ONLY, GL46C.GL_RGBA16F);

		worldShader.loadFloat("zoom", game.getZoom());
		worldShader.loadVec2("cameraPos", game.getOrigin());
		worldShader.loadVec2("screenSize", game.screenSize);
		worldShader.loadInt("time", totalFrames);
		worldShader.loadFloat("glowPower", debug.glowPower);
		

		//draw fullscreen quad
		blockQuad.bind();
		blockQuad.bindAttribute(0);
		GL46C.glDrawArrays(GL46C.GL_TRIANGLE_STRIP, 0, 4);
		blockQuad.unbindAttribute(0);
		blockQuad.unbind();

		//GL46C.glDispatchCompute((colorTexture.width + 15) / 16, (colorTexture.height + 15) / 16, 1);
		//GL46C.glMemoryBarrier(GL46C.GL_ALL_BARRIER_BITS);

		blockAtlas.unbindAsTexture(0);
		blockGlowAtlas.unbindAsTexture(1);

		//GL46C.glBindImageTexture(0, 0, 0, false, 0, GL46C.GL_WRITE_ONLY, GL46C.GL_RGBA16F);
		//GL46C.glBindImageTexture(1, 0, 0, false, 0, GL46C.GL_WRITE_ONLY, GL46C.GL_RGBA16F);

		GL46C.glBindBufferBase(GL46C.GL_SHADER_STORAGE_BUFFER, 0, 0);
		GL46C.glBindBufferBase(GL46C.GL_SHADER_STORAGE_BUFFER, 1, 0);
		GL46C.glBindBufferBase(GL46C.GL_SHADER_STORAGE_BUFFER, 2, 0);
		GL46C.glBindBufferBase(GL46C.GL_SHADER_STORAGE_BUFFER, 3, 0);
		worldShader.stop();
		fbo.unbind();

		// Bloom effect
		if(debug.isBloomEnabled){
			bloomCascade.add(0, fbo.getAttachment("LIGHT"));

			downScale.start();
			for (int i = 0; i < bloomCascade.size() - 1; i++) {
				bloomCascade.get(i).bindAsTexture(0);
				GL46C.glBindImageTexture(0, bloomCascade.get(i + 1).getID(), 0, false, 0, GL46C.GL_WRITE_ONLY, GL46C.GL_RGBA16F);

				GL46C.glDispatchCompute(
						(bloomCascade.get(i + 1).getWidth() + 15) / 16,
						(bloomCascade.get(i + 1).getHeight() + 15) / 16, 
						1);
				GL46C.glMemoryBarrier(GL46C.GL_ALL_BARRIER_BITS);
			}
			downScale.stop();

			upScale.start();
			upScale.loadFloat("weight", debug.bloomWeight);

			for (int i = bloomCascade.size() - 2; i >= 0; i--) {
				bloomCascade.get(i + 1).bindAsTexture(0);
				GL46C.glBindImageTexture(0, bloomCascade.get(i).getID(), 0, false, 0, GL46C.GL_READ_WRITE, GL46C.GL_RGBA16F);

				GL46C.glDispatchCompute((
						bloomCascade.get(i).getWidth() + 15) / 16, 
						(bloomCascade.get(i).getHeight() + 15) / 16, 
						1);
				GL46C.glMemoryBarrier(GL46C.GL_ALL_BARRIER_BITS);
			}
			bloomCascade.get(0).unbindAsTexture(0);
			upScale.stop();

			bloomCascade.remove(0);
		}

		// Tone mapping

//		GL46C.glBindImageTexture(0, fbo.getAttachment("COLOR").getID(), 0, false, 0, GL46C.GL_READ_ONLY, GL46C.GL_RGBA16F);
//		GL46C.glBindImageTexture(1, fbo.getAttachment("LIGHT").getID(), 0, false, 0, GL46C.GL_READ_ONLY, GL46C.GL_RGBA16F);
	
	
		GL46C.glBindImageTexture(0, fbo.getAttachment("COLOR").getID(), 0, false, 0, GL46C.GL_READ_ONLY, GL46C.GL_RGBA16F);
		GL46C.glBindImageTexture(1, fbo.getAttachment("LIGHT").getID(), 0, false, 0, GL46C.GL_READ_ONLY, GL46C.GL_RGBA16F);
		GL46C.glBindImageTexture(2, postProcessTex.id, 0, false, 0, GL46C.GL_WRITE_ONLY, GL46C.GL_RGBA8);

		postProcessShader.start();
		postProcessShader.loadFloat("exposure", debug.toneMappingExposure);
		GL46C.glDispatchCompute((postProcessTex.width + 15) / 16, (postProcessTex.height + 15) / 16, 1);
		GL46C.glMemoryBarrier(GL46C.GL_ALL_BARRIER_BITS);
		postProcessShader.stop();

		GL46C.glBindImageTexture(0, 0, 0, false, 0, GL46C.GL_WRITE_ONLY, GL46C.GL_RGBA16F);
		GL46C.glBindImageTexture(1, 0, 0, false, 0, GL46C.GL_WRITE_ONLY, GL46C.GL_RGBA16F);
		GL46C.glBindImageTexture(2, 0, 0, false, 0, GL46C.GL_WRITE_ONLY, GL46C.GL_RGBA8);

		//select which texture to display

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
		if(times.size() > 120){
			times.remove(0);
		}

		double averageMS = times.stream().mapToDouble(l -> (double)l * 1.0E-6).average().getAsDouble();

		DecimalFormat format = new DecimalFormat("#.###");
		System.out.println(format.format(averageMS) + " ms");



//		Light.renderDebug(game);
	}

	public void save() {
		try {
			DataOutputStream dos = new DataOutputStream(
					new BufferedOutputStream(new FileOutputStream(new File(savePath + "/chunks.save"))));
			DataOutputStream dos2 = new DataOutputStream(
					new BufferedOutputStream(new FileOutputStream(new File(savePath + "/blocks.save"))));

			for (Chunk c : chunks) {
				for (int i = 0; i < c.blocks.length; i++) {
					dos2.writeInt(c.blocks[i]);
				}
				dos.writeInt(c.x);
				dos.writeInt(c.y);
			}
			dos.close();
			dos2.close();
		} catch (Exception e3) {
			e3.printStackTrace();
		}
	}
}
