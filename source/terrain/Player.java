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
	int animation = 0;
	boolean sprint = false;

	Vector2f velocity = new Vector2f();

	static final Vector2f PlayerSize = new Vector2f(1f, 1f);
	static final Vector2f HitboxSize = new Vector2f(0.3f, 0.95f);
	static final float moveSpeed = 3f;
	static final float gravity = +40;

	Light light;

	public Player(Vector2f position) {
		super(position, new Vector2f(PlayerSize));

		light = new Light(position, 5);
		light.innerRadius = 2;

		try {
			texture.add(new Texture("images/entity/player/player_front.png"));

			texture.add(new Texture("images/entity/player/player_left.png"));
			texture.add(new Texture("images/entity/player/player_left2.png"));
			texture.add(new Texture("images/entity/player/player_right.png"));
			texture.add(new Texture("images/entity/player/player_right2.png"));

			glowTexture.add(new Texture("images/entity/player/player_front_glow.png"));
			glowTexture.add(new Texture("images/entity/player/player_left_glow.png"));
			glowTexture.add(new Texture("images/entity/player/player_left_glow2.png"));
			glowTexture.add(new Texture("images/entity/player/player_right_glow.png"));
			glowTexture.add(new Texture("images/entity/player/player_right_glow2.png"));
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

		float timeStep = 1.0f / Math.max(60, window.gui.debug.fps);

		Vector2f delta = new Vector2f(velocity).mul(timeStep);

		boolean wantsToJump = false;

		if (!window.gui.debug.freeCamera) {
			orientation = Orientation.FRONT;
			sprint = window.keyPressed(GLFW.GLFW_KEY_LEFT_SHIFT);

			if (window.keyPressed(GLFW.GLFW_KEY_A) || window.keyPressed(GLFW.GLFW_KEY_LEFT)) {
				delta.x -= (moveSpeed + (sprint ? 7 : 0)) * timeStep;
				orientation = Orientation.LEFT;
			}
			if (window.keyPressed(GLFW.GLFW_KEY_D) || window.keyPressed(GLFW.GLFW_KEY_RIGHT)) {
				delta.x += (moveSpeed + (sprint ? 7 : 0)) * timeStep;
				orientation = Orientation.RIGHT;
			}
			if (window.keyPressed(GLFW.GLFW_KEY_W) || window.keyPressed(GLFW.GLFW_KEY_UP)
					|| window.keyPressed(GLFW.GLFW_KEY_SPACE)) {
				wantsToJump = true;
			}
		}

		if (sprint) {
			velocity.x = velocity.x * 0.9f + delta.x * 0.1f;
		} else {
			velocity.x = 0f;
		}
		velocity.y += gravity * timeStep;

		if (w.getBlock(position).isCollideable) {
			velocity.set(0.0f); // stuck in a wall
		}

		Vector2f hitPoint = new Vector2f();

		if (delta.x > 0) {
			boolean ok = true;
			hitPoint.x = position.x + HitboxSize.x + delta.x;
			hitPoint.y = position.y - HitboxSize.y;

			int n = (int) (2.0f * HitboxSize.y) + 1;
			for (int i = 0; i <= n; i++) {
				ok &= !w.getBlock(hitPoint).isCollideable || hitPoint.y == (int) (hitPoint.y);
				hitPoint.y += 2.0f * HitboxSize.y / n;
			}

			if (!ok) {
				delta.x = (float) Math.floor(hitPoint.x) - (position.x + HitboxSize.x);
				velocity.x = 0;
			}
		} else if (delta.x < 0) {
			boolean ok = true;
			hitPoint.x = position.x - HitboxSize.x + delta.x;
			hitPoint.y = position.y - HitboxSize.y;

			int n = (int) (2.0f * HitboxSize.y) + 1;
			for (int i = 0; i <= n; i++) {
				ok &= !w.getBlock(hitPoint).isCollideable || hitPoint.y == (int) (hitPoint.y);
				hitPoint.y += 2.0f * HitboxSize.y / n;
			}

			if (!ok) {
				delta.x = (float) Math.ceil(hitPoint.x) - (position.x - HitboxSize.x);
				velocity.x = 0;
			}
		}

		if (delta.y > 0) {
			boolean ok = true;
			hitPoint.y = position.y + HitboxSize.y + delta.y;
			hitPoint.x = position.x - HitboxSize.x;

			int n = (int) (2.0f * HitboxSize.x) + 1;
			for (int i = 0; i <= n; i++) {
				ok &= !w.getBlock(hitPoint).isCollideable || hitPoint.x == (int) (hitPoint.x);
				hitPoint.x += 2.0f * HitboxSize.x / n;
			}

			if (!ok) {
				delta.y = (float) Math.floor(hitPoint.y) - (position.y + HitboxSize.y);
				velocity.y = 0;

				if (wantsToJump) {
					velocity.y -= 10;
				}
			}
		} else if (delta.y < 0) {
			boolean ok = true;
			hitPoint.y = position.y - HitboxSize.y + delta.y;
			hitPoint.x = position.x - HitboxSize.x;

			int n = (int) (2.0f * HitboxSize.x) + 1;
			for (int i = 0; i <= n; i++) {
				ok &= !w.getBlock(hitPoint).isCollideable || hitPoint.x == (int) (hitPoint.x);
				hitPoint.x += 2.0f * HitboxSize.x / n;
			}

			if (!ok) {
				delta.y = (float) Math.ceil(hitPoint.y) - (position.y - HitboxSize.y);
				velocity.y = 0;
			}
		}

		position.add(delta);

		Vector2f screenSize = new Vector2f(window.getWidth(), window.getHeight());
		Vector2f mouseCoords = window.cursorPos();

		Vector2f mouseNDCCoords = new Vector2f(mouseCoords).div(screenSize).mul(new Vector2f(2.0f, -2.0f)).add(-1.0f,
				1.0f);
		Vector2f mouseTexCoords = new Vector2f(mouseNDCCoords).mul(0.5f, -0.5f).add(0.5f, 0.5f);
		Vector2f worldCoords = new Vector2f(mouseTexCoords).add(-0.5f, -0.5f).mul(screenSize)
				.mul(game.getZoom() / World.blockPixelHeight).add(game.cameraPos());

		Vector2f direction = new Vector2f(worldCoords).sub(position);
		direction.normalize();

		light.direction.set(direction);
		light.angle = (float) (Math.PI / 2);
		light.radius = 15;

		light.pos.set(position).add(0, -0.95f);

		light.update();

		int offset = (Math.cos(w.totalFrames / (sprint ? 1.5 : 3)) < 0 ? 0 : 1);

		switch (orientation) {
		case FRONT:
			animation = 0;
			break;
		case LEFT:
			animation = 1 + offset;
			break;
		case RIGHT:
			animation = 3 + offset;
			break;
		}
		return true;
	}

	protected void internalDelete() {
		texture.forEach(Texture::delete);
		glowTexture.forEach(Texture::delete);
	}

	Texture getTexture() {
		return texture.get(animation);
	}

	Texture getGlowTexture() {
		return glowTexture.get(animation);
	}
}
