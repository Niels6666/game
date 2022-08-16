package engine;

public class DigIncMain {
//	private static long lastTime = 0;
//	private static double FPS = 65;

	public static void main(String[] args) {
		
		
		
//		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
//		try {
//			ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, new File("font/zx-spectrum.ttf")));
//		} catch (FontFormatException | IOException e1) {
//			e1.printStackTrace();
//		}
//		Screen sc = new Screen();

//		Timer t = new Timer(1000, (ae) -> {
//			sc.setTitle("Dig Inc | fps : " + World.frames.get());
//			World.frames.set(0);
//		});
//		t.start();
//
//		sc.setVisible(true);
//		
//		while (true) {
//			sc.repaint();
//			try {
//				cap();
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}
		
		GameLogic logic = new Game();
		try {
			GameEngine ge = new GameEngine("Window", 800, 600, logic);
			ge.start();
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("a fatal exception occured, the program wil exit");
			System.exit(0);
		}
		
//		new GUI().run();
	}

//	private static void cap() throws InterruptedException {
//		double wait = Math.round(1000.0 / FPS);
//		long currentTime = System.currentTimeMillis();
//		long diff = currentTime - lastTime;
//		if (diff >= wait) {
//			lastTime = System.currentTimeMillis();
//			return;
//		}
//		Thread.sleep(Math.round(wait - diff));
//		lastTime = System.currentTimeMillis();
//	}
}
