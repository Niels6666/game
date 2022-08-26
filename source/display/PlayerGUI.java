package display;

import static org.lwjgl.nuklear.Nuklear.*;
import static org.lwjgl.system.MemoryStack.stackPush;

import java.awt.geom.Rectangle2D;
import java.util.List;

import org.joml.Vector4i;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.NkImage;
import org.lwjgl.nuklear.NkRect;
import org.lwjgl.nuklear.NkStyle;
import org.lwjgl.nuklear.NkStyleButton;
import org.lwjgl.nuklear.NkVec2;
import org.lwjgl.system.MemoryStack;

import engine.Game;
import engine.Window;
import terrain.Block;

/**
 * a set of nuklear windows
 */
public class PlayerGUI implements Component {
	public Game game;

	class ToolBar implements Component {
		List<Block> items;
		int itemSize = 40;
		int border = 2;
		
		int selected = 0;

		public ToolBar(List<Block> items) {
			this.items = items;
		}

		@Override
		public void layout(Window window, NkContext ctx, int x, int y) {
			try (MemoryStack stack = stackPush()) {
				NkRect rect = NkRect.malloc(stack);
				nk_rect(x, y, window.getWidth(), 100, rect);
				if (nk_begin(ctx, title(), rect, NK_WINDOW_DYNAMIC)) {

					nk_layout_row_begin(ctx, NK_LAYOUT_STATIC, itemSize + border, items.size() + 2);

					NkStyleButton style = ctx.style().button();
					style.border(border);

					items.forEach(b -> {
						nk_layout_row_push(ctx, itemSize + border);
						NkImage img = NkImage.create().handle(h -> h.id(b.textures[0].id));
						style.border_color(items.indexOf(b) == selected ? Color.cyan : Color.gray);
						nk_button_image_styled(ctx, style, img);
					});
					nk_layout_row_end(ctx);
					
					nk_layout_row_dynamic(ctx, 0, 1);
					nk_label_colored(ctx, items.get(selected).name().toLowerCase(), NK_TEXT_CENTERED, Color.cyan);
				}
				nk_end(ctx);
			}
		}

		@Override
		public String title() {
			return "toolbar";
		}

		int itemWidth() {
			return itemSize + border;
		}
	}

	public ToolBar bar;

	public PlayerGUI(Game game) {
		this.game = game;
		bar = new ToolBar(List.of(Block.values()));
	}

	@Override
	public void layout(Window window, NkContext ctx, int x, int y) {
		NkStyle old = NkStyle.create().set(ctx.style());
		//do all the things
		ctx.style().window().background().set(Color.transparent);
		bar.layout(window, ctx, 0, 0);
		
		
		//reset style
		ctx.style().set(old);
	}

	@Override
	public String title() {
		return null;
	}

	public Block getBlock() {
		return bar.items.get(bar.selected);
	}

	public void select(double scroll) {
		bar.selected = (bar.selected + bar.items.size() - (int) scroll) % bar.items.size();
	}
}
