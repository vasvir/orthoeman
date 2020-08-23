package org.orthoeman.shared;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Ellipse extends Drawing {
	private int x;
	private int y;
	private int width;
	private int height;

	private final Log log = LogFactory.getLog(getClass());

	public Ellipse(Kind kind, int x, int y, int width, int height) {
		super(Type.ELLIPSE, kind);
		set(x, y, width, height);
	}

	public Ellipse(Kind kind) {
		this(kind, 0, 0, 0, 0);
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
		final Ellipse ellipse = new Ellipse(getKind(), (int) (x / level)
				+ rect.getX(), (int) (y / level) + rect.getY(),
				(int) (width / level), (int) (height / level));
		log.trace("toImage: level " + level + " target " + rect + " from "
				+ this + " to " + ellipse);
		return ellipse;
	}

	@Override
	public Drawing toCanvas(Zoom zoom) {
		final double level = zoom.getLevel();
		final Rectangle rect = zoom.getTarget();
		final Ellipse ellipse = new Ellipse(getKind(),
				(int) ((x - rect.getX()) * level),
				(int) ((y - rect.getY()) * level), (int) (width * level),
				(int) (height * level));
		log.trace("toCanvas: level " + level + " target " + rect + " from "
				+ this + " to " + ellipse);
		return ellipse;
	}

	@Override
	public double distance(Point point) {
		final double a = width / 2.;
		final double b = height / 2.;
		final double dx = point.x - x;
		final double dy = point.y - y;
		final double a2 = a * a;
		final double b2 = b * b;
		final double dx2 = dx * dx;
		final double dy2 = dy * dy;

		final double distance = Math.abs(Math.sqrt(Math.sqrt(dx2 * b2 + dy2
				* a2))
				- Math.sqrt(a * b));

		log.trace("Distance " + this + " from point " + point + " distance = "
				+ distance + " a = " + a + " b = " + b + " dx = " + dx
				+ " dy = " + dy);
		return distance;
	}
}
