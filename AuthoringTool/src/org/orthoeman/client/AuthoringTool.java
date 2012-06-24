package org.orthoeman.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import org.orthoeman.client.log.DivLogger;
import org.orthoeman.shared.Drawing;
import org.orthoeman.shared.Ellipse;
import org.orthoeman.shared.Lesson;
import org.orthoeman.shared.Lesson.Page.ImageItem.Zoom.Type;
import org.orthoeman.shared.Line;
import org.orthoeman.shared.Point;
import org.orthoeman.shared.Polygon;
import org.orthoeman.shared.Rectangle;
import org.orthoeman.shared.Lesson.Page;
import org.orthoeman.shared.Lesson.Page.QuizItem;
import org.orthoeman.shared.Lesson.Page.ImageItem.Zoom;

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
import com.google.gwt.user.client.ui.SimpleCheckBox;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class AuthoringTool implements EntryPoint {
	private static final String itemTypeSeparator = " - ";
	private static final double polygonDistanceThreshold = 20;

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

	private TextBox weight_tb;
	private SimpleCheckBox block_cb;

	private final Point start_point = new Point();
	private final Point old_point = new Point();

	private final Collection<Collection<? extends Widget>> equal_width_widget_groups = new ArrayList<Collection<? extends Widget>>();

	private final Queue<UserDrawingRequest> udr_queue = new LinkedList<UserDrawingRequest>();
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
		final Page page = getCurrentPage();
		if (page == null
				|| !Arrays.asList(page.getItemTypeCombination()).contains(
						Lesson.Page.Item.Type.IMAGE)) {
			Log.trace("Image does not exist. Nothing to redraw. Exiting...");
			return;
		}

		final PreloadedImage img = page.getImageItem().getImage();
		final Zoom zoom = page.getImageItem().getZoom();

		start_point.valid = false;
		old_point.valid = false;

		final int window_height = event.getHeight();
		final int menubar_height = getMenuBarContainer().getOffsetHeight();

		final RootPanel pageLabelContainer = RootPanel
				.get("pageLabelContainer");
		final RootPanel upDownButtonlContainer = RootPanel
				.get("upDownButtonlContainer");
		final RootPanel addRemoveButtonContainer = RootPanel
				.get("addRemoveButtonContainer");

		final RootPanel pageContainer = getPageContainer();
		final int page_width = event.getWidth()
				- getLeftPanelContainer().getOffsetWidth();
		final int page_height = window_height - menubar_height;
		Log.trace("Browser resized page container (offset size) " + page_width
				+ " x " + page_height + " style "
				+ pageContainer.getStyleName());
		pageContainer.setSize(page_width + "px", page_height + "px");

		// page button container
		final RootPanel pageButtonContainer = getPageButtonContainer();
		final int page_button_cnt_height = window_height - menubar_height
				- pageLabelContainer.getOffsetHeight()
				- upDownButtonlContainer.getOffsetHeight()
				- addRemoveButtonContainer.getOffsetHeight();
		pageButtonContainer.setHeight(page_button_cnt_height + "px");

		final int canvas_100 = page_width - 2;
		int canvas_width = 0;
		int canvas_height = 0;

		if (img == null) {
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

		if (img == null) {
			context.setFillStyle(CssColor.make("white"));
			context.fillRect(0, 0, canvas_width, canvas_height);
		} else {
			img.setVisible(false);
			Log.trace("Zoom Target (src) rectangle " + zoom.getTarget());
			context.drawImage((ImageElement) (Object) img.getElement(), zoom
					.getTarget().getX(), zoom.getTarget().getY(), zoom
					.getTarget().getWidth(), zoom.getTarget().getHeight(), 0,
					0, canvas_width, canvas_height);
			for (final Drawing drawing : page.getImageItem().getHotSpots()) {
				draw(context, drawing);
			}

		}
		back_canvas.getContext2d().drawImage(canvas.getCanvasElement(), 0, 0);
		Log.trace("-----------------------------------------");
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
		final Button erase_b = getButton("eraseButton");
		final Button edit_image_b = getButton("editImageButton");

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
		onResize(event);

		final Ellipse ellipse = new Ellipse();
		final Rectangle rect = new Rectangle();
		final Line line = new Line();
		final Polygon polygon = new Polygon();

		canvas.addClickHandler(new ClickHandler() {
			private UserDrawingRequest udr;

			private void startDrawingOperation(UserDrawingRequest udr, int x,
					int y) {
				this.udr = udr;
				Log.trace(udr.type + " Starting Point " + x + " " + y);
				start_point.x = x;
				start_point.y = y;
				start_point.valid = true;

				polygon.getPoints().clear();
			}

			private void finishDrawingOperation() {
				start_point.valid = false;
				old_point.valid = false;

				back_canvas.getContext2d().drawImage(canvas.getCanvasElement(),
						0, 0);

				// pop the request and run the handler that returns the
				// information
				udr_queue.poll();
				switch (udr.type) {
				case ELLIPSE:
					udr.handler.onUserDrawingFinishedEventHandler(new Ellipse(
							ellipse));
					break;
				case LINE:
					udr.handler
							.onUserDrawingFinishedEventHandler(new Line(line));
					break;
				case POLYGON:
					udr.handler.onUserDrawingFinishedEventHandler(new Polygon(
							polygon));
					break;
				case RECTANGLE:
					udr.handler
							.onUserDrawingFinishedEventHandler(new Rectangle(
									rect));
					break;
				case ERASER:
					break;
				}
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
					 * his is the end of the drawing operation. Click ends the
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

					final double distance = getDistance(start_point, x, y);
					if (polygon.getPoints().size() > 1 && distance >= 0
							&& distance < polygonDistanceThreshold) {
						polygon.getPoints().add(new Point(start_point));
						finishDrawingOperation();
					} else {
						if (!start_point.valid) {
							polygon.getPoints().add(new Point(x, y));
							startDrawingOperation(udr, x, y);
						}
						polygon.getPoints().add(new Point(x, y));
					}
				}
			}
		});

		canvas.addMouseMoveHandler(new MouseMoveHandler() {
			@Override
			public void onMouseMove(MouseMoveEvent event) {
				if (!start_point.valid)
					return;
				final UserDrawingRequest udr = udr_queue.peek();
				if (udr == null)
					return;
				final int x = event.getRelativeX(canvas.getElement());
				final int y = event.getRelativeY(canvas.getElement());

				// restore original image
				if (old_point.valid) {
					context.drawImage(back_canvas.getCanvasElement(), 0, 0);
				}

				// here we draw the user interaction
				switch (udr.type) {
				case ELLIPSE:
					// find the bounding box
					final int w = 2 * (x > start_point.x ? x - start_point.x
							: start_point.x - x);
					final int h = 2 * (y > start_point.y ? y - start_point.y
							: start_point.y - y);
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
					polygon.getPoints().remove(polygon.getPoints().size() - 1);

					final double distance = getDistance(start_point, x, y);
					if (polygon.getPoints().size() > 1 && distance > 0
							&& distance < polygonDistanceThreshold) {
						context.beginPath();
						context.arc(start_point.x, start_point.y,
								polygonDistanceThreshold, 0, Math.PI * 2, true);
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
				case ERASER:
					break;
				}

				old_point.x = x;
				old_point.y = y;
				old_point.valid = true;
			}
		});

		final OnLoadPreloadedImageHandler showImageHandler = new OnLoadPreloadedImageHandler() {
			@Override
			public void onLoad(PreloadedImage img) {
				final Zoom zoom = getCurrentPage().getImageItem().getZoom();
				Log.trace("Got image " + img.getRealWidth() + " x "
						+ img.getRealHeight());
				zoom.setType(Type.ZOOM_TO_FIT_WIDTH);
				zoom.setLevel(1);
				zoom.getTarget().set(0, 0, img.getRealWidth(),
						img.getRealHeight());
				getCurrentPage().getImageItem().getHotSpots().clear();
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

		zoom_121_b.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				final Zoom zoom = getCurrentPage().getImageItem().getZoom();
				zoom.setType(Zoom.Type.ZOOM_121);
				zoom.setLevel(1);
				final PreloadedImage img = getCurrentPage().getImageItem()
						.getImage();
				if (img != null) {
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
				if (img != null) {
					zoom.setLevel(((double) canvas.getOffsetWidth())
							/ ((double) img.getWidth()));
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
				waitUserDrawing(Drawing.Type.RECTANGLE,
						new UserDrawingFinishedEventHandler() {
							@Override
							public void onUserDrawingFinishedEventHandler(
									Drawing drawing) {
								zoom.getTarget().set((Rectangle) drawing);
								zoom.setLevel(1);
								redrawCanvas();
							}
						});
			}
		});

		rect_hsp_b.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				waitUserDrawing(Drawing.Type.RECTANGLE,
						new UserDrawingFinishedEventHandler() {
							@Override
							public void onUserDrawingFinishedEventHandler(
									Drawing drawing) {
								getCurrentPage().getImageItem().getHotSpots()
										.add(drawing);
								redrawCanvas();
							}
						});
			}
		});

		ellipse_hsp_b.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				waitUserDrawing(Drawing.Type.ELLIPSE,
						new UserDrawingFinishedEventHandler() {
							@Override
							public void onUserDrawingFinishedEventHandler(
									Drawing drawing) {
								getCurrentPage().getImageItem().getHotSpots()
										.add(drawing);
								redrawCanvas();
							}
						});
			}
		});

		polygon_hsp_b.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				waitUserDrawing(Drawing.Type.POLYGON,
						new UserDrawingFinishedEventHandler() {
							@Override
							public void onUserDrawingFinishedEventHandler(
									Drawing drawing) {
								getCurrentPage().getImageItem().getHotSpots()
										.add(drawing);
								redrawCanvas();
							}
						});
			}
		});

		line_b.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				waitUserDrawing(Drawing.Type.LINE,
						new UserDrawingFinishedEventHandler() {
							@Override
							public void onUserDrawingFinishedEventHandler(
									Drawing drawing) {
								getCurrentPage().getImageItem().getHotSpots()
										.add(drawing);
								redrawCanvas();
							}
						});
			}
		});

		erase_b.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				waitUserDrawing(Drawing.Type.ERASER,
						new UserDrawingFinishedEventHandler() {
							@Override
							public void onUserDrawingFinishedEventHandler(
									Drawing drawing) {
								getCurrentPage().getImageItem().getHotSpots()
										.remove(drawing);
								redrawCanvas();
							}
						});
			}
		});

		edit_image_b.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				// TODO brightness contrast
			}
		});

		// video
		final SingleUploader video_uploader = new SingleUploaderModal();
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

	private static RootPanel getLeftPanelContainer() {
		return RootPanel.get("leftPanelContainer");
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
		weight_tb.setText(page.getWeight() + "");
		block_cb.setValue(page.isBlock());
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

	private void waitUserDrawing(Drawing.Type type,
			UserDrawingFinishedEventHandler handler) {
		udr_queue.add(new UserDrawingRequest(type, handler));
	}

	private static double getDistance(Point start_point, double x, double y) {
		return start_point.valid ? Math.sqrt((start_point.x - x)
				* (start_point.x - x) + (start_point.y - y)
				* (start_point.y - y)) : -1;
	}

	private static void draw(Context2d context, Drawing drawing) {
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

			context.beginPath();
			context.moveTo(xl, ym);
			context.bezierCurveTo(xl, ym - oy, xm - ox, yt, xm, yt);
			context.bezierCurveTo(xm + ox, yt, xe, ym - oy, xe, ym);
			context.bezierCurveTo(xe, ym + oy, xm + ox, ye, xm, ye);
			context.bezierCurveTo(xm - ox, ye, xl, ym + oy, xl, ym);
			context.closePath();
			context.stroke();
			break;
		case LINE:
			final Line line = (Line) drawing;
			context.beginPath();
			context.moveTo(line.getA().x, line.getA().y);
			context.lineTo(line.getB().x, line.getB().y);
			context.closePath();
			context.stroke();
			break;
		case POLYGON:
			final Polygon polygon = (Polygon) drawing;
			context.beginPath();
			boolean start = true;
			for (final Point point : polygon.getPoints()) {
				if (start) {
					context.moveTo(point.x, point.y);
					start = false;
				} else {
					context.lineTo(point.x, point.y);
				}
			}
			context.closePath();
			context.stroke();
			break;
		case RECTANGLE:
			final Rectangle rect = (Rectangle) drawing;

			context.beginPath();
			context.moveTo(rect.getX(), rect.getY());
			context.rect(rect.getX(), rect.getY(), rect.getWidth(),
					rect.getHeight());
			context.closePath();
			context.stroke();
			break;
		case ERASER:
			break;
		}
	}
}
