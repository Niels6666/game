package terrain.noise;

public class Noise2D extends Noise{

	public Noise2D(long seed, int size, float minValue, float maxValue) {
		super(seed, size, 2, minValue, maxValue);
	}
	
	public float sample(float x, float y, float amplitude, float frequency) {
		x *= frequency;
		int intX = toInt(x) & texture_mask;
		
		y *= frequency;
		int intY = toInt(y) & texture_mask;
		
		float density = noiseTexture[intY * texture_size + intX];
		return amplitude * density;
	}
	
	public float linearSampling(float x, float y, float amplitude, float frequency) {
		x *= frequency;
		int intX = toInt(x);
		float fracSamplingCoordsX = x - intX;
		intX &= texture_mask;
		int intX_plus_1 = (intX+1) & texture_mask;
		
		y *= frequency;
		int intY = toInt(y);
		float fracSamplingCoordsY = y - intY;
		intY &= texture_mask;
		int intY_plus_1 = (intY+1) & texture_mask;
		
		float v0 = noiseTexture[intY * texture_size + intX];
		float v1 = noiseTexture[intY * texture_size + intX_plus_1];
		
		float v2 = noiseTexture[intY_plus_1 * texture_size + intX];
		float v3 = noiseTexture[intY_plus_1 * texture_size + intX_plus_1];
		
		float i1 = Interpolation.linearInterpolation(v0, v1, fracSamplingCoordsX);
		float i2 = Interpolation.linearInterpolation(v2, v3, fracSamplingCoordsX);
		
		return amplitude * Interpolation.linearInterpolation(i1, i2, fracSamplingCoordsY);
	}
	
	public float cubicSampling(float x, float y, float amplitude, float frequency) {
		x *= frequency;
		int intX = toInt(x);
		float fracSamplingCoordsX = x - intX;
		intX &= texture_mask;
		int intX_plus_1 = (intX+1) & texture_mask;
		
		y *= frequency;
		int intY = toInt(y);
		float fracSamplingCoordsY = y - intY;
		intY &= texture_mask;
		int intY_plus_1 = (intY+1) & texture_mask;
		
		float v0 = noiseTexture[intY * texture_size + intX];
		float v1 = noiseTexture[intY * texture_size + intX_plus_1];
		
		float v2 = noiseTexture[intY_plus_1 * texture_size + intX];
		float v3 = noiseTexture[intY_plus_1 * texture_size + intX_plus_1];
		
		float i1 = Interpolation.cubicInterpolation(v0, v1, fracSamplingCoordsX);
		float i2 = Interpolation.cubicInterpolation(v2, v3, fracSamplingCoordsX);
		
		return amplitude * Interpolation.cubicInterpolation(i1, i2, fracSamplingCoordsY);
	}
	
}
