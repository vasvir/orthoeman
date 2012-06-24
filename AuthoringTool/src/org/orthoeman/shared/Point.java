package org.orthoeman.shared;

public class Point {
	public double x;
	public double y;
	public boolean valid;

	public Point() {
	}

	public Point(double x, double y, boolean valid) {
		this.x = x;
		this.y = y;
		this.valid = valid;
	}

	public Point(double x, double y) {
		this(x, y, true);
	}

	public Point(Point p) {
		this(p.x, p.y, p.valid);
	}
}
