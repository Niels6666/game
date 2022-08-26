package display;

import org.lwjgl.nuklear.NkContext;

import engine.Window;

public interface Component {
	public void layout(Window window, NkContext ctx, int x, int y);

	public String title();
}
