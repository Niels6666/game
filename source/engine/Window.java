package engine;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.glfwGetWindowMonitor;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowMonitor;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.nuklear.Nuklear.NK_WINDOW_BORDER;
import static org.lwjgl.nuklear.Nuklear.NK_WINDOW_CLOSABLE;
import static org.lwjgl.nuklear.Nuklear.NK_WINDOW_MINIMIZABLE;
import static org.lwjgl.nuklear.Nuklear.NK_WINDOW_TITLE;
import static org.lwjgl.nuklear.Nuklear.nk_begin;
import static org.lwjgl.nuklear.Nuklear.nk_end;
import static org.lwjgl.nuklear.Nuklear.nk_rect;
import static org.lwjgl.nuklear.Nuklear.nk_window_is_closed;
import static org.lwjgl.system.MemoryUtil.NULL;

import java.nio.IntBuffer;
import java.util.Objects;

import org.joml.Vector2f;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCursorPosCallbackI;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWScrollCallbackI;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.NkRect;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import display.GUI;

public class Window {
	private long window;
	private int width;
	private int height;
	public String title;
	private double scroll = 0;
	private Vector2f mousePos = new Vector2f();

	public GUI gui;

	public boolean events = true;

	public Window(int width, int height, String title) {
		this.width = width;
		this.height = height;
		this.title = title;
		gui = new GUI();
	}

	public void init(Game game) {
		GLFWErrorCallback.createPrint(System.err).set();

		if (!GLFW.glfwInit())
			throw new IllegalStateException("Unable to initialize GLFW");

		GLFW.glfwDefaultWindowHints();
		GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 4);
		GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 6);
		GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
		GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GL11.GL_TRUE);
		GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
		GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);
		GLFW.glfwWindowHint(GLFW.GLFW_DECORATED, GLFW.GLFW_TRUE);

		// Create the window
		window = GLFW.glfwCreateWindow(width, height, title, MemoryUtil.NULL, MemoryUtil.NULL);
		if (window == MemoryUtil.NULL)
			throw new RuntimeException("Failed to create the GLFW window");

		GLFW.glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
			if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_RELEASE)
				GLFW.glfwSetWindowShouldClose(window, true);
		});

		GLFW.glfwSetFramebufferSizeCallback(window, (window, w, h) -> {
			width = w;
			height = h;
		});

		GLFWScrollCallbackI guiSCB = gui.getScrollCallBack();
		GLFW.glfwSetScrollCallback(window, (window, xoffset, yoffset) -> {
			if (events) {
				scroll = yoffset;
				guiSCB.invoke(window, 0, 0);
			} else {
				guiSCB.invoke(window, xoffset, yoffset);
			}
		});

		GLFWCursorPosCallbackI guiCCB = gui.getCursorPosCallBack();
		GLFW.glfwSetCursorPosCallback(window, (window, xpos, ypos) -> {
			mousePos.set(xpos, ypos);
			guiCCB.invoke(window, xpos, ypos);
		});

		// Get the thread stack and push a new frame
		try (MemoryStack stack = MemoryStack.stackPush()) {
			IntBuffer pWidth = stack.mallocInt(1); // int*
			IntBuffer pHeight = stack.mallocInt(1); // int*

			// Get the window size passed to glfwCreateWindow
			GLFW.glfwGetWindowSize(window, pWidth, pHeight);

			// Get the resolution of the primary monitor
			GLFWVidMode vidmode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());
			
			// Center the window
			GLFW.glfwSetWindowPos(window, (vidmode.width() - pWidth.get(0)) / 2,
					(vidmode.height() - pHeight.get(0)) / 2);
		} // the stack frame is popped automatically
			// Make the OpenGL context current
		GLFW.glfwMakeContextCurrent(window);
		GL.createCapabilities();

		gui.init(this, game);

		// Enable v-sync
		GLFW.glfwSwapInterval(0);

		// Make the window visible
		GLFW.glfwShowWindow(window);
	}

	public void renderGUI() {
		gui.render(this);
	}

	public void beforeInput() {
		gui.input(this);
	}

	public boolean keyPressed(int keyCode) {
		return GLFW.glfwGetKey(window, keyCode) == GLFW.GLFW_PRESS;
	}

	public Vector2f cursorPos() {
		return mousePos;
	}

	public boolean rmb() {
		return GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
	}

	public boolean lmb() {
		return GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
	}

	public double scroll() {
		return scroll;
	}

	public void resetScroll() {
		scroll = 0;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public long getID() {
		return window;
	}

	public void destroy() {
		gui.destroy();
		glfwFreeCallbacks(window);
		GLFW.glfwDestroyWindow(window);
	}

	/**
	 * must be called before any rendering process
	 */
	public void beforeRender() {
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
		GL11.glViewport(0, 0, getWidth(), getHeight());
	}

	/**
	 * must be called after any rendering process
	 */
	public void afterRender() {
		GLFW.glfwSwapBuffers(window);
	}
}
