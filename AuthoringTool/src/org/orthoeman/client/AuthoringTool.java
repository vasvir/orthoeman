package org.orthoeman.client;

import gwtupload.client.IUploadStatus.Status;
import gwtupload.client.IUploader;
import gwtupload.client.PreloadedImage;
import gwtupload.client.PreloadedImage.OnLoadPreloadedImageHandler;
import gwtupload.client.SingleUploader;

import org.orthoeman.shared.FieldVerifier;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.canvas.dom.client.CssColor;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.touch.client.Point;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class AuthoringTool implements EntryPoint {
	/**
	 * The message displayed to the user when the server cannot be reached or
	 * returns an error.
	 */
	private static final String SERVER_ERROR = "An error occurred while "
			+ "attempting to contact the server. Please check your network "
			+ "connection and try again.";

	/**
	 * Create a remote service proxy to talk to the server-side Greeting
	 * service.
	 */
	private final GreetingServiceAsync greetingService = GWT
			.create(GreetingService.class);

	/**
	 * This is the entry point method.
	 */
	public void onModuleLoad() {
		final Button sendButton = new Button("Send");
		final TextBox nameField = new TextBox();
		nameField.setText("GWT User");
		final Label errorLabel = new Label();

		// We can add style names to widgets
		sendButton.addStyleName("sendButton");

		// Add the nameField and sendButton to the RootPanel
		// Use RootPanel.get() to get the entire body element
		RootPanel.get("nameFieldContainer").add(nameField);
		RootPanel.get("sendButtonContainer").add(sendButton);
		RootPanel.get("errorLabelContainer").add(errorLabel);

		final Canvas canvas = Canvas.createIfSupported();
		if (canvas == null) {
			RootPanel.get("errorLabelContainer").add(
					new Label("No canvas, get a proper browser!"));
			return;
		}
		final Canvas back_canvas = Canvas.createIfSupported();

		RootPanel.get("canvasContainer").add(canvas);

		final int width = 800;
		final int height = 600;
		canvas.setWidth(width + "px");
		canvas.setHeight(height + "px");
		canvas.setCoordinateSpaceWidth(width);
		canvas.setCoordinateSpaceHeight(height);

		back_canvas.setWidth(width + "px");
		back_canvas.setHeight(height + "px");
		back_canvas.setCoordinateSpaceWidth(width);
		back_canvas.setCoordinateSpaceHeight(height);

		final Context2d context = canvas.getContext2d();
		context.setFillStyle(CssColor.make("yellow"));
		context.fillRect(0, 0, width, height);

		back_canvas.getContext2d().drawImage(canvas.getCanvasElement(), 0, 0);

		class Point {
			double x;
			double y;
			boolean valid;
		}
		final Point start_point = new Point();
		final Point old_point = new Point();

		canvas.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				if (start_point.valid) {
					start_point.valid = false;
					old_point.valid = false;

					back_canvas.getContext2d().drawImage(
							canvas.getCanvasElement(), 0, 0);

					return;
				}
				final int x = event.getRelativeX(canvas.getElement());
				final int y = event.getRelativeY(canvas.getElement());
				errorLabel.setText("Point " + x + " " + y);
				start_point.x = x;
				start_point.y = y;
				start_point.valid = true;

			}
		});

		canvas.addMouseMoveHandler(new MouseMoveHandler() {

			@Override
			public void onMouseMove(MouseMoveEvent event) {
				if (!start_point.valid)
					return;
				final int x = event.getRelativeX(canvas.getElement());
				final int y = event.getRelativeY(canvas.getElement());

				if (old_point.valid) {
					context.drawImage(back_canvas.getCanvasElement(), 0, 0);
				}

				context.beginPath();
				context.moveTo(start_point.x, start_point.y);
				context.lineTo(x, y);
				context.closePath();
				context.stroke();

				old_point.x = x;
				old_point.y = y;
				old_point.valid = true;
			}
		});

		final OnLoadPreloadedImageHandler showImageHandler = new OnLoadPreloadedImageHandler() {
			public void onLoad(PreloadedImage img) {
				context.drawImage((ImageElement) (Object) img.getElement(), 0, 0);
				//context.drawImage(img, 0, 0);
				back_canvas.getContext2d().drawImage(canvas.getCanvasElement(), 0, 0);
			}
		};

		// protected UploaderConstants i18nStrs;

		final IUploader.OnFinishUploaderHandler onFinishUploaderHandler = new IUploader.OnFinishUploaderHandler() {
			public void onFinish(IUploader uploader) {
				if (uploader.getStatus() == Status.SUCCESS) {
					new PreloadedImage(uploader.fileUrl(), showImageHandler);
				}
			}
		};

		final SingleUploader upload = new SingleUploader();
		upload.addOnFinishUploadHandler(onFinishUploaderHandler);
		RootPanel.get("uploadContainer").add(upload);

		// Focus the cursor on the name field when the app loads
		nameField.setFocus(true);
		nameField.selectAll();

		// Create the popup dialog box
		final DialogBox dialogBox = new DialogBox();
		dialogBox.setText("Remote Procedure Call");
		dialogBox.setAnimationEnabled(true);
		final Button closeButton = new Button("Close");
		// We can set the id of a widget by accessing its Element
		closeButton.getElement().setId("closeButton");
		final Label textToServerLabel = new Label();
		final HTML serverResponseLabel = new HTML();
		VerticalPanel dialogVPanel = new VerticalPanel();
		dialogVPanel.addStyleName("dialogVPanel");
		dialogVPanel.add(new HTML("<b>Sending name to the server:</b>"));
		dialogVPanel.add(textToServerLabel);
		dialogVPanel.add(new HTML("<br><b>Server replies:</b>"));
		dialogVPanel.add(serverResponseLabel);
		dialogVPanel.setHorizontalAlignment(VerticalPanel.ALIGN_RIGHT);
		dialogVPanel.add(closeButton);
		dialogBox.setWidget(dialogVPanel);

		// Add a handler to close the DialogBox
		closeButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				dialogBox.hide();
				sendButton.setEnabled(true);
				sendButton.setFocus(true);
			}
		});

		// Create a handler for the sendButton and nameField
		class MyHandler implements ClickHandler, KeyUpHandler {
			/**
			 * Fired when the user clicks on the sendButton.
			 */
			public void onClick(ClickEvent event) {
				sendNameToServer();
			}

			/**
			 * Fired when the user types in the nameField.
			 */
			public void onKeyUp(KeyUpEvent event) {
				if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
					sendNameToServer();
				}
			}

			/**
			 * Send the name from the nameField to the server and wait for a
			 * response.
			 */
			private void sendNameToServer() {
				// First, we validate the input.
				errorLabel.setText("");
				String textToServer = nameField.getText();
				if (!FieldVerifier.isValidName(textToServer)) {
					errorLabel.setText("Please enter at least four characters");
					return;
				}

				// Then, we send the input to the server.
				sendButton.setEnabled(false);
				textToServerLabel.setText(textToServer);
				serverResponseLabel.setText("");
				greetingService.greetServer(textToServer,
						new AsyncCallback<String>() {
							public void onFailure(Throwable caught) {
								// Show the RPC error message to the user
								dialogBox
										.setText("Remote Procedure Call - Failure");
								serverResponseLabel
										.addStyleName("serverResponseLabelError");
								serverResponseLabel.setHTML(SERVER_ERROR);
								dialogBox.center();
								closeButton.setFocus(true);
							}

							public void onSuccess(String result) {
								dialogBox.setText("Remote Procedure Call");
								serverResponseLabel
										.removeStyleName("serverResponseLabelError");
								serverResponseLabel.setHTML(result);
								dialogBox.center();
								closeButton.setFocus(true);
							}
						});
			}
		}

		// Add a handler to send the name to the server
		MyHandler handler = new MyHandler();
		sendButton.addClickHandler(handler);
		nameField.addKeyUpHandler(handler);
	}
}
