package terrain.noise;

public class Noise1D extends Noise{

	public Noise1D(long seed, int size, float minValue, float maxValue) {
		super(seed, size, 1, minValue, maxValue);
	}
	
	public float sample(float x, float amplitude, float frequency) {
		x *= frequency;
		int intX = toInt(x) & texture_mask;
		float density = noiseTexture[intX];
		return amplitude * density;
	}
	
	public float linearSampling(float x, float amplitude, float frequency) {
		x *= frequency;
		int intX = toInt(x);
		float fracSamplingCoordsX = x - intX;
		intX &= texture_mask;
		int intX_plus_1 = (intX+1) & texture_mask;
		
		float v0 = noiseTexture[intX];
		float v1 = noiseTexture[intX_plus_1];
		
		return amplitude * Interpolation.linearInterpolation(v0, v1, fracSamplingCoordsX);
	}
	
	public float cubicSampling(float x, float amplitude, float frequency) {
		x *= frequency;
		int intX = toInt(x);
		float fracSamplingCoordsX = x - intX;
		intX &= texture_mask;
		int intX_plus_1 = (intX+1) & texture_mask;
		
		float v0 = noiseTexture[intX];
		float v1 = noiseTexture[intX_plus_1];
		
		return amplitude * Interpolation.cubicInterpolation(v0, v1, fracSamplingCoordsX);
	}
	

}
