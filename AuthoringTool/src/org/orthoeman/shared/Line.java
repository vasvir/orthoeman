package org.orthoeman.shared;

public class Line extends Drawing {
	private Point a;
	private Point b;

	public Line(Point a, Point b) {
		super(Type.LINE);
		this.a = new Point(a);
		this.b = new Point(b);
	}
	
	public Point getA() {
		return a;
	}

	public void setA(Point a) {
		this.a = a;
	}

	public Point getB() {
		return b;
	}

	public void setB(Point b) {
		this.b = b;
	}

}