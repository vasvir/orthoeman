package org.orthoeman.shared;

public class Point {
	public int x;
	public int y;
	public boolean valid;

	public Point() {
	}

	public Point(int x, int y, boolean valid) {
		this.x = x;
		this.y = y;
		this.valid = valid;
	}

	public Point(int x, int y) {
		this(x, y, true);
	}

	public Point(Point p) {
		this(p.x, p.y, p.valid);
	}
}
