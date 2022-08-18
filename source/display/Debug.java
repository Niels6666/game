package display;

import static org.lwjgl.nuklear.Nuklear.*;
import static org.lwjgl.system.MemoryStack.stackPush;

import java.awt.geom.Rectangle2D;

import org.lwjgl.nuklear.NkColor;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.NkImage;
import org.lwjgl.nuklear.NkRect;
import org.lwjgl.nuklear.NkStyleButton;
import org.lwjgl.nuklear.NkVec2;
import org.lwjgl.system.MemoryStack;

import engine.Game;
import engine.Window;
import opengl.Texture;

public class Debug implements Component {

	enum AllTextures {
		color, light, postprocess, light1, light2, light3;
	}

	int flags = NK_WINDOW_TITLE | NK_WINDOW_DYNAMIC;
	boolean visible = true;
	public int fps = 0;
	public long lastCall = System.currentTimeMillis();

	private Game game;

	public AllTextures selected_texture = AllTextures.postprocess;
	public boolean isBloomEnabled = true;
	public int bloomCascades = 10;
	public float bloomWeight = 1.0f;
	public float toneMappingExposure = 0.25f;
	public float glowPower = 1.0f;
	public boolean rebuildBloomCascades = false;
	public int scaleFactor = 2;
	
	public boolean freeCamera = true;

	public Debug(NkContext ctx, Game game) {
		this.game = game;
	}

	public void layout(Window window, NkContext ctx, int x, int y) {
		if (!visible) {
			return;
		}
		try (MemoryStack stack = stackPush()) {
			NkRect rect = NkRect.malloc(stack);
			nk_rect(x, y, 500, window.getWidth(), rect);

			NkStyleButton close = ctx.style().window().header().close_button();
			close.normal().data().color().set(Color.transparent);
			close.text_normal().set(Color.white);
			close.hover().data().color().set(Color.red);
			close.text_hover().set(Color.black);

			NkStyleButton minim = ctx.style().window().header().minimize_button();
			minim.normal().data().color().set(Color.transparent);
			minim.text_normal().set(Color.white);
			minim.hover().data().color().set(Color.lightGray);
			minim.text_hover().set(Color.black);

			ctx.style().window().header().active().data().color().set(Color.create(20, 20, 20, 100));
			ctx.style().window().background().set(Color.create(20, 20, 20, 100));

			if (nk_begin(ctx, title(), rect, flags)) {
				float height = 25;
				NkColor info = Color.create(100, 255, 100);

				nk_layout_row_dynamic(ctx, height, 1);
				nk_label_colored(ctx, "fps:" + fps, NK_TEXT_LEFT, info);

				nk_layout_row_dynamic(ctx, height, 1);
				nk_label_colored(ctx, "mouse:" + window.cursorPos(), NK_TEXT_LEFT, info);

				nk_layout_row_dynamic(ctx, height, 1);
				nk_label_colored(ctx, "camera:" + game.cameraPos(), NK_TEXT_LEFT, info);
				
				nk_layout_row_dynamic(ctx, height, 1);
				nk_label_colored(ctx, "player:" + game.w.entities.get(0).getPosition(), NK_TEXT_LEFT, info);

				nk_layout_row_dynamic(ctx, height, 1);
				nk_label_colored(ctx, "number of lights:" + game.w.lights.size(), NK_TEXT_LEFT, info);

				nk_layout_row_dynamic(ctx, height, 1);
				
				rebuildBloomCascades = false;
				int res = nk_propertyi(ctx, "Bloom cascades", 1, bloomCascades, 16, 1, 0.005f);
				if(res != bloomCascades){
					bloomCascades = res;
					rebuildBloomCascades = true;
				}
				
				bloomWeight = nk_propertyf(ctx, "Bloom weight", 0, bloomWeight, 2, 0.01f, 0.005f);
				toneMappingExposure = nk_propertyf(ctx, "Exposure", 0, toneMappingExposure, 100, 0.01f, 0.005f);
				glowPower = nk_propertyf(ctx, "Glow power", 0, glowPower, 100, 0.01f, 0.005f);
				scaleFactor = nk_propertyi(ctx, "scale factor", 1, scaleFactor, 5, 1, 0.005f);
				//public float bloomWeight = 1.0f;
				//public float toneMappingExposure = 1.0f;
				//public int bloomCascades = 4;
				nk_layout_row_dynamic(ctx, height, 1);
				isBloomEnabled = nk_check_text(ctx, "enable bloom", isBloomEnabled);
				
				nk_layout_row_dynamic(ctx, height, 1);
				freeCamera = nk_check_text(ctx, "free camera", freeCamera);

//				nk_layout_row_dynamic(ctx, height, 1);
//
//				if (nk_combo_begin_text(ctx, selected_texture.name(),
//						NkVec2.malloc(stack).set(nk_widget_width(ctx), AllTextures.values().length * height))) {
//					for (var item : AllTextures.values()) {
//						nk_layout_row_dynamic(ctx, height, 1);
//						if (nk_combo_item_label(ctx, item.name(), NK_LEFT)) {
//							selected_texture = item;
//						}
//					}
//					nk_combo_end(ctx);
//				}
//				NkImage image = NkImage.create();
//				Texture tex = null;
//				switch (selected_texture) {
//				case color:
//					tex = game.w.colorTexture;
//					break;
//				case light:
//					tex = game.w.lightTexture.get(0);
//					break;
//				case light1:
//					tex = game.w.lightTexture.get(1);
//					break;
//				case light2:
//					tex = game.w.lightTexture.get(2);
//					break;
//				case light3:
//					tex = game.w.lightTexture.get(3);
//					break;
//				case postprocess:
//					tex = game.w.postProcessTex;
//					break;
//				}
//				image.handle().id(tex.id);
//				nk_layout_row_static(ctx, 400 * tex.height / tex.width, 400, 1);
//				nk_image(ctx, image);
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
		return "Debug";
	}

	@Override
	public int flags() {
		return flags;
	}

	public void show() {
		if (System.currentTimeMillis() - lastCall > 100) {
			visible = !visible;
		}
		lastCall = System.currentTimeMillis();
	}
}
