package engine;

import static org.lwjgl.glfw.GLFW.glfwInit;

import java.awt.Color;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import javax.swing.Timer;

import org.joml.Vector2f;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL46C;

import opengl.Camera;
import opengl.MouseInfo;
import terrain.BlockIDs;
import terrain.World;

public class Game implements GameLogic {
	public World w;
	public Camera cam;
	public MouseInfo mouse;
	public Vector2f screenSize;

	public Game() {
	}

	public Vector2f getOrigin() {
		return new Vector2f(cam.pos);
	}

	public float getZoom() {
		return cam.zoom;
	}

	@Override
	public void init() throws Exception {
		GL11.glClearColor(1f, 0f, 0f, 1f);
		GL46C.glEnable(GL46C.GL_DEBUG_OUTPUT);

		System.out.println(GL46C.glGetString(GL46C.GL_VERSION));
		System.out.println(GL46C.glGetString(GL46C.GL_VENDOR));
		System.out.println(GL46C.glGetString(GL46C.GL_RENDERER));
		System.out.println(GL46C.glGetString(GL46C.GL_SHADING_LANGUAGE_VERSION));
		System.out.println(GL46C.glGetString(GL46C.GL_EXTENSIONS));

		cam = new Camera();
		w = new World("save");
	}

	@Override
	public void input(Window window) {
		if (!window.events) {
			screenSize = new Vector2f(window.getWidth(), window.getHeight());
			mouse = null;
			window.resetScroll();
			return;
		}
		if (window.keyPressed(GLFW.GLFW_KEY_A) || window.keyPressed(GLFW.GLFW_KEY_LEFT)) {
			cam.moveH(-1);
		}

		if (window.keyPressed(GLFW.GLFW_KEY_D) || window.keyPressed(GLFW.GLFW_KEY_RIGHT)) {
			cam.moveH(1);
		}

		if (window.keyPressed(GLFW.GLFW_KEY_W) || window.keyPressed(GLFW.GLFW_KEY_UP)) {
			cam.moveV(-1);
		}

		if (window.keyPressed(GLFW.GLFW_KEY_S) || window.keyPressed(GLFW.GLFW_KEY_DOWN)) {
			cam.moveV(1);
		}

		if (window.keyPressed(GLFW.GLFW_KEY_ENTER)) {
			cam.reset();
		}
		screenSize = new Vector2f(window.getWidth(), window.getHeight());
		mouse = new MouseInfo(window);
		if (mouse.scroll() != 0) {
			cam.zoom(mouse.scroll() < 0 ? 1.1f : 0.95f);
		}
		window.resetScroll();
	}

	@Override
	public void update() {
		if (mouse != null) {
			BlockIDs dig = BlockIDs.CANDLE;
			if (mouse.rmb()) {
				dig = BlockIDs.URANIUM;
			}
			w.update(getOrigin(), getZoom(), new Vector2f(screenSize), mouse.pos(), mouse.lmb() || mouse.rmb(), dig);
		}
	}

	@Override
	public void render(Window window) {
		w.render(this, window.gui.debug);
	}

	public void save() {
		w.save();
	}

	@Override
	public void setWindowTitle(Window window) {
		if (window.gui.debug != null) {
			window.gui.debug.fps = w.frames;
			w.frames = 0;
		}
	}
}
