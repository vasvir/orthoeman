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

	@Override
	public double distance(Point point) {
		final double A = point.x - a.x;
		final double B = point.y - a.y;
		final double C = b.x - a.x;
		final double D = b.y - a.y;

		final double dot = A * C + B * D;
		final double len_sq = C * C + D * D;
		final double param = dot / len_sq;

		double xx, yy;

		if (param < 0 || (a.x == b.x && a.y == b.y)) {
			xx = a.x;
			yy = a.y;
		} else if (param > 1) {
			xx = b.x;
			yy = b.y;
		} else {
			xx = a.x + param * C;
			yy = a.y + param * D;
		}

		final double dx = point.x - xx;
		final double dy = point.y - yy;
		return Math.sqrt(dx * dx + dy * dy);
	}
}
