package org.orthoeman.shared;

import com.allen_sauer.gwt.log.client.Log;

public class Point extends Drawing {
	public int x;
	public int y;
	public boolean valid;

	public Point() {
		super(null);
	}

	public Point(int x, int y, boolean valid) {
		super(null);
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

	@Override
	public Point toImage(Zoom zoom) {
		final double level = zoom.getLevel();
		final Rectangle rect = zoom.getTarget();
		final Point point = new Point((int) (x / level) + rect.getX(),
				(int) (y / level) + rect.getY());
		Log.trace("toImage: level " + level + " target " + rect + " from "
				+ this + " to " + point);
		return point;
	}

	@Override
	public Point toCanvas(Zoom zoom) {
		final double level = zoom.getLevel();
		final Rectangle rect = zoom.getTarget();
		final Point point = new Point((int) ((x - rect.getX()) * level),
				(int) ((y - rect.getY()) * level));
		Log.trace("toCanvas: level " + level + " target " + rect + " from "
				+ this + " to " + point);
		return point;
	}

	@Override
	public double distance(Point point) {
		return Math.sqrt((x - point.x) * (x - point.x) + (y - point.y)
				* (y - point.y));
	}
}
