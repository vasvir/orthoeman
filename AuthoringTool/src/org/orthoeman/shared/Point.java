package org.orthoeman.shared;

import com.allen_sauer.gwt.log.client.Log;

public class Point {
	public int x;
	public int y;
	public boolean valid;

	public Point() {
	}

	public Point(int x, int y, boolean valid) {
		set(x, y);
		this.valid = valid;
	}

	public Point(int x, int y) {
		this(x, y, true);
	}

	public Point(Point p) {
		this(p.x, p.y, p.valid);
	}

	public void set(int x, int y) {
		this.x = x;
		this.y = y;
	}

	@Override
	public String toString() {
		return "Point:" + x + ":" + y;
	}

	public Point toImage(Zoom zoom) {
		final double level = zoom.getLevel();
		final Rectangle rect = zoom.getTarget();
		final Point point = new Point((int) (x / level) + rect.getX(),
				(int) (y / level) + rect.getY());
		Log.trace("toImage: level " + level + " target " + rect + " from "
				+ this + " to " + point);
		return point;
	}

	public Point toCanvas(Zoom zoom) {
		final double level = zoom.getLevel();
		final Rectangle rect = zoom.getTarget();
		final Point point = new Point((int) ((x - rect.getX()) * level),
				(int) ((y - rect.getY()) * level));
		Log.trace("toCanvas: level " + level + " target " + rect + " from "
				+ this + " to " + point);
		return point;
	}
}
