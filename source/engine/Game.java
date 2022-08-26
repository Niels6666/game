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

import display.Debug;
import opengl.Camera;
import opengl.MouseInfo;
import terrain.Block;
import terrain.Player;
import terrain.World;

public class Game {
	public World w;
	public Camera cam;
	public Vector2f screenSize;

	public Game() {
	}

	/**
	 * @return a copy of game.cam.pos
	 */
	public Vector2f cameraPos() {
		return new Vector2f(cam.pos);
	}

	public float getZoom() {
		return cam.zoom;
	}

	public void init() throws Exception {
		GL11.glClearColor(1f, 0f, 0f, 1f);
		GL46C.glEnable(GL46C.GL_DEBUG_OUTPUT);

		System.out.println(GL46C.glGetString(GL46C.GL_VERSION));
		System.out.println(GL46C.glGetString(GL46C.GL_VENDOR));
		System.out.println(GL46C.glGetString(GL46C.GL_RENDERER));
		System.out.println(GL46C.glGetString(GL46C.GL_SHADING_LANGUAGE_VERSION));
		// System.out.println(GL46C.glGetString(GL46C.GL_EXTENSIONS));

		cam = new Camera();
		w = new World("save");
	}

	public void input(Window window) {
		if (window.gui.debug.freeCamera) {
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
			
			if(window.keyPressed(GLFW.GLFW_KEY_KP_ENTER)) {
				w.generate();
			}
		} else {

			Vector2f target = w.entities.get(0).getPosition();
			cam.pos.lerp(target, 0.1f);

//			cam.pos.mul(20).round().div(20);

		}

		screenSize = new Vector2f(window.getWidth(), window.getHeight());
		if (!window.events) {
			return;
		}

		double scroll = window.scroll();
		if (scroll != 0) {
			if(window.keyPressed(GLFW.GLFW_KEY_LEFT_CONTROL)) {
				cam.zoom(scroll < 0 ? 1.1f : 0.95f);
			}else {
				window.gui.playerGUI.select(scroll);
			}
		}
		window.resetScroll();
	}

	public void update(Window window) {
		w.update(this, window);
	}

	public void render(Window window) {
		w.render(this, window.gui.debug);
	}

	public void save() {
		w.save();
	}
}
