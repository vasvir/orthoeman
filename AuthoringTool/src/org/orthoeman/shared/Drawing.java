package org.orthoeman.shared;

public class Drawing {
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
}
