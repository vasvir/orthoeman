package org.orthoeman.shared;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Polygon extends Drawing {
	private List<Point> points;

	public Polygon(Kind kind, List<Point> points) {
		super(Type.POLYGON, kind);
		this.points = points;
	}

	public Polygon(Kind kind) {
		this(kind, new ArrayList<Point>());
	}

	public List<Point> getPoints() {
		return points;
	}

	public void setPoints(List<Point> points) {
		this.points = points;
	}

	@Override
	public Drawing toImage(Zoom zoom) {
		final List<Point> newpoints = new ArrayList<Point>();
		for (final Point point : points)
			newpoints.add(point.toImage(zoom));
		return new Polygon(getKind(), newpoints);
	}

	@Override
	public Drawing toCanvas(Zoom zoom) {
		final List<Point> newpoints = new ArrayList<Point>();
		for (final Point point : points)
			newpoints.add(point.toCanvas(zoom));
		return new Polygon(getKind(), newpoints);
	}

	@Override
	public double distance(Point point) {
		double min_distance = Double.MAX_VALUE;
		if (points.isEmpty())
			return min_distance;
		final Iterator<Point> it = points.iterator();
		Point p1 = it.next();

		while (it.hasNext()) {
			Point p2 = it.next();
			final double distance = new Line(p1, p2).distance(point);
			if (distance < min_distance)
				min_distance = distance;
			p1 = p2;
		}

		return min_distance;
	}
}
