package org.orthoeman.client;

import org.orthoeman.shared.Lesson;

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
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.touch.client.Point;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class AuthoringTool implements EntryPoint {
	private Lesson lesson = null;
	private Lesson.Page currentPage = null;

	private TextBox title_tb;
	private TextArea text_area;

	/**
	 * This is the entry point method.
	 */
	@Override
	public void onModuleLoad() {
		final Label splashScreenLabel = getLabel("splashScreenLabel");
		final Label errorLabel = getLabel("errorLabel");

		final ListBox lb = getListBox("itemCombobox");
		for (final Lesson.Page.Item.Type[] item_type_combination : Lesson.Page.validItemTypeCombinations) {
			lb.addItem(item_type_combination[0].getName() + " - "
					+ item_type_combination[1].getName());
		}

		final Command command = new Command() {
			@Override
			public void execute() {
				Window.alert("Command Fired");
			}
		};

		splashScreenLabel.setText("Loading menu...");
		final MenuBar file_menu = new MenuBar(true);
		file_menu.addItem(new MenuItem("Save", command));
		file_menu.addItem(new MenuItem("Preview", command));

		final MenuBar edit_menu = new MenuBar(true);
		edit_menu.addItem(new MenuItem("Undo", command));
		edit_menu.addItem(new MenuItem("Redo", command));

		final MenuBar help_menu = new MenuBar(true);
		help_menu.addItem(new MenuItem("Help Contents", command));
		help_menu.addItem(new MenuItem("Report a bug...", command));
		help_menu.addItem(new MenuItem("About", command));

		final MenuBar menu_bar = new MenuBar();
		menu_bar.addItem("File", file_menu);
		menu_bar.addItem("Edit", edit_menu);
		menu_bar.addItem("Help", help_menu);
		RootPanel.get("htmlMenuBar").setVisible(false);
		RootPanel.get("menuBarContainer").add(menu_bar);

		title_tb = getTextBox("titleTextBox");
		title_tb.addValueChangeHandler(new ValueChangeHandler<String>() {
			@Override
			public void onValueChange(ValueChangeEvent<String> event) {
				getCurrentPage().setTitle(event.getValue());
			}
		});

		text_area = getTextArea("textArea");
		text_area.addValueChangeHandler(new ValueChangeHandler<String>() {
			@Override
			public void onValueChange(ValueChangeEvent<String> event) {
				getCurrentPage().getTextItem().setText(event.getValue());
			}
		});

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
			@Override
			public void onLoad(PreloadedImage img) {
				context.drawImage((ImageElement) (Object) img.getElement(), 0,
						0);
				back_canvas.getContext2d().drawImage(canvas.getCanvasElement(),
						0, 0);
			}
		};

		// protected UploaderConstants i18nStrs;

		final IUploader.OnFinishUploaderHandler onFinishUploaderHandler = new IUploader.OnFinishUploaderHandler() {
			@Override
			public void onFinish(IUploader uploader) {
				if (uploader.getStatus() == Status.SUCCESS) {
					final PreloadedImage preloadedImage = new PreloadedImage(
							uploader.fileUrl(), showImageHandler);
					preloadedImage.setTitle("Ttile to set");
				}
			}
		};

		final SingleUploader upload = new SingleUploader();
		upload.addOnFinishUploadHandler(onFinishUploaderHandler);
		// RootPanel.get("uploadContainer").add(upload);

		splashScreenLabel.setText("Reading Lesson...");
		final String url = null;
		lesson = Lesson.readXML(url);

		splashScreenLabel.setText("Updating GUI...");
		updateGUI();

		if (lesson.isEmpty())
			setCurrentPage(new Lesson.Page());
		else
			setCurrentPage(lesson.get(0));

		getSplashPopup().setVisible(false);
	}

	private static ListBox getListBox(String id) {
		return ListBox.wrap(DOM.getElementById(id));
	}

	private static Label getLabel(String id) {
		return Label.wrap(DOM.getElementById(id));
	}

	private static TextBox getTextBox(String id) {
		return TextBox.wrap(DOM.getElementById(id));
	}

	private static TextArea getTextArea(String id) {
		return TextArea.wrap(DOM.getElementById(id));
	}

	private static RootPanel getSplashPopup() {
		return RootPanel.get("splashPopup");
	}

	private static RootPanel getPageButtonContainer() {
		return RootPanel.get("pageButtonContainer");
	}

	// private Element getPageButtonContainer() {
	// return DOM.getElementById("pageButtonContainer");
	// }

	private Button createPageButton(final Lesson.Page page) {
		final Button button = new Button(page.getTitle());
		button.setWidth("100%");

		page.addTitleChangedListener(new Lesson.Page.TitleChangedListener() {
			@Override
			public void titleChanged(String title) {
				button.setText(title);
			}
		});

		button.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				setCurrentPage(page);
			}
		});
		return button;
	}

	private void addPage(Lesson.Page page) {
		final Button button = createPageButton(page);
		getPageButtonContainer().add(button);
	}

	private void updatePages() {
		getPageButtonContainer().clear();
		for (final Lesson.Page page : lesson) {
			addPage(page);
		}
	}

	private void updateGUI() {
		updatePages();
	}

	private void setCurrentPage(Lesson.Page page) {
		this.currentPage = page;
		title_tb.setText(page.getTitle());
		for (final Lesson.Page.Item item : page) {
			switch (item.getType()) {
			case TEXT:
				final Lesson.Page.TextItem text_item = (Lesson.Page.TextItem) item;
				text_area.setText(text_item.getText());
				break;
			case QUIZ:
				break;
			case IMAGE:
				break;
			case VIDEO:
				break;
			default:
				break;
			}
		}
	}

	private Lesson.Page getCurrentPage() {
		return currentPage;
	}
}
