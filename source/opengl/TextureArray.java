package opengl;

import static org.lwjgl.opengl.GL46C.*;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.GL46C;
import org.lwjgl.stb.STBImage;

public class TextureArray {
	public int id;
	public int width;
	public int height;
	public int depth;

	public TextureArray(int width, int height, int depth, int internalformat, 
				int minFilter, int magFilter) {
		id = glGenTextures();

		this.width = width;
		this.height = height;
		this.depth = depth;
		
		GL46C.glBindTexture(GL46C.GL_TEXTURE_2D_ARRAY, id);
		GL46C.glTexImage3D(GL46C.GL_TEXTURE_2D_ARRAY, 0, 
				internalformat, width, height, depth, 0, GL46C.GL_RGBA,
				GL46C.GL_UNSIGNED_BYTE, (ByteBuffer) null);
		GL46C.glTexParameteri(GL46C.GL_TEXTURE_2D_ARRAY, GL46C.GL_TEXTURE_MIN_FILTER, minFilter);
		GL46C.glTexParameteri(GL46C.GL_TEXTURE_2D_ARRAY, GL46C.GL_TEXTURE_MAG_FILTER, magFilter);
		GL46C.glTexParameteri(GL46C.GL_TEXTURE_2D_ARRAY, GL46C.GL_TEXTURE_WRAP_S, GL46C.GL_REPEAT);
		GL46C.glTexParameteri(GL46C.GL_TEXTURE_2D_ARRAY, GL46C.GL_TEXTURE_WRAP_T, GL46C.GL_REPEAT);
		GL46C.glBindTexture(GL46C.GL_TEXTURE_2D_ARRAY, 0);
	}

	public void clearLayer(int layer) {
		glClearTexSubImage(id, 0, 0, 0, layer, width, height, 1, GL46C.GL_RGBA, GL46C.GL_UNSIGNED_BYTE,
				(ByteBuffer) null);
	}

	public void createLayer(int layer, String path) {
		System.out.println("Creating layer " + layer + " of a texture array using " + path);

		int width[] = new int[1];
		int height[] = new int[1];
		int channels[] = new int[1];
		ByteBuffer buff = STBImage.stbi_load(path, width, height, channels, 4);

		assert (this.width == width[0]);
		assert (this.height == height[0]);
		
		bind();
		GL46C.glTexSubImage3D(GL_TEXTURE_2D_ARRAY, 0, 0, 0, layer, this.width, this.height, 1, GL46C.GL_RGBA,
				GL46C.GL_UNSIGNED_BYTE, buff);
		unbind();
	}

	public void genMipMaps() {
		GL46C.glGenerateTextureMipmap(id);
	}

	public void bind() {
		glBindTexture(GL_TEXTURE_2D_ARRAY, id);
	}

	public void unbind() {
		glBindTexture(GL_TEXTURE_2D_ARRAY, 0);
	}

	public void bindAsTexture(int unit) {
		glActiveTexture(GL_TEXTURE0 + unit);
		glBindTexture(GL_TEXTURE_2D_ARRAY, id);
	}

	public void unbindAsTexture(int unit) {
		glActiveTexture(GL_TEXTURE0 + unit);
		glBindTexture(GL_TEXTURE_2D_ARRAY, 0);
	}

	public void delete() {
		glDeleteTextures(id);
	}

	public int getID() {
		return id;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

}
