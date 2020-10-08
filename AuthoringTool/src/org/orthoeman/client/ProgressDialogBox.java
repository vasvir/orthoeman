package org.orthoeman.client;

import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.RootPanel;

public class ProgressDialogBox extends DialogBox {
	public ProgressDialogBox(String text) {
		setText(text != null ? text : "Retrieving Data...");
		setGlassEnabled(true);
		setAnimationEnabled(true);
	}

	public ProgressDialogBox() {
		this(null);
	}

	@Override
	public void show() {
		showWaitCursor();
		super.show();
	}
	
	public void showOnCenter() {
		center();
	}

	@Override
	public void hide() {
		super.hide();
		showDefaultCursor();
	}

	public static void showWaitCursor() {
		RootPanel.getBodyElement().getStyle().setProperty("cursor", "wait");
	}

	public static void showDefaultCursor() {
		RootPanel.getBodyElement().getStyle().setProperty("cursor", "default");
	}
}
