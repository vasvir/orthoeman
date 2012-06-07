package org.orthoeman.client;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.orthoeman.client.log.DivLogger;
import org.orthoeman.shared.Lesson;
import org.orthoeman.shared.Lesson.Page;

import gwtupload.client.IUploadStatus.Status;
import gwtupload.client.IUploader;
import gwtupload.client.PreloadedImage;
import gwtupload.client.PreloadedImage.OnLoadPreloadedImageHandler;
import gwtupload.client.SingleUploader;
import gwtupload.client.SingleUploaderModal;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.canvas.dom.client.CssColor;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
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
import com.google.gwt.user.client.ui.Widget;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class AuthoringTool implements EntryPoint {
	private static final String itemTypeSeparator = " - ";

	private Lesson lesson = null;
	private Lesson.Page currentPage = null;

	private Map<Lesson.Page, Button> page_button_map = new HashMap<Lesson.Page, Button>();

	private TextBox title_tb;
	private TextArea text_area;

	private RootPanel quizContainer;
	private RootPanel canvasContainer;
	private ListBox combobox;

	private Canvas canvas;
	private Canvas back_canvas;

	private class Point {
		double x;
		double y;
		boolean valid;
	}

	final Point start_point = new Point();
	final Point old_point = new Point();

	/**
	 * This field gets compiled out when <code>log_level=OFF</code>, or any
	 * <code>log_level</code> higher than <code>DEBUG</code>.
	 */
	private long startTimeMillis;

	//
	/**
	 * Note, we defer all application initialization code to
	 * {@link #onModuleLoad2()} so that the UncaughtExceptionHandler can catch
	 * any unexpected exceptions.
	 */
	@Override
	public void onModuleLoad() {
		final DivLogger div_logger = new DivLogger();
		div_logger.setCurrentLogLevel(Log.LOG_LEVEL_TRACE);
		Log.addLogger(div_logger);
		/*
		 * Install an UncaughtExceptionHandler which will produce
		 * <code>FATAL</code> log messages
		 */
		Log.setUncaughtExceptionHandler();

		// use deferred command to catch initialization exceptions in
		// onModuleLoad2
		Scheduler.get().scheduleDeferred(new ScheduledCommand() {
			@Override
			public void execute() {
				onModuleLoad2();
			}
		});
	}

	/**
	 * This is the entry point method.
	 */
	public void onModuleLoad2() {
		if (Log.isDebugEnabled()) {
			startTimeMillis = System.currentTimeMillis();
		}
		final Widget divLogger = Log.getLogger(DivLogger.class).getWidget();

		final Label splashScreenLabel = getLabel("splashScreenLabel");
		final Label errorLabel = getLabel("errorLabel");

		combobox = getListBox("itemCombobox");
		combobox.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				final Lesson.Page page = getCurrentPage();
				page.setItemTypeCombination(getCurrentItemTypeCombination());
				setCurrentPage(page);
			}
		});
		for (final Lesson.Page.Item.Type[] item_type_combination : Lesson.Page.validItemTypeCombinations) {
			combobox.addItem(getComboboxOptionText(item_type_combination));
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
		help_menu.addItem(new MenuItem("Toggle Console", new Command() {
			@Override
			public void execute() {
				divLogger.setVisible(!divLogger.isVisible());
			}
		}));

		help_menu.addItem(new MenuItem("Report a bug...", command));
		help_menu.addItem(new MenuItem("About", command));

		final MenuBar menu_bar = new MenuBar();
		menu_bar.addItem("File", file_menu);
		menu_bar.addItem("Edit", edit_menu);
		menu_bar.addItem("Help", help_menu);
		getHTMLMenuBar().setVisible(false);
		getHTMLMenuBar().removeFromParent();
		getMenuBarContainer().add(menu_bar);

		final Button add_b = getButton("addButton");
		add_b.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				final Lesson.Page page = new Lesson.Page("New Page");
				lesson.add(page);
				addPageButton(page);
				setCurrentPage(page);
			}
		});

		final Button remove_b = getButton("removeButton");
		remove_b.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				final Lesson.Page page = getCurrentPage();

				final Lesson.Page new_page = findCurrentItemAfterRemove(lesson,
						page);

				setCurrentPage(new_page);

				lesson.remove(page);
				removePageButton(page);
			}
		});

		// BUG: workaround of GWT weird behaviour
		// A widget that has an existing parent widget may not be added to the
		// detach list
		final boolean work_around_bug = true;
		text_area = getTextTextArea();
		if (work_around_bug) {
			getTextContainer();
			getImageUploaderContainer();
			getVideoUploaderContainer();
			getVideoContainer();
		}

		title_tb = getTextBox("titleTextBox");
		title_tb.addValueChangeHandler(new ValueChangeHandler<String>() {
			@Override
			public void onValueChange(ValueChangeEvent<String> event) {
				getCurrentPage().setTitle(event.getValue());
			}
		});

		text_area.addValueChangeHandler(new ValueChangeHandler<String>() {
			@Override
			public void onValueChange(ValueChangeEvent<String> event) {
				getCurrentPage().getTextItem().setText(event.getValue());
			}
		});

		quizContainer = getQuizContainer();

		canvas = Canvas.createIfSupported();
		if (canvas == null) {
			getErrorLabelContainer().add(
					new Label("No canvas, get a proper browser!"));
			return;
		}
		canvas.setStyleName("border", true);
		back_canvas = Canvas.createIfSupported();

		canvasContainer = getCanvasContainer();
		canvasContainer.add(canvas);

		canvas.setWidth("100%");

		final Context2d context = canvas.getContext2d();

		class MyResizeHandler implements ResizeHandler {
			@Override
			public void onResize(ResizeEvent event) {
				Log.trace("Browser resized " + event.getWidth() + " x "
						+ event.getHeight());
				onResize();
			}

			public void onResize() {
				redrawCanvas();
			}
		}

		final MyResizeHandler rh = new MyResizeHandler();

		Window.addResizeHandler(rh);
		rh.onResize();

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
				redrawCanvas();
			}
		};

		final IUploader.OnFinishUploaderHandler onImageFinishUploaderHandler = new IUploader.OnFinishUploaderHandler() {
			@Override
			public void onFinish(IUploader uploader) {
				final Page.ImageItem image_item = getCurrentPage()
						.getImageItem();
				final PreloadedImage img = image_item.getImage();
				if (img != null) {
					img.removeFromParent();
					image_item.setImage(null);
				}
				if (uploader.getStatus() == Status.SUCCESS) {
					image_item.setImage(new PreloadedImage(uploader.fileUrl(),
							showImageHandler));
				}
			}
		};

		final IUploader.OnFinishUploaderHandler onVideoFinishUploaderHandler = new IUploader.OnFinishUploaderHandler() {
			@Override
			public void onFinish(IUploader uploader) {
				if (uploader.getStatus() != Status.SUCCESS)
					return;
				final Page.VideoItem video_item = getCurrentPage()
						.getVideoItem();
				video_item.setVideoURL(uploader.fileUrl());
			}
		};

		final SingleUploader image_uploader = new SingleUploaderModal();
		image_uploader.setValidExtensions(".png", ".jpg", ".jpeg", ".tiff",
				".gif");
		image_uploader.setAutoSubmit(true);
		image_uploader.addOnFinishUploadHandler(onImageFinishUploaderHandler);
		getImageUploaderContainer().add(image_uploader);

		final SingleUploader video_uploader = new SingleUploaderModal();
		video_uploader.setValidExtensions(".mp4", ".mpeg", ".mpg", ".avi",
				".mov");
		video_uploader.setAutoSubmit(true);
		video_uploader.addOnFinishUploadHandler(onVideoFinishUploaderHandler);
		getVideoUploaderContainer().add(video_uploader);

		splashScreenLabel.setText("Reading Lesson...");
		final String url = null;
		lesson = Lesson.readXML(url);

		lesson.addPageListener(new Lesson.PageListener() {
			@Override
			public void pageRemoved(Page page) {
				remove_b.setEnabled(!lesson.isEmpty());
			}

			@Override
			public void pageAdded(Page page) {
				remove_b.setEnabled(!lesson.isEmpty());
			}
		});

		splashScreenLabel.setText("Updating GUI...");
		updateGUI();

		if (lesson.isEmpty())
			setCurrentPage(new Lesson.Page());
		else
			setCurrentPage(lesson.get(0));

		getSplashPopup().setVisible(false);

		/*
		 * Again, we need a guard here, otherwise <code>log_level=OFF</code>
		 * would still produce the following useless JavaScript: <pre> var
		 * durationSeconds, endTimeMillis; endTimeMillis =
		 * currentTimeMillis_0(); durationSeconds = (endTimeMillis -
		 * this$static.startTimeMillis) / 1000.0; </pre>
		 */
		if (Log.isDebugEnabled()) {
			long endTimeMillis = System.currentTimeMillis();
			float durationSeconds = (endTimeMillis - startTimeMillis) / 1000F;
			Log.debug("Duration: " + durationSeconds + " seconds");
		}
		// divLogger.setVisible(false);
	}

	private static int getScaledImageHeight(PreloadedImage img, int canvas_width) {
		if (img == null)
			return 3 * canvas_width / 4;
		return img.getRealHeight() * canvas_width / img.getRealWidth();
	}

	private void redrawCanvas() {
		final Page page = getCurrentPage();
		if (page == null)
			return;
		final Page.ImageItem image_item = page.getImageItem();
		if (image_item == null)
			return;
		final PreloadedImage img = image_item.getImage();
		final int div_width = canvasContainer.getOffsetWidth();
		final int div_height = canvasContainer.getOffsetHeight();
		Log.trace("Browser resized canvas container (offset size) " + div_width
				+ " x " + div_height + " style "
				+ canvasContainer.getStyleName());
		final int width = canvas.getOffsetWidth();
		final int height = canvas.getOffsetHeight();
		Log.trace("Browser resized canvas (offset size) " + width + " x "
				+ height + " style " + canvas.getStyleName());

		final int border_width = 1;
		// let's keep the aspect ratio of the image if exists
		// account for border (-2);
		final int canvas_width = width - 2 * border_width;
		final int new_height = getScaledImageHeight(img, canvas_width) + 2
				* border_width;
		canvas.setHeight(new_height + "px");
		final int canvas_height = new_height - 2 * border_width;

		canvas.setCoordinateSpaceWidth(canvas_width);
		canvas.setCoordinateSpaceHeight(canvas_height);

		back_canvas.setWidth(canvas_width + "px");
		back_canvas.setHeight(canvas_height + "px");
		back_canvas.setCoordinateSpaceWidth(canvas_width);
		back_canvas.setCoordinateSpaceHeight(canvas_height);

		start_point.valid = false;
		old_point.valid = false;

		final Context2d context = canvas.getContext2d();
		if (img != null) {
			img.setVisible(false);
			context.drawImage((ImageElement) (Object) img.getElement(), 0, 0,
					canvas_width, canvas_height);
		} else {
			context.setFillStyle(CssColor.make("white"));
			context.fillRect(0, 0, canvas_width, canvas_height);
		}
		back_canvas.getContext2d().drawImage(canvas.getCanvasElement(), 0, 0,
				canvas_width, canvas_height);
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

	private static TextArea getTextTextArea() {
		return getTextArea("textArea");
	}

	private static Button getButton(String id) {
		return Button.wrap(DOM.getElementById(id));
	}

	private static RootPanel getSplashPopup() {
		return RootPanel.get("splashPopup");
	}

	private static RootPanel getPageButtonContainer() {
		return RootPanel.get("pageButtonContainer");
	}

	private static RootPanel getTextContainer() {
		return RootPanel.get("textContainer");
	}

	private static RootPanel getImageUploaderContainer() {
		return RootPanel.get("imageUploaderContainer");
	}

	private static RootPanel getCanvasContainer() {
		return RootPanel.get("canvasContainer");
	}

	private static RootPanel getVideoContainer() {
		return RootPanel.get("videoContainer");
	}

	private static RootPanel getVideoUploaderContainer() {
		return RootPanel.get("videoUploaderContainer");
	}

	private static RootPanel getQuizContainer() {
		return RootPanel.get("quizContainer");
	}

	private static RootPanel getErrorLabelContainer() {
		return RootPanel.get("errorLabelContainer");
	}

	private static RootPanel getHTMLMenuBar() {
		return RootPanel.get("htmlMenuBar");
	}

	private static RootPanel getMenuBarContainer() {
		return RootPanel.get("menuBarContainer");
	}

	private static RootPanel getPageContainer() {
		return RootPanel.get("pageContainer");
	}

	private void addPageButton(final Lesson.Page page) {
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

		getPageButtonContainer().add(button);

		page_button_map.put(page, button);
	}

	private void removePageButton(Lesson.Page page) {
		final Button button = page_button_map.get(page);
		getPageButtonContainer().remove(button);
		page_button_map.remove(page);
	}

	private void updatePages() {
		getPageButtonContainer().clear();
		page_button_map.clear();
		for (final Lesson.Page page : lesson) {
			addPageButton(page);
		}
	}

	private void updateGUI() {
		updatePages();
	}

	private Lesson.Page getCurrentPage() {
		return currentPage;
	}

	private void setCurrentPage(Lesson.Page page) {
		this.currentPage = page;
		final RootPanel pageContainer = getPageContainer();

		if (page == null) {
			pageContainer.setVisible(false);
			return;
		}
		pageContainer.setVisible(true);

		getTextContainer().setVisible(false);
		quizContainer.setVisible(false);
		canvasContainer.setVisible(false);
		getVideoContainer().setVisible(false);

		final Page.Item.Type[] itemTypeCombination = page
				.getItemTypeCombination();
		combobox.setSelectedIndex(getComboboxOptionIndex(itemTypeCombination));

		// Log.trace("Where am I: ", new Exception("Stacktrace"));
		title_tb.setText(page.getTitle());
		for (final Lesson.Page.Item.Type type : itemTypeCombination) {
			switch (type) {
			case TEXT:
				getTextContainer().setVisible(true);
				text_area.setText(page.getTextItem().getText());
				break;
			case QUIZ:
				quizContainer.setVisible(true);
				break;
			case IMAGE:
				canvasContainer.setVisible(true);
				redrawCanvas();
				break;
			case VIDEO:
				getVideoContainer().setVisible(true);
				break;
			/** todo TODO handle the other types */
			}
		}
	}

	private static <T> T findCurrentItemAfterRemove(Collection<T> collection,
			T remove_item) {
		boolean found = false;
		T previous_item = null;

		for (final T item : collection) {
			if (found) {
				return item;
			}

			if (item.equals(remove_item)) {
				found = true;
				continue;
			}

			previous_item = item;
		}
		if (found)
			return previous_item;

		return null;
	}

	private static String getComboboxOptionText(
			Lesson.Page.Item.Type[] itemTypeCombination) {
		return itemTypeCombination[0].getName() + itemTypeSeparator
				+ itemTypeCombination[1].getName();
	}

	private int getComboboxOptionIndex(
			Lesson.Page.Item.Type[] itemTypeCombination) {
		final String option = getComboboxOptionText(itemTypeCombination);
		final int total = combobox.getItemCount();
		for (int i = 0; i < total; i++)
			if (option.equals(combobox.getItemText(i)))
				return i;
		return -1;
	}

	private Lesson.Page.Item.Type[] getCurrentItemTypeCombination() {
		final String value = combobox.getItemText(combobox.getSelectedIndex());
		final String[] type_names_a = value.split(itemTypeSeparator);
		final Lesson.Page.Item.Type item1_type = Lesson.Page.Item.Type
				.getTypeByName(type_names_a[0]);
		final Lesson.Page.Item.Type item2_type = Lesson.Page.Item.Type
				.getTypeByName(type_names_a[1]);
		return new Page.Item.Type[] { item1_type, item2_type };
	}
}
