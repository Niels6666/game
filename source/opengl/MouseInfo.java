package opengl;

import org.joml.Vector2f;

import engine.Window;

public class MouseInfo {
	public MouseInfo(Window window) {
		pos = window.cursorPos();
		rmb = window.rmb();
		lmb = window.lmb();
		scroll = window.scroll();
	}

	private Vector2f pos;
	private boolean rmb;
	private boolean lmb;
	private double scroll;

	public Vector2f pos() {
		return pos;
	}

	public boolean rmb() {
		return rmb;
	}

	public boolean lmb() {
		return lmb;
	}

	public double scroll() {
		return scroll;
	}
}
