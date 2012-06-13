package org.orthoeman.shared;

import java.util.List;

public class Polygon extends Drawing {
	private List<Point> points;

	public List<Point> getPoints() {
		return points;
	}

	public void setPoints(List<Point> points) {
		this.points = points;
	}
}
