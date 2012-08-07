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

import org.orthoeman.shared.Lesson.Page.QuizItem;
import org.orthoeman.shared.Lesson.Page.RangeQuizItem;

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
				static {
					for (final Type type : values()) {
						name2TypeMap.put(type.getName(), type);
					}
				}

				private final String name;

				private Type(String name) {
					this.name = name;
				}

				public String getName() {
					return name;
				}

				public static Type getTypeByName(String name) {
					return name2TypeMap.get(name);
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
			public static class HotSpotList extends ArrayList<Drawing> {
				private final List<List<Point>> intersection_point_lists = new ArrayList<List<Point>>();

				@Override
				public boolean add(Drawing e) {
					if (e.getType() == Drawing.Type.LINE) {
						final Line line1 = (Line) e;

						final List<Point> new_intersection_point_list = new ArrayList<Point>();
						for (Drawing drawing : this) {
							if (drawing.getType() != Drawing.Type.LINE)
								continue;
							final Line line2 = (Line) drawing;
							new_intersection_point_list.add(Line
									.getIntersectionPoint(line1, line2));
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

						// remove columns after the row
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
			}

			private PreloadedImage image;
			private Zoom zoom = new Zoom();
			private HotSpotList hotSpots = new HotSpotList();

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

			public HotSpotList getHotSpots() {
				return hotSpots;
			}
		}

		public static class VideoItem extends ResourceItem {
			private String videoURL;

			public VideoItem() {
				super(Type.VIDEO);
			}

			public String getVideoURL() {
				return videoURL;
			}

			public void setVideoURL(String videoURL) {
				this.videoURL = videoURL;
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

	private String title;
	private String author;

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public void addPageListener(PageListener li) {
		pageListeners.add(li);
	}

	public void removePageListener(PageListener li) {
		pageListeners.remove(li);
	}

	public static Lesson readXML(String url) {
		// TODO read from url
		final Lesson lesson = new Lesson();

		lesson.add(new Page("opa"));
		lesson.get(0).getTextItem().setText("This is a very interesting opa");
		final QuizItem quiz_item = lesson.get(0).getQuizItem();
		quiz_item.setText("This statement is correct.");
		quiz_item.createAnswer("I don't think so", false);
		quiz_item.createAnswer("really?", false);
		quiz_item.createAnswer("Not sure", false);
		quiz_item.createAnswer("Yes", true);
		quiz_item.createAnswer("I don't know. I don't want to answer", false);
		final RangeQuizItem range_quiz_item = lesson.get(0).getRangeQuizItem();
		range_quiz_item.setText("What is the correct range?");
		range_quiz_item.setMin(-1);
		range_quiz_item.setMax(4.8);

		lesson.add(new Page("ouf"));
		lesson.get(1).getTextItem().setText("ouf is a nice concept");

		return lesson;
	}

	public static void writeXML(Lesson lesson) {
		// TODO writeXML
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
