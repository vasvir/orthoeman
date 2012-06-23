package org.orthoeman.shared;

public class Zoom {
	public static enum Type {
		ZOOM_121, ZOOM_LEVEL, ZOOM_TO_FIT_WIDTH, ZOOM_TARGET
	}

	private Type type = Type.ZOOM_TO_FIT_WIDTH;
	private double level = 1;
	private Rectangle target;

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
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

	public void setTarget(Rectangle target) {
		this.target = target;
	}
}
