package testbox2d;

import java.awt.AWTEvent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.ChainShape;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.EdgeShape;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.Shape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;

public class MainClass {
	static World w;
	static Array<Body> bodies = new Array<>(false, 0, Body.class);
	static String t = "";

	public static void main(String[] args) {
		JFrame frame = new JFrame();
		frame.setTitle("test jbox2d");
		frame.setUndecorated(true);
		frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		float width = (1920f / 100f) / 2f;
		float height = (1080f / 100f) / 2f;

		Vector2 gravity = new Vector2(0f, -9.81f);
		w = new World(gravity, true);

		BodyDef dGround = new BodyDef();
		dGround.type = BodyType.StaticBody;
		Body bground = w.createBody(dGround);
		ChainShape ground = new ChainShape();
		List<Vector2> v = new ArrayList<>();
		float r = Math.min(width, height);
		for (float a = 0f; a < 2f * Math.PI; a += 0.01f) {
			v.add(new Vector2(r * (float) Math.cos(a), r * (float) Math.sin(a)));
		}

		ground.createLoop(v.toArray(new Vector2[v.size()]));

//		ground.createLoop(new Vec2[] { //
//				new Vec2(-width, height), //
//				new Vec2(-width, -height), //
//				new Vec2(width, -height), //
//				new Vec2(width, height), //
//		}, 4);
		bground.createFixture(ground, 1f);
		bground.setUserData(Color.white);

		// gear
		BodyDef mixerdef = new BodyDef();
		mixerdef.type = BodyType.KinematicBody;
		Body mixer = w.createBody(mixerdef);
		PolygonShape mixershape = new PolygonShape();
		Vector2[] vertices = new Vector2[] { //
				new Vector2(-height, 0.1f), //
				new Vector2(-height, -0.1f), //
				new Vector2(height, -0.1f), //
				new Vector2(height, 0.1f) //
		};//
		mixershape.set(vertices);
		mixer.createFixture(mixershape, 1f);
		mixer.setUserData(Color.pink);

		Body mixer2 = w.createBody(mixerdef);
		PolygonShape mixershape2 = new PolygonShape();
		Vector2[] vertices2 = new Vector2[] { //
				new Vector2(0.1f, -height), //
				new Vector2(-0.1f, -height), //
				new Vector2(-0.1f, height), //
				new Vector2(0.1f, height) //
		};//
		mixershape2.set(vertices2);
		mixer2.createFixture(mixershape2, 1f);
		mixer2.setUserData(Color.pink);

//		DistanceJointDef def = new DistanceJointDef();
//		def.bodyA = bground;
//		def.bodyB = gear;
//		def.length = 0f;
//		def.dampingRatio = 1f;
//		def.frequencyHz = 1000f;
//		w.createJoint(def);

		JPanel canvas = new JPanel() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				Graphics2D g2d = (Graphics2D) g;
//				g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2d.setColor(Color.black);
				g2d.fillRect(0, 0, getWidth(), getHeight());
				mixer.setAngularVelocity(1f);
				mixer2.setAngularVelocity(1f);

				g2d.setColor(Color.red);
				g2d.drawString(t, 20, 20);
				g2d.drawString("number of balls : " + (w.getBodyCount() - 3), 20, 40);

				g2d.translate(getWidth() / 2f, getHeight() / 2f);
				g2d.scale(100f * 0.8f, -100f * 0.8f);

				// origin
				g2d.setStroke(new BasicStroke(1f / 100f));
				g2d.setColor(Color.green);
				g2d.drawLine(-1, 0, 1, 0);// x
				g2d.setColor(Color.red);
				g2d.drawLine(0, -1, 0, 1);// y

				g2d.setColor(Color.white);
				// lib gdx-type array
				for (Body next : Arrays.copyOf(bodies.items, bodies.size)) {
					Shape s = next.getFixtureList().first().getShape();
					Vector2 pos = next.getPosition();
					AffineTransform baseT = g2d.getTransform();
					g2d.translate(pos.x, pos.y);
					g2d.rotate(next.getAngle());
					g2d.setColor((Color) next.getUserData());

					switch (s.getType()) {
					case Chain:
						drawChain((ChainShape) s, g2d);
						break;
					case Circle:
						drawCircle((CircleShape) s, g2d);
						break;
					case Edge:
						drawEdge((EdgeShape) s, g2d);
						break;
					case Polygon:
						drawPolygon((PolygonShape) s, g2d);
						break;
					}
					g2d.setTransform(baseT);
				}
			}

