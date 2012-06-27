package org.orthoeman.client;

import org.orthoeman.shared.Drawing;

import com.google.gwt.event.shared.EventHandler;

public interface UserDrawingFinishedEventHandler extends EventHandler {
	public void onUserDrawingFinishedEventHandler(Drawing drawing);
}
