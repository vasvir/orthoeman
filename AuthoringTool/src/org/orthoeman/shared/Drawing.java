package org.orthoeman.shared;

public abstract class Drawing {
	public enum Type {
		RECTANGLE, ELLIPSE, POLYGON, LINE, ERASER
	}

	private Type type;

	public Drawing(Type type) {
		setType(type);
	}
	
	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}
	
	public abstract Drawing toImage(Zoom zoom);
	public abstract Drawing toCanvas(Zoom zoom);
}
