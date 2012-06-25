package org.orthoeman.shared;

import com.allen_sauer.gwt.log.client.Log;

public class Ellipse extends Drawing {
	private int x;
	private int y;
	private int width;
	private int height;

	public Ellipse(int x, int y, int width, int height) {
		super(Type.ELLIPSE);
		set(x, y, width, height);
	}

	public Ellipse(Ellipse ellipse) {
		this(ellipse.x, ellipse.y, ellipse.width, ellipse.height);
	}

	public Ellipse() {
		this(0, 0, 0, 0);
	}

	public void set(int x, int y, int width, int height) {
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

	@Override
	public String toString() {
		return "Ellipse:" + x + ":" + y + ":" + width + ":" + height;
	}

	@Override
	public Drawing toImage(Zoom zoom) {
		final double level = zoom.getLevel();
		final Rectangle rect = zoom.getTarget();
		final Ellipse ellipse = new Ellipse((int) (x / level) + rect.getX(),
				(int) (y / level) + rect.getY(), (int) (width / level),
				(int) (height / level));
		Log.trace("toImage: level " + level + " target " + rect + " from "
				+ this + " to " + ellipse);
		return ellipse;
	}

	@Override
	public Drawing toCanvas(Zoom zoom) {
		final double level = zoom.getLevel();
		final Rectangle rect = zoom.getTarget();
		final Ellipse ellipse = new Ellipse((int) ((x - rect.getX()) * level),
				(int) ((y - rect.getY()) * level), (int) (width * level),
				(int) (height * level));
		Log.trace("toCanvas: level " + level + " target " + rect + " from "
				+ this + " to " + ellipse);
		return ellipse;
	}

	@Override
	public double distance(Point point) {
		// TODO Auto-generated method stub
		return 0;
	}
}