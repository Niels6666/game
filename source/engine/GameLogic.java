package engine;

public interface GameLogic {

	public void init() throws Exception;

	public void input(Window window);

	public void update();

	public void render(Window window);

	public void save();
	
	public void setWindowTitle(Window window);
}
