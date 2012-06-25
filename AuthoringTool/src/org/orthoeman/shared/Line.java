package org.orthoeman.shared;

public class Line extends Drawing {
	private Point a;
	private Point b;

	public Line(Point a, Point b) {
		super(Type.LINE);
		this.a = new Point(a);
		this.b = new Point(b);
	}

	public Line(Line line) {
		this(line.a, line.b);
	}

	public Line() {
		this(new Point(), new Point());
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

	public void set(int xa, int ya, int xb, int yb) {
		getA().set(xa, ya);
		getB().set(xb, yb);
	}

	@Override
	public Drawing toImage(Zoom zoom) {
		return new Line(a.toImage(zoom), b.toImage(zoom));
	}

	@Override
	public Drawing toCanvas(Zoom zoom) {
		return new Line(a.toCanvas(zoom), b.toCanvas(zoom));
	}
}
