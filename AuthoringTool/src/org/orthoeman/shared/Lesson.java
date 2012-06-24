package org.orthoeman.shared;

import gwtupload.client.PreloadedImage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.orthoeman.shared.Lesson.Page.QuizItem;

public class Lesson extends ArrayList<Lesson.Page> {
	public interface PageListener {
		public void pageAdded(Page page);

		public void pageRemoved(Page page);
	}

	public static class Page {
		public static final Page.Item.Type[][] validItemTypeCombinations = {
				{ Item.Type.TEXT, Item.Type.IMAGE },
				{ Item.Type.IMAGE, Item.Type.QUIZ },
				{ Item.Type.TEXT, Item.Type.VIDEO },
				{ Item.Type.VIDEO, Item.Type.QUIZ },
				{ Item.Type.TEXT, Item.Type.QUIZ } };

		public interface TitleChangedListener {
			public void titleChanged(String title);
		}

		public static class Item {
			public static enum Type {
				TEXT("Text"), QUIZ("Quiz"), IMAGE("Image"), VIDEO("Video");

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
			public static class Zoom {
				public static enum Type {
					ZOOM_121, ZOOM_LEVEL, ZOOM_TO_FIT_WIDTH, ZOOM_TARGET
				}

				private Type type = Type.ZOOM_TO_FIT_WIDTH;
				private double level = 1;
				private Rectangle target = new Rectangle();

				public Type getType() {
					return type;
				}

				public void setType(Type type) {
					this.type = type;
				}

				public double getLevel() {
					return level;
				}

				public void setLevel(double level) {
					this.level = level;
				}

				public void increaseLevel() {
					level *= 1.2;
				}

				public void decreaseLevel() {
					level /= 1.2;
				}

				public Rectangle getTarget() {
					return target;
				}
			}

			private PreloadedImage image;
			private Zoom zoom = new Zoom();

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

		private String title;
		private Page.Item.Type[] itemTypeCombination;
		private ImageItem imageItem;
		private TextItem textItem;
		private QuizItem quizItem;
		private VideoItem videoItem;
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

		public void addHotSpot(Drawing drawing) {
			// TODO Auto-generated method stub
		}

		public void removeHotSpot(Drawing drawing) {
			// TODO Auto-generated method stub
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
