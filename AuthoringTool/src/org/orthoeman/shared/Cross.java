package org.orthoeman.shared;

public class Cross extends Point {
	public Cross(Kind kind) {
		super(Type.CROSS, kind);
	}

	public Cross(Kind kind, Point point) {
		this(kind);
		set(point.x, point.y);
	}

	@Override
	public String toString() {
		return "Cross:" + x + ":" + y;
	}

	@Override
	public Cross toImage(Zoom zoom) {
		return new Cross(getKind(), super.toImage(zoom));
	}

	@Override
	public Cross toCanvas(Zoom zoom) {
		return new Cross(getKind(), super.toCanvas(zoom));
	}

	@Override
	public double distance(Point point) {
		return Math.min(Math.abs(point.x - x), Math.abs(point.y - y));
	}
}
