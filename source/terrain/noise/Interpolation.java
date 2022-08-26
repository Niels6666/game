package terrain.noise;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class Interpolation {

	public static float linearInterpolation(float a, float b, float amount) {
		return a + (b - a) * amount;
	}

	public static double linearInterpolation(double a, double b, double amount) {
		return a + (b - a) * amount;
	}

	public static Vector4f linearInterpolation(Vector4f a, Vector4f b, Vector4f dest, float amount) {
		if (dest == null) {
			dest = new Vector4f();
		}
		dest.set(linearInterpolation(a.x, b.x, amount), linearInterpolation(a.y, b.y, amount),
				linearInterpolation(a.z, b.z, amount), linearInterpolation(a.w, b.w, amount));
		return dest;
	}

	public static Vector3f linearInterpolation(Vector3f a, Vector3f b, Vector3f dest, float amount) {
		if (dest == null) {
			dest = new Vector3f();
		}
		dest.set(linearInterpolation(a.x, b.x, amount), linearInterpolation(a.y, b.y, amount),
				linearInterpolation(a.z, b.z, amount));
		return dest;
	}

	public static Vector2f linearInterpolation(Vector2f a, Vector2f b, Vector2f dest, float amount) {
		if (dest == null) {
			dest = new Vector2f();
		}
		dest.set(linearInterpolation(a.x, b.x, amount), linearInterpolation(a.y, b.y, amount));
		return dest;
	}

	public static float cubicInterpolation(float a, float b, float amount) {
		amount = amount * amount * (3.0f - 2.0f * amount);
		return a + (b - a) * amount;
	}

	public static double cubicInterpolation(double a, double b, double amount) {
		amount = amount * amount * (3.0 - 2.0 * amount);
		return a + (b - a) * amount;
	}

	public static Vector4f cubicInterpolation(Vector4f a, Vector4f b, Vector4f dest, float amount) {
		if (dest == null) {
			dest = new Vector4f();
		}
		dest.set(cubicInterpolation(a.x, b.x, amount), cubicInterpolation(a.y, b.y, amount),
				cubicInterpolation(a.z, b.z, amount), cubicInterpolation(a.w, b.w, amount));
		return dest;
	}

	public static Vector3f cubicInterpolation(Vector3f a, Vector3f b, Vector3f dest, float amount) {
		if (dest == null) {
			dest = new Vector3f();
		}
		dest.set(cubicInterpolation(a.x, b.x, amount), cubicInterpolation(a.y, b.y, amount),
				cubicInterpolation(a.z, b.z, amount));
		return dest;
	}

	public static Vector2f cubicInterpolation(Vector2f a, Vector2f b, Vector2f dest, float amount) {
		if (dest == null) {
			dest = new Vector2f();
		}
		dest.set(cubicInterpolation(a.x, b.x, amount), cubicInterpolation(a.y, b.y, amount));
		return dest;
	}

	public static float angularInterpolation(float a, float b, float amount) {

		a = a % 360f;
		b = b % 360f;
		if (a < 0)
			a += 360;
		if (b < 0)
			b += 360;

		if (Math.abs(a - b) < 180) {
			return linearInterpolation(a, b, amount);
		}

		if (a < b) {
			a += 360;
		} else {
			b += 360;
		}

		float angle = linearInterpolation(a, b, amount);
		if (angle > 360)
			angle -= 360;

		return angle;
	}

}
