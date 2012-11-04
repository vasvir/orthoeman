package org.orthoeman.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.orthoeman.client.log.DivLogger;
import org.orthoeman.shared.Cross;
import org.orthoeman.shared.Drawing;
import org.orthoeman.shared.Ellipse;
import org.orthoeman.shared.Lesson;
import org.orthoeman.shared.Line;
import org.orthoeman.shared.Point;
import org.orthoeman.shared.Polygon;
import org.orthoeman.shared.Rectangle;
import org.orthoeman.shared.Drawing.Type;
import org.orthoeman.shared.Lesson.Page;
import org.orthoeman.shared.Zoom;

import gwtupload.client.IUploadStatus.Status;
import gwtupload.client.IUploader;
import gwtupload.client.PreloadedImage;
import gwtupload.client.PreloadedImage.OnLoadPreloadedImageHandler;
import gwtupload.client.SingleUploader;
import gwtupload.client.SingleUploaderModal;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.CanvasPixelArray;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.canvas.dom.client.ImageData;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.dom.client.Style.Unit;
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
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.URL;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Hidden;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimpleCheckBox;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class AuthoringTool implements EntryPoint {
	private static final String itemTypeSeparator = " - ";
	private static final double polygonDistanceThreshold = 20;
	private static final double angleBulletRadius = 5;
	private static final double angleDistanceThreshold = 30;
	private static final double eraseDistanceThreshold = 5;

	private Lesson lesson = null;
	private Page currentPage = null;

	private Map<Page, Button> page_button_map = new HashMap<Page, Button>();
	private Button up_b;
	private Button down_b;

	private TextBox title_tb;
	private ListBox combobox;

	private TextArea text_text_area;

	private RootPanel quizContainer;
	private TextArea quiz_text_area;

	private RootPanel rangeQuizContainer;
	private TextArea range_quiz_text_area;
	private TextBox range_quiz_min_tb;
	private TextBox range_quiz_max_tb;

	private RootPanel imageContainer;
	private RootPanel canvasContainer;
	private Canvas canvas;
	private Canvas back_canvas;
	private ListBox areaTypeCombobox;

	private TextBox weight_tb;
	private SimpleCheckBox block_cb;

	private final Point start_point = new Point();
	private final Point old_point = new Point();

	private final Collection<Collection<? extends Widget>> equal_width_widget_groups = new ArrayList<Collection<? extends Widget>>();

	private final Queue<UserDrawingRequest> udr_queue = new LinkedList<UserDrawingRequest>();

	private Collection<Button> image_edit_buttons;

	private UserDrawingRequest udr;
	private Ellipse ellipse;
	private Rectangle rect;
	private Polygon polygon;
	private Line line;
	private Cross cross;
	private Point erase_point;
	private Drawing erase_drawing = null;

	private Slider brightness_sl;
	private Slider contrast_sl;
	private SimpleCheckBox invert_cb;

	private String orthoeman_id = "-1";

	/**
	 * This field gets compiled out when <code>log_level=OFF</code>, or any
	 * <code>log_level</code> higher than <code>DEBUG</code>.
	 */
	private long startTimeMillis;

	private static class MyResizeEvent extends ResizeEvent {
		public MyResizeEvent() {
			super(Window.getClientWidth(), Window.getClientHeight());
		}
	}

	public enum ResourceType {
		XML, IMAGE, VIDEO;
	}

	public class ImageItemOnLoadPreloadedImageHandler implements
			OnLoadPreloadedImageHandler {
		private final Page.ImageItem image_item;
		private final boolean clear_drawings;

		public ImageItemOnLoadPreloadedImageHandler(Page.ImageItem image_item,
				boolean clear_drawings) {
			this.image_item = image_item;
			this.clear_drawings = clear_drawings;
		}

		@Override
		public void onLoad(PreloadedImage img) {
			img.setVisible(false);
			final Zoom zoom = image_item.getZoom();
			Log.debug("Got image " + img.getRealWidth() + " x "
					+ img.getRealHeight());
			zoom.setType(Zoom.Type.ZOOM_TO_FIT_WIDTH);
			zoom.setLevel(((double) canvas.getOffsetWidth())
					/ ((double) img.getRealWidth()));
			zoom.getTarget().set(0, 0, img.getRealWidth(), img.getRealHeight());
			if (clear_drawings)
				image_item.getDrawings().clear();
			final Page current_page = getCurrentPage();
			if (current_page != null
					&& current_page.getImageItem() == image_item) {
				Log.debug("CurrentPage: " + current_page
						+ " with imageItem(): " + current_page.getImageItem()
						+ " image_item: " + image_item);
				redrawCanvas();
			}
			setButtonsEnabled(image_edit_buttons, true);
		}
	};

	public class SetupVideoPlayerHandler {
		public void setupVideoPlayer(Page.VideoItem video_item,
				Map<String, String> id_map) {
			final Page current_page = getCurrentPage();
			if (current_page != null
					&& current_page.getVideoItem() == video_item) {
				Log.debug("CurrentPage: " + current_page
						+ " with videoItem(): " + current_page.getVideoItem()
						+ " video_item: " + video_item);
				final RootPanel videoPlayerContainer = RootPanel
						.get("videoPlayerContainer");
				final StringBuilder sb = new StringBuilder();
				sb.append("<video controls>"); // poster="image_url
				for (final Map.Entry<String, String> entry : id_map.entrySet()) {
					final String url = Lesson.getResourceURL(orthoeman_id,
							entry.getKey());
					final String content_type = entry.getValue();
					sb.append("<source src='" + url + "' type='" + content_type
							+ "'/>");
				}
				sb.append("<p class='serverResponseLabelError'>Cannot find valid content / codec combination.</p>");
				sb.append("</video>");
				videoPlayerContainer.getElement().setInnerHTML(sb.toString());
			}
		}
	};

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

	private void onResize(ResizeEvent event) {
		for (final Collection<? extends Widget> equal_width_widget_group : equal_width_widget_groups) {
			Log.trace("Browser resized " + event.getWidth() + " x "
					+ event.getHeight());

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
		redrawCanvas(event);
	}

	private void redrawCanvas() {
		redrawCanvas(new MyResizeEvent());
	}

	private void redrawCanvas(ResizeEvent event) {
		final int window_width = event.getWidth();
		final int window_height = event.getHeight();
		final int menubar_height = getMenuBarContainer().getOffsetHeight();

		final RootPanel pageLabelContainer = RootPanel
				.get("pageLabelContainer");
		final RootPanel upDownButtonlContainer = RootPanel
				.get("upDownButtonlContainer");
		final RootPanel addRemoveButtonContainer = RootPanel
				.get("addRemoveButtonContainer");

		final RootPanel pageContainer = getPageContainer();
		final int page_width = window_width
				- getLeftPanelContainer().getOffsetWidth();
		final int page_height = window_height - menubar_height;
		Log.trace("Browser resized page container (offset size) " + page_width
				+ " x " + page_height + " style "
				+ pageContainer.getStyleName());
		pageContainer.setSize(page_width + "px", page_height + "px");

		// page button container
		final RootPanel pageButtonContainer = getPageButtonContainer();
		Log.trace("Chrome weird behavior: label_cnt_height: "
				+ pageLabelContainer.getOffsetHeight()
				+ " up_down_cnt_height: "
				+ upDownButtonlContainer.getOffsetHeight()
				+ " add_remove_cnt_heigth: "
				+ addRemoveButtonContainer.getOffsetHeight());
		final int page_button_cnt_height = window_height - menubar_height
				- pageLabelContainer.getOffsetHeight()
				- upDownButtonlContainer.getOffsetHeight()
				- addRemoveButtonContainer.getOffsetHeight();
		pageButtonContainer.setHeight(page_button_cnt_height + "px");
		Log.trace("Browser resized button container (offset size) "
				+ pageButtonContainer.getOffsetWidth() + " x "
				+ page_button_cnt_height + " style "
				+ pageButtonContainer.getStyleName());
		final double ratio = ((double) (window_width))
				/ ((double) (window_height));
		for (final Button page_button : page_button_map.values()) {
			page_button
					.setHeight((page_button.getElement().getClientWidth() / ratio)
							+ "px");
		}

		final Page page = getCurrentPage();
		if (page == null
				|| !Arrays.asList(page.getItemTypeCombination()).contains(
						Page.Item.Type.IMAGE)) {
			Log.trace("Image does not exist. Nothing to redraw. Exiting...");
			return;
		}

		final PreloadedImage img = page.getImageItem().getImage();
		final Zoom zoom = page.getImageItem().getZoom();

		start_point.valid = false;
		old_point.valid = false;

		final int canvas_100 = page_width - 2;
		int canvas_width = 0;
		int canvas_height = 0;

		if (!isImageLoaded(img)) {
			canvas_width = canvas_100;
			canvas_height = (canvas_width * 3 / 4);
		} else {
			switch (zoom.getType()) {
			case ZOOM_121:
			case ZOOM_LEVEL:
			case ZOOM_TARGET:
				canvas_width = (int) (zoom.getLevel() * zoom.getTarget()
						.getWidth());
				canvas_height = (int) (zoom.getLevel() * zoom.getTarget()
						.getHeight());
				break;
			case ZOOM_TO_FIT_WIDTH:
				// keep aspect ratio
				canvas_width = canvas_100;
				canvas_height = img.getRealHeight() * canvas_width
						/ img.getRealWidth();
				zoom.setLevel(((double) canvas_width)
						/ ((double) img.getRealWidth()));
				break;
			}
		}

		canvas.setSize(canvas_width + "px", canvas_height + "px");
		Log.trace("Zoom resized canvas (offset size) " + canvas_width + " x "
				+ canvas_height + " style " + canvas.getStyleName());

		canvas.setCoordinateSpaceWidth(canvas_width);
		canvas.setCoordinateSpaceHeight(canvas_height);

		back_canvas.setWidth(canvas_width + "px");
		back_canvas.setHeight(canvas_height + "px");
		back_canvas.setCoordinateSpaceWidth(canvas_width);
		back_canvas.setCoordinateSpaceHeight(canvas_height);

		final Context2d context = canvas.getContext2d();

		if (!isImageLoaded(img)) {
			context.setFillStyle("white");
			context.fillRect(0, 0, canvas_width, canvas_height);
		} else {
			img.setVisible(false);
			Log.trace("Zoom Level: " + zoom.getLevel()
					+ " Target (src) rectangle " + zoom.getTarget());
			context.drawImage((ImageElement) (Object) img.getElement(), zoom
					.getTarget().getX(), zoom.getTarget().getY(), zoom
					.getTarget().getWidth(), zoom.getTarget().getHeight(), 0,
					0, canvas_width, canvas_height);
			// apply image processing filters
			final double brightness = brightness_sl.getValue();
			final double contrast = contrast_sl.getValue();
			final boolean invert = invert_cb.getValue();
			if ((brightness != 0 || contrast != 0 || invert)) {
				final ImageData imgData = context.getImageData(0, 0,
						canvas_width, canvas_height);
				final CanvasPixelArray data = imgData.getData();
				final double contrast_factor = 259. * (contrast + 255)
						/ (255 * (259 - contrast));
				final int length = data.getLength();

				for (int i = 0; i < length; i += 4) {
					final double r = data.get(i);
					final double g = data.get(i + 1);
					final double b = data.get(i + 2);
					final double r1 = r + brightness;
					final double g1 = g + brightness;
					final double b1 = b + brightness;
					final double r2 = contrast_factor * (r1 - 128) + 128;
					final double g2 = contrast_factor * (g1 - 128) + 128;
					final double b2 = contrast_factor * (b1 - 128) + 128;
					final double r3 = (!invert) ? r2 : 255 - r2;
					final double g3 = (!invert) ? g2 : 255 - g2;
					final double b3 = (!invert) ? b2 : 255 - b2;
					data.set(i, truncate(r3));
					data.set(i + 1, truncate(g3));
					data.set(i + 2, truncate(b3));
				}
				context.putImageData(imgData, 0, 0);
			}
			for (final Drawing drawing : page.getImageItem().getDrawings()) {
				draw(context, drawing.toCanvas(page.getImageItem().getZoom()));
			}
			if (angleBulletRadius > 0) {
				for (final Point point : page.getImageItem().getDrawings()
						.getIntersectionPoints()) {
					final Point draw_point = point.toCanvas(page.getImageItem()
							.getZoom());
					context.beginPath();
					context.setStrokeStyle("yellow");
					context.setFillStyle(point.getColor());
					context.arc(draw_point.x, draw_point.y, angleBulletRadius,
							0, Math.PI * 2, true);
					context.fill();
					context.closePath();
					context.stroke();
				}
			}
		}
		back_canvas.getContext2d().drawImage(canvas.getCanvasElement(), 0, 0);
		Log.trace("-----------------------------------------");
	}

	private static int truncate(double value) {
		if (value >= 0 && value <= 255)
			return (int) value;
		if (value < 0)
			return 0;
		return 255;
	}

	/**
	 * This is the entry point method.
	 */
	public void onModuleLoad2() {
		if (Log.isDebugEnabled()) {
			startTimeMillis = System.currentTimeMillis();
		}
		final Widget divLogger = Log.getLogger(DivLogger.class).getWidget();

		orthoeman_id = Window.Location.getParameter("id");
		Log.info("Starting Authoring Tool with orthoeman_id " + orthoeman_id);

		final Label splashScreenLabel = getLabel("splashScreenLabel");

		combobox = getListBox("itemCombobox");
		combobox.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				final Page page = getCurrentPage();
				page.setItemTypeCombination(getCurrentItemTypeCombination());
				setCurrentPage(page);
			}
		});
		for (final Page.Item.Type[] item_type_combination : Page.validItemTypeCombinations) {
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
		file_menu.addItem(new MenuItem("Save", new Command() {
			@Override
			public void execute() {
				final ProgressDialogBox pd = new ProgressDialogBox(
						"Saving Lesson...");
				pd.show();
				putResource(ResourceType.XML, Lesson.writeXML(lesson), null, pd);
			}
		}));
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
				final Page page = new Page("New Page");
				lesson.add(page);
				addPageButton(page);
				setCurrentPage(page);
			}
		});

		final Button remove_b = getButton("removeButton");
		remove_b.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				final Page page = getCurrentPage();

				final Page new_page = findCurrentItemAfterRemove(lesson, page);

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
		range_quiz_text_area = getRangeQuizTextArea();
		range_quiz_min_tb = getTextBox("rangeQuizMinTextBox");
		range_quiz_max_tb = getTextBox("rangeQuizMaxTextBox");
		weight_tb = getTextBox("weightTextBox");
		block_cb = getSimpleCheckBox("blockCheckBox");
		final Button add_answer_b = getButton("quizAddAnswerButton");
		final Button zoom_121_b = getButton("zoomOne2OneButton");
		final Button zoom_in_b = getButton("zoomInButton");
		final Button zoom_out_b = getButton("zoomOutButton");
		final Button zoom_fit_b = getButton("zoomToFitWidthButton");
		final Button zoom_target_b = getButton("zoomTargetButton");
		final Button rect_hsp_b = getButton("rectangleHotspotButton");
		final Button ellipse_hsp_b = getButton("ellipseHotspotButton");
		final Button polygon_hsp_b = getButton("polygonHotspotButton");
		final Button line_b = getButton("lineButton");
		final Button cross_b = getButton("crossButton");
		final Button erase_b = getButton("eraseButton");
		final Button edit_image_b = getButton("editImageButton");

		image_edit_buttons = Arrays.asList(zoom_121_b, zoom_in_b, zoom_out_b,
				zoom_fit_b, zoom_target_b, rect_hsp_b, ellipse_hsp_b,
				polygon_hsp_b, line_b, cross_b, erase_b, edit_image_b);

		areaTypeCombobox = getListBox("areaTypeCombobox");
		areaTypeCombobox.addItem(Drawing.Kind.BLOCKING.getDisplayName());
		areaTypeCombobox.addItem(Drawing.Kind.INFORMATIONAL.getDisplayName());

		if (work_around_bug) {
			getTextContainer();
			getImageUploaderContainer();
			getCanvasContainer();
			getVideoUploaderContainer();
			getVideoContainer();
			getQuizAnswerContainer();
			getPageButtonContainer();
		}

		brightness_sl = new Slider(1024, -255, 255, 0);
		contrast_sl = new Slider(1024, -255, 255, 0);
		invert_cb = new SimpleCheckBox();

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

		rangeQuizContainer = getRangeQuizContainer();

		canvas = Canvas.createIfSupported();
		if (canvas == null) {
			getErrorLabelContainer().add(
					new Label("No canvas, get a proper browser!"));
			return;
		}
		canvas.addStyleName("block");
		canvas.addStyleName("noborder");

		back_canvas = Canvas.createIfSupported();

		canvasContainer = getCanvasContainer();
		canvasContainer.add(canvas);

		final Context2d context = canvas.getContext2d();

		class MyResizeHandler implements ResizeHandler {
			@Override
			public void onResize(final ResizeEvent event) {
				Scheduler.get().scheduleDeferred(
						new Scheduler.ScheduledCommand() {
							@Override
							public void execute() {
								AuthoringTool.this.onResize(event);
							}
						});
			}
		}
		final MyResizeHandler rh = new MyResizeHandler();

		Window.addResizeHandler(rh);
		final ResizeEvent event = new MyResizeEvent();
		rh.onResize(event);

		canvas.addClickHandler(new ClickHandler() {
			private void finishDrawingOperation() {
				start_point.valid = false;
				old_point.valid = false;

				back_canvas.getContext2d().drawImage(canvas.getCanvasElement(),
						0, 0);

				// pop the request and run the handler that returns the
				// information
				udr_queue.poll();
				final Zoom zoom = getCurrentPage().getImageItem().getZoom();
				switch (udr.type) {
				case ELLIPSE:
					udr.handler.onUserDrawingFinishedEventHandler(ellipse
							.toImage(zoom));
					break;
				case LINE:
					udr.handler.onUserDrawingFinishedEventHandler(line
							.toImage(zoom));
					break;
				case POLYGON:
					udr.handler.onUserDrawingFinishedEventHandler(polygon
							.toImage(zoom));
					break;
				case RECTANGLE:
					udr.handler.onUserDrawingFinishedEventHandler(rect
							.toImage(zoom));
					break;
				case CROSS:
					udr.handler.onUserDrawingFinishedEventHandler(cross
							.toImage(zoom));
					break;
				case ERASER:
					udr.handler
							.onUserDrawingFinishedEventHandler(erase_drawing);
					break;
				}
				setButtonsEnabled(image_edit_buttons, true);
			}

			@Override
			public void onClick(ClickEvent event) {
				final UserDrawingRequest udr = udr_queue.peek();
				if (udr == null) {
					if (start_point.valid)
						Log.error("Valid starting point without a drawing request. Please report");
					return;
				}

				if (udr.type != Drawing.Type.POLYGON) {
					/*
					 * This is the end of the drawing operation. Click ends the
					 * operation in all cases except polygon which is multi
					 * click operation
					 */
					if (start_point.valid) {
						finishDrawingOperation();
					} else {
						final int x = event.getRelativeX(canvas.getElement());
						final int y = event.getRelativeY(canvas.getElement());
						startDrawingOperation(udr, x, y);
					}
				} else {
					// polygon case
					final int x = event.getRelativeX(canvas.getElement());
					final int y = event.getRelativeY(canvas.getElement());

					if (start_point.valid) {
						final double distance = getDistance(start_point, x, y);
						if (polygon.getPoints().size() > 1 && distance >= 0
								&& distance < polygonDistanceThreshold) {
							polygon.getPoints().add(new Point(start_point));
							finishDrawingOperation();
						} else {
							polygon.getPoints().add(new Point(x, y));
						}
					} else {
						startDrawingOperation(udr, x, y);
						polygon.getPoints().add(new Point(x, y));
					}
				}
			}
		});

		canvas.addMouseMoveHandler(new MouseMoveHandler() {
			@Override
			public void onMouseMove(MouseMoveEvent event) {
				final int x = event.getRelativeX(canvas.getElement());
				final int y = event.getRelativeY(canvas.getElement());

				// restore original image
				if (old_point.valid) {
					context.drawImage(back_canvas.getCanvasElement(), 0, 0);
				}

				if (!start_point.valid) {
					// here we display angles
					final Page page = getCurrentPage();

					final Point query_point = (new Point(x, y)).toImage(page
							.getImageItem().getZoom());
					final Point min_distance_point = Point.getNearestPoint(page
							.getImageItem().getDrawings()
							.getIntersectionPoints(), query_point);

					// maybe we are out
					old_point.valid = false;

					if (min_distance_point == null)
						return;

					double min_distance = query_point
							.distance(min_distance_point);

					if (min_distance > angleDistanceThreshold)
						return;

					final Line[] intersection_lines = page.getImageItem()
							.getDrawings()
							.getInterSectionLines(min_distance_point);

					final String angle_str = NumberFormat.getFormat("0.0")
							.format(getAngle(intersection_lines[0],
									intersection_lines[1], query_point));

					context.fillText(angle_str, x, y);
				} else {
					final UserDrawingRequest udr = udr_queue.peek();
					if (udr == null)
						return;

					// here we draw the user interaction
					switch (udr.type) {
					case ELLIPSE:
						// find the bounding box
						final int w = 2 * (x > start_point.x ? x
								- start_point.x : start_point.x - x);
						final int h = 2 * (y > start_point.y ? y
								- start_point.y : start_point.y - y);
						ellipse.set(start_point.x, start_point.y, w, h);
						draw(context, ellipse);
						break;
					case LINE:
						line.set(start_point.x, start_point.y, x, y);
						draw(context, line);
						break;
					case POLYGON:
						polygon.getPoints().add(new Point(x, y));
						draw(context, polygon);
						polygon.getPoints().remove(
								polygon.getPoints().size() - 1);

						final double distance = getDistance(start_point, x, y);
						if (polygon.getPoints().size() > 1 && distance > 0
								&& distance < polygonDistanceThreshold) {
							context.beginPath();
							context.arc(start_point.x, start_point.y,
									polygonDistanceThreshold, 0, Math.PI * 2,
									true);
							context.closePath();
							context.stroke();
						}

						break;
					case RECTANGLE:
						final int wr = x > start_point.x ? x - start_point.x
								: start_point.x - x;
						final int hr = y > start_point.y ? y - start_point.y
								: start_point.y - y;
						final int xlr = x > start_point.x ? start_point.x : x;
						final int ytr = y > start_point.y ? start_point.y : y;

						rect.set(xlr, ytr, wr, hr);
						draw(context, rect);
						break;
					case CROSS:
						cross.set(x, y);
						draw(context, cross);
						break;
					case ERASER:
						final Zoom zoom = getCurrentPage().getImageItem()
								.getZoom();
						erase_point.set(x, y);
						erase_drawing = getNearestDrawing(getCurrentPage()
								.getImageItem().getDrawings(), erase_point
								.toImage(zoom));
						if (erase_drawing != null)
							draw(context, erase_drawing.toCanvas(zoom));
						break;
					}
				}
				old_point.x = x;
				old_point.y = y;
				old_point.valid = true;
			}
		});

		final String php_session_id = "PHPSESSID";

		final IUploader.OnFinishUploaderHandler onImageFinishUploaderHandler = new IUploader.OnFinishUploaderHandler() {
			@Override
			public void onFinish(IUploader uploader) {
				final Page.ImageItem image_item = getCurrentPage()
						.getImageItem();
				final PreloadedImage img = image_item.getImage();
				if (img != null) {
					setButtonsEnabled(image_edit_buttons, false);
					img.removeFromParent();
					image_item.setImage(null);
				}
				if (uploader.getStatus() != Status.SUCCESS)
					return;
				final ProgressDialogBox pd = new ProgressDialogBox(
						"Saving Image...");
				pd.show();
				putResource(ResourceType.IMAGE, uploader.fileUrl()
						+ "&session_id=" + Cookies.getCookie(php_session_id),
						image_item, pd);
			}
		};

		final IUploader.OnFinishUploaderHandler onVideoFinishUploaderHandler = new IUploader.OnFinishUploaderHandler() {
			@Override
			public void onFinish(IUploader uploader) {
				if (uploader.getStatus() != Status.SUCCESS)
					return;
				final ProgressDialogBox pd = new ProgressDialogBox(
						"Saving & Converting video. Please wait...");
				pd.show();
				putResource(ResourceType.VIDEO, uploader.fileUrl()
						+ "&session_id=" + Cookies.getCookie(php_session_id),
						getCurrentPage().getVideoItem(), pd);
			}
		};

		// image
		final SingleUploader image_uploader = new SingleUploaderModal();
		image_uploader
				.add(new Hidden("APC_UPLOAD_PROGRESS", image_uploader
						.getInputName()), 0);
		image_uploader.setServletPath("../jsupload.php");
		image_uploader.setValidExtensions(".png", ".jpg", ".jpeg", ".tiff",
				".gif");
		image_uploader.setAutoSubmit(true);
		image_uploader.addOnFinishUploadHandler(onImageFinishUploaderHandler);
		getImageUploaderContainer().add(image_uploader);

		zoom_121_b.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				final Zoom zoom = getCurrentPage().getImageItem().getZoom();
				zoom.setType(Zoom.Type.ZOOM_121);
				zoom.setLevel(1);
				final PreloadedImage img = getCurrentPage().getImageItem()
						.getImage();
				if (isImageLoaded(img)) {
					zoom.getTarget().set(0, 0, img.getRealWidth(),
							img.getRealHeight());
				}
				redrawCanvas();
			}
		});

		zoom_in_b.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				final Zoom zoom = getCurrentPage().getImageItem().getZoom();
				zoom.setType(Zoom.Type.ZOOM_LEVEL);
				zoom.increaseLevel();
				redrawCanvas();
			}
		});

		zoom_out_b.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				final Zoom zoom = getCurrentPage().getImageItem().getZoom();
				zoom.setType(Zoom.Type.ZOOM_LEVEL);
				zoom.decreaseLevel();
				redrawCanvas();
			}
		});

		zoom_fit_b.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				final Zoom zoom = getCurrentPage().getImageItem().getZoom();
				zoom.setType(Zoom.Type.ZOOM_TO_FIT_WIDTH);
				final PreloadedImage img = getCurrentPage().getImageItem()
						.getImage();
				if (isImageLoaded(img)) {
					zoom.setLevel(((double) canvas.getOffsetWidth())
							/ ((double) img.getRealWidth()));
					zoom.getTarget().set(0, 0, img.getRealWidth(),
							img.getRealHeight());
				}
				redrawCanvas();
			}
		});

		zoom_target_b.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				final Zoom zoom = getCurrentPage().getImageItem().getZoom();
				zoom.setType(Zoom.Type.ZOOM_TARGET);
				waitUserDrawing(Drawing.Type.RECTANGLE, Drawing.Kind.ZOOM,
						new UserDrawingFinishedEventHandler() {
							@Override
							public void onUserDrawingFinishedEventHandler(
									Drawing drawing) {
								final Rectangle rect = (Rectangle) drawing;
								zoom.getTarget().set(rect);
								zoom.setLevel(((double) canvas.getOffsetWidth())
										/ ((double) rect.getWidth()));
								redrawCanvas();
							}
						});
			}
		});

		rect_hsp_b.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				waitUserDrawing(Drawing.Type.RECTANGLE, Drawing.Kind
						.getByDisplayName(areaTypeCombobox
								.getItemText(areaTypeCombobox
										.getSelectedIndex())),
						new UserDrawingFinishedEventHandler() {
							@Override
							public void onUserDrawingFinishedEventHandler(
									Drawing drawing) {
								getCurrentPage().getImageItem().getDrawings()
										.add(drawing);
								redrawCanvas();
							}
						});
			}
		});

		ellipse_hsp_b.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				waitUserDrawing(Drawing.Type.ELLIPSE, Drawing.Kind
						.getByDisplayName(areaTypeCombobox
								.getItemText(areaTypeCombobox
										.getSelectedIndex())),
						new UserDrawingFinishedEventHandler() {
							@Override
							public void onUserDrawingFinishedEventHandler(
									Drawing drawing) {
								getCurrentPage().getImageItem().getDrawings()
										.add(drawing);
								redrawCanvas();
							}
						});
			}
		});

		polygon_hsp_b.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				waitUserDrawing(Drawing.Type.POLYGON, Drawing.Kind
						.getByDisplayName(areaTypeCombobox
								.getItemText(areaTypeCombobox
										.getSelectedIndex())),
						new UserDrawingFinishedEventHandler() {
							@Override
							public void onUserDrawingFinishedEventHandler(
									Drawing drawing) {
								getCurrentPage().getImageItem().getDrawings()
										.add(drawing);
								redrawCanvas();
							}
						});
			}
		});

		line_b.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				waitUserDrawing(Drawing.Type.LINE, Drawing.Kind.HELPER,
						new UserDrawingFinishedEventHandler() {
							@Override
							public void onUserDrawingFinishedEventHandler(
									Drawing drawing) {
								getCurrentPage().getImageItem().getDrawings()
										.add(drawing);
								redrawCanvas();
							}
						});
			}
		});

		cross_b.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				waitUserDrawing(Drawing.Type.CROSS, Drawing.Kind.HELPER,
						new UserDrawingFinishedEventHandler() {
							@Override
							public void onUserDrawingFinishedEventHandler(
									Drawing drawing) {
								getCurrentPage().getImageItem().getDrawings()
										.add(drawing);
								redrawCanvas();
							}
						});
			}
		});

		erase_b.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				waitUserDrawing(Drawing.Type.ERASER, Drawing.Kind.HELPER,
						new UserDrawingFinishedEventHandler() {
							@Override
							public void onUserDrawingFinishedEventHandler(
									Drawing drawing) {
								if (drawing == null)
									return;
								getCurrentPage().getImageItem().getDrawings()
										.remove(drawing);
								redrawCanvas();
							}
						});
			}
		});

		edit_image_b.addClickHandler(new ClickHandler() {
			private PopupPanel pp;
			private Panel panel;
			private Label brightness_l;
			private Label contrast_l;
			private Button reset_b;
			private NumberFormat format;

			@Override
			public void onClick(ClickEvent event) {
				if (pp == null) {
					pp = new PopupPanel(true, true);
					panel = new FlowPanel();
					panel.addStyleName("center");

					format = NumberFormat.getFormat("#.##");

					brightness_l = new Label();
					brightness_l
							.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
					setBrightnessLabel(brightness_sl.getValue());

					contrast_l = new Label();
					contrast_l
							.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
					setContrastLabel(brightness_sl.getValue());

					final Label invert_l = new Label("Invert: ");
					invert_l.addStyleName("inline");

					reset_b = new Button("Reset");

					panel.add(brightness_l);
					panel.add(brightness_sl);

					panel.add(createSpace(10));
					panel.add(contrast_l);
					panel.add(contrast_sl);

					panel.add(createSpace(10));

					panel.add(invert_l);
					panel.add(invert_cb);

					panel.add(createSpace(15));
					panel.add(reset_b);

					pp.setWidget(panel);

					brightness_sl
							.addValueChangeHandler(new ValueChangeHandler<Double>() {
								@Override
								public void onValueChange(
										ValueChangeEvent<Double> event) {
									setBrightnessLabel(brightness_sl.getValue());
									redrawCanvas();
								}
							});

					contrast_sl
							.addValueChangeHandler(new ValueChangeHandler<Double>() {
								@Override
								public void onValueChange(
										ValueChangeEvent<Double> event) {
									setContrastLabel(contrast_sl.getValue());
									redrawCanvas();
								}
							});

					invert_cb.addClickHandler(new ClickHandler() {
						@Override
						public void onClick(ClickEvent event) {
							redrawCanvas();
						}
					});

					reset_b.addClickHandler(new ClickHandler() {
						@Override
						public void onClick(ClickEvent event) {
							brightness_sl.reset();
							contrast_sl.reset();
							invert_cb.setValue(false);
							redrawCanvas();
						}
					});
				}

				final int slider_width = Window.getClientWidth() * 2 / 10;

				brightness_sl.setWidth(slider_width);
				contrast_sl.setWidth(slider_width);

				pp.setPopupPositionAndShow(new PopupPanel.PositionCallback() {
					@Override
					public void setPosition(int offsetWidth, int offsetHeight) {
						int left = (Window.getClientWidth() - offsetWidth) / 2;
						int top = (Window.getClientHeight() - offsetHeight) / 2;
						pp.setPopupPosition(left, top);
					}
				});
			}

			private void setLabel(Label label, String text, double value) {
				label.setText(text + ": " + format.format(value));
			}

			private void setBrightnessLabel(double value) {
				setLabel(brightness_l, "Brightness", value);
			}

			private void setContrastLabel(double value) {
				setLabel(contrast_l, "Contrast", value);
			}

			private Panel createSpace(int height) {
				final Panel space = new SimplePanel();
				space.setSize("100%", height + "px");
				return space;
			}
		});

		// video
		final SingleUploader video_uploader = new SingleUploaderModal();
		video_uploader
				.add(new Hidden("APC_UPLOAD_PROGRESS", video_uploader
						.getInputName()), 0);
		video_uploader.setServletPath("../jsupload.php");
		video_uploader.setValidExtensions(".mp4", ".mpeg", ".mpg", ".avi",
				".mov");
		video_uploader.setAutoSubmit(true);
		video_uploader.addOnFinishUploadHandler(onVideoFinishUploaderHandler);
		getVideoUploaderContainer().add(video_uploader);

		// quiz
		quiz_text_area.addValueChangeHandler(new ValueChangeHandler<String>() {
			@Override
			public void onValueChange(ValueChangeEvent<String> event) {
				getCurrentPage().getQuizItem().setText(event.getValue());
			}
		});

		add_answer_b.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				final QuizAnswer quiz_question = new QuizAnswer(
						getCurrentPage().getQuizItem());
				getQuizAnswerContainer().add(quiz_question);
			}
		});

		// range quiz
		range_quiz_text_area
				.addValueChangeHandler(new ValueChangeHandler<String>() {
					@Override
					public void onValueChange(ValueChangeEvent<String> event) {
						getCurrentPage().getRangeQuizItem().setText(
								event.getValue());
					}
				});

		range_quiz_min_tb
				.addValueChangeHandler(new ValueChangeHandler<String>() {
					@Override
					public void onValueChange(ValueChangeEvent<String> event) {
						final Page.RangeQuizItem range_quiz_item = getCurrentPage()
								.getRangeQuizItem();
						double min = range_quiz_item.getMin();
						try {
							min = Double.valueOf(event.getValue());
						} catch (Exception e) {
							Log.warn("Invalid Range Quiz min value "
									+ event.getValue());
							range_quiz_min_tb.setText(min + "");
						}
						range_quiz_item.setMin(min);
					}
				});

		range_quiz_max_tb
				.addValueChangeHandler(new ValueChangeHandler<String>() {
					@Override
					public void onValueChange(ValueChangeEvent<String> event) {
						final Page.RangeQuizItem range_quiz_item = getCurrentPage()
								.getRangeQuizItem();
						double max = range_quiz_item.getMax();
						try {
							max = Double.valueOf(event.getValue());
						} catch (Exception e) {
							Log.warn("Invalid Range Quiz max value "
									+ event.getValue());
							range_quiz_max_tb.setText(max + "");
						}
						range_quiz_item.setMax(max);
					}
				});

		weight_tb.addValueChangeHandler(new ValueChangeHandler<String>() {
			@Override
			public void onValueChange(ValueChangeEvent<String> event) {
				double weight = getCurrentPage().getWeight();
				try {
					weight = Double.valueOf(event.getValue());
				} catch (Exception e) {
					Log.warn("Invalid weight " + event.getValue());
					weight_tb.setText(weight + "");
				}
				getCurrentPage().setWeight(weight);
			}
		});

		block_cb.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				getCurrentPage().setBlock(block_cb.getValue());
			}
		});

		splashScreenLabel.setText("Reading Lesson...");

		final String url = Lesson.getResourceURL(orthoeman_id, null);
		final RequestBuilder rb = new RequestBuilder(RequestBuilder.GET, url);
		try {
			rb.sendRequest(null, new RequestCallback() {
				@Override
				public void onError(final Request request, final Throwable e) {
					Log.error(e.getMessage(), e);
				}

				@Override
				public void onResponseReceived(final Request request,
						final Response response) {
					lesson = Lesson.readXML(response.getText(), orthoeman_id,
							AuthoringTool.this);

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

					splashScreenLabel.setText("Updating Lesson pages...");
					updatePages();

					if (lesson.isEmpty()) {
						final Page page = new Page();
						lesson.add(page);
						addPageButton(page);
					}
					setCurrentPage(lesson.get(0));

					getSplashPopup().setVisible(false);

					if (Log.isDebugEnabled()) {
						long endTimeMillis = System.currentTimeMillis();
						float durationSeconds = (endTimeMillis - startTimeMillis) / 1000F;
						Log.debug("Lesson loading finished in: "
								+ durationSeconds + " seconds");
					}
				}
			});
		} catch (final Exception e) {
			Log.error(e.getMessage(), e);
		}

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
		divLogger.setVisible(false);
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

	private static SimpleCheckBox getSimpleCheckBox(String id) {
		return SimpleCheckBox.wrap(DOM.getElementById(id));
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

	private static TextArea getRangeQuizTextArea() {
		return getTextArea("rangeQuizTextArea");
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

	private static RootPanel getRangeQuizContainer() {
		return RootPanel.get("rangeQuizContainer");
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

	private static RootPanel getLeftPanelContainer() {
		return RootPanel.get("leftPanelContainer");
	}

	private static RootPanel getPageContainer() {
		return RootPanel.get("pageContainer");
	}

	private void addPageButton(final Page page) {
		final FlowPanel p = new FlowPanel();
		p.getElement().getStyle().setBorderWidth(12, Unit.PX);
		p.getElement().getStyle().setBorderColor("transparent");
		p.addStyleName("border");

		final Button button = new Button(page.getTitle());
		button.setWidth("100%");

		page.addTitleChangedListener(new Page.TitleChangedListener() {
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

		p.add(button);
		getPageButtonContainer().add(p);

		page_button_map.put(page, button);
	}

	private void removePageButton(Page page) {
		final Button button = page_button_map.get(page);
		getPageButtonContainer().remove(button.getParent());
		page_button_map.remove(page);
	}

	private void updatePages() {
		getPageButtonContainer().clear();
		page_button_map.clear();
		for (final Page page : lesson) {
			addPageButton(page);
		}
	}

	private Page getCurrentPage() {
		return currentPage;
	}

	private void updateUpDownButtons(int index, int count) {
		Log.debug("Checking index " + index + " vs. count " + count);
		up_b.setEnabled(index != 0);
		down_b.setEnabled(index != count - 1);
	}

	private void updateUpDownButtons(Page page) {
		if (page == null) {
			up_b.setEnabled(false);
			down_b.setEnabled(false);
			return;
		}
		final RootPanel page_button_container = getPageButtonContainer();
		updateUpDownButtons(
				page_button_container.getWidgetIndex(page_button_map.get(page)
						.getParent()), page_button_container.getWidgetCount());
	}

	private void setCurrentPage(Page page) {
		this.currentPage = page;

		// resets user drawing requests
		udr_queue.clear();

		final RootPanel pageContainer = getPageContainer();

		if (page == null) {
			pageContainer.setVisible(false);
			return;
		}
		updateUpDownButtons(page);
		pageContainer.setVisible(true);

		getTextContainer().setVisible(false);
		quizContainer.setVisible(false);
		rangeQuizContainer.setVisible(false);
		imageContainer.setVisible(false);
		getVideoContainer().setVisible(false);

		final Page.Item.Type[] itemTypeCombination = page
				.getItemTypeCombination();
		combobox.setSelectedIndex(getComboboxOptionIndex(itemTypeCombination));

		// Log.trace("Where am I: ", new Exception("Stacktrace"));
		title_tb.setText(page.getTitle());
		for (final Page.Item.Type type : itemTypeCombination) {
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
				quizAnswerContainer.clear();
				final Page.QuizItem quiz_item = page.getQuizItem();
				quiz_text_area.setText(quiz_item.getText());
				for (final Integer id : quiz_item.getAnswerMap().keySet()) {
					quizAnswerContainer.add(new QuizAnswer(quiz_item, id));
				}
				break;
			case RANGE_QUIZ:
				rangeQuizContainer.setVisible(true);
				final Page.RangeQuizItem range_quiz_item = page
						.getRangeQuizItem();
				range_quiz_text_area.setText(range_quiz_item.getText());
				range_quiz_min_tb.setText(range_quiz_item.getMin() + "");
				range_quiz_max_tb.setText(range_quiz_item.getMax() + "");
				break;
			}
		}
		weight_tb.setText(page.getWeight() + "");
		block_cb.setValue(page.isBlock());
		setButtonsEnabled(image_edit_buttons,
				page.getImageItem().getImage() != null);
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
			Page.Item.Type[] itemTypeCombination) {
		return itemTypeCombination[0].getName() + itemTypeSeparator
				+ itemTypeCombination[1].getName();
	}

	private int getComboboxOptionIndex(Page.Item.Type[] itemTypeCombination) {
		final String option = getComboboxOptionText(itemTypeCombination);
		final int total = combobox.getItemCount();
		for (int i = 0; i < total; i++)
			if (option.equals(combobox.getItemText(i)))
				return i;
		return -1;
	}

	private Page.Item.Type[] getCurrentItemTypeCombination() {
		final String value = combobox.getItemText(combobox.getSelectedIndex());
		final String[] type_names_a = value.split(itemTypeSeparator);
		final Page.Item.Type item1_type = Page.Item.Type
				.getTypeByName(type_names_a[0]);
		final Page.Item.Type item2_type = Page.Item.Type
				.getTypeByName(type_names_a[1]);
		return new Page.Item.Type[] { item1_type, item2_type };
	}

	private void onUpDownClick(boolean up) {
		final Page current_page = getCurrentPage();
		final Button current_button = page_button_map.get(current_page);
		final RootPanel page_button_container = getPageButtonContainer();
		final int current_index = page_button_container
				.getWidgetIndex(current_button.getParent());
		final int next_index = current_index + ((up) ? (-1) : 2);
		final int check_index = current_index + ((up) ? (-1) : 1);
		Log.debug("Up " + up + " next_index " + next_index + " check_index "
				+ check_index);
		page_button_container.insert(current_button.getParent(), next_index);
		updateUpDownButtons(check_index, page_button_container.getWidgetCount());
	}

	private void waitUserDrawing(Drawing.Type type, Drawing.Kind kind,
			UserDrawingFinishedEventHandler handler) {
		final UserDrawingRequest udr = new UserDrawingRequest(type, kind,
				handler);
		udr_queue.add(udr);
		if (type == Type.ERASER || type == Type.CROSS) {
			startDrawingOperation(udr, -1, -1);
		}
	}

	private static double getDistance(Point start_point, double x, double y) {
		return start_point.valid ? Math.sqrt((start_point.x - x)
				* (start_point.x - x) + (start_point.y - y)
				* (start_point.y - y)) : -1;
	}

	private static void draw(Context2d context, Drawing drawing) {
		context.beginPath();
		context.setStrokeStyle(drawing.getColor());

		switch (drawing.getType()) {
		case ELLIPSE:
			final Ellipse ellipse = (Ellipse) drawing;
			final double w = ellipse.getWidth();
			final double h = ellipse.getHeight();
			final double xl = ellipse.getX() - w / 2;
			final double yt = ellipse.getY() - h / 2;

			final double kappa = .5522848;
			// control point offset horizontal
			final double ox = (w / 2) * kappa;
			// control point offset vertical
			final double oy = (h / 2) * kappa;
			final double xe = xl + w; // x-end
			final double ye = yt + h; // y-end
			final double xm = xl + w / 2; // x-middle
			final double ym = yt + h / 2; // y-middle

			context.moveTo(xl, ym);
			context.bezierCurveTo(xl, ym - oy, xm - ox, yt, xm, yt);
			context.bezierCurveTo(xm + ox, yt, xe, ym - oy, xe, ym);
			context.bezierCurveTo(xe, ym + oy, xm + ox, ye, xm, ye);
			context.bezierCurveTo(xm - ox, ye, xl, ym + oy, xl, ym);
			break;
		case LINE:
			final Line line = (Line) drawing;
			context.moveTo(line.getA().x, line.getA().y);
			context.lineTo(line.getB().x, line.getB().y);
			break;
		case POLYGON:
			final Polygon polygon = (Polygon) drawing;
			boolean start = true;
			for (final Point point : polygon.getPoints()) {
				if (start) {
					context.moveTo(point.x, point.y);
					start = false;
				} else {
					context.lineTo(point.x, point.y);
				}
			}
			break;
		case RECTANGLE:
			final Rectangle rect = (Rectangle) drawing;

			context.moveTo(rect.getX(), rect.getY());
			context.rect(rect.getX(), rect.getY(), rect.getWidth(),
					rect.getHeight());
			break;
		case CROSS:
			final Cross cross = (Cross) drawing;
			context.moveTo(0, cross.y);
			context.lineTo(context.getCanvas().getWidth(), cross.y);
			context.moveTo(cross.x, 0);
			context.lineTo(cross.x, context.getCanvas().getHeight());
			break;
		case ERASER:
			break;
		}
		context.closePath();
		context.stroke();
	}

	private void setButtonsEnabled(Collection<Button> buttons, boolean enabled) {
		for (final Button button : buttons)
			button.setEnabled(enabled);
		areaTypeCombobox.setEnabled(enabled);
	}

	private void startDrawingOperation(UserDrawingRequest udr, int x, int y) {
		AuthoringTool.this.udr = udr;
		Log.trace(udr.type + " Starting Point " + x + " " + y);
		start_point.x = x;
		start_point.y = y;
		start_point.valid = true;

		switch (udr.type) {
		case CROSS:
			cross = new Cross(udr.kind);
			break;
		case ELLIPSE:
			ellipse = new Ellipse(udr.kind);
			break;
		case ERASER:
			erase_point = new Point();
			break;
		case LINE:
			line = new Line(udr.kind);
			break;
		case POLYGON:
			polygon = new Polygon(udr.kind);
			break;
		case RECTANGLE:
			rect = new Rectangle(udr.kind);
			break;
		}
		setButtonsEnabled(image_edit_buttons, false);
	}

	private static Drawing getNearestDrawing(List<Drawing> drawings, Point point) {
		Drawing min_distance_drawing = null;
		double min_distance = Double.MAX_VALUE;
		for (final Drawing drawing : drawings) {
			final double distance = drawing.distance(point);
			if (distance < min_distance) {
				min_distance = distance;
				min_distance_drawing = drawing;
			}
		}

		if (min_distance > eraseDistanceThreshold)
			min_distance_drawing = null;

		return min_distance_drawing;
	}

	private static double[] getLineEquation(Line line) {
		final double x1 = line.getA().x;
		final double y1 = line.getA().y;
		final double x2 = line.getB().x;
		final double y2 = line.getB().y;

		return new double[] { y2 - y1, -x2 + x1, x2 * y1 - x1 * y2 };
	}

	private static double getAngle(Line line1, Line line2, Point query_point) {
		final double[] line1_eq = getLineEquation(line1);
		final double A1 = line1_eq[0];
		final double B1 = line1_eq[1];
		final double C1 = line1_eq[2];

		final double[] line2_eq = getLineEquation(line2);
		final double A2 = line2_eq[0];
		final double B2 = line2_eq[1];
		final double C2 = line2_eq[2];

		final double angle_cos = (A1 * A2 + B1 * B2)
				/ (Math.sqrt(A1 * A1 + B1 * B1) * Math.sqrt(A2 * A2 + B2 * B2));
		double angle = Math.acos(angle_cos);

		// In which quadrant are we?
		int cnt = 0;
		if (A1 * query_point.x + B1 * query_point.y + C1 >= 0)
			cnt++;
		if (A2 * query_point.x + B2 * query_point.y + C2 >= 0)
			cnt++;

		if (cnt != 1) {
			angle = Math.PI - angle;
		}

		return angle * 180 / Math.PI;
	}

	private static boolean isImageLoaded(PreloadedImage img) {
		if (img == null)
			return false;
		if (img.getRealWidth() == 0 && img.getRealHeight() == 0)
			return false;
		return true;
	}

	private void putResource(final ResourceType resource_type,
			final String data, final Page.Item item, final ProgressDialogBox pd) {
		final RequestBuilder rb = new RequestBuilder(RequestBuilder.POST,
				"../put_resource.php?id=" + orthoeman_id + "&type="
						+ resource_type);
		rb.setHeader("Content-Type", "application/x-www-form-urlencoded");
		try {
			rb.sendRequest(URL.encodeQueryString(data), new RequestCallback() {
				@Override
				public void onResponseReceived(Request request,
						Response response) {
					if (response.getStatusCode() != Response.SC_OK) {
						final String error_msg = "HTTP Error for request: "
								+ request + " Response: " + response
								+ " status code: " + response.getStatusCode();
						Log.error(error_msg);
						pd.hide();
						Window.alert(error_msg);
						return;
					}
					final String response_text = response.getText();
					Log.debug("Successfull request: " + request + " response: "
							+ response_text);
					if (resource_type == ResourceType.IMAGE) {
						final Page.ImageItem image_item = (Page.ImageItem) item;
						final String id = Page.ImageItem
								.getImageIdString(response_text);
						image_item.setId(id);
						image_item.setImage(new PreloadedImage(Lesson
								.getResourceURL(orthoeman_id, id),
								new ImageItemOnLoadPreloadedImageHandler(
										image_item, true)));
						setButtonsEnabled(image_edit_buttons, true);
					} else if (resource_type == ResourceType.VIDEO) {
						final Page.VideoItem video_item = (Page.VideoItem) item;
						final Map<String, String> id2ContentTypeMap = Page.VideoItem
								.getVideoIdMap(response_text);
						video_item.setIdMap(id2ContentTypeMap,
								new SetupVideoPlayerHandler());
					}
					pd.hide();
				}

				@Override
				public void onError(Request request, Throwable exception) {
					final String error_msg = "RequestError for request: "
							+ request;
					Log.error(error_msg, exception);
					pd.hide();
					Window.alert(error_msg);
				}
			});
		} catch (RequestException e) {
			final String error_msg = "Cannot save lesson. Please wait for server "
					+ "communication to be restored " + "and retry later.";
			Log.error(error_msg, e);
			Window.alert(error_msg);
		}
	}
}
