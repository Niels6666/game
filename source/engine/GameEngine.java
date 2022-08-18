package engine;

import javax.swing.Timer;

import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

public class GameEngine implements Runnable {
	private final Thread gameLoopThread;
	private Window window;
	private Game game;
	private long lastTime = System.currentTimeMillis();

	public GameEngine(String windowTitle, int width, int height) throws Exception {
		gameLoopThread = new Thread(this, "GAME_LOOP_THREAD");
		window = new Window(width, height, windowTitle);
		this.game = new Game();
	}

	public void start() {
		gameLoopThread.start();
	}

	public void init() {
		try {
			window.init(game);
			game.init();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
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

	private void loop() {
		while (!GLFW.glfwWindowShouldClose(window.getID())) {
			if (System.currentTimeMillis() - lastTime > 1000) {
				game.setWindowTitle(window);
				lastTime = System.currentTimeMillis();
			}
			// first reacts to inputs, then update, finally we can render
			input();
			update();
			render();
		}
		game.save();
	}

	protected void input() {
		window.beforeInput();
		game.input(window);
	}

	protected void update() {
		game.update(window);
	}

	protected void render() {
		window.beforeRender();

		game.render(window);
		window.renderGUI();

		window.afterRender();
	}
}
