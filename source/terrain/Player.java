package terrain;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;

import engine.Game;
import engine.Window;
import opengl.Texture;

public class Player extends Entity {
	Orientation orientation = Orientation.FRONT;
	List<Texture> texture = new ArrayList<>();
	List<Texture> glowTexture = new ArrayList<>();

	Vector2f velocity = new Vector2f();

	static final Vector2f PlayerSize = new Vector2f(1.0f);
	static final float moveSpeed = 5f;
	static final float gravity = +40;

	public Player(Vector2f position) {
		super(position, new Vector2f(PlayerSize));

		try {
			texture.add(new Texture("images/entity/player/player_front.png"));
			texture.add(new Texture("images/entity/player/player_left.png"));
			texture.add(new Texture("images/entity/player/player_right.png"));

			glowTexture.add(new Texture("images/entity/player/player_front_glow.png"));
			glowTexture.add(new Texture("images/entity/player/player_left_glow.png"));
			glowTexture.add(new Texture("images/entity/player/player_right_glow.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	Matrix4f getTransform() {
		Matrix4f transform = new Matrix4f();
		// s 0 0 tx
		// 0 s 0 ty
		// 0 0 1 0
		// 0 0 0 1

		transform.m00(PlayerSize.x);
		transform.m11(PlayerSize.y);

		transform.m30(position.x);
		transform.m31(position.y);

		return transform;
	}

	protected boolean internalUpdate(World w, Game game, Window window) {
		orientation = Orientation.FRONT;

		float timeStep = 1.0f / 60.0f;

		Vector2f delta = new Vector2f(velocity).mul(timeStep);
		
		boolean wantsToJump = false;

		if (!window.gui.debug.freeCamera) {
			if (window.keyPressed(GLFW.GLFW_KEY_A) || window.keyPressed(GLFW.GLFW_KEY_LEFT)) {
				delta.x -= moveSpeed * timeStep;
			}
			if (window.keyPressed(GLFW.GLFW_KEY_D) || window.keyPressed(GLFW.GLFW_KEY_RIGHT)) {
				delta.x += moveSpeed * timeStep;
			}
			if (window.keyPressed(GLFW.GLFW_KEY_W) || window.keyPressed(GLFW.GLFW_KEY_UP)) {
				wantsToJump = true;
			}
			if (window.keyPressed(GLFW.GLFW_KEY_S) || window.keyPressed(GLFW.GLFW_KEY_DOWN)) {
				//delta.y += moveSpeed * timeStep;
			}
		}
		
		velocity.y += gravity * timeStep;

		if(w.getBlock(position).isCollideable){
			velocity.set(0.0f); //stuck in a wall
		}

		Vector2f hitPoint = new Vector2f();


		if(delta.x > 0){
			boolean ok = true;
			hitPoint.x = position.x + PlayerSize.x + delta.x;
			hitPoint.y = position.y - PlayerSize.y;

			int n = (int)(2.0f * PlayerSize.y) + 1;
			for(int i=0; i<=n; i++) {
				ok &= !w.getBlock(hitPoint).isCollideable;
				hitPoint.y += 2.0f * PlayerSize.y / n;
			}
			
			if(!ok){
				delta.x = (float)Math.floor(hitPoint.x) - (position.x + PlayerSize.x);
				velocity.x = 0;
			}
		}else if(delta.x < 0){
			boolean ok = true;
			hitPoint.x = position.x - PlayerSize.x + delta.x;
			hitPoint.y = position.y - PlayerSize.y;
			
			int n = (int)(2.0f * PlayerSize.y) + 1;
			for(int i=0; i<=n; i++) {
				ok &= !w.getBlock(hitPoint).isCollideable;
				hitPoint.y += 2.0f * PlayerSize.y / n;
			}
			
			if(!ok){
				delta.x = (float)Math.ceil(hitPoint.x) - (position.x - PlayerSize.x);
				velocity.x = 0;
			}
		}

		if(delta.y > 0){
			boolean ok = true;
			hitPoint.y = position.y + PlayerSize.y + delta.y;
			hitPoint.x = position.x - PlayerSize.x;

			int n = (int)(2.0f * PlayerSize.x) + 1;
			for(int i=0; i<=n; i++) {
				ok &= !w.getBlock(hitPoint).isCollideable;
				hitPoint.x += 2.0f * PlayerSize.x / n;
			}
			
			if(!ok){
				delta.y = (float)Math.floor(hitPoint.y) - (position.y + PlayerSize.y);
				velocity.y = 0;
				
				if(wantsToJump) {
					velocity.y -= 20;
				}
			}
		}else if(delta.y < 0) {
			boolean ok = true;
			hitPoint.y = position.y - PlayerSize.y + delta.y;
			hitPoint.x = position.x - PlayerSize.x;

			int n = (int)(2.0f * PlayerSize.x) + 1;
			for(int i=0; i<=n; i++) {
				ok &= !w.getBlock(hitPoint).isCollideable;
				hitPoint.x += 2.0f * PlayerSize.x / n;
			}
			
			if(!ok){
				delta.y = (float)Math.ceil(hitPoint.y) - (position.y - PlayerSize.y);
				velocity.y = 0;
			}
		}
		
		if (delta.x > 0) {
			orientation = Orientation.RIGHT;
		} else if (delta.x < 0) {
			orientation = Orientation.LEFT;
		}
	
		position.add(delta);
		return true;
	}

	protected void internalDelete() {
		texture.forEach(Texture::delete);
		glowTexture.forEach(Texture::delete);
	}

	Texture getTexture() {
		return texture.get(orientation.ordinal());
	}

	Texture getGlowTexture() {
		return glowTexture.get(orientation.ordinal());
	}
}
