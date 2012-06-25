package org.orthoeman.shared;

import com.allen_sauer.gwt.log.client.Log;

public class Rectangle extends Drawing {
	private int x;
	private int y;
	private int width;
	private int height;

	public Rectangle(int x, int y, int width, int heigth) {
		super(Type.RECTANGLE);
		set(x, y, width, heigth);
	}

	public Rectangle(Rectangle rect) {
		this(rect.x, rect.y, rect.width, rect.height);
	}

	public Rectangle() {
		this(0, 0, 0, 0);
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
		return "Rectangle:" + x + ":" + y + ":" + width + ":" + height;
	}

	@Override
	public Drawing toImage(Zoom zoom) {
		final double level = zoom.getLevel();
		final Rectangle rect = zoom.getTarget();
		final Rectangle rectangle = new Rectangle((int) (x / level)
				+ rect.getX(), (int) (y / level) + rect.getY(),
				(int) (width / level), (int) (height / level));
		Log.trace("toImage: level " + level + " target " + rect + " from "
				+ this + " to " + rectangle);
		return rectangle;
	}

	@Override
	public Drawing toCanvas(Zoom zoom) {
		final double level = zoom.getLevel();
		final Rectangle rect = zoom.getTarget();
		final Rectangle rectangle = new Rectangle(
				(int) ((x - rect.getX()) * level),
				(int) ((y - rect.getY()) * level), (int) (width * level),
				(int) (height * level));
		Log.trace("toCanvas: level " + level + " target " + rect + " from "
				+ this + " to " + rectangle);
		return rectangle;
	}

	@Override
	public double distance(Point point) {
		final Point p1 = new Point(x, y);
		final Point p2 = new Point(x + width, y);
		final Point p3 = new Point(x + width, y + height);
		final Point p4 = new Point(x, y + height);

		final double d1 = new Line(p1, p2).distance(point);
		final double d2 = new Line(p2, p3).distance(point);
		final double d3 = new Line(p3, p4).distance(point);
		final double d4 = new Line(p4, p1).distance(point);

		return Math.min(Math.min(d1, d2), Math.min(d3, d4));
	}
}
