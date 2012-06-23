package org.orthoeman.shared;

public class Rectangle extends Drawing {
	private int x;
	private int y;
	private int width;
	private int height;

	public Rectangle() {
	}

	public Rectangle(int x, int y, int width, int heigth) {
		set(x, y, width, heigth);
	}

	public void set(int x, int y, int width, int heigth) {
		setX(x);
		setY(y);
		setWidth(width);
		setHeight(heigth);
	}

	public void set(Rectangle target) {
		set(target.getX(), target.getY(), target.getWidth(), target.getHeight());
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

	@Override
	public String toString() {
		return "R:" + x + ":" + y + ":" + width + ":" + height;
	}
}
