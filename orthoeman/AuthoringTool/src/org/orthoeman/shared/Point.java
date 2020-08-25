package org.orthoeman.shared;

import java.util.Collection;

public class Point extends Drawing {
	public int x;
	public int y;
	public boolean valid;

	public Point() {
		super(null, null);
	}

	public Point(Type type, Kind kind) {
		super(type, kind);
	}

	public Point(int x, int y, boolean valid) {
		this();
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
		//Log.trace("toImage: level " + level + " target " + rect + " from "
		//		+ this + " to " + point);
		return point;
	}

	@Override
	public Point toCanvas(Zoom zoom) {
		final double level = zoom.getLevel();
		final Rectangle rect = zoom.getTarget();
		final Point point = new Point((int) ((x - rect.getX()) * level),
				(int) ((y - rect.getY()) * level));
		//Log.trace("toCanvas: level " + level + " target " + rect + " from "
		//		+ this + " to " + point);
		return point;
	}

	@Override
	public double distance(Point point) {
		return Math.sqrt((x - point.x) * (x - point.x) + (y - point.y)
				* (y - point.y));
	}

	public static Point getNearestPoint(Collection<Point> points,
			Point query_point) {
		Point min_distance_point = null;
		double min_distance = Double.MAX_VALUE;

		for (final Point point : points) {
			final double distance = query_point.distance(point);
			if (distance < min_distance) {
				min_distance = distance;
				min_distance_point = point;
			}
		}
		return min_distance_point;
	}
}
