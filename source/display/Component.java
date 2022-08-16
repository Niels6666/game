package display;

import static org.lwjgl.nuklear.Nuklear.*;
import static org.lwjgl.system.MemoryStack.stackPush;

import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.NkRect;
import org.lwjgl.system.MemoryStack;

import engine.Window;

public interface Component {
	public void layout(Window window, NkContext ctx, int x, int y);

	public String title();

	public int flags();
}