			void drawPolygon(PolygonShape s, Graphics2D g2d) {
				Vector2 getter = new Vector2();
				for (int i = 0; i < s.getVertexCount(); i++) {
					s.getVertex(i, getter);
					Vector2 current = getter;
					Vector2 next = new Vector2();
					if (i + 1 == s.getVertexCount()) {
						s.getVertex(0, next);
					} else {
						s.getVertex(i + 1, next);

					}
					g2d.draw(new Line2D.Float(current.x, current.y, next.x, next.y));
				}
			}

			void drawEdge(EdgeShape s, Graphics2D g2d) {
				Vector2 p1 = new Vector2();
				s.getVertex1(p1);
				Vector2 p2 = new Vector2();
				s.getVertex1(p2);
				g2d.draw(new Line2D.Float(p1.x, p1.y, p2.x, p2.y));
			}

			void drawCircle(CircleShape s, Graphics2D g2d) {
				float r = s.getRadius();
				g2d.fill(new Ellipse2D.Float(-r, -r, r * 2f, r * 2f));
			}

			void drawChain(ChainShape s, Graphics2D g2d) {
				Vector2 getter = new Vector2();
				for (int i = 0; i < s.getVertexCount(); i++) {
					s.getVertex(i, getter);
					Vector2 current = getter;
					Vector2 next = new Vector2();
					if (i + 1 == s.getVertexCount()) {
						s.getVertex(0, next);
					} else {
						s.getVertex(i + 1, next);

					}
					g2d.draw(new Line2D.Float(current.x, current.y, next.x, next.y));
				}
			}

		};
		canvas.getInputMap().put(KeyStroke.getKeyStroke("ESCAPE"), "quit");
		canvas.getActionMap().put("quit", new Action() {
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}

			public void setEnabled(boolean b) {
			}

			public void removePropertyChangeListener(PropertyChangeListener listener) {
			}

			public void putValue(String key, Object value) {
			}

			public boolean isEnabled() {
				return true;
			}

			public Object getValue(String key) {
				return null;
			}

			public void addPropertyChangeListener(PropertyChangeListener listener) {
			}
		});
		frame.setContentPane(canvas);

		frame.setVisible(true);

		final AWTEventListener listener = new AWTEventListener() {
			public void eventDispatched(AWTEvent event) {
				// Event and component that recieved that event

				MouseEvent me = (MouseEvent) event;
				if (me.getID() == MouseEvent.MOUSE_DRAGGED) {
					Point p = me.getPoint();
					float x = (p.x * 1.25f - (1920f / 2f)) * 0.01f;
					float y = -(p.y * 1.25f - (1080f / 2f)) * 0.01f;
					Vector2 pos = new Vector2(x, y);
					addBody(pos);
				}
			}
		};
		Toolkit.getDefaultToolkit().addAWTEventListener(listener, AWTEvent.MOUSE_MOTION_EVENT_MASK);
		Timer timer = new Timer(16, ae -> frame.repaint());
		timer.start();

		while (true) {
			try {
				Thread.sleep(16);
				synchronized (w) {
					long start = System.nanoTime();
					w.step(1f / 60f, 3, 3);
					String n = new DecimalFormat("#.###").format(((System.nanoTime() - start) * 1.0e-6));
					t = "update took " + n + " ms";
					bodies = new Array<>(false, w.getBodyCount(), Body.class);
					w.getBodies(bodies);
				}
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}

	static void addBody(Vector2 pos) {
		synchronized (w) {
			BodyDef def = new BodyDef();
			def.type = BodyType.DynamicBody;
			Body b = w.createBody(def);
			if (b != null) {
//			final float pi = (float) Math.PI;
//			final float r = 0.2f;
//			PolygonShape s = new PolygonShape();
//			s.set(new Vec2[] { //
//					new Vec2(r * (float) Math.cos(2 * pi * (1f / 3)), r * (float) Math.sin(2 * pi * (1f / 3))), //
//					new Vec2(r * (float) Math.cos(2 * pi * (2f / 3)), r * (float) Math.sin(2 * pi * (2f / 3))), //
//					new Vec2(r * (float) Math.cos(2 * pi * (3f / 3)), r * (float) Math.sin(2 * pi * (3f / 3))) //
//			}, 3);//

				CircleShape s = new CircleShape();
				s.setRadius(0.1f);
				Fixture f = b.createFixture(s, 1f);
				f.setRestitution(0.5f);
//			Random rand = new Random(System.nanoTime());
//			float x = rand.nextFloat(20) - 10f;
//			float y = rand.nextFloat(20) - 10f;
//
//			b.setLinearVelocity(new Vec2(x, y));
				b.setAngularVelocity(2f);
				b.setTransform(pos, 0f);
				b.setUserData(Color.cyan);
			}
		}
	}
}
