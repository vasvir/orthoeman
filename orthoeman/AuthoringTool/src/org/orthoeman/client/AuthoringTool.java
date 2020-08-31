package org.orthoeman.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.orthoeman.shared.Cross;
import org.orthoeman.shared.Drawing;
import org.orthoeman.shared.Ellipse;
import org.orthoeman.shared.Lesson;
import org.orthoeman.shared.Line;
import org.orthoeman.shared.Point;
import org.orthoeman.shared.Polygon;
import org.orthoeman.shared.PreloadedImage;
import org.orthoeman.shared.PreloadedImage.OnLoadPreloadedImageHandler;
import org.orthoeman.shared.Rectangle;
import org.orthoeman.shared.Drawing.Type;
import org.orthoeman.shared.Lesson.Page;
import org.orthoeman.shared.Zoom;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.CanvasPixelArray;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.canvas.dom.client.ImageData;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.ParagraphElement;
import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.Style.Visibility;
import com.google.gwt.dom.client.Text;
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
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
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

	private static final Log log = LogFactory.getLog(AuthoringTool.class);

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
	private SimpleCheckBox showRegions_cb;

	private TextBox positive_grade_tb;
	private TextBox negative_grade_tb;

	private final Point start_point = new Point();
	private final Point old_point = new Point();

	private final Collection<Collection<? extends Widget>> equal_width_widget_groups = new ArrayList<Collection<? extends Widget>>();

	private final Queue<UserDrawingRequest> udr_queue = new LinkedList<UserDrawingRequest>();

	private Collection<FocusWidget> image_edit_buttons;

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

	private String cm_id = "-1";

	private int scrollbar_width;

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

	public class ImageItemOnLoadPreloadedImageHandler implements OnLoadPreloadedImageHandler {
		private final Page.ImageItem image_item;
		private final boolean clear_drawings;

		public ImageItemOnLoadPreloadedImageHandler(Page.ImageItem image_item, boolean clear_drawings) {
			this.image_item = image_item;
			this.clear_drawings = clear_drawings;
		}

		@Override
		public void onLoad(PreloadedImage img) {
			img.setVisible(false);
			final Zoom zoom = image_item.getZoom();
			log.debug("Got image " + img.getRealWidth() + " x " + img.getRealHeight());
			zoom.setType(Zoom.Type.ZOOM_TO_FIT_WIDTH);
			zoom.setLevel(((double) canvas.getOffsetWidth()) / ((double) img.getRealWidth()));
			zoom.getTarget().set(0, 0, img.getRealWidth(), img.getRealHeight());
			if (clear_drawings)
				image_item.getDrawings().clear();
			final Page current_page = getCurrentPage();
			if (current_page != null && current_page.getImageItem() == image_item) {
				log.debug("CurrentPage: " + current_page + " with imageItem(): " + current_page.getImageItem()
						+ " image_item: " + image_item);
				redrawCanvas();
			}
			setButtonsEnabled(image_edit_buttons, true);
		}
	};

	public class SetupVideoPlayerHandler {
		public void setupVideoPlayer(Page.VideoItem video_item) {
			updateVideoPlayerContainer(video_item);
		}
	};

	private void onResize(ResizeEvent event) {
		for (final Collection<? extends Widget> equal_width_widget_group : equal_width_widget_groups) {
			log.trace("Browser resized " + event.getWidth() + " x " + event.getHeight());

			// find the maximum width
			// assumes sane layout info
			// (width include padding border no margin)
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

	private int getWidthLeft() {
		return Window.getClientWidth() - getWidth(getLeftPanelContainer());
	}

	private int getHeightLeft() {
		final int window_height = Window.getClientHeight();
		final int menubar_height = getHeight(getMenuBarContainer());
		final int page_height = window_height - menubar_height;
		final int pageTitleContainerHeight = getHeight(getPageTitleContainer());
		final List<Page.Item.Type> itemTypeCombinationList = Arrays.asList(getCurrentPage().getItemTypeCombination());
		final boolean image = itemTypeCombinationList.contains(Page.Item.Type.IMAGE);
		final boolean text_quiz = itemTypeCombinationList.contains(Page.Item.Type.TEXT)
				&& itemTypeCombinationList.contains(Page.Item.Type.QUIZ);
		if (text_quiz) {
			final int textTitleContainerHeight = getHeight(getTextTitleContainer());
			final int quizTitleContainerHeight = getHeight(getQuizTitleContainer());
			final int height_left = page_height - pageTitleContainerHeight - textTitleContainerHeight
					- quizTitleContainerHeight - getDecorationHeight(getTextContainer())
					- getDecorationHeight(getQuizContainer());
			return height_left / 2;
		}

		final int mediaTitleContainerHeight = image ? getHeight(getImageTitleContainer())
				: getHeight(getVideoTitleContainer());
		final int mediaUploaderContainerHeight = image ? getHeight(getImageUploaderContainer())
				: getHeight(getVideoUploaderContainer());
		final int mediaButtonContainerHeight = image ? getHeight(getImageButtonContainer()) : 0;
		final int mediaContainerDecorationHeight = image ? getDecorationHeight(getImageContainer())
				: getDecorationHeight(getVideoContainer());
		final int height_left = page_height - pageTitleContainerHeight - mediaTitleContainerHeight
				- mediaUploaderContainerHeight - mediaButtonContainerHeight - mediaContainerDecorationHeight;
		log.trace("page_height = " + page_height + " pageTitleContainerHeight=" + pageTitleContainerHeight
				+ " mediaTitleContainerHeight =" + mediaTitleContainerHeight + " mediaUploaderContainerHeight="
				+ mediaUploaderContainerHeight + " mediaButtonContainerHeight=" + mediaButtonContainerHeight
				+ " height_left=" + height_left);
		return height_left;
	}

	private void resizeSecondaryContainers(int width_left, int height_left) {
		getNonMediaContainer().setWidth((width_left * 7 / 20) + "px");
		String uploaderContainerPX = getImageUploaderContainer().getOffsetHeight() + "px";

		final Page.Item.Type[] itemTypeCombination = getCurrentPage().getItemTypeCombination();

		for (final Page.Item.Type type : itemTypeCombination) {
			switch (type) {
			case IMAGE:
				uploaderContainerPX = getImageUploaderContainer().getOffsetHeight() + "px";
				break;
			case VIDEO:
				uploaderContainerPX = getVideoUploaderContainer().getOffsetHeight() + "px";
				getVideoPlayerContainer().setHeight(height_left + "px");
				break;
			case TEXT:
				getTextUploadAlignmentContainer().setHeight(uploaderContainerPX);
				getTextTextAreaContainer().setHeight((height_left) + "px");
				break;
			case QUIZ:
				getQuizUploadAlignmentContainer().setHeight(uploaderContainerPX);
				getQuizQuizAreaContainer().setHeight((height_left) + "px");
				getQuizAnswerScrollContainer().setHeight((height_left * 5 / 10) + "px");
				break;
			case RANGE_QUIZ:
				getRangeQuizUploadAlignmentContainer().setHeight(uploaderContainerPX);
				getRangeQuizRangeQuizAreaContainer().setHeight((height_left) + "px");
				break;
			}
		}
	}

	private void redrawCanvas(ResizeEvent event) {
		final int window_width = event.getWidth();
		final int window_height = event.getHeight();
		final int menubar_height = getHeight(getMenuBarContainer());

		final RootPanel pageLabelContainer = getPageLabelContainer();
		final RootPanel upDownButtonlContainer = getUpDownButtonlContainer();
		final RootPanel addRemoveButtonContainer = getAddRemoveButtonContainer();

		final RootPanel pageContainer = getPageContainer();
		if (scrollbar_width == 0) {
			scrollbar_width = getScrollBarWidth();
			log.debug("Setting scrollbar width to " + scrollbar_width);
		}
		final int page_width = window_width - getWidth(getLeftPanelContainer());
		final int page_height = window_height - menubar_height;
		log.trace("Browser resized page container (offset size) " + page_width + " x " + page_height + " style "
				+ pageContainer.getStyleName());
		pageContainer.setSize(page_width + "px", page_height + "px");

		// page button container
		final RootPanel pageButtonContainer = getPageButtonContainer();
		log.trace("Chrome weird behavior: label_cnt_height: " + pageLabelContainer.getOffsetHeight()
				+ " up_down_cnt_height: " + upDownButtonlContainer.getOffsetHeight() + " add_remove_cnt_heigth: "
				+ addRemoveButtonContainer.getOffsetHeight());
		final int page_button_cnt_height = window_height - menubar_height - getHeight(pageLabelContainer)
				- getHeight(upDownButtonlContainer) - getHeight(addRemoveButtonContainer)
				- getDecorationHeight(getLeftPanelContainerParent());
		pageButtonContainer.setHeight(page_button_cnt_height + "px");
		log.trace("Browser resized button container (offset size) " + pageButtonContainer.getOffsetWidth() + " x "
				+ page_button_cnt_height + " style " + pageButtonContainer.getStyleName());
		final double ratio = ((double) (window_width)) / ((double) (window_height));
		for (final Button page_button : page_button_map.values()) {
			page_button.setHeight((page_button.getElement().getClientWidth() / ratio) + "px");
		}

		final Page page = getCurrentPage();
		if (page == null || !Arrays.asList(page.getItemTypeCombination()).contains(Page.Item.Type.IMAGE)) {
			log.trace("Image does not exist. Nothing to redraw. Exiting...");
			return;
		}

		final int height_left = getHeightLeft();
		resizeSecondaryContainers(page_width, height_left);

		final PreloadedImage img = page.getImageItem().getImage();
		final Zoom zoom = page.getImageItem().getZoom();

		start_point.valid = false;
		old_point.valid = false;

		final Element cnt_e = canvasContainer.getElement();
		final int border_horizontal = getPixels(ComputedStyle.getStyleProperty(cnt_e, "borderLeftWidth"))
				+ getPixels(ComputedStyle.getStyleProperty(cnt_e, "borderRightWidth"));
		log.trace("Borders horizontal = " + border_horizontal);
		canvasContainer.setSize("auto", (height_left) + "px");
		log.trace("cnt_e.getOffsetWidth() = " + cnt_e.getOffsetWidth() + " getWidth(canvasContainer) = "
				+ getWidth(canvasContainer));
		final int canvas_100 = cnt_e.getOffsetWidth() - border_horizontal - scrollbar_width;
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
				canvas_width = (int) (zoom.getLevel() * zoom.getTarget().getWidth());
				canvas_height = (int) (zoom.getLevel() * zoom.getTarget().getHeight());
				break;
			case ZOOM_TO_FIT_WIDTH:
				// keep aspect ratio
				canvas_width = canvas_100;
				canvas_height = img.getRealHeight() * canvas_width / img.getRealWidth();
				zoom.setLevel(((double) canvas_width) / ((double) img.getRealWidth()));
				break;
			}
		}

		canvasContainer.setSize(cnt_e.getOffsetWidth() + "px", (height_left) + "px");
		canvas.setSize(canvas_width + "px", canvas_height + "px");
		log.trace("Zoom resized canvas (offset size) " + canvas_width + " x " + canvas_height + " style "
				+ canvas.getStyleName());
		log.trace("cnt_e.getOffsetWidth() = " + cnt_e.getOffsetWidth() + " getWidth(canvasContainer) = "
				+ getWidth(canvasContainer));

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
			log.trace("Zoom Level: " + zoom.getLevel() + " Target (src) rectangle " + zoom.getTarget());
			context.drawImage((ImageElement) (Object) img.getElement(), zoom.getTarget().getX(),
					zoom.getTarget().getY(), zoom.getTarget().getWidth(), zoom.getTarget().getHeight(), 0, 0,
					canvas_width, canvas_height);
			// apply image processing filters
			final double brightness = brightness_sl.getValue();
			final double contrast = contrast_sl.getValue();
			final boolean invert = invert_cb.getValue();
			if ((brightness != 0 || contrast != 0 || invert)) {
				final ImageData imgData = context.getImageData(0, 0, canvas_width, canvas_height);
				final CanvasPixelArray data = imgData.getData();
				final double contrast_factor = 259. * (contrast + 255) / (255 * (259 - contrast));
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
				draw(context, drawing.toCanvas(page.getImageItem().getZoom()), false);
			}
			if (angleBulletRadius > 0) {
				for (final Point point : page.getImageItem().getDrawings().getIntersectionPoints()) {
					final Point draw_point = point.toCanvas(page.getImageItem().getZoom());
					context.beginPath();
					context.setStrokeStyle("yellow");
					context.setFillStyle(point.getColor());
					context.arc(draw_point.x, draw_point.y, angleBulletRadius, 0, Math.PI * 2, true);
					context.fill();
					context.closePath();
					context.stroke();
				}
			}
		}
		back_canvas.getContext2d().drawImage(canvas.getCanvasElement(), 0, 0);
		log.trace("-----------------------------------------");
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
	@Override
	public void onModuleLoad() {
		if (log.isDebugEnabled()) {
			startTimeMillis = System.currentTimeMillis();
		}
		cm_id = Window.Location.getParameter("id");
		log.info("Starting Authoring Tool with orthoeman_id " + cm_id);

		final Label splashScreenLabel = getLabel("splashScreenLabel");

		combobox = getListBox("itemCombobox");
		combobox.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				final Page page = getCurrentPage();
				final Page.Item.Type[] current_item_combination = getCurrentItemTypeCombination();
				final int old_item_combination_index = getComboboxOptionIndex(page.getItemTypeCombination());

				final boolean contains_image_quiz = containsImageQuiz(current_item_combination);
				if (contains_image_quiz) {
					final Page current_page = getCurrentPage();
					final Page.ImageItem.DrawingList drawings = current_page.getImageItem().getDrawings();
					final int hotspots = drawings.getHotSpotCount();
					if (hotspots > 0) {
						final boolean convert_hotspots = Window
								.confirm("You are trying to introduce a quiz in a " + "page with hotspots. Convert "
										+ hotspots + " hotspots of this page to plain " + "informational areas.");
						if (convert_hotspots) {
							for (final Drawing drawing : drawings) {
								drawing.setKind(Drawing.Kind.INFO);
							}
						} else {
							combobox.setSelectedIndex(old_item_combination_index);
							return;
						}
					}
				}
				page.setItemTypeCombination(current_item_combination);
				setCurrentPage(page);
			}
		});
		for (final Page.Item.Type[] item_type_combination : Page.validItemTypeCombinations) {
			combobox.addItem(getComboboxOptionText(item_type_combination));
		}

		splashScreenLabel.setText("Loading...");

		final Button saveButton = getButton("saveButton");
		saveButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				final ProgressDialogBox pd = new ProgressDialogBox("Saving Lesson...");
				pd.show();
				putResource(ResourceType.XML, Lesson.writeXML(lesson), lesson.getResourceIds(), pd);
			}
		});

		final Button previewButton = getButton("previewButton");
		previewButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				final String iframe_display_tool_url = "view.php?id=" + cm_id;
				final String full_display_tool_url = "Display/index.html?id=" + cm_id;
				final boolean fullscreen_display_tool = false;
				final String display_tool_url = fullscreen_display_tool ? full_display_tool_url
						: iframe_display_tool_url;
				Window.open(
						// weird: IE cannot stand space in context name
						Window.Location.getPath().replaceAll("AuthoringTool/AuthoringTool.html.*$", display_tool_url),
						"AuthoringTool_Preview_DisplayTool_" + cm_id, "");
			}
		});

		final TextBox bugReportSubjectTextBox = getTextBox("bugReportSubjectTextBox");
		final TextArea bugReportBodyTextArea = getTextArea("bugReportBodyTextArea");
		final Button bugReportSendButton = getButton("bugReportSendButton");
		final Button bugReportCancelButton = getButton("bugReportCancelButton");
		final RootPanel bugReportPopup = getBugReportPopup();

		final Button reportBugButton = getButton("reportBugButton");
		reportBugButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				bugReportSubjectTextBox.setText("");
				bugReportBodyTextArea.setText("");
				bugReportPopup.setVisible(true);
			}
		});

		bugReportSendButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				final RequestBuilder rb = new RequestBuilder(RequestBuilder.POST,
						"../report_bug.php?id=" + cm_id + "&subject=" + bugReportSubjectTextBox.getText());
				rb.setHeader("Content-Type", "application/x-www-form-urlencoded");
				try {
					rb.sendRequest(URL.encodeQueryString(bugReportBodyTextArea.getText()) + "\n\nAuthoring Tool Log\n\n"
							+ getLoggedText(), new RequestCallback() {
								@Override
								public void onResponseReceived(Request request, Response response) {
									if (response.getStatusCode() != Response.SC_OK) {
										final String error_msg = "HTTP Error for request: " + request + " Response: "
												+ response + " status code: " + response.getStatusCode();
										log.error(error_msg);
										Window.alert(error_msg);
										return;
									}
									final String response_text = response.getText();
									log.debug("Successfull request: " + request + " response: " + response_text);
								}

								@Override
								public void onError(Request request, Throwable exception) {
									final String error_msg = "RequestError for request: " + request;
									log.error(error_msg, exception);
									Window.alert(error_msg);
								}
							});
				} catch (RequestException e) {
					final String error_msg = "Cannot save lesson. Please wait for server "
							+ "communication to be restored and retry later.";
					log.error(error_msg, e);
					Window.alert(error_msg);
				}
				bugReportPopup.setVisible(false);
			}
		});

		bugReportCancelButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				bugReportPopup.setVisible(false);
			}
		});

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
		equal_width_widget_groups.add(Arrays.asList(add_b, remove_b, up_b, down_b));

		// BUG: workaround of GWT weird behavior
		// A widget that has an existing parent widget may not be added to the
		// detach list
		final boolean work_around_bug = true;
		text_text_area = getTextTextArea();
		quiz_text_area = getQuizTextArea();
		range_quiz_text_area = getRangeQuizTextArea();
		range_quiz_min_tb = getTextBox("rangeQuizMinTextBox");
		range_quiz_max_tb = getTextBox("rangeQuizMaxTextBox");
		positive_grade_tb = getTextBox("positiveGradeTextBox");
		negative_grade_tb = getTextBox("negativeGradeTextBox");
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
		showRegions_cb = getSimpleCheckBox("showRegionsCheckBox");

		areaTypeCombobox = getListBox("areaTypeCombobox");
		areaTypeCombobox.addItem(Drawing.Kind.HOTSPOT.getDisplayName());
		areaTypeCombobox.addItem(Drawing.Kind.INFO.getDisplayName());

		image_edit_buttons = Arrays.asList(zoom_121_b, zoom_in_b, zoom_out_b, zoom_fit_b, zoom_target_b, rect_hsp_b,
				ellipse_hsp_b, polygon_hsp_b, line_b, cross_b, erase_b, edit_image_b, showRegions_cb);

		title_tb = getTextBox("titleTextBox");

		if (work_around_bug) {
			getRangeQuizUploadAlignmentContainer();
			getRangeQuizRangeQuizAreaContainer();
			getRangeQuizContainer();
			getQuizAnswerContainer();
			getQuizAnswerScrollContainer();
			getQuizQuizAreaContainer();
			getQuizUploadAlignmentContainer();
			getQuizContainer();
			getTextTextAreaContainer();
			getTextUploadAlignmentContainer();
			getTextContainer();
			getNonMediaContainer();
			getVideoPlayerContainer();
			getVideoUploaderContainer();
			getVideoTitleContainer();
			getVideoContainer();
			getImageButtonContainer();
			getCanvasContainer();
			getImageUploaderContainer();
			getImageTitleContainer();
			getImageContainer();
			getPageTitleContainer();
			getPageContainer();
			getAddRemoveButtonContainer();
			getPageButtonContainer();
			getUpDownButtonlContainer();
			getPageLabelContainer();
			getLeftPanelContainer();
			getLeftPanelContainerParent();
			getMenuBarContainer();
		}

		brightness_sl = new Slider(1024, -255, 255, 0);
		contrast_sl = new Slider(1024, -255, 255, 0);
		invert_cb = new SimpleCheckBox();

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
			getErrorLabelContainer().add(new Label("No canvas, get a proper browser!"));
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
				Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
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

				back_canvas.getContext2d().drawImage(canvas.getCanvasElement(), 0, 0);

				// pop the request and run the handler that returns the
				// information
				udr_queue.poll();
				final Zoom zoom = getCurrentPage().getImageItem().getZoom();
				switch (udr.type) {
				case ELLIPSE:
					udr.handler.onUserDrawingFinishedEventHandler(ellipse.toImage(zoom));
					break;
				case LINE:
					udr.handler.onUserDrawingFinishedEventHandler(line.toImage(zoom));
					break;
				case POLYGON:
					udr.handler.onUserDrawingFinishedEventHandler(polygon.toImage(zoom));
					break;
				case RECTANGLE:
					udr.handler.onUserDrawingFinishedEventHandler(rect.toImage(zoom));
					break;
				case CROSS:
					udr.handler.onUserDrawingFinishedEventHandler(cross.toImage(zoom));
					break;
				case ERASER:
					udr.handler.onUserDrawingFinishedEventHandler(erase_drawing);
					break;
				}
				setButtonsEnabled(image_edit_buttons, true);
			}

			@Override
			public void onClick(ClickEvent event) {
				final UserDrawingRequest udr = udr_queue.peek();
				if (udr == null) {
					if (start_point.valid)
						log.error("Valid starting point without a drawing request. Please report");
					return;
				}

				if (udr.type != Drawing.Type.POLYGON) {
					/*
					 * This is the end of the drawing operation. Click ends the operation in all
					 * cases except polygon which is multi click operation
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
						if (polygon.getPoints().size() > 1 && distance >= 0 && distance < polygonDistanceThreshold) {
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

					final Point query_point = (new Point(x, y)).toImage(page.getImageItem().getZoom());
					final Point min_distance_point = Point
							.getNearestPoint(page.getImageItem().getDrawings().getIntersectionPoints(), query_point);

					// maybe we are out
					old_point.valid = false;

					if (min_distance_point == null)
						return;

					double min_distance = query_point.distance(min_distance_point);

					if (min_distance > angleDistanceThreshold)
						return;

					final Line[] intersection_lines = page.getImageItem().getDrawings()
							.getInterSectionLines(min_distance_point);

					final String angle_str = NumberFormat.getFormat("0.0")
							.format(getAngle(intersection_lines[0], intersection_lines[1], query_point));

					context.fillText(angle_str, x, y);
				} else {
					final UserDrawingRequest udr = udr_queue.peek();
					if (udr == null)
						return;

					// here we draw the user interaction
					switch (udr.type) {
					case ELLIPSE:
						// find the bounding box
						final int w = 2 * (x > start_point.x ? x - start_point.x : start_point.x - x);
						final int h = 2 * (y > start_point.y ? y - start_point.y : start_point.y - y);
						ellipse.set(start_point.x, start_point.y, w, h);
						draw(context, ellipse, false);
						break;
					case LINE:
						line.set(start_point.x, start_point.y, x, y);
						draw(context, line, false);
						break;
					case POLYGON:
						polygon.getPoints().add(new Point(x, y));
						draw(context, polygon, false);
						polygon.getPoints().remove(polygon.getPoints().size() - 1);

						final double distance = getDistance(start_point, x, y);
						if (polygon.getPoints().size() > 1 && distance > 0 && distance < polygonDistanceThreshold) {
							context.beginPath();
							context.arc(start_point.x, start_point.y, polygonDistanceThreshold, 0, Math.PI * 2, true);
							context.closePath();
							context.stroke();
						}

						break;
					case RECTANGLE:
						final int wr = x > start_point.x ? x - start_point.x : start_point.x - x;
						final int hr = y > start_point.y ? y - start_point.y : start_point.y - y;
						final int xlr = x > start_point.x ? start_point.x : x;
						final int ytr = y > start_point.y ? start_point.y : y;

						rect.set(xlr, ytr, wr, hr);
						draw(context, rect, false);
						break;
					case CROSS:
						cross.set(x, y);
						draw(context, cross, false);
						break;
					case ERASER:
						final Zoom zoom = getCurrentPage().getImageItem().getZoom();
						erase_point.set(x, y);
						erase_drawing = getNearestDrawing(getCurrentPage().getImageItem().getDrawings(),
								erase_point.toImage(zoom));
						if (erase_drawing != null)
							draw(context, erase_drawing.toCanvas(zoom), true);
						break;
					}
				}
				old_point.x = x;
				old_point.y = y;
				old_point.valid = true;
			}
		});

		final String php_session_id = "PHPSESSID";

//		final IUploader.OnFinishUploaderHandler onImageFinishUploaderHandler = new IUploader.OnFinishUploaderHandler() {
//			@Override
//			public void onFinish(IUploader uploader) {
//				final Page.ImageItem image_item = getCurrentPage().getImageItem();
//				final PreloadedImage img = image_item.getImage();
//				if (img != null) {
//					setButtonsEnabled(image_edit_buttons, false);
//					img.removeFromParent();
//					image_item.setImage(null);
//				}
//				if (uploader.getStatus() != Status.SUCCESS)
//					return;
//				final ProgressDialogBox pd = new ProgressDialogBox("Saving Image...");
//				pd.show();
//				putResource(ResourceType.IMAGE, uploader.fileUrl() + "&session_id=" + Cookies.getCookie(php_session_id),
//						image_item, pd);
//			}
//		};

//		final IUploader.OnFinishUploaderHandler onVideoFinishUploaderHandler = new IUploader.OnFinishUploaderHandler() {
//			@Override
//			public void onFinish(IUploader uploader) {
//				if (uploader.getStatus() != Status.SUCCESS)
//					return;
//				final ProgressDialogBox pd = new ProgressDialogBox("Saving & Converting video. Please wait...");
//				pd.show();
//				putResource(ResourceType.VIDEO, uploader.fileUrl() + "&session_id=" + Cookies.getCookie(php_session_id),
//						getCurrentPage().getVideoItem(), pd);
//			}
//		};

		// image
//		final SingleUploader image_uploader = new SingleUploaderModal();
//		image_uploader.add(new Hidden("APC_UPLOAD_PROGRESS", image_uploader.getInputName()), 0);
//		image_uploader.setServletPath("../jsupload.php");
//		image_uploader.setValidExtensions(".png", ".jpg", ".jpeg", ".tiff", ".gif");
//		image_uploader.setAutoSubmit(true);
//		image_uploader.addOnFinishUploadHandler(onImageFinishUploaderHandler);
//		getImageUploaderContainer().add(image_uploader);

		zoom_121_b.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				final Zoom zoom = getCurrentPage().getImageItem().getZoom();
				zoom.setType(Zoom.Type.ZOOM_121);
				zoom.setLevel(1);
				final PreloadedImage img = getCurrentPage().getImageItem().getImage();
				if (isImageLoaded(img)) {
					zoom.getTarget().set(0, 0, img.getRealWidth(), img.getRealHeight());
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
				final PreloadedImage img = getCurrentPage().getImageItem().getImage();
				if (isImageLoaded(img)) {
					zoom.setLevel(((double) canvas.getOffsetWidth()) / ((double) img.getRealWidth()));
					zoom.getTarget().set(0, 0, img.getRealWidth(), img.getRealHeight());
				}
				redrawCanvas();
			}
		});

		zoom_target_b.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				final Zoom zoom = getCurrentPage().getImageItem().getZoom();
				zoom.setType(Zoom.Type.ZOOM_TARGET);
				waitUserDrawing(Drawing.Type.RECTANGLE, Drawing.Kind.ZOOM, new UserDrawingFinishedEventHandler() {
					@Override
					public void onUserDrawingFinishedEventHandler(Drawing drawing) {
						final Rectangle rect = (Rectangle) drawing;
						zoom.getTarget().set(rect);
						zoom.setLevel(((double) canvas.getOffsetWidth()) / ((double) rect.getWidth()));
						redrawCanvas();
					}
				});
			}
		});

		rect_hsp_b.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				waitUserDrawing(Drawing.Type.RECTANGLE,
						Drawing.Kind
								.getByDisplayName(areaTypeCombobox.getItemText(areaTypeCombobox.getSelectedIndex())),
						new UserDrawingFinishedEventHandler() {
							@Override
							public void onUserDrawingFinishedEventHandler(Drawing drawing) {
								getCurrentPage().getImageItem().getDrawings().add(drawing);
								redrawCanvas();
							}
						});
			}
		});

		ellipse_hsp_b.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				waitUserDrawing(Drawing.Type.ELLIPSE,
						Drawing.Kind
								.getByDisplayName(areaTypeCombobox.getItemText(areaTypeCombobox.getSelectedIndex())),
						new UserDrawingFinishedEventHandler() {
							@Override
							public void onUserDrawingFinishedEventHandler(Drawing drawing) {
								getCurrentPage().getImageItem().getDrawings().add(drawing);
								redrawCanvas();
							}
						});
			}
		});

		polygon_hsp_b.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				waitUserDrawing(Drawing.Type.POLYGON,
						Drawing.Kind
								.getByDisplayName(areaTypeCombobox.getItemText(areaTypeCombobox.getSelectedIndex())),
						new UserDrawingFinishedEventHandler() {
							@Override
							public void onUserDrawingFinishedEventHandler(Drawing drawing) {
								getCurrentPage().getImageItem().getDrawings().add(drawing);
								redrawCanvas();
							}
						});
			}
		});

		line_b.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				waitUserDrawing(Drawing.Type.LINE, Drawing.Kind.HELPER, new UserDrawingFinishedEventHandler() {
					@Override
					public void onUserDrawingFinishedEventHandler(Drawing drawing) {
						getCurrentPage().getImageItem().getDrawings().add(drawing);
						redrawCanvas();
					}
				});
			}
		});

		cross_b.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				waitUserDrawing(Drawing.Type.CROSS, Drawing.Kind.HELPER, new UserDrawingFinishedEventHandler() {
					@Override
					public void onUserDrawingFinishedEventHandler(Drawing drawing) {
						getCurrentPage().getImageItem().getDrawings().add(drawing);
						redrawCanvas();
					}
				});
			}
		});

		erase_b.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				waitUserDrawing(Drawing.Type.ERASER, Drawing.Kind.HELPER, new UserDrawingFinishedEventHandler() {
					@Override
					public void onUserDrawingFinishedEventHandler(Drawing drawing) {
						if (drawing == null)
							return;
						getCurrentPage().getImageItem().getDrawings().remove(drawing);
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
					brightness_l.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
					setBrightnessLabel(brightness_sl.getValue());

					contrast_l = new Label();
					contrast_l.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
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

					brightness_sl.addValueChangeHandler(new ValueChangeHandler<Double>() {
						@Override
						public void onValueChange(ValueChangeEvent<Double> event) {
							setBrightnessLabel(brightness_sl.getValue());
							redrawCanvas();
						}
					});

					contrast_sl.addValueChangeHandler(new ValueChangeHandler<Double>() {
						@Override
						public void onValueChange(ValueChangeEvent<Double> event) {
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

		showRegions_cb.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				getCurrentPage().getImageItem().setShowRegions(showRegions_cb.getValue());
			}
		});

		// video
//		final SingleUploader video_uploader = new SingleUploaderModal();
//		video_uploader.add(new Hidden("APC_UPLOAD_PROGRESS", video_uploader.getInputName()), 0);
//		video_uploader.setServletPath("../jsupload.php");
//		video_uploader.setValidExtensions(".mp4", ".mpeg", ".mpg", ".avi", ".mov");
//		video_uploader.setAutoSubmit(true);
//		video_uploader.addOnFinishUploadHandler(onVideoFinishUploaderHandler);
//		getVideoUploaderContainer().add(video_uploader);

		// quiz
		quiz_text_area.addValueChangeHandler(new ValueChangeHandler<String>() {
			@Override
			public void onValueChange(ValueChangeEvent<String> event) {
				getCurrentPage().getQuizItem().setText(event.getValue());
			}
		});

		add_answer_b.setEnabled(true);
		add_answer_b.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				final QuizAnswer quiz_question = new QuizAnswer(getCurrentPage().getQuizItem());
				getQuizAnswerContainer().add(quiz_question);
			}
		});

		// range quiz
		range_quiz_text_area.addValueChangeHandler(new ValueChangeHandler<String>() {
			@Override
			public void onValueChange(ValueChangeEvent<String> event) {
				getCurrentPage().getRangeQuizItem().setText(event.getValue());
			}
		});

		range_quiz_min_tb.addValueChangeHandler(new ValueChangeHandler<String>() {
			@Override
			public void onValueChange(ValueChangeEvent<String> event) {
				final Page.RangeQuizItem range_quiz_item = getCurrentPage().getRangeQuizItem();
				double min = range_quiz_item.getMin();
				try {
					min = Double.valueOf(event.getValue());
				} catch (Exception e) {
					log.warn("Invalid Range Quiz min value " + event.getValue());
					range_quiz_min_tb.setText(min + "");
				}
				range_quiz_item.setMin(min);
			}
		});

		range_quiz_max_tb.addValueChangeHandler(new ValueChangeHandler<String>() {
			@Override
			public void onValueChange(ValueChangeEvent<String> event) {
				final Page.RangeQuizItem range_quiz_item = getCurrentPage().getRangeQuizItem();
				double max = range_quiz_item.getMax();
				try {
					max = Double.valueOf(event.getValue());
				} catch (Exception e) {
					log.warn("Invalid Range Quiz max value " + event.getValue());
					range_quiz_max_tb.setText(max + "");
				}
				range_quiz_item.setMax(max);
			}
		});

		positive_grade_tb.addValueChangeHandler(new ValueChangeHandler<String>() {
			@Override
			public void onValueChange(ValueChangeEvent<String> event) {
				setGrade(event, true);
			}
		});

		negative_grade_tb.addValueChangeHandler(new ValueChangeHandler<String>() {
			@Override
			public void onValueChange(ValueChangeEvent<String> event) {
				setGrade(event, false);
			}
		});

		splashScreenLabel.setText("Reading Lesson...");

		final String url = Lesson.getResourceURL(cm_id, null);
		final RequestBuilder rb = new RequestBuilder(RequestBuilder.GET, url);
		try {
			rb.sendRequest(null, new RequestCallback() {
				@Override
				public void onError(final Request request, final Throwable e) {
					log.error(e.getMessage(), e);
				}

				@Override
				public void onResponseReceived(final Request request, final Response response) {
					lesson = Lesson.readXML(response.getText(), cm_id, AuthoringTool.this);

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

					if (log.isDebugEnabled()) {
						long endTimeMillis = System.currentTimeMillis();
						float durationSeconds = (endTimeMillis - startTimeMillis) / 1000F;
						log.debug("Lesson loading finished in: " + durationSeconds + " seconds");
					}
				}
			});
		} catch (final Exception e) {
			log.error(e.getMessage(), e);
		}

		/*
		 * Again, we need a guard here, otherwise <code>log_level=OFF</code> would still
		 * produce the following useless JavaScript: <pre> var durationSeconds,
		 * endTimeMillis; endTimeMillis = currentTimeMillis_0(); durationSeconds =
		 * (endTimeMillis - this$static.startTimeMillis) / 1000.0; </pre>
		 */
		if (log.isDebugEnabled()) {
			long endTimeMillis = System.currentTimeMillis();
			float durationSeconds = (endTimeMillis - startTimeMillis) / 1000F;
			log.debug("Duration: " + durationSeconds + " seconds");
		}
	}

	private void setGrade(ValueChangeEvent<String> event, boolean positive) {
		final Page page = getCurrentPage();
		final TextBox grade_tb = positive ? positive_grade_tb : negative_grade_tb;
		final int grade_default = positive ? page.getPositiveGrade() : page.getNegativeGrade();
		final int grade = Page.parseGrade(event.getValue(), grade_default);
		grade_tb.setText(grade + "");
		if (positive)
			page.setPositiveGrade(grade);
		else
			page.setNegativeGrade(grade);
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

	private static RootPanel getPageTitleContainer() {
		return RootPanel.get("pageTitleContainer");
	}

	private static RootPanel getImageTitleContainer() {
		return RootPanel.get("imageTitleContainer");
	}

	private static RootPanel getVideoTitleContainer() {
		return RootPanel.get("videoTitleContainer");
	}

	private static RootPanel getTextUploadAlignmentContainer() {
		return RootPanel.get("textUploadAlignmentContainer");
	}

	private static RootPanel getTextTextAreaContainer() {
		return RootPanel.get("textTextAreaContainer");
	}

	private static RootPanel getTextContainer() {
		return RootPanel.get("textContainer");
	}

	private static RootPanel getTextTitleContainer() {
		return RootPanel.get("textTitleContainer");
	}

	private static RootPanel getQuizTitleContainer() {
		return RootPanel.get("quizTitleContainer");
	}

	private static RootPanel getNonMediaContainer() {
		return RootPanel.get("nonMediaContainer");
	}

	private static RootPanel getImageUploaderContainer() {
		return RootPanel.get("imageUploaderContainer");
	}

	private static RootPanel getImageButtonContainer() {
		return RootPanel.get("imageButtonContainer");
	}

	private static RootPanel getCanvasContainer() {
		return RootPanel.get("canvasContainer");
	}

	private static RootPanel getImageContainer() {
		return RootPanel.get("imageContainer");
	}

	private static RootPanel getVideoPlayerContainer() {
		return RootPanel.get("videoPlayerContainer");
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

	private static RootPanel getMenuBarContainer() {
		return RootPanel.get("menuBarContainer");
	}

	private static RootPanel getLeftPanelContainer() {
		return RootPanel.get("leftPanelContainer");
	}

	private static RootPanel getPageContainer() {
		return RootPanel.get("pageContainer");
	}

	private static RootPanel getBugReportPopup() {
		return RootPanel.get("bugReportPopup");
	}

	private static RootPanel getQuizUploadAlignmentContainer() {
		return RootPanel.get("quizUploadAlignmentContainer");
	}

	private static RootPanel getQuizQuizAreaContainer() {
		return RootPanel.get("quizQuizAreaContainer");
	}

	private static RootPanel getQuizAnswerScrollContainer() {
		return RootPanel.get("quizAnswerScrollContainer");
	}

	private static RootPanel getRangeQuizUploadAlignmentContainer() {
		return RootPanel.get("rangeQuizUploadAlignmentContainer");
	}

	private static RootPanel getRangeQuizRangeQuizAreaContainer() {
		return RootPanel.get("rangeQuizRangeQuizAreaContainer");
	}

	private static RootPanel getPageLabelContainer() {
		return RootPanel.get("pageLabelContainer");
	}

	private static RootPanel getUpDownButtonlContainer() {
		return RootPanel.get("upDownButtonlContainer");
	}

	private static RootPanel getAddRemoveButtonContainer() {
		return RootPanel.get("addRemoveButtonContainer");
	}

	private static RootPanel getLeftPanelContainerParent() {
		return RootPanel.get("leftPanelContainerParent");
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
		log.debug("Checking index " + index + " vs. count " + count);
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
		updateUpDownButtons(page_button_container.getWidgetIndex(page_button_map.get(page).getParent()),
				page_button_container.getWidgetCount());
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

		final Page.Item.Type[] itemTypeCombination = page.getItemTypeCombination();
		combobox.setSelectedIndex(getComboboxOptionIndex(itemTypeCombination));

		// log.trace("Where am I: ", new Exception("Stacktrace"));
		title_tb.setText(page.getTitle());
		for (final Page.Item.Type type : itemTypeCombination) {
			switch (type) {
			case TEXT:
				getTextContainer().setVisible(true);
				text_text_area.setText(page.getTextItem().getText());
				break;
			case IMAGE:
				showRegions_cb.setValue(page.getImageItem().isShowRegions());
				imageContainer.setVisible(true);
				redrawCanvas();
				break;
			case VIDEO:
				getVideoContainer().setVisible(true);
				updateVideoPlayerContainer(page.getVideoItem());
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
				final Page.RangeQuizItem range_quiz_item = page.getRangeQuizItem();
				range_quiz_text_area.setText(range_quiz_item.getText());
				range_quiz_min_tb.setText(range_quiz_item.getMin() + "");
				range_quiz_max_tb.setText(range_quiz_item.getMax() + "");
				break;
			}
		}
		positive_grade_tb.setText(page.getPositiveGrade() + "");
		negative_grade_tb.setText(page.getNegativeGrade() + "");
		setButtonsEnabled(image_edit_buttons, page.getImageItem().getImage() != null);
		resizeSecondaryContainers(getWidthLeft(), getHeightLeft());
	}

	private static <T> T findCurrentItemAfterRemove(Collection<T> collection, T remove_item) {
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

	private static String getComboboxOptionText(Page.Item.Type[] itemTypeCombination) {
		return itemTypeCombination[0].getName() + itemTypeSeparator + itemTypeCombination[1].getName();
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
		final Page.Item.Type item1_type = Page.Item.Type.getTypeByName(type_names_a[0]);
		final Page.Item.Type item2_type = Page.Item.Type.getTypeByName(type_names_a[1]);
		return new Page.Item.Type[] { item1_type, item2_type };
	}

	private void onUpDownClick(boolean up) {
		final Page current_page = getCurrentPage();
		final Button current_button = page_button_map.get(current_page);
		final RootPanel page_button_container = getPageButtonContainer();
		final int current_index = page_button_container.getWidgetIndex(current_button.getParent());
		final int next_index = current_index + ((up) ? (-1) : 2);
		final int check_index = current_index + ((up) ? (-1) : 1);
		log.debug("Up " + up + " next_index " + next_index + " check_index " + check_index);
		page_button_container.insert(current_button.getParent(), next_index);
		lesson.remove(current_index);
		lesson.add(check_index, current_page);
		updateUpDownButtons(check_index, page_button_container.getWidgetCount());
	}

	private void waitUserDrawing(Drawing.Type type, Drawing.Kind kind, UserDrawingFinishedEventHandler handler) {
		final UserDrawingRequest udr = new UserDrawingRequest(type, kind, handler);
		udr_queue.add(udr);
		if (type == Type.ERASER || type == Type.CROSS) {
			startDrawingOperation(udr, -1, -1);
		}
	}

	private static double getDistance(Point start_point, double x, double y) {
		return start_point.valid
				? Math.sqrt((start_point.x - x) * (start_point.x - x) + (start_point.y - y) * (start_point.y - y))
				: -1;
	}

	private static void draw(Context2d context, Drawing drawing, boolean erase_color) {
		context.beginPath();
		context.setStrokeStyle(erase_color ? Drawing.getEraserColor() : drawing.getColor());
		context.setLineWidth(3);

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
			context.rect(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
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

	private void setButtonsEnabled(Collection<FocusWidget> buttons, boolean enable) {
		for (final FocusWidget button : buttons)
			button.setEnabled(enable);
		setAreaTypeComboboxEnabled(enable);
	}

	private void startDrawingOperation(UserDrawingRequest udr, int x, int y) {
		AuthoringTool.this.udr = udr;
		log.trace(udr.type + " Starting Point " + x + " " + y);
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

		final double angle_cos = (A1 * A2 + B1 * B2) / (Math.sqrt(A1 * A1 + B1 * B1) * Math.sqrt(A2 * A2 + B2 * B2));
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

	private static String getResourceIdsArgument(final ResourceType resource_type, final Object extra_info) {
		if (resource_type != ResourceType.XML)
			return "";
		@SuppressWarnings("unchecked")
		final Set<String> resource_ids = (Set<String>) extra_info;
		final StringBuilder sb = new StringBuilder("&resource_ids=");
		for (final String id : resource_ids) {
			sb.append(id + ",");
		}
		return sb.substring(0, sb.length() - 1);
	}

	private void putResource(final ResourceType resource_type, final String data, final Object extra_info,
			final ProgressDialogBox pd) {
		final RequestBuilder rb = new RequestBuilder(RequestBuilder.POST, "../put_resource.php?id=" + cm_id + "&type="
				+ resource_type + getResourceIdsArgument(resource_type, extra_info));
		rb.setHeader("Content-Type", "application/x-www-form-urlencoded");
		try {
			rb.sendRequest(URL.encodeQueryString(data), new RequestCallback() {
				@Override
				public void onResponseReceived(Request request, Response response) {
					if (response.getStatusCode() != Response.SC_OK) {
						final String error_msg = "HTTP Error for request: " + request + " Response: " + response
								+ " status code: " + response.getStatusCode();
						pd.hide();
						log.error(error_msg);
						Window.alert(error_msg);
						return;
					}
					final String response_text = response.getText();
					log.debug("Successfull request: " + request + " response: " + response_text);
					if (resource_type == ResourceType.IMAGE) {
						final Page.ImageItem image_item = (Page.ImageItem) extra_info;
						final String id = Page.ImageItem.getImageIdString(response_text);
						image_item.setId(id);
						image_item.setImage(new PreloadedImage(Lesson.getResourceURL(cm_id, id),
								new ImageItemOnLoadPreloadedImageHandler(image_item, true)));
						setButtonsEnabled(image_edit_buttons, true);
					} else if (resource_type == ResourceType.VIDEO) {
						final Page.VideoItem video_item = (Page.VideoItem) extra_info;
						video_item.setSources(Page.VideoItem.parseSources(response_text),
								new SetupVideoPlayerHandler());
					}
					pd.hide();
				}

				@Override
				public void onError(Request request, Throwable exception) {
					final String error_msg = "RequestError for request: " + request;
					pd.hide();
					log.error(error_msg, exception);
					Window.alert(error_msg);
				}
			});
		} catch (RequestException e) {
			final String error_msg = "Cannot save lesson. Please wait for server "
					+ "communication to be restored and retry later.";
			log.error(error_msg, e);
			pd.hide();
			Window.alert(error_msg);
		}
	}

	private void updateVideoPlayerContainer(Page.VideoItem video_item) {
		final Page current_page = getCurrentPage();
		log.debug("CurrentPage: " + current_page);
		if (current_page == null)
			return;
		log.debug("Current videoItem(): " + current_page.getVideoItem() + " video_item: " + video_item);
		if (current_page.getVideoItem() != video_item)
			return;
		final RootPanel videoPlayerContainer = getVideoPlayerContainer();
		final StringBuilder sb = new StringBuilder();
		sb.append("<video class=\"fill_x\" controls preload=\"none\">"); // poster="image_url
		for (final Page.VideoItem.Source source : video_item.getSources()) {
			final String url = Lesson.getResourceURL(cm_id, source.id);
			sb.append("<source src='" + url + "' type='" + source.content_type + "'/>");
			// sb.append("<source src='" + url + "' type='" +
			// source.content_type
			// + "; codecs=\"" + source.codecs + "\"'/>");
		}
		sb.append("<p class='serverResponseLabelError'>Cannot find valid content / codec combination.</p>");
		sb.append("</video>");
		videoPlayerContainer.getElement().setInnerHTML(sb.toString());
	}

	private static boolean containsType(Page.Item.Type[] current_item_combination, Page.Item.Type type) {
		return current_item_combination[0] == type || current_item_combination[1] == type;
	}

	private static boolean containsQuiz(Page.Item.Type[] current_item_combination) {
		return containsType(current_item_combination, Page.Item.Type.QUIZ)
				|| containsType(current_item_combination, Page.Item.Type.RANGE_QUIZ);
	}

	private static boolean containsImage(Page.Item.Type[] current_item_combination) {
		return containsType(current_item_combination, Page.Item.Type.IMAGE);
	}

	private static boolean containsImageQuiz(Page.Item.Type[] current_item_combination) {
		return containsImage(current_item_combination) && containsQuiz(current_item_combination);
	}

	private int getAreaTypeComboboxKindValueIndex(Drawing.Kind kind) {
		final int total = areaTypeCombobox.getItemCount();
		for (int i = 0; i < total; i++)
			if (kind.equals(Drawing.Kind.getByDisplayName(areaTypeCombobox.getItemText(i))))
				return i;
		return -1;
	}

	private void setAreaTypeComboboxEnabled(boolean enable, boolean contains_image_quiz) {
		if (contains_image_quiz)
			areaTypeCombobox.setSelectedIndex(getAreaTypeComboboxKindValueIndex(Drawing.Kind.INFO));
		areaTypeCombobox.setEnabled(enable && !contains_image_quiz);
	}

	private void setAreaTypeComboboxEnabled(boolean enable) {
		final Page.Item.Type[] current_item_combination = getCurrentItemTypeCombination();
		final boolean contains_image_quiz = containsImageQuiz(current_item_combination);
		setAreaTypeComboboxEnabled(enable, contains_image_quiz);
	}

	private static int getScrollBarWidth() {
		final Document document = Document.get();
		final ParagraphElement p = document.createPElement();
		p.getStyle().setWidth(100, Unit.PCT);
		p.getStyle().setHeight(200, Unit.PX);

		final DivElement div = document.createDivElement();
		div.getStyle().setPosition(Position.ABSOLUTE);
		div.getStyle().setTop(0, Unit.PX);
		div.getStyle().setLeft(0, Unit.PX);
		div.getStyle().setVisibility(Visibility.HIDDEN);
		div.getStyle().setWidth(200, Unit.PX);
		div.getStyle().setHeight(150, Unit.PX);
		div.getStyle().setOverflow(Overflow.HIDDEN);
		div.appendChild(p);

		document.getBody().appendChild(div);
		final int w1 = p.getOffsetWidth();
		div.getStyle().setOverflow(Overflow.SCROLL);
		int w2 = p.getOffsetWidth();

		if (w1 == w2)
			w2 = div.getClientWidth();

		document.getBody().removeChild(div);
		return w1 - w2;
	}

	private static void getNodeTextRecursively(Node n, StringBuilder sb) {
		if (n.getNodeType() == Node.TEXT_NODE) {
			final Text text_node = (Text) n;
			// log.trace("Got text node: " + text_node + " value: "
			// + text_node.getNodeValue() + " data: "
			// + text_node.getData());
			sb.append(text_node.getData() + "\n");
			return;
		}

		for (final Node node : new DOMNodeListWrapperList(n.getChildNodes())) {
			// log.trace("Got child node: " + node);
			getNodeTextRecursively(node, sb);
		}
	}

	private static String getLoggedText() {
		final StringBuilder sb = new StringBuilder();
		final Element logTextArea = DOM.getElementById("logTextArea");
		// log.debug("Got logTextArea " + logTextArea);
		getNodeTextRecursively(logTextArea, sb);
		return sb.toString();
	}

	private static int getPixels(String str) {
		if (str == null || str.isEmpty())
			return 0;
		return Math.round(Float.valueOf(str.replaceAll("\\s.*$", "").replaceAll("px", "")));
	}

	private static int getHeight(Element el) {
		log.trace("el.getOffsetHeight() = " + el.getOffsetHeight() + " marginTop: "
				+ ComputedStyle.getStyleProperty(el, "marginTop"));
		return el.getOffsetHeight() + getPixels(ComputedStyle.getStyleProperty(el, "marginTop"))
				+ getPixels(ComputedStyle.getStyleProperty(el, "marginBottom"));
	}

	private static int getHeight(RootPanel rp) {
		return getHeight(rp.getElement());
	}

	private static int getDecorationHeight(Element el) {
		return getPixels(ComputedStyle.getStyleProperty(el, "marginTop"))
				+ getPixels(ComputedStyle.getStyleProperty(el, "marginBottom"))
				+ getPixels(ComputedStyle.getStyleProperty(el, "paddingTop"))
				+ getPixels(ComputedStyle.getStyleProperty(el, "paddingBottom"))
				+ getPixels(ComputedStyle.getStyleProperty(el, "borderTopWidth"))
				+ getPixels(ComputedStyle.getStyleProperty(el, "borderBottomWidth"));
	}

	private static int getDecorationHeight(RootPanel rp) {
		return getDecorationHeight(rp.getElement());
	}

	private static int getWidth(Element el) {
		return el.getOffsetWidth() + getPixels(ComputedStyle.getStyleProperty(el, "marginLeft"))
				+ getPixels(ComputedStyle.getStyleProperty(el, "marginRight"));
	}

	private static int getWidth(RootPanel rp) {
		return getWidth(rp.getElement());
	}

	// private static int getWidth(String id) {
	// return getWidth(RootPanel.get(id));
	// }
}
