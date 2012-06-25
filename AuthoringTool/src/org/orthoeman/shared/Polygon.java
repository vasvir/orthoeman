package org.orthoeman.shared;

import java.util.ArrayList;
import java.util.List;

public class Polygon extends Drawing {
	private List<Point> points;

	public Polygon(List<Point> points) {
		super(Type.POLYGON);
		this.points = points;
	}
	
	public Polygon(Polygon polygon) {
		this(copyPoints(polygon));
	}

	public Polygon() {
		this(new ArrayList<Point>());
	}
	
	public List<Point> getPoints() {
		return points;
	}

	public void setPoints(List<Point> points) {
		this.points = points;
	}
	
	private static List<Point> copyPoints(Polygon polygon) {
		final List<Point> points = new ArrayList<Point>();
		for (final Point point : polygon.points)
			points.add(new Point(point));
		return points;
	}

	@Override
	public Drawing toImage(Zoom zoom) {
		final List<Point> newpoints = new ArrayList<Point>();
		for (final Point point : points)
			newpoints.add(point.toImage(zoom));
		return new Polygon(newpoints);
	}

	@Override
	public Drawing toCanvas(Zoom zoom) {
		final List<Point> newpoints = new ArrayList<Point>();
		for (final Point point : points)
			newpoints.add(point.toCanvas(zoom));
		return new Polygon(newpoints);
	}
}
