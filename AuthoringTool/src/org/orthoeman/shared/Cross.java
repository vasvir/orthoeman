package org.orthoeman.shared;

public class Cross extends Point {
	public Cross() {
		super();
		setType(Type.CROSS);
	}

	public Cross(Point point) {
		super(point);
		setType(Type.CROSS);
	}

	@Override
	public String toString() {
		return "Cross:" + x + ":" + y;
	}

	@Override
	public Cross toImage(Zoom zoom) {
		return new Cross(super.toImage(zoom));
	}

	@Override
	public Cross toCanvas(Zoom zoom) {
		return new Cross(super.toCanvas(zoom));
	}
}
