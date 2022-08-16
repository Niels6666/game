package display;

import static org.lwjgl.nuklear.Nuklear.*;
import static org.lwjgl.system.MemoryStack.stackPush;

import java.awt.geom.Rectangle2D;
import java.io.IOException;

import org.joml.Vector2f;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.NkHandle;
import org.lwjgl.nuklear.NkImage;
import org.lwjgl.nuklear.NkRect;
import org.lwjgl.nuklear.NkStyleButton;
import org.lwjgl.nuklear.NkVec2;
import org.lwjgl.system.MemoryStack;

import engine.Window;
import opengl.Texture;

public class Menu implements Component {
	int[] paddings = new int[3];

	final int flags = NK_WINDOW_DYNAMIC | NK_WINDOW_MINIMIZABLE | NK_WINDOW_TITLE;

	public Menu(NkContext ctx) {
	}

	public void layout(Window window, NkContext ctx, int x, int y) {
		try (MemoryStack stack = stackPush()) {
			NkRect rect = NkRect.malloc(stack);
			nk_rect(x, y, 300, window.getHeight(), rect);
			if (nk_begin(ctx, title(), rect, flags)) {
				float height = 30;
				NkStyleButton sb = NkStyleButton.create();
				sb.text_alignment(NK_TEXT_LEFT);
				sb.text_active().set(Color.white);
				sb.text_hover().set(Color.lightGray);
				sb.text_normal().set(Color.gray);

				sb.padding().x(paddings[0]);
				nk_layout_row_dynamic(ctx, height, 1);
				if (nk_button_label_styled(ctx, sb, "Play")) {
				}

				// button hovered
				if (ctx.last_widget_state() == 82) {
					paddings[0] += 2;
					if (paddings[0] > 30)
						paddings[0] = 30;
				} else {
					paddings[0] = Math.max(paddings[0] - 2, 0);
				}

				sb.padding().x(paddings[1]);
				nk_layout_row_dynamic(ctx, height, 1);
				if (nk_button_label_styled(ctx, sb, "Options")) {
				}

				// button hovered
				if (ctx.last_widget_state() == 82) {
					paddings[1] += 2;
					if (paddings[1] > 30)
						paddings[1] = 30;
				} else {
					paddings[1] = Math.max(paddings[1] - 2, 0);
				}

				sb.padding().x(paddings[2]);
				nk_layout_row_dynamic(ctx, height, 1);
				if (nk_button_label_styled(ctx, sb, "Quit")) {
				}

				// button hovered
				if (ctx.last_widget_state() == 82) {
					paddings[2] += 2;
					if (paddings[2] > 30)
						paddings[2] = 30;
				} else {
					paddings[2] = Math.max(paddings[2] - 2, 0);
				}
			}

			NkRect r = NkRect.create();
			nk_window_get_bounds(ctx, r);
			Rectangle2D bounds = new Rectangle2D.Double(r.x(), r.y(), r.w(), r.h());
			NkVec2 v = ctx.input().mouse().pos();
			window.events = !bounds.contains(v.x(), v.y());

			nk_end(ctx);
		}
	}

	@Override
	public String title() {
		return "Menu";
	}

	@Override
	public int flags() {
		return flags;
	}
}
