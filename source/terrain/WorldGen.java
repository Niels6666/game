package terrain;

import java.util.Random;

import terrain.noise.Noise1D;
import terrain.noise.Noise2D;

public class WorldGen {
	private Noise1D noise1D;
	private Noise2D noise2D;
	public final long worldSeed;
	private final float terrainHeight = 20 * 20;
	private final float terrainAmplitude = 50;
	private final float dirtDepth = 6;

	public WorldGen() {
		this(System.nanoTime());
	}

	public WorldGen(long worldSeed) {
		this.worldSeed = worldSeed;
		noise1D = new Noise1D(worldSeed, 256, -1.0f, 1.0f);
		noise2D = new Noise2D(worldSeed, 32, 0.0f, 1.0f);
	}

	public static float clamp(float x, float min, float max) {
		return Math.max(Math.min(x, max), min);
	}

	public Block genBlock(int x, int y) {
		float h = terrainHeight + noise1D.cubicSampling(x, terrainAmplitude, 0.008f);
		h += noise1D.cubicSampling(x, terrainAmplitude * 0.5f, 0.008f * 2.01f);
		h += noise1D.cubicSampling(x, terrainAmplitude * 0.25f, 0.008f * 4.01f);
		h += noise1D.cubicSampling(x, terrainAmplitude * 0.125f, 0.008f * 8.01f);
		h += noise1D.cubicSampling(x, terrainAmplitude * 0.0625f, 0.008f * 17.01f);

		float e = Math.abs(noise1D.cubicSampling(x - 173456, dirtDepth, 0.01f)) + 5;

		if (y < h - e) {
			return Block.AIR;
		}
		if (y == (int) (h - e) + 1) {
			return Block.GRASS;
		}
		if (y < h + e - 3) {
			return Block.DIRT;
		}
		if (y < h + e) {
			return (noise2D.sample(x + 9632, y + 3578, 1, 1.5f) > 0.5f ? Block.DIRT : Block.STONE);
		}

		// from 0 to 1 as y increases
		float depth = clamp((y - (h + e)) / 200.0f, 0.0f, 1.0f);

		float v = noise2D.cubicSampling(x * 0.25f, y, 1.0f - 0.1542f, 0.025f);
		v += noise2D.cubicSampling(x + v * 10, y, 0.1542f, 0.05f);

		// solid
		if (v < 1.0f - depth * 0.5f) {
			// generate ores

			float coal = noise2D.cubicSampling(x, y, 1.0f, 0.051f);
			coal += noise2D.cubicSampling(x, y, 0.5f, 0.123f);
			coal = noise2D.cubicSampling(x + coal * 20.0f, y - coal * 20.56f, 1.0f, 0.1f);
			if (coal > 0.9f) {
				return Block.COAL;
			}
			float uranium = noise2D.cubicSampling(x + 1036, y + 789, 1.0f, 0.1f);
			if (uranium > 0.9f) {
				return Block.URANIUM;
			}

			return Block.STONE;
		} else {
			// air or lava

			if (y > 2400) {
				return Block.LAVA;
			}

			return Block.AIR;
		}
	}

	public Block genBackground(int x, int y) {
		float h = terrainHeight + noise1D.cubicSampling(x, terrainAmplitude, 0.008f);
		h += noise1D.cubicSampling(x + 13456, terrainAmplitude * 0.01f, 0.10f);

		float e = Math.abs(noise1D.cubicSampling(x - 173456, dirtDepth, 0.01f)) + 5;
		if (y < h - e) {
			// the only place were there will be air in the background is the sky
			return Block.AIR;
		}

		if (y == (int) (h - e) + 1) {
			return Block.GRASS;
		}
		if (y < h + e - 3) {
			return Block.DIRT;
		}
		if (y < h + e) {
			return (noise2D.sample(x+9632, y+3578, 1, 1.5f) > 0.5f ? Block.DIRT : Block.STONE);
		}

		// from 0 to 1 as y increases
//		float depth = clamp((y - (h + e)) / 200.0f, 0.0f, 1.0f);

		float v = noise2D.cubicSampling(x * 0.25f, y, 1.0f - 0.1542f, 0.025f);
		v += noise2D.cubicSampling(x + v * 10, y, 0.1542f, 0.05f);

		// solid
//		if (v < 1.0f - depth * 0.5f) {
//			return Block.STONE;
//		} else {
//			return Block.LAVA;
//		}
		return Block.STONE;
	}

}
