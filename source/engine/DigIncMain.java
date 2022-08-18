package engine;

public class DigIncMain {
	public static void main(String[] args) {
		try {
			GameEngine ge = new GameEngine("GAME !", 800, 600);
			ge.start();
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("a fatal exception occured, the program will exit");
			System.exit(0);
		}
	}
}
