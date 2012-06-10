package org.orthoeman.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.orthoeman.client.log.DivLogger;
import org.orthoeman.shared.Lesson;
import org.orthoeman.shared.Lesson.Page;
import org.orthoeman.shared.Lesson.Page.QuizItem;

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
	private Button up_b;
	private Button down_b;

	private TextBox title_tb;
	private ListBox combobox;

	private TextArea text_text_area;

	private RootPanel quizContainer;
	private TextArea quiz_text_area;

	private RootPanel imageContainer;
	private RootPanel canvasContainer;
	private Canvas canvas;
	private Canvas back_canvas;

	private class Point {
		double x;
		double y;
		boolean valid;
	}

	private final Point start_point = new Point();
	private final Point old_point = new Point();

	private final Collection<Collection<? extends Widget>> equal_width_widget_groups = new ArrayList<Collection<? extends Widget>>();

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

				lesson.remove(page);
				removePageButton(page);

				setCurrentPage(new_page);
			}
		});

		up_b = getButton("upButton");
		up_b.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				onUpDownClick(true);
			}
		});

		down_b = getButton("downButton");
		down_b.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				onUpDownClick(false);
			}
		});
		equal_width_widget_groups.add(Arrays.asList(add_b, remove_b, up_b,
				down_b));

		// BUG: workaround of GWT weird behaviour
		// A widget that has an existing parent widget may not be added to the
		// detach list
		final boolean work_around_bug = true;
		text_text_area = getTextTextArea();
		quiz_text_area = getQuizTextArea();
		final Button add_answer_b = getButton("quizAddAnswerButton");
		if (work_around_bug) {
			getTextContainer();
			getImageUploaderContainer();
			getCanvasContainer();
			getVideoUploaderContainer();
			getVideoContainer();
			getQuizAnswerContainer();
		}

		title_tb = getTextBox("titleTextBox");
		title_tb.addValueChangeHandler(new ValueChangeHandler<String>() {
			@Override
			public void onValueChange(ValueChangeEvent<String> event) {
				getCurrentPage().setTitle(event.getValue());
			}
		});

		text_text_area.addValueChangeHandler(new ValueChangeHandler<String>() {
			@Override
			public void onValueChange(ValueChangeEvent<String> event) {
				getCurrentPage().getTextItem().setText(event.getValue());
			}
		});

		imageContainer = getImageContainer();
		quizContainer = getQuizContainer();

		canvas = Canvas.createIfSupported();
		if (canvas == null) {
			getErrorLabelContainer().add(
					new Label("No canvas, get a proper browser!"));
			return;
		}
		canvas.addStyleName("block");
		canvas.addStyleName("noborder");
		canvas.addStyleName("fill_x");

		back_canvas = Canvas.createIfSupported();

		canvasContainer = getCanvasContainer();
		canvasContainer.add(canvas);

		final Context2d context = canvas.getContext2d();

		class MyResizeHandler implements ResizeHandler {
			@Override
			public void onResize(ResizeEvent event) {
				Log.trace("Browser resized " + event.getWidth() + " x "
						+ event.getHeight());
				onResize();
			}

			public void onResize() {
				for (final Collection<? extends Widget> equal_width_widget_group : equal_width_widget_groups) {
					// find the maximum width
					// assumes sane layout info (width include padding border
					// margin)
					int max_width = 0;
					for (final Widget w : equal_width_widget_group) {
						final int width = w.getOffsetWidth();
						if (width > max_width) {
							max_width = width;
						}
					}
					// set everybody to max_width
					for (final Widget w : equal_width_widget_group) {
						w.setWidth(max_width + "px");
					}
				}
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
				Log.trace("Point " + x + " " + y);
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

		// image
		final SingleUploader image_uploader = new SingleUploaderModal();
		image_uploader.setValidExtensions(".png", ".jpg", ".jpeg", ".tiff",
				".gif");
		image_uploader.setAutoSubmit(true);
		image_uploader.addOnFinishUploadHandler(onImageFinishUploaderHandler);
		getImageUploaderContainer().add(image_uploader);

		// video
		final SingleUploader video_uploader = new SingleUploaderModal();
		video_uploader.setValidExtensions(".mp4", ".mpeg", ".mpg", ".avi",
				".mov");
		video_uploader.setAutoSubmit(true);
		video_uploader.addOnFinishUploadHandler(onVideoFinishUploaderHandler);
		getVideoUploaderContainer().add(video_uploader);

		// quiz
		add_answer_b.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				final QuizAnswer quiz_question = new QuizAnswer(
						getCurrentPage().getQuizItem());
				getQuizAnswerContainer().add(quiz_question);
			}
		});

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

		// let's keep the aspect ratio of the image if exists
		final int canvas_width = width;
		final int new_height = getScaledImageHeight(img, canvas_width);
		canvas.setHeight(new_height + "px");
		final int canvas_height = new_height;

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
		if (canvas_width != 0 && canvas_height != 0)
			back_canvas.getContext2d().drawImage(canvas.getCanvasElement(), 0,
					0, canvas_width, canvas_height);
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
		return getTextArea("textTextArea");
	}

	private static TextArea getQuizTextArea() {
		return getTextArea("quizTextArea");
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

	private static RootPanel getImageContainer() {
		return RootPanel.get("imageContainer");
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

	private static RootPanel getQuizAnswerContainer() {
		return RootPanel.get("quizAnswerContainer");
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

	private void updateUpDownButtons(int index, int count) {
		Log.debug("Checking index " + index + " vs. count " + count);
		up_b.setEnabled(index != 0);
		down_b.setEnabled(index != count - 1);
	}

	private void updateUpDownButtons(Lesson.Page page) {
		if (page == null) {
			up_b.setEnabled(false);
			down_b.setEnabled(false);
			return;
		}
		final RootPanel page_button_container = getPageButtonContainer();
		updateUpDownButtons(
				page_button_container.getWidgetIndex(page_button_map.get(page)),
				page_button_container.getWidgetCount());
	}

	private void setCurrentPage(Lesson.Page page) {
		this.currentPage = page;
		final RootPanel pageContainer = getPageContainer();

		if (page == null) {
			pageContainer.setVisible(false);
			return;
		}
		updateUpDownButtons(page);
		pageContainer.setVisible(true);

		getTextContainer().setVisible(false);
		quizContainer.setVisible(false);
		imageContainer.setVisible(false);
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
				text_text_area.setText(page.getTextItem().getText());
				break;
			case IMAGE:
				imageContainer.setVisible(true);
				redrawCanvas();
				break;
			case VIDEO:
				getVideoContainer().setVisible(true);
				break;
			case QUIZ:
				quizContainer.setVisible(true);
				final RootPanel quizAnswerContainer = getQuizAnswerContainer();
				quiz_text_area.setText("");
				quizAnswerContainer.clear();
				final QuizItem quiz_item = page.getQuizItem();
				if (quiz_item != null) {
					quiz_text_area.setText(quiz_item.getText());
					for (final Integer id : quiz_item.getAnswerMap().keySet()) {
						quizAnswerContainer.add(new QuizAnswer(quiz_item, id));
					}
				}
				break;
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

	private void onUpDownClick(boolean up) {
		final Page current_page = getCurrentPage();
		final Button current_button = page_button_map.get(current_page);
		final RootPanel page_button_container = getPageButtonContainer();
		final int current_index = page_button_container
				.getWidgetIndex(current_button);
		final int next_index = current_index + ((up) ? (-1) : 2);
		final int check_index = current_index + ((up) ? (-1) : 1);
		Log.debug("Up " + up + " next_index " + next_index + " check_index "
				+ check_index);
		page_button_container.insert(current_button, next_index);
		updateUpDownButtons(check_index, page_button_container.getWidgetCount());
	}
}
