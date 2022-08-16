package engine;

import javax.swing.Timer;

import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

public class GameEngine implements Runnable {
	private final Thread gameLoopThread;
	private Window window;
	private GameLogic gameLogic;
	private long lastTime = System.currentTimeMillis();

	public GameEngine(String windowTitle, int width, int height, GameLogic gameLogic) throws Exception {
		gameLoopThread = new Thread(this, "GAME_LOOP_THREAD");
		window = new Window(width, height, windowTitle);
		this.gameLogic = gameLogic;
	}

	public void start() {
		gameLoopThread.start();
	}

	public void init() {
		try {
			window.init((Game) gameLogic);
			gameLogic.init();
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
				gameLogic.setWindowTitle(window);
				lastTime = System.currentTimeMillis();
			}
			// first reacts to inputs, then update, finally we can render
			input();
			update();
			render();
		}
		gameLogic.save();
	}

	protected void input() {
		window.beforeInput();
		gameLogic.input(window);
	}

	protected void update() {
		gameLogic.update();
	}

	protected void render() {
		window.beforeRender();

		gameLogic.render(window);
		window.renderGUI();

		window.afterRender();
	}
}
