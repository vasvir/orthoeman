package org.orthoeman.shared;

public class Ellipse extends Drawing {
	private int x;
	private int y;
	private int width;
	private int height;

	public Ellipse(int x, int y, int width, int height) {
		super(Type.ELLIPSE);
		setX(x);
		setY(y);
		setWidth(width);
		setHeight(height);
	}
	
	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}
}