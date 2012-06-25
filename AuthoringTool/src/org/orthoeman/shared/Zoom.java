package org.orthoeman.shared;

public class Zoom {
	public static enum Type {
		ZOOM_121, ZOOM_LEVEL, ZOOM_TO_FIT_WIDTH, ZOOM_TARGET
	}

	private Zoom.Type type = Type.ZOOM_TO_FIT_WIDTH;
	private double level = 1;
	private Rectangle target = new Rectangle();

	public Zoom.Type getType() {
		return type;
	}

	public void setType(Zoom.Type type) {
		this.type = type;
	}

	public double getLevel() {
		return level;
	}

	public void setLevel(double level) {
		this.level = level;
	}

	public void increaseLevel() {
		level *= 1.2;
	}

	public void decreaseLevel() {
		level /= 1.2;
	}

	public Rectangle getTarget() {
		return target;
	}
}