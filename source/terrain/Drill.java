package terrain;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;
import org.joml.RoundingMode;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL46C;

import engine.Game;
import engine.Window;
import opengl.Shader;
import opengl.Texture;
import terrain.Entity.Orientation;

public class Drill extends Entity {

	Texture cockpitTex, drillBitTex;

	Light headlight1, headlight2;

	float angle = 0;
	public Vector2f drillDir = new Vector2f();
	Vector2f lastLightPos = null;

	List<Vector2f> links = new ArrayList<>();
	List<Vector2f> trajectories = new ArrayList<>();

	private static final float cockpitRadius = 2.5f;
	private static final Vector2f cockpitSize = new Vector2f(cockpitRadius);
	private static final float PI = (float) Math.PI;
	private static final float TWO_PI = (float) Math.PI * 2.0f;

	protected Drill(Vector2f position) {
		super(position, cockpitSize); // cockpit size

		headlight1 = new Light(position, 20);
		headlight2 = new Light(position, 20);

		try {
			cockpitTex = new Texture("images/cockpit.png");
			drillBitTex = new Texture("images/textures/vehicles/drill/drill.png");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		for(int i=0; i<5; i++) {
			Vector2f p = new Vector2f(position).add(cockpitRadius*2*(i+1), 0);
			links.add(p);
			trajectories.add(p);
		}
	}

	float normalize_angle(float x) {
		// return in [-PI, PI[
		return ((x + PI + TWO_PI) % TWO_PI) - PI;
	}

	@Override
	protected boolean internalUpdate(World w, Game game, Window window) {

		if (window.gui.debug.controllingDrill) {
			Vector2f mouseWorldCoords = w.getMouseWorldCoords(window, game);

			float drillSpeed = 20.0f / 60.0f;
			float currentSpeed = 0;

			//keyboard control
			if (window.keyPressed(GLFW.GLFW_KEY_W) || window.keyPressed(GLFW.GLFW_KEY_UP)
					|| window.keyPressed(GLFW.GLFW_KEY_SPACE)) {
				
				if (window.keyPressed(GLFW.GLFW_KEY_A) || window.keyPressed(GLFW.GLFW_KEY_LEFT)) {
					angle -= 0.05f;
				}
				if (window.keyPressed(GLFW.GLFW_KEY_D) || window.keyPressed(GLFW.GLFW_KEY_RIGHT)) {
					angle += 0.05f;
				}
				
				currentSpeed = drillSpeed;
			}
			
			if (window.rmb()) {

				Vector2f direction = new Vector2f(mouseWorldCoords).sub(position);
				direction.normalize();
				float mouseAngle = (float) Math.atan2(direction.y, direction.x);
	
				float angleDiff = normalize_angle(mouseAngle - angle);
				float maxRotationSpeed = 0.1f;
				angle += Math.max(Math.min(angleDiff * 0.05f, maxRotationSpeed), -maxRotationSpeed);
				
				currentSpeed = drillSpeed;
			}
			
			
			angle = normalize_angle(angle);
			drillDir.set((float) Math.cos(angle), (float) Math.sin(angle));
			
			position.add(drillDir.x * currentSpeed, drillDir.y * currentSpeed);

			float headlightOffset = PI / 4;

			headlight1.pos.set(position).add(cockpitRadius * (float) Math.cos(angle + headlightOffset),
					cockpitRadius * (float) Math.sin(angle + headlightOffset));
			headlight1.angle = PI / 12;
			headlight1.direction.set(mouseWorldCoords).sub(headlight1.pos).normalize();
			headlight1.radius = 25;
			headlight1.innerRadius = 5;
			headlight1.update();

			headlight2.pos.set(position).add(cockpitRadius * (float) Math.cos(angle - headlightOffset),
					cockpitRadius * (float) Math.sin(angle - headlightOffset));
			headlight2.angle = PI / 12;
			headlight2.direction.set(mouseWorldCoords).sub(headlight2.pos).normalize();
			headlight2.radius = 25;
			headlight2.innerRadius = 5;
			headlight2.update();

			if (currentSpeed > 0) {
				dig(w);

				if (lastLightPos == null) {
					lastLightPos = new Vector2f(position);
				} else {
					float lightDist = 20;
					if (lastLightPos.distanceSquared(position) > lightDist * lightDist) {
						w.setBlock(new Vector2i().set(position, RoundingMode.FLOOR), Block.CANDLE);
						lastLightPos.set(position);
					}
				}
			}
			
			trajectories.add(0, new Vector2f(position));
			float L = 0;
			Vector2f prevPos = position;
			int k = 0;//link index
			for(int i=1; i<trajectories.size(); i++) {
				if(k == links.size()) {
					trajectories.remove(i);
					continue;
				}
				
				Vector2f p = trajectories.get(i);
				Vector2f v = new Vector2f(p).sub(prevPos);
				float l = v.length();
				
				float d = (k+1) * cockpitRadius * 2.0f;
				if(L < d && d <= L+l) {
					links.get(k).set(prevPos).lerp(p, (d-L)/l);
					k++;
				}
				
				L+=l;
				prevPos = p;
			}
			

		}

		return false;
	}

	static float sign(Vector2f p1, Vector2f p2, Vector2f p3) {
		return (p1.x - p3.x) * (p2.y - p3.y) - (p2.x - p3.x) * (p1.y - p3.y);
	}

	// https://stackoverflow.com/questions/2049582/how-to-determine-if-a-point-is-in-a-2d-triangle
	static boolean PointInTriangle(Vector2f pt, Vector2f v1, Vector2f v2, Vector2f v3) {

		float d1, d2, d3;
		boolean has_neg, has_pos;

		d1 = sign(pt, v1, v2);
		d2 = sign(pt, v2, v3);
		d3 = sign(pt, v3, v1);

		has_neg = (d1 < 0) || (d2 < 0) || (d3 < 0);
		has_pos = (d1 > 0) || (d2 > 0) || (d3 > 0);

		return !(has_neg && has_pos);
	}

	private void dig(World w) {

		float drillWidth = cockpitRadius + 1;
		float drillLength = cockpitRadius + 1;

		Vector2f a = new Vector2f(position).add(drillDir.x * cockpitRadius - drillDir.y * drillWidth,
				drillDir.y * cockpitRadius + drillDir.x * drillWidth);

		Vector2f b = new Vector2f(position).add(drillDir.x * cockpitRadius + drillDir.y * drillWidth,
				drillDir.y * cockpitRadius - drillDir.x * drillWidth);

		Vector2f c = new Vector2f(position).add(drillDir.x * (cockpitRadius + drillLength),
				drillDir.y * (cockpitRadius + drillLength));

		Vector2f cornerMin = new Vector2f(a).min(b).min(c);
		Vector2f cornerMax = new Vector2f(a).max(b).max(c);

		for (int y = (int) Math.floor(cornerMin.y); y <= (int) Math.ceil(cornerMax.y); y++) {
			for (int x = (int) Math.floor(cornerMin.x); x <= (int) Math.ceil(cornerMax.x); x++) {

				Vector2f p = new Vector2f(x + 0.5f, y + 0.5f);
				if (PointInTriangle(p, a, b, c)) {
					w.setBlock(new Vector2i(x, y), Block.AIR);
				}

			}
		}

	}

	@Override
	protected void internalDelete() {
		cockpitTex.delete();
		drillBitTex.delete();
	}

	@Override
	public void render(Shader entityShader, Matrix4f worldToNDC) {

		for(int i=0; i<links.size(); i++) {
			//draw links
			// Draw cockpit
			Vector2f position = links.get(i);
			Matrix4f cockpitTransform = new Matrix4f(worldToNDC).translate(position.x, position.y, 0).scale(cockpitSize.x,
					cockpitSize.y, 1);
			entityShader.loadMat4("transform", cockpitTransform);

			cockpitTex.bindAsTexture(0);
			// glowTexture.get(animation).bindAsTexture(1);
			GL46C.glDrawArrays(GL46C.GL_TRIANGLE_STRIP, 0, 4);// draw quad
			cockpitTex.unbindAsTexture(0);
			// glowTexture.get(animation).unbindAsTexture(1);

		}

		// Draw cockpit
		Matrix4f cockpitTransform = new Matrix4f(worldToNDC).translate(position.x, position.y, 0).scale(cockpitSize.x,
				cockpitSize.y, 1);
		entityShader.loadMat4("transform", cockpitTransform);

		cockpitTex.bindAsTexture(0);
		// glowTexture.get(animation).bindAsTexture(1);
		GL46C.glDrawArrays(GL46C.GL_TRIANGLE_STRIP, 0, 4);// draw quad
		cockpitTex.unbindAsTexture(0);
		// glowTexture.get(animation).unbindAsTexture(1);

		// Draw drill bit
		Matrix4f drillBitTransform = new Matrix4f(worldToNDC)
				.translate(position.x + cockpitRadius * drillDir.x, position.y + cockpitRadius * drillDir.y, 0)
				.rotateZ(angle).scale(cockpitRadius, cockpitRadius, 1);
		entityShader.loadMat4("transform", drillBitTransform);

		drillBitTex.bindAsTexture(0);
		// glowTexture.get(animation).bindAsTexture(1);
		GL46C.glDrawArrays(GL46C.GL_TRIANGLE_STRIP, 0, 4);// draw quad
		drillBitTex.unbindAsTexture(0);
		// glowTexture.get(animation).unbindAsTexture(1);

	}

}
