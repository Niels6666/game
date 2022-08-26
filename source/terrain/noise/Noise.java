package terrain.noise;

import java.util.Random;

public abstract class Noise {

	protected final float[] noiseTexture;

	protected final int texture_size; // must be a power of 2 !
	protected final int texture_mask;
	protected final int dimensions;

	Noise(long seed, int size, int dimensions, float minValue, float maxValue) {
		if (size <= 0 || ((size & (size - 1)) != 0)) {
			throw new IllegalArgumentException("The noise texture size must be a power of 2: " + size);
		}
		this.texture_size = size;
		this.texture_mask = size - 1;
		this.dimensions = dimensions;

		int texture_length = 1;
		for (int i = 0; i < dimensions; i++) {
			texture_length *= texture_size;
		}
		noiseTexture = new float[texture_length];

		generateNoiseTexture(seed, minValue, maxValue);
	}

	public void generateNoiseTexture(long seed, float minValue, float maxValue) {
		Random random = new Random(seed);

		float amplitude = maxValue - minValue;
		for (int i = 0; i < noiseTexture.length; i++) {
			noiseTexture[i] = amplitude * random.nextFloat() + minValue;
		}
	}

	protected int toInt(float coord) {
		return (coord > 0) ? (int) coord : ((int) coord) - 1;
	}

}
