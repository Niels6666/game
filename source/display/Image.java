package display;

import java.io.IOException;

import org.lwjgl.nuklear.NkImage;

import opengl.Texture;

public class Image {
	public Texture tex;
	public NkImage nk;
	
	public Image(String path) {
		try {
			tex = new Texture(path);
			nk = NkImage.create();
			nk.handle(it -> it.id(tex.id));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
