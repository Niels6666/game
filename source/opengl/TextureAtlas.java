package opengl;

import java.util.List;

import org.lwjgl.opengl.GL46C;

import terrain.World;

public class TextureAtlas extends Texture {
	public TextureAtlas(List<Texture> textures) {
		super((int) Math.ceil(Math.sqrt(textures.size())) * World.blockPixelHeight,
				(int) Math.ceil(Math.sqrt(textures.size())) * World.blockPixelHeight, GL46C.GL_RGBA8, GL46C.GL_LINEAR);

		int drawY = 0;
		int currentCol = 0;
		for (Texture t : textures) {
			if (t != null) {
				GL46C.glCopyImageSubData(t.id, GL46C.GL_TEXTURE_2D, 0, 0, 0, 0, id, GL46C.GL_TEXTURE_2D, 0,
						currentCol * World.blockPixelHeight, drawY, 0, World.blockPixelHeight, World.blockPixelHeight,
						1);
			}
			currentCol++;
			if (currentCol > (int) Math.ceil(Math.sqrt(textures.size())) - 1) {
				drawY += World.blockPixelHeight;
				currentCol = 0;
			}
		}
	}
}
