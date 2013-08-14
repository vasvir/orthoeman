package org.orthoeman.client;

import org.orthoeman.shared.Drawing;

public class UserDrawingRequest {
	public final Drawing.Type type;
	public final Drawing.Kind kind;
	public final UserDrawingFinishedEventHandler handler;

	public UserDrawingRequest(Drawing.Type type, Drawing.Kind kind,
			UserDrawingFinishedEventHandler handler) {
		this.type = type;
		this.kind = kind;
		this.handler = handler;
	}
}
