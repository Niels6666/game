package terrain.noise;

public class Noise3D extends Noise {

	public Noise3D(long seed, int size, float minValue, float maxValue) {
		super(seed, size, 3, minValue, maxValue);
	}

	public float sample(float x, float y, float z, float amplitude, float frequency) {
		x *= frequency;
		int intX = toInt(x) & texture_mask;

		y *= frequency;
		int intY = toInt(y) & texture_mask;

		z *= frequency;
		int intZ = toInt(z) & texture_mask;

		float density = noiseTexture[texture_size * (intZ * texture_size + intY) + intX];
		return amplitude * density;
	}

	public float linearSampling(float x, float y, float z, float amplitude, float frequency) {
		x *= frequency;
		int intX = toInt(x);
		float fracSamplingCoordsX = x - intX;
		intX &= texture_mask;
		int intX_plus_1 = (intX + 1) & texture_mask;

		y *= frequency;
		int intY = toInt(y);
		float fracSamplingCoordsY = y - intY;
		intY &= texture_mask;
		int intY_plus_1 = (intY + 1) & texture_mask;
		
		z *= frequency;
		int intZ = toInt(z);
		float fracSamplingCoordsZ = z - intZ;
		intZ &= texture_mask;
		int intZ_plus_1 = (intZ + 1) & texture_mask;

		float v0 = noiseTexture[texture_size * (intZ * texture_size + intY) + intX];
		float v1 = noiseTexture[texture_size * (intZ * texture_size + intY) + intX_plus_1];

		float v2 = noiseTexture[texture_size * (intZ * texture_size + intY_plus_1) + intX];
		float v3 = noiseTexture[texture_size * (intZ * texture_size + intY_plus_1) + intX_plus_1];

		float v4 = noiseTexture[texture_size * (intZ_plus_1 * texture_size + intY) + intX];
		float v5 = noiseTexture[texture_size * (intZ_plus_1 * texture_size + intY) + intX_plus_1];

		float v6 = noiseTexture[texture_size * (intZ_plus_1 * texture_size + intY_plus_1) + intX];
		float v7 = noiseTexture[texture_size * (intZ_plus_1 * texture_size + intY_plus_1) + intX_plus_1];

		float i1 = Interpolation.linearInterpolation(v0, v1, fracSamplingCoordsX);
		float i2 = Interpolation.linearInterpolation(v2, v3, fracSamplingCoordsX);
		float i3 = Interpolation.linearInterpolation(v4, v5, fracSamplingCoordsX);
		float i4 = Interpolation.linearInterpolation(v6, v7, fracSamplingCoordsX);

		float i5 = Interpolation.linearInterpolation(i1, i2, fracSamplingCoordsY);
		float i6 = Interpolation.linearInterpolation(i3, i4, fracSamplingCoordsY);

		return amplitude * Interpolation.linearInterpolation(i5, i6, fracSamplingCoordsZ);
	}

	public float cubicSampling(float x, float y, float z, float amplitude, float frequency) {
		x *= frequency;
		int intX = toInt(x);
		float fracSamplingCoordsX = x - intX;
		intX &= texture_mask;
		int intX_plus_1 = (intX + 1) & texture_mask;

		y *= frequency;
		int intY = toInt(y);
		float fracSamplingCoordsY = y - intY;
		intY &= texture_mask;
		int intY_plus_1 = (intY + 1) & texture_mask;
		
		z *= frequency;
		int intZ = toInt(z);
		float fracSamplingCoordsZ = z - intZ;
		intZ &= texture_mask;
		int intZ_plus_1 = (intZ + 1) & texture_mask;

		float v0 = noiseTexture[texture_size * (intZ * texture_size + intY) + intX];
		float v1 = noiseTexture[texture_size * (intZ * texture_size + intY) + intX_plus_1];

		float v2 = noiseTexture[texture_size * (intZ * texture_size + intY_plus_1) + intX];
		float v3 = noiseTexture[texture_size * (intZ * texture_size + intY_plus_1) + intX_plus_1];

		float v4 = noiseTexture[texture_size * (intZ_plus_1 * texture_size + intY) + intX];
		float v5 = noiseTexture[texture_size * (intZ_plus_1 * texture_size + intY) + intX_plus_1];

		float v6 = noiseTexture[texture_size * (intZ_plus_1 * texture_size + intY_plus_1) + intX];
		float v7 = noiseTexture[texture_size * (intZ_plus_1 * texture_size + intY_plus_1) + intX_plus_1];

		float i1 = Interpolation.cubicInterpolation(v0, v1, fracSamplingCoordsX);
		float i2 = Interpolation.cubicInterpolation(v2, v3, fracSamplingCoordsX);
		float i3 = Interpolation.cubicInterpolation(v4, v5, fracSamplingCoordsX);
		float i4 = Interpolation.cubicInterpolation(v6, v7, fracSamplingCoordsX);

		float i5 = Interpolation.cubicInterpolation(i1, i2, fracSamplingCoordsY);
		float i6 = Interpolation.cubicInterpolation(i3, i4, fracSamplingCoordsY);

		return amplitude * Interpolation.cubicInterpolation(i5, i6, fracSamplingCoordsZ);
	}


}
