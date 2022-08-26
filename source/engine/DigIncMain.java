package engine;

import org.lwjgl.glfw.GLFW;

public class DigIncMain {
	private static Window window;
	private static Game game;
	private static long lastTime = System.currentTimeMillis();
	
	public static void main(String[] args) {
		int width = 800;
		int height = 600;
		String title = "game";
		window = new Window(width, height, title);
		game = new Game();
		run();
	}
	
	private static void init() {
		try {
			window.init(game);
			game.init();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void run() {
		try {
			init();
			loop();
			window.destroy();
		} catch (Exception excp) {
			excp.printStackTrace();
		} finally {
			GLFW.glfwTerminate();
			GLFW.glfwSetErrorCallback(null).free();
		}
	}

	private static void loop() {
		while (!GLFW.glfwWindowShouldClose(window.getID())) {
			if (System.currentTimeMillis() - lastTime > 1000) {
				window.gui.debug.fps = game.w.frames;
				game.w.frames = 0;
				lastTime = System.currentTimeMillis();
			}
			// first reacts to inputs, then update, finally we can render
			input();
			update();
			render();
		}
		game.save();
	}

	private static void input() {
		window.beforeInput();
		game.input(window);
	}

	private static void update() {
		game.update(window);
	}

	private static void render() {
		window.beforeRender();

		game.render(window);
		window.renderGUI();

		window.afterRender();
	}
}
