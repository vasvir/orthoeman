package org.orthoeman.shared;

import gwtupload.client.PreloadedImage;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.orthoeman.client.AuthoringTool;
import org.orthoeman.client.NodeListWrapperList;
import org.orthoeman.shared.Drawing.Kind;
import org.orthoeman.shared.Lesson.Page.ImageItem;
import org.orthoeman.shared.Lesson.Page.Item.Type;
import org.orthoeman.shared.Lesson.Page.QuizItem;
import org.orthoeman.shared.Lesson.Page.QuizItem.Answer;
import org.orthoeman.shared.Lesson.Page.RangeQuizItem;
import org.orthoeman.shared.Lesson.Page.VideoItem;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Element;
import com.google.gwt.xml.client.Node;
import com.google.gwt.xml.client.Text;
import com.google.gwt.xml.client.XMLParser;

public class Lesson extends ArrayList<Lesson.Page> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final String namespace = "http://orthoeman.org/";
	private static final String schemaLocation = namespace + "orthoeman.xsd";

	public interface PageListener {
		public void pageAdded(Page page);

		public void pageRemoved(Page page);
	}

	public static class Page {
		public static final Page.Item.Type[][] validItemTypeCombinations = {
				{ Item.Type.IMAGE, Item.Type.TEXT },
				{ Item.Type.IMAGE, Item.Type.QUIZ },
				{ Item.Type.IMAGE, Item.Type.RANGE_QUIZ },
				{ Item.Type.VIDEO, Item.Type.TEXT },
				{ Item.Type.VIDEO, Item.Type.QUIZ },
				{ Item.Type.TEXT, Item.Type.QUIZ } };

		public interface TitleChangedListener {
			public void titleChanged(String title);
		}

		public static class Item {
			public static enum Type {
				TEXT("Text"), QUIZ("Quiz"), IMAGE("Image"), VIDEO("Video"), RANGE_QUIZ(
						"Range Quiz");

				// enums are initialized before any static initializers are run
				private static Map<String, Type> name2TypeMap = new HashMap<String, Type>();
				private static Map<String, Type> typeName2TypeMap = new HashMap<String, Type>();
				static {
					for (final Type type : values()) {
						name2TypeMap.put(type.getName(), type);
						typeName2TypeMap.put(type.getTypeName(), type);
					}
				}

				private final String name;

				private Type(String name) {
					this.name = name;
				}

				public String getName() {
					return name;
				}

				public String getTypeName() {
					return name.replaceAll(" ", "");
				}

				public String getAttributeValue() {
					return getTypeName().toLowerCase();
				}

				public static Type getTypeByName(String name) {
					return name2TypeMap.get(name);
				}

				public static Type getTypeByTypeName(String name) {
					return typeName2TypeMap.get(name);
				}
			}

			private Type type;

			public Item(Type type) {
				setType(type);
			}

			public Type getType() {
				return type;
			}

			public void setType(Type type) {
				this.type = type;
			}
		}

		public static class TextItem extends Item {
			private String text;

			public TextItem(String text) {
				super(Type.TEXT);
				setText(text);
			}

			public TextItem() {
				this("");
			}

			public String getText() {
				return text;
			}

			public void setText(String text) {
				this.text = text;
			}

			@Override
			public String toString() {
				return getClass() + ":" + getText();
			}
		}

		public static class ImageItem extends Item {
			public static class DrawingList extends ArrayList<Drawing> {
				/**
				 * 
				 */
				private static final long serialVersionUID = 1L;
				private final List<List<Point>> intersection_point_lists = new ArrayList<List<Point>>();
				private final Map<Point, Line[]> intersection_point_lines_map = new HashMap<Point, Line[]>();

				@Override
				public boolean add(Drawing e) {
					if (e.getType() == Drawing.Type.LINE) {
						final Line line1 = (Line) e;

						final List<Point> new_intersection_point_list = new ArrayList<Point>();
						for (Drawing drawing : this) {
							if (drawing.getType() != Drawing.Type.LINE)
								continue;
							final Line line2 = (Line) drawing;
							final Point intersection_point = Line
									.getIntersectionPoint(line1, line2);
							new_intersection_point_list.add(intersection_point);
							intersection_point_lines_map.put(
									intersection_point, new Line[] { line1,
											line2 });
						}
						intersection_point_lists
								.add(new_intersection_point_list);
					}
					return super.add(e);
				}

				@Override
				public boolean remove(Object o) {
					if (o instanceof Line) {
						final Line line = (Line) o;

						// we need to find the index to remove
						int line_index = 0;
						for (Drawing drawing : this) {
							if (drawing.getType() != Drawing.Type.LINE)
								continue;
							if (line == drawing)
								break;
							line_index++;
						}

						// first remove elements from the map
						// remove columns in rows bigger than line_index
						for (int list_index = line_index + 1; list_index < intersection_point_lists
								.size(); list_index++) {
							intersection_point_lines_map
									.remove(intersection_point_lists.get(
											list_index).get(line_index));
						}
						// remove the row
						for (int j = 0; j < line_index; j++)
							intersection_point_lines_map
									.remove(intersection_point_lists.get(
											line_index).get(j));

						// now the data store: lower triangular 2d array
						// remove columns in rows bigger than line_index
						for (int list_index = line_index + 1; list_index < intersection_point_lists
								.size(); list_index++) {
							final List<Point> list = intersection_point_lists
									.get(list_index);
							list.remove(line_index);
						}
						// remove the row
						intersection_point_lists.remove(line_index);
					}
					return super.remove(o);
				}

				@Override
				public void clear() {
					intersection_point_lists.clear();
					intersection_point_lines_map.clear();
					super.clear();
				}

				public Collection<Point> getIntersectionPoints() {
					return new AbstractCollection<Point>() {
						class TriangularIterator implements Iterator<Point> {
							private int i = 1;
							private int j = 0;

							@Override
							public boolean hasNext() {
								final boolean finished = (j == 0)
										&& i >= intersection_point_lists.size();
								return !finished;
							}

							@Override
							public Point next() {
								final Point next_point = intersection_point_lists
										.get(i).get(j);

								if (j != i - 1) {
									j++;
								} else {
									i++;
									j = 0;
								}

								return next_point;
							}

							@Override
							public void remove() {
								throw new RuntimeException(
										"Cannot alter the Triangular collection "
												+ "through the iterator");
							}
						}

						@Override
						public Iterator<Point> iterator() {
							return new TriangularIterator();
						}

						@Override
						public int size() {
							final int n = intersection_point_lists.size();
							return n * (n - 1) / 2;
						}
					};
				}

				public Line[] getInterSectionLines(Point intersection_point) {
					return intersection_point_lines_map.get(intersection_point);
				}

				public int getHotSpotCount() {
					int count = 0;
					for (final Drawing drawing : this) {
						if (drawing.getKind() == Kind.HOTSPOT)
							count++;
					}
					return count;
				}
			}

			private String id;
			private PreloadedImage image;
			private Zoom zoom = new Zoom();
			private DrawingList drawings = new DrawingList();
			private boolean showRegions = true;

			public ImageItem() {
				super(Type.IMAGE);
			}

			public String getId() {
				return id;
			}

			public void setId(String id) {
				this.id = id;
			}

			public PreloadedImage getImage() {
				return image;
			}

			public void setImage(PreloadedImage image) {
				this.image = image;
			}

			public Zoom getZoom() {
				return zoom;
			}

			public void setZoom(Zoom zoom) {
				this.zoom = zoom;
			}

			public DrawingList getDrawings() {
				return drawings;
			}

			public boolean isShowRegions() {
				return showRegions;
			}

			public void setShowRegions(boolean showRegions) {
				this.showRegions = showRegions;
			}

			public static String getImageIdString(String response_text) {
				return response_text.split(":")[0];
			}
		}

		public static class VideoItem extends Item {
			public static class Source {
				public final String id;
				public final String content_type;
				public final String codecs;

				public Source(String id, String content_type, String codecs) {
					this.id = id;
					this.content_type = content_type;
					this.codecs = codecs;
				}

				@Override
				public String toString() {
					return id + ":" + content_type + ":" + codecs;
				}
			};

			private Collection<Source> sources = new ArrayList<Source>();

			public VideoItem() {
				super(Type.VIDEO);
			}

			public Collection<Source> getSources() {
				return sources;
			}

			public void setSources(Collection<Source> sources,
					AuthoringTool.SetupVideoPlayerHandler video_player_handler) {
				this.sources = sources;
				Log.debug("Setting " + this + " sources: " + sources);
				video_player_handler.setupVideoPlayer(this);
			}

			public static Collection<Source> parseSources(String response_text) {
				final Collection<Source> sources = new ArrayList<Source>();
				final String[] response_text_a = response_text.split("\\|");
				for (final String val : response_text_a) {
					final String[] val_a = val.split(":");
					sources.add(new Source(val_a[0], val_a[1], val_a[2]));
				}
				return sources;
			}
		}

		public static class QuizItem extends Item {
			public static class Answer {
				private String text;
				private boolean correct;

				public Answer(String text, boolean correct) {
					setText(text);
					setCorrect(correct);
				}

				public Answer() {
					this("", false);
				}

				public String getText() {
					return text;
				}

				public void setText(String text) {
					this.text = text;
				}

				public boolean isCorrect() {
					return correct;
				}

				public void setCorrect(boolean correct) {
					this.correct = correct;
				}
			}

			private String text;
			private LinkedHashMap<Integer, Answer> answerMap = new LinkedHashMap<Integer, Answer>();
			private int uniqueIdentifier;

			public QuizItem(String text) {
				super(Type.QUIZ);
				setText(text);
			}

			public QuizItem() {
				this("");
			}

			public String getText() {
				return text;
			}

			public void setText(String text) {
				this.text = text;
			}

			public LinkedHashMap<Integer, Answer> getAnswerMap() {
				return answerMap;
			}

			public int createAnswer(String text, boolean correct) {
				getAnswerMap().put(uniqueIdentifier, new Answer(text, correct));
				return uniqueIdentifier++;
			}

			public int createAnswer() {
				return createAnswer("", false);
			}
		}

		public static class RangeQuizItem extends Item {
			private String text;
			private double min;
			private double max;

			public RangeQuizItem(String text, double min, double max) {
				super(Type.RANGE_QUIZ);
				setText(text);
				setMin(min);
				setMax(max);
			}

			public RangeQuizItem() {
				this("", 0, 1);
			}

			public String getText() {
				return text;
			}

			public void setText(String text) {
				this.text = text;
			}

			public double getMin() {
				return min;
			}

			public void setMin(double min) {
				this.min = min;
			}

			public double getMax() {
				return max;
			}

			public void setMax(double max) {
				this.max = max;
			}
		}

		private String title;
		private Page.Item.Type[] itemTypeCombination;
		private ImageItem imageItem;
		private TextItem textItem;
		private QuizItem quizItem;
		private VideoItem videoItem;
		private RangeQuizItem rangeQuizItem;
		private static final int positiveGradeDefault = 10;
		private static final int negativeGradeDefault = 5;
		private int positiveGrade = positiveGradeDefault;
		private int negativeGrade = negativeGradeDefault;

		private Collection<TitleChangedListener> titleChangedListeners = new ArrayList<TitleChangedListener>();

		public Page(String title) {
			setTitle(title);
			setItemTypeCombination(validItemTypeCombinations[0]);
			setTextItem(new TextItem());
			setImageItem(new ImageItem());
			setVideoItem(new VideoItem());
			setQuizItem(new QuizItem());
			setRangeQuizItem(new RangeQuizItem());
		}

		public Page() {
			this("");
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;

			for (final TitleChangedListener li : titleChangedListeners)
				li.titleChanged(title);
		}

		public void addTitleChangedListener(TitleChangedListener li) {
			titleChangedListeners.add(li);
		}

		public void removeTitleChangedListener(TitleChangedListener li) {
			titleChangedListeners.remove(li);
		}

		public Page.Item.Type[] getItemTypeCombination() {
			return itemTypeCombination;
		}

		public void setItemTypeCombination(Page.Item.Type[] itemTypeCombination) {
			this.itemTypeCombination = itemTypeCombination;
		}

		public ImageItem getImageItem() {
			return imageItem;
		}

		public void setImageItem(ImageItem imageItem) {
			this.imageItem = imageItem;
		}

		public TextItem getTextItem() {
			return textItem;
		}

		public void setTextItem(TextItem textItem) {
			this.textItem = textItem;
		}

		public QuizItem getQuizItem() {
			return quizItem;
		}

		public void setQuizItem(QuizItem quizItem) {
			this.quizItem = quizItem;
		}

		public VideoItem getVideoItem() {
			return videoItem;
		}

		public void setVideoItem(VideoItem videoItem) {
			this.videoItem = videoItem;
		}

		public RangeQuizItem getRangeQuizItem() {
			return rangeQuizItem;
		}

		public void setRangeQuizItem(RangeQuizItem rangeQuizItem) {
			this.rangeQuizItem = rangeQuizItem;
		}

		public int getPositiveGrade() {
			return positiveGrade;
		}

		public void setPositiveGrade(int positive_grade) {
			this.positiveGrade = positive_grade;
		}

		public int getNegativeGrade() {
			return negativeGrade;
		}

		public void setNegativeGrade(int negative_grade) {
			this.negativeGrade = negative_grade;
		}

		@Override
		public String toString() {
			return "Page: " + getTitle() + " items: " + super.toString();
		}

		@Override
		public int hashCode() {
			return System.identityHashCode(this);
		}

		@Override
		public boolean equals(Object o) {
			if (o == this)
				return true;
			return false;
		}

		public static int parseGrade(String grade_str, int grade_default) {
			int grade = grade_default;
			try {
				final double value = Math.abs(Double.valueOf(grade_str));
				if (value > 100)
					grade = 100;
				else if (value < 1)
					grade = 1;
				else
					grade = (int) Math.rint(value);
			} catch (Exception e) {
				Log.warn("Invalid grade " + grade_str);
			}
			return grade;
		}
	}

	private Collection<PageListener> pageListeners = new ArrayList<PageListener>();

	public void addPageListener(PageListener li) {
		pageListeners.add(li);
	}

	public void removePageListener(PageListener li) {
		pageListeners.remove(li);
	}

	private static String getTextValue(Text text_n) {
		if (text_n == null)
			return "";
		return text_n.getNodeValue();
	}

	private static String getTextValue(Element e) {
		return getTextValue((Text) e.getFirstChild());
	}

	private static String getTextValue(Element e, String tag_name) {
		return getTextValue((Text) e.getElementsByTagName(tag_name).item(0)
				.getFirstChild());
	}

	public static Lesson readXML(String contents, String orthoeman_id,
			AuthoringTool authoring_tool) {
		// Log.trace("Parsing: " + contents);
		final Lesson lesson = new Lesson();

		if (contents == null || contents.trim().isEmpty())
			return lesson;

		final Document doc = XMLParser.parse(contents);
		final NodeListWrapperList lessons = new NodeListWrapperList(
				doc.getElementsByTagName("Lesson"));
		if (lessons.isEmpty())
			return lesson;
		final Element lesson_e = (Element) lessons.get(0);

		final NodeListWrapperList pages_nl = new NodeListWrapperList(
				lesson_e.getElementsByTagName("Page"));
		for (final Node page_n : pages_nl) {
			final Page page = new Page();
			final Element page_e = (Element) page_n;

			page.setTitle(page_e.getAttribute("title"));
			page.setPositiveGrade(Page.parseGrade(
					page_e.getAttribute("positiveGrade"),
					Page.positiveGradeDefault));
			page.setNegativeGrade(Page.parseGrade(
					page_e.getAttribute("negativeGrade"),
					Page.negativeGradeDefault));

			final Type[] itemTypeCombinationsFound = { null, null };
			int itemTypeCombinationsFoundCount = 0;

			final NodeListWrapperList widget_nl = new NodeListWrapperList(
					page_e.getChildNodes());
			for (final Node widget_n : widget_nl) {
				final Element widget_e = (Element) widget_n;
				final Type item_type = Type.getTypeByTypeName(widget_e
						.getTagName());
				itemTypeCombinationsFound[itemTypeCombinationsFoundCount++] = item_type;
				switch (item_type) {
				case IMAGE:
					final Element image_e = widget_e;
					final String id = image_e.getAttribute("id");

					final ImageItem image_item = page.getImageItem();
					image_item.setShowRegions(parseBoolean(image_e
							.getAttribute("showRegions")));
					if (id != null) {
						image_item.setId(id);
						final String url = getResourceURL(orthoeman_id, id);
						/*
						 * Here is the trick. Either the loading finishes real
						 * fast or not
						 * 
						 * Case 1: Real fast: Then the lesson has not been
						 * initialized yet. The handler does not redraw because
						 * current page is null. When readXML returns the master
						 * code will set the currentPage to the first and a
						 * redraw will be issued
						 * 
						 * Case 2: Real slow: Lesson is returned with images
						 * half loaded. The guard checks inside redraw protect
						 * the image. When the image is loaded a redraw is
						 * executed.
						 */
						image_item
								.setImage(new PreloadedImage(
										url,
										authoring_tool.new ImageItemOnLoadPreloadedImageHandler(
												image_item, false)));
					}

					// now let's get all drawings
					final NodeListWrapperList drawing_nl = new NodeListWrapperList(
							image_e.getChildNodes());
					for (final Node drawing_n : drawing_nl) {
						final Element drawing_e = (Element) drawing_n;
						final String tagname = drawing_e.getTagName();
						final Kind kind = parseBoolean(drawing_e
								.getAttribute("isHotSpot")) ? Kind.HOTSPOT
								: Kind.INFO;
						Drawing drawing = null;
						if (tagname.equals("Polygon")) {
							final NodeListWrapperList points_nl = new NodeListWrapperList(
									drawing_e.getChildNodes());
							final List<Point> points = new ArrayList<Point>();
							for (final Node point_n : points_nl) {
								final Element point_e = (Element) point_n;
								final int x = Integer.valueOf(point_e
										.getAttribute("x"));
								final int y = Integer.valueOf(point_e
										.getAttribute("y"));
								final Point point = new Point(x, y);
								points.add(point);
							}
							drawing = new Polygon(kind, points);
						} else if (tagname.equals("Ellipse")) {
							final int width = Integer.valueOf(drawing_e
									.getAttribute("radiusX")) * 2;
							final int height = Integer.valueOf(drawing_e
									.getAttribute("radiusY")) * 2;
							final Element center_e = (Element) drawing_e
									.getElementsByTagName("Center").item(0);
							final int x = Integer.valueOf(center_e
									.getAttribute("x"));
							final int y = Integer.valueOf(center_e
									.getAttribute("y"));
							drawing = new Ellipse(kind, x, y, width, height);
						} else if (tagname.equals("Rectangle")) {
							final int width = Integer.valueOf(drawing_e
									.getAttribute("width"));
							final int height = Integer.valueOf(drawing_e
									.getAttribute("height"));
							final Element top_left_point_e = (Element) drawing_e
									.getElementsByTagName("Point").item(0);
							final int x = Integer.valueOf(top_left_point_e
									.getAttribute("x"));
							final int y = Integer.valueOf(top_left_point_e
									.getAttribute("y"));
							drawing = new Rectangle(kind, x, y, width, height);
						} else {
							Log.error("Unknown drawing type " + tagname);
							continue;
						}
						page.getImageItem().getDrawings().add(drawing);
					}
					break;
				case QUIZ:
					final Element quiz_e = widget_e;
					page.getQuizItem()
							.setText(getTextValue(quiz_e, "Question"));
					final NodeListWrapperList answer_nl = new NodeListWrapperList(
							quiz_e.getElementsByTagName("Answer"));
					for (final Node answer_n : answer_nl) {
						final Element answer_e = (Element) answer_n;
						page.getQuizItem()
								.createAnswer(
										getTextValue(answer_e),
										parseBoolean(answer_e
												.getAttribute("isCorrect")));
					}
					break;
				case RANGE_QUIZ:
					final Element range_quiz_e = widget_e;
					page.getRangeQuizItem().setText(
							getTextValue(range_quiz_e, "Question"));
					page.getRangeQuizItem().setMin(
							Double.valueOf(range_quiz_e
									.getAttribute("minValue")));
					page.getRangeQuizItem().setMax(
							Double.valueOf(range_quiz_e
									.getAttribute("maxValue")));
					break;
				case TEXT:
					page.getTextItem().setText(getTextValue(widget_e));
					break;
				case VIDEO:
					final Element video_e = widget_e;
					final NodeListWrapperList source_nl = new NodeListWrapperList(
							video_e.getElementsByTagName("Source"));
					final Collection<VideoItem.Source> sources = new ArrayList<VideoItem.Source>();
					for (final Node source_n : source_nl) {
						final Element source_e = (Element) source_n;
						final VideoItem.Source source = new VideoItem.Source(
								source_e.getAttribute("id"),
								source_e.getAttribute("type"),
								source_e.getAttribute("codecs"));
						sources.add(source);
					}
					page.getVideoItem().setSources(sources,
							authoring_tool.new SetupVideoPlayerHandler());
					break;
				}

				for (final Type[] validItemTypeCombination : Page.validItemTypeCombinations) {
					if ((validItemTypeCombination[0] == itemTypeCombinationsFound[0] && validItemTypeCombination[1] == itemTypeCombinationsFound[1])
							|| (validItemTypeCombination[0] == itemTypeCombinationsFound[1] && validItemTypeCombination[1] == itemTypeCombinationsFound[0])) {
						page.setItemTypeCombination(validItemTypeCombination);
						break;
					}
				}
			}

			lesson.add(page);
		}

		return lesson;
	}

	public static String writeXML(Lesson lesson) {
		final Document doc = XMLParser.createDocument();

		final Element root_e = doc.createElement("Lesson");
		// root_e.setAttribute("xmlns", namespace);
		root_e.setAttribute("xmlns:xsi",
				"http://www.w3.org/2001/XMLSchema-instance");
		root_e.setAttribute("xsi:schemaLocation", schemaLocation);

		for (final Page page : lesson) {
			final Element page_e = doc.createElement("Page");
			page_e.setAttribute("title", page.getTitle());
			page_e.setAttribute("positiveGrade",
					"" + Math.abs(page.getPositiveGrade()));
			page_e.setAttribute("negativeGrade",
					"" + Math.abs(page.getNegativeGrade()));

			final Type[] item_types = page.getItemTypeCombination();
			for (Type item_type : item_types) {
				switch (item_type) {
				case IMAGE:
					final Element image_e = doc.createElement(item_type
							.getTypeName());
					// image.setAttribute("showRegions", "yes");
					final ImageItem image_item = page.getImageItem();
					image_e.setAttribute("showRegions",
							booleanToString(image_item.isShowRegions()));
					final String id = image_item.getId();
					if (id != null) {
						image_e.setAttribute("id", id);
						image_e.setAttribute("width", ""
								+ image_item.getImage().getRealWidth());
						image_e.setAttribute("height", ""
								+ image_item.getImage().getRealHeight());

						for (final Drawing drawing : image_item.getDrawings()) {
							switch (drawing.getType()) {
							case ELLIPSE:
								final Element ellipse_e = doc
										.createElement("Ellipse");
								final Ellipse ellipse = (Ellipse) drawing;
								ellipse_e
										.setAttribute(
												"isHotSpot",
												booleanToString(ellipse
														.getKind() == Kind.HOTSPOT));
								ellipse_e.setAttribute("radiusX",
										"" + ellipse.getWidth() / 2);
								ellipse_e.setAttribute("radiusY",
										"" + ellipse.getHeight() / 2);
								final Element center_e = doc
										.createElement("Center");
								center_e.setAttribute("x", "" + ellipse.getX());
								center_e.setAttribute("y", "" + ellipse.getY());
								ellipse_e.appendChild(center_e);
								image_e.appendChild(ellipse_e);
								break;
							case POLYGON:
								final Element polygon_e = doc
										.createElement("Polygon");
								final Polygon polygon = (Polygon) drawing;
								polygon_e
										.setAttribute(
												"isHotSpot",
												booleanToString(polygon
														.getKind() == Kind.HOTSPOT));
								for (final Point point : polygon.getPoints()) {
									final Element point_e = doc
											.createElement("Point");
									point_e.setAttribute("x", "" + point.x);
									point_e.setAttribute("y", "" + point.y);
									polygon_e.appendChild(point_e);
								}
								image_e.appendChild(polygon_e);
								break;
							case RECTANGLE:
								final Element rectangle_e = doc
										.createElement("Rectangle");
								final Rectangle rectangle = (Rectangle) drawing;
								rectangle_e
										.setAttribute(
												"isHotSpot",
												booleanToString(rectangle
														.getKind() == Kind.HOTSPOT));
								rectangle_e.setAttribute("width", ""
										+ rectangle.getWidth());
								rectangle_e.setAttribute("height", ""
										+ rectangle.getHeight());
								final Element top_left_point_e = doc
										.createElement("Point");
								top_left_point_e.setAttribute("x", ""
										+ rectangle.getX());
								top_left_point_e.setAttribute("y", ""
										+ rectangle.getY());
								rectangle_e.appendChild(top_left_point_e);
								image_e.appendChild(rectangle_e);
								break;
							case CROSS:
							case LINE:
							case ERASER:
								// skip
								break;
							}
						}
					}
					page_e.appendChild(image_e);
					break;
				case QUIZ:
					final Element quiz_e = doc.createElement(item_type
							.getTypeName());
					final QuizItem quiz_item = page.getQuizItem();
					final Element question_e = doc.createElement("Question");
					question_e.appendChild(doc.createTextNode(quiz_item
							.getText()));
					quiz_e.appendChild(question_e);
					for (final Answer answer : quiz_item.getAnswerMap()
							.values()) {
						final Element answer_e = doc.createElement("Answer");
						answer_e.setAttribute("isCorrect",
								booleanToString(answer.isCorrect()));
						answer_e.appendChild(doc.createTextNode(answer
								.getText()));
						quiz_e.appendChild(answer_e);
					}
					page_e.appendChild(quiz_e);
					break;
				case RANGE_QUIZ:
					final Element range_quiz_e = doc.createElement(item_type
							.getTypeName());
					final RangeQuizItem range_quiz_item = page
							.getRangeQuizItem();
					range_quiz_e.setAttribute("minValue",
							"" + range_quiz_item.getMin());
					range_quiz_e.setAttribute("maxValue",
							"" + range_quiz_item.getMax());
					final Element range_question_e = doc
							.createElement("Question");
					range_question_e.appendChild(doc
							.createTextNode(range_quiz_item.getText()));
					range_quiz_e.appendChild(range_question_e);
					page_e.appendChild(range_quiz_e);
					break;
				case TEXT:
					final Element text_e = doc.createElement(item_type
							.getTypeName());
					text_e.appendChild(doc.createTextNode(page.getTextItem()
							.getText()));
					page_e.appendChild(text_e);
					break;
				case VIDEO:
					final Element video_e = doc.createElement(item_type
							.getTypeName());
					for (final VideoItem.Source source : page.getVideoItem()
							.getSources()) {
						final Element source_e = doc.createElement("Source");
						source_e.setAttribute("id", source.id);
						source_e.setAttribute("type", source.content_type);
						// source_e.setAttribute("codecs", source.codecs);
						video_e.appendChild(source_e);
					}
					page_e.appendChild(video_e);
					break;
				}
			}

			root_e.appendChild(page_e);
		}

		doc.appendChild(root_e);

		final String xmlns_namespace = "xmlns=\"" + namespace + "\"";
		return doc
				.toString()
				.replaceAll("<Lesson", "<Lesson " + xmlns_namespace)
				.replaceAll(xmlns_namespace + " " + xmlns_namespace,
						xmlns_namespace);
	}

	private void notifyPageListeners(Page page, boolean added) {
		if (added) {
			for (final PageListener li : pageListeners) {
				li.pageAdded(page);
			}
		} else {
			for (final PageListener li : pageListeners) {
				li.pageRemoved(page);
			}
		}
	}

	@Override
	public boolean add(Page e) {
		final boolean status = super.add(e);
		if (status)
			notifyPageListeners(e, true);
		return status;
	}

	@Override
	public void add(int index, Page element) {
		super.add(index, element);
		notifyPageListeners(element, true);
	}

	@Override
	public Page remove(int index) {
		final Page page = super.remove(index);
		notifyPageListeners(page, true);
		return page;
	}

	@Override
	public boolean remove(Object o) {
		final boolean status = super.remove(o);
		if (status)
			notifyPageListeners((Page) o, false);
		return status;
	}

	public Set<String> getResourceIds() {
		final Set<String> resource_ids = new HashSet<String>();
		for (final Page page : this) {
			final String image_id = page.getImageItem().getId();
			if (image_id != null) {
				resource_ids.add(image_id);
			}
			for (final Page.VideoItem.Source source : page.getVideoItem()
					.getSources()) {
				resource_ids.add(source.id);
			}
		}
		return resource_ids;
	}

	public static String getResourceURL(String orthoeman_id, String resource_id) {
		final String get_resource_php = "../get_resource.php?id="
				+ orthoeman_id;
		if (resource_id == null)
			return get_resource_php;
		return get_resource_php + "&resource_id=" + resource_id;
	}

	public static boolean parseBoolean(String text) {
		if (text != null && text.toLowerCase().equals("yes"))
			return true;
		return false;
	}

	public static String booleanToString(boolean b) {
		return b ? "yes" : "no";
	}
}
