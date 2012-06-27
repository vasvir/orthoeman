package org.orthoeman.client;

import org.orthoeman.shared.Drawing;

public class UserDrawingRequest {
	public final Drawing.Type type;
	public final UserDrawingFinishedEventHandler handler;

	public UserDrawingRequest(Drawing.Type type,
			UserDrawingFinishedEventHandler handler) {
		this.type = type;
		this.handler = handler;
	}
}
