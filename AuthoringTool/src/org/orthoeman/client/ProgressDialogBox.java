package org.orthoeman.client;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.RootPanel;

public class ProgressDialogBox extends DialogBox {
    public ProgressDialogBox(String text) {
        setText(text != null ? text : "Retrieving Data...");
        setGlassEnabled(true);
        setAnimationEnabled(true);
        center();
    }

    public ProgressDialogBox() {
    	this(null);
    }

    	@Override
    public void show() {
        showWaitCursor();
        super.show();
    }

    @Override
    public void hide() {
        super.hide();
        showDefaultCursor();
    }

    public static void showWaitCursor() {
        DOM.setStyleAttribute(RootPanel.getBodyElement(), "cursor", "wait");
    }

    public static void showDefaultCursor() {
        DOM.setStyleAttribute(RootPanel.getBodyElement(), "cursor", "default");
    }
}
