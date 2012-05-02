package org.orthoeman.client;

import gwtupload.client.IUploadStatus.Status;
import gwtupload.client.IUploader;
import gwtupload.client.PreloadedImage;
import gwtupload.client.PreloadedImage.OnLoadPreloadedImageHandler;
import gwtupload.client.SingleUploader;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.canvas.dom.client.CssColor;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.touch.client.Point;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class AuthoringTool implements EntryPoint {
	/**
	 * This is the entry point method.
	 */
	public void onModuleLoad() {
		final Label errorLabel = Label.wrap(DOM.getElementById("errorLabel"));

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
				context.drawImage((ImageElement) (Object) img.getElement(), 0,
						0);
				back_canvas.getContext2d().drawImage(canvas.getCanvasElement(),
						0, 0);
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
	}
}
