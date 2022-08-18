package opengl;

public interface OpenGLSurface {
    int getID();
    int getWidth();
    int getHeight();
    void bindAsTexture(int unit);
	void unbindAsTexture(int unit);
}
