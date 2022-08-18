package terrain;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL46C;

import opengl.Texture;
import opengl.TextureAtlas;
import opengl.VAO;
import opengl.VBO;

public enum Block {
	AIR(false, false, 0, 0, false), STONE(), DIRT(), CANDLE(false, true, 4, 15, false), URANIUM(true, false, 0, 0, true);

	public final boolean isAnimated;
	public final boolean isGlowing;
	public final boolean isCollideable;
	public final int animationLength;
	public final int animationSpeed;
	public final Texture[] textures;
	public final Texture[] glowTextures;

	public int blockID;
	
	private static List<Block> blocksFromID;

	private Block(boolean isCollideable, boolean isAnimated, int animationLength, 
			int animationSpeed, boolean isGlowing) {
		this.isAnimated = isAnimated;
		this.isGlowing = isGlowing;
		this.isCollideable = isCollideable;
		this.animationLength = animationLength;
		this.animationSpeed = animationSpeed;
		if (isAnimated) {
			textures = new Texture[animationLength];
			glowTextures = new Texture[animationLength];

			for (int i = 0; i < animationLength; i++) {
				try {
					textures[i] = new Texture("images/textures/" + this.name().toLowerCase() + i + ".png");
					if (isGlowing)
						glowTextures[i] = new Texture("images/textures/" + this.name().toLowerCase() + i + "_glow.png");

				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} else {
			textures = new Texture[1];
			glowTextures = new Texture[1];
			try {
				textures[0] = new Texture("images/textures/" + this.name().toLowerCase() + ".png");
				if (isGlowing)
					glowTextures[0] = new Texture("images/textures/" + this.name().toLowerCase() + "_glow.png");

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private Block() {
		this(true, false, 0, 0, false);
	}

	static {
		int counter = 0;
		blocksFromID = new ArrayList<>();
		
		for (Block v : values()) {
			v.blockID = counter;
			counter = counter + (v.isAnimated ? v.animationLength : 1);
			for(int k=0; k<(v.isAnimated ? v.animationLength : 1); k++) {
				blocksFromID.add(v);
			}
		}
	}

	public boolean isLight() {
		return this == CANDLE;
	}

	public static VBO blocksInfo;
	public static TextureAtlas atlas;
	public static TextureAtlas glowAtlas;

	public static void createBlocksInfo() {
		int[] data = new int[4 * Block.values().length];
		for (Block b : Block.values()) {
			data[4 * b.ordinal() + 0] = b.isAnimated ? 1 : 0;
			data[4 * b.ordinal() + 1] = b.animationLength;
			data[4 * b.ordinal() + 2] = b.animationSpeed;
			data[4 * b.ordinal() + 3] = 0;
		}
		blocksInfo = new VBO(GL46C.GL_SHADER_STORAGE_BUFFER);
		blocksInfo.bind();
		blocksInfo.storeData(data, GL46C.GL_STATIC_DRAW);
		blocksInfo.unbind();
		List<Texture> textures = new ArrayList<>();
		List.of(values()).forEach(v -> {
			textures.addAll(List.of(v.textures));
		});
		atlas = new TextureAtlas(textures);

		List<Texture> glowTextures = new ArrayList<>();
		List.of(values()).forEach(v -> {
			for (var t : v.glowTextures) {
				glowTextures.add(t);
			}
		});

		glowAtlas = new TextureAtlas(glowTextures);
	}

	public static boolean isLight(int id) {
		return id == CANDLE.blockID;
	}
	
	public static Block blockFromID(int id) {
		return blocksFromID.get(id);
	}
}
