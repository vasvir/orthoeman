package org.orthoeman.shared;

import gwtupload.client.PreloadedImage;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.orthoeman.client.NodeListWrapperList;
import org.orthoeman.shared.Drawing.Kind;
import org.orthoeman.shared.Lesson.Page.Item.Type;
import org.orthoeman.shared.Lesson.Page.QuizItem;
import org.orthoeman.shared.Lesson.Page.QuizItem.Answer;
import org.orthoeman.shared.Lesson.Page.RangeQuizItem;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Element;
import com.google.gwt.xml.client.Node;
import com.google.gwt.xml.client.Text;
import com.google.gwt.xml.client.XMLParser;

public class Lesson extends ArrayList<Lesson.Page> {
	public interface PageListener {
		public void pageAdded(Page page);

		public void pageRemoved(Page page);
	}

	public static class Page {
		public static final Page.Item.Type[][] validItemTypeCombinations = {
				{ Item.Type.TEXT, Item.Type.IMAGE },
				{ Item.Type.IMAGE, Item.Type.QUIZ },
				{ Item.Type.IMAGE, Item.Type.RANGE_QUIZ },
				{ Item.Type.TEXT, Item.Type.VIDEO },
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

		public static class ResourceItem extends Item {
			private String url;

			public ResourceItem(Type type) {
				super(type);
			}

			public String getURL() {
				return url;
			}

			public void setURL(String url) {
				this.url = url;
			}
		}

		public static class ImageItem extends ResourceItem {
			public static class DrawingList extends ArrayList<Drawing> {
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
			}

			private PreloadedImage image;
			private Zoom zoom = new Zoom();
			private DrawingList drawings = new DrawingList();

			public ImageItem() {
				super(Type.IMAGE);
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
		}

		public static class VideoItem extends ResourceItem {
			public VideoItem() {
				super(Type.VIDEO);
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
		private double weight;
		private boolean block;

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

		public double getWeight() {
			return weight;
		}

		public void setWeight(double weight) {
			this.weight = weight;
		}

		public boolean isBlock() {
			return block;
		}

		public void setBlock(boolean block) {
			this.block = block;
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
	}

	private Collection<PageListener> pageListeners = new ArrayList<PageListener>();

	/** TODO Lesson must be initialized with id, author, title, abstract */
	private String id = "lesson_id";
	private String author = "A. Authoropoulos";
	private String title = "Nice title";
	private String abstrakt = "A very nice lesson. Really...";

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public String getAbstract() {
		return abstrakt;
	}

	public void setAbstract(String abstrakt) {
		this.abstrakt = abstrakt;
	}

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

	public static Lesson readXML(String contents,
			PreloadedImage.OnLoadPreloadedImageHandler onload_handler) {
		// Log.trace("Parsing: " + contents);
		final Lesson lesson = new Lesson();

		final Document doc = XMLParser.parse(contents);
		final Element lesson_e = (Element) doc.getElementsByTagName("Lesson")
				.item(0);

		lesson.setId(lesson_e.getAttribute("Id"));
		lesson.setAuthor(lesson_e.getAttribute("Author"));
		lesson.setTitle(lesson_e.getAttribute("Title"));
		lesson.setAbstract(getTextValue(lesson_e, "Abstract"));

		final NodeListWrapperList pages_nl = new NodeListWrapperList(
				lesson_e.getElementsByTagName("Page"));
		for (final Node page_n : pages_nl) {
			final Page page = new Page();
			final Element page_e = (Element) page_n;

			page.setTitle(page_e.getAttribute("Title"));
			page.setWeight(Double.valueOf(page_e.getAttribute("Grade")));
			page.setBlock(Boolean.valueOf(page_e.getAttribute("Blocked")));

			final Type[] itemTypeCombinationsFound = { null, null };
			int itemTypeCombinationsFoundCount = 0;

			final NodeListWrapperList widget_nl = new NodeListWrapperList(
					page_e.getElementsByTagName("Widget"));
			for (final Node widget_n : widget_nl) {
				final Element widget_e = (Element) widget_n;
				final Type item_type = Type.getTypeByTypeName(widget_e
						.getAttribute("Type"));
				itemTypeCombinationsFound[itemTypeCombinationsFoundCount++] = item_type;
				switch (item_type) {
				case IMAGE:
					final Element image_e = (Element) widget_e
							.getElementsByTagName(item_type.getTypeName())
							.item(0);
					final String url = image_e.getAttribute("uri");
					if (url != null) {
						final PreloadedImage img = new PreloadedImage(url,
								onload_handler);
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
						page.getImageItem().setImage(img);
					}

					// now let's get all drawings
					final NodeListWrapperList drawing_nl = new NodeListWrapperList(
							image_e.getChildNodes());
					for (final Node drawing_n : drawing_nl) {
						final Element drawing_e = (Element) drawing_n;
						final String tagname = drawing_e.getTagName();
						final Kind kind = Boolean.valueOf(drawing_e
								.getAttribute("IsHotSpot")) ? Kind.BLOCKING
								: Kind.INFORMATIONAL;
						Drawing drawing = null;
						if (tagname.equals("Polygon")) {
							final NodeListWrapperList points_nl = new NodeListWrapperList(
									drawing_e.getChildNodes());
							final List<Point> points = new ArrayList<Point>();
							for (final Node point_n : points_nl) {
								final Element point_e = (Element) point_n;
								final int x = Integer.valueOf(point_e
										.getAttribute("X"));
								final int y = Integer.valueOf(point_e
										.getAttribute("Y"));
								final Point point = new Point(x, y);
								points.add(point);
							}
							drawing = new Polygon(kind, points);
						} else if (tagname.equals("Ellipse")
								|| tagname.equals("Rectangle")) {
							final int x = Integer.valueOf(drawing_e
									.getAttribute("X"));
							final int y = Integer.valueOf(drawing_e
									.getAttribute("Y"));
							final int width = Integer.valueOf(drawing_e
									.getAttribute("Width"));
							final int height = Integer.valueOf(drawing_e
									.getAttribute("Height"));
							drawing = tagname.equals("Ellipse") ? new Ellipse(
									kind, x, y, width, height) : new Rectangle(
									kind, x, y, width, height);
						} else {
							Log.error("Unknown drawing type " + tagname);
							continue;
						}
						page.getImageItem().getDrawings().add(drawing);
					}
					break;
				case QUIZ:
					final Element quiz_e = (Element) widget_e
							.getElementsByTagName(item_type.getTypeName())
							.item(0);
					page.getQuizItem()
							.setText(getTextValue(quiz_e, "Question"));
					final NodeListWrapperList answer_nl = new NodeListWrapperList(
							quiz_e.getElementsByTagName("Answer"));
					for (final Node answer_n : answer_nl) {
						final Element answer_e = (Element) answer_n;
						page.getQuizItem().createAnswer(
								getTextValue(answer_e),
								Boolean.valueOf(answer_e
										.getAttribute("IsCorrect")));
					}
					break;
				case RANGE_QUIZ:
					final Element range_quiz_e = (Element) widget_e
							.getElementsByTagName(item_type.getTypeName())
							.item(0);
					page.getRangeQuizItem().setText(
							getTextValue(range_quiz_e, "Question"));
					page.getRangeQuizItem().setMin(
							Double.valueOf(getTextValue(range_quiz_e, "min")));
					page.getRangeQuizItem().setMax(
							Double.valueOf(getTextValue(range_quiz_e, "max")));
					break;
				case TEXT:
					page.getTextItem().setText(
							getTextValue(widget_e, item_type.getTypeName()));
					break;
				case VIDEO:
					final Element video_e = (Element) widget_e
							.getElementsByTagName(item_type.getTypeName())
							.item(0);
					page.getVideoItem().setURL(video_e.getAttribute("uri"));
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
		root_e.setAttribute("xmlns", "http://orthoeman.iit.demokritos.gr/");
		root_e.setAttribute("xmlns:xsi",
				"http://www.w3.org/2001/XMLSchema-instance");
		root_e.setAttribute("xsi:schemaLocation",
				"http://orthoeman.iit.demokritos.gr/orthoeman.xsd");
		root_e.setAttribute("Id", lesson.getId());
		root_e.setAttribute("Author", lesson.getAuthor());
		root_e.setAttribute("Title", lesson.getTitle());

		final Element abstrakt_e = doc.createElement("Abstract");
		abstrakt_e.appendChild(doc.createTextNode(lesson.getAbstract()));
		root_e.appendChild(abstrakt_e);

		for (final Page page : lesson) {
			final Element page_e = doc.createElement("Page");
			page_e.setAttribute("Title", "" + page.getTitle());
			page_e.setAttribute("Grade", "" + page.getWeight());
			page_e.setAttribute("Blocked", "" + page.isBlock());

			final Type[] item_types = page.getItemTypeCombination();
			for (Type item_type : item_types) {
				final Element widget_e = doc.createElement("Widget");
				widget_e.setAttribute("Type", item_type.getTypeName());

				switch (item_type) {
				case IMAGE:
					final Element image_e = doc.createElement(item_type
							.getTypeName());
					// image.setAttribute("ShowRegions", "yes");
					final PreloadedImage img = page.getImageItem().getImage();
					if (img != null) {
						image_e.setAttribute("uri", img.getUrl());

						for (final Drawing drawing : page.getImageItem()
								.getDrawings()) {
							switch (drawing.getType()) {
							case ELLIPSE:
								final Element ellipse_e = doc
										.createElement("Ellipse");
								final Ellipse ellipse = (Ellipse) drawing;
								ellipse_e.setAttribute("IsHotSpot", ""
										+ (ellipse.getKind() == Kind.BLOCKING));
								ellipse_e
										.setAttribute("X", "" + ellipse.getX());
								ellipse_e
										.setAttribute("Y", "" + ellipse.getY());
								ellipse_e.setAttribute("Width",
										"" + ellipse.getWidth());
								ellipse_e.setAttribute("Height",
										"" + ellipse.getHeight());
								image_e.appendChild(ellipse_e);
								break;
							case POLYGON:
								final Element polygon_e = doc
										.createElement("Polygon");
								final Polygon polygon = (Polygon) drawing;
								polygon_e.setAttribute("IsHotSpot", ""
										+ (polygon.getKind() == Kind.BLOCKING));
								for (final Point point : polygon.getPoints()) {
									final Element point_e = doc
											.createElement("Point");
									point_e.setAttribute("X", "" + point.x);
									point_e.setAttribute("Y", "" + point.y);
									polygon_e.appendChild(point_e);
								}
								image_e.appendChild(polygon_e);
								break;
							case RECTANGLE:
								final Element rectangle_e = doc
										.createElement("Ellipse");
								final Rectangle rectangle = (Rectangle) drawing;
								rectangle_e
										.setAttribute(
												"IsHotSpot",
												""
														+ (rectangle.getKind() == Kind.BLOCKING));
								rectangle_e.setAttribute("X",
										"" + rectangle.getX());
								rectangle_e.setAttribute("Y",
										"" + rectangle.getY());
								rectangle_e.setAttribute("Width", ""
										+ rectangle.getWidth());
								rectangle_e.setAttribute("Height", ""
										+ rectangle.getHeight());
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
					widget_e.appendChild(image_e);
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
						answer_e.setAttribute("IsCorrect",
								"" + answer.isCorrect());
						;
						answer_e.appendChild(doc.createTextNode(answer
								.getText()));
						quiz_e.appendChild(answer_e);
					}
					widget_e.appendChild(quiz_e);
					break;
				case RANGE_QUIZ:
					final Element range_quiz_e = doc.createElement(item_type
							.getTypeName());
					final RangeQuizItem range_quiz_item = page
							.getRangeQuizItem();
					final Element range_question_e = doc
							.createElement("Question");
					range_question_e.appendChild(doc
							.createTextNode(range_quiz_item.getText()));
					range_quiz_e.appendChild(range_question_e);
					final Element min_e = doc.createElement("min");
					min_e.appendChild(doc.createTextNode(""
							+ range_quiz_item.getMin()));
					range_quiz_e.appendChild(min_e);
					final Element max_e = doc.createElement("max");
					max_e.appendChild(doc.createTextNode(""
							+ range_quiz_item.getMax()));
					range_quiz_e.appendChild(max_e);
					widget_e.appendChild(range_quiz_e);
					break;
				case TEXT:
					final Element text_e = doc.createElement(item_type
							.getTypeName());
					text_e.appendChild(doc.createTextNode(page.getTextItem()
							.getText()));
					widget_e.appendChild(text_e);
					break;
				case VIDEO:
					final Element video_e = doc.createElement(item_type
							.getTypeName());
					video_e.setAttribute("uri", page.getVideoItem().getURL());
					widget_e.appendChild(video_e);
					break;
				}
				page_e.appendChild(widget_e);
			}

			root_e.appendChild(page_e);
		}

		doc.appendChild(root_e);

		return doc.toString();
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
}
