package display;

import org.lwjgl.nuklear.NkColor;
import org.lwjgl.nuklear.NkColorf;

public class Color {

	/**
	 * copy of awt colors
	 */
	public static final NkColor white = create(255, 255, 255), lightGray = create(192, 192, 192),
			gray = create(128, 128, 128), darkGray = create(64, 64, 64), black = create(0, 0, 0),
			red = create(255, 0, 0), pink = create(255, 175, 175), orange = create(255, 200, 0),
			yellow = create(255, 255, 0), green = create(0, 255, 0), magenta = create(255, 0, 255),
			cyan = create(0, 255, 255), blue = create(0, 0, 255);

	/**
	 * alpha = 0
	 */
	public static final NkColor transparent = create(0, 0, 0, 0);

	public static NkColor create(int r, int g, int b, int a) {
		return create((byte) r, (byte) g, (byte) b, (byte) a);
	}

	public static NkColor create(int r, int g, int b) {
		return create(r, g, b, 255);
	}

	public static NkColor create(byte r, byte g, byte b, byte a) {
		return NkColor.create().set(r, g, b, a);
	}

	public Color(float r, float g, float b) {
		this(r, g, b, 1f);
	}

	public Color(float r, float g, float b, float a) {
		color = NkColorf.create().set(r, g, b, a);
	}

	public NkColorf get() {
		return color;
	}

	public NkColorf color;
}
