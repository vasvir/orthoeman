package org.orthoeman.shared;

public class Zoom {
	public static enum Type {
		ZOOM_121, ZOOM_LEVEL, ZOOM_TO_FIT, ZOOM_TARGET
	}

	private Type type;
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
	}
	
	public void decreaseLevel() {
	}

	public Rectangle getTarget() {
		return target;
	}

	public void setTarget(Rectangle target) {
		this.target = target;
	}
}