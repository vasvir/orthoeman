package org.orthoeman.shared;

import java.util.HashMap;
import java.util.Map;

public abstract class Drawing {
	private static final String drawingBlockingColor = "orange";
	private static final String drawingInformationalColor = "blue";
	private static final String drawingHelperColor = "yellow";
	private static final String drawingEraseColor = "red";
	private static final String drawingDefaultColor = "black";

	public enum Type {
		RECTANGLE, ELLIPSE, POLYGON, LINE, ERASER, CROSS
	}

	public enum Kind {
		HOTSPOT("HotSpot"), INFO("Info"), HELPER("Helper"), ZOOM("Zoom");

		private final String displayName;
		private static final Map<String, Kind> display_name_map = new HashMap<String, Kind>();

		static {
			for (final Kind kind : values()) {
				display_name_map.put(kind.getDisplayName(), kind);
			}
		}

		private Kind(String displayName) {
			this.displayName = displayName;
		}

		public String getDisplayName() {
			return displayName;
		}

		public static Kind getByDisplayName(String display_name) {
			return display_name_map.get(display_name);
		}
	}

	private final Type type;
	private Kind kind;

	public Drawing(Type type, Kind kind) {
		this.type = type;
		this.kind = kind;
	}

	public Type getType() {
		return type;
	}

	public Kind getKind() {
		return kind;
	}

	public void setKind(Kind kind) {
		this.kind = kind;
	}

	public abstract Drawing toImage(Zoom zoom);

	public abstract Drawing toCanvas(Zoom zoom);

	public abstract double distance(Point point);

	public String getColor() {
		if (getKind() == Kind.HOTSPOT)
			return drawingBlockingColor;
		if (getKind() == Kind.INFO)
			return drawingInformationalColor;
		if (getKind() == Kind.HELPER) {
			if (getType() == Type.ERASER)
				return drawingEraseColor;
			return drawingHelperColor;
		}
		return drawingDefaultColor;
	}

	public static String getEraserColor() {
		return drawingEraseColor;
	}
}
