package org.orthoeman.shared;

public class Drawing {
	public enum Type {
		RECTANGLE, ELLIPSE, POLYGON, LINE
	}

	private Type type;

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}
}
