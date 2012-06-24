package org.orthoeman.shared;

import java.util.ArrayList;
import java.util.List;

public class Polygon extends Drawing {
	private List<Point> points;

	public Polygon(List<Point> points) {
		super(Type.POLYGON);
		this.points = points;
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
}
