package org.orthoeman.shared;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public class Lesson extends ArrayList<Lesson.Page> {

	public static class Page extends ArrayList<Page.Item> {
		public static final Page.Item.Type[][] validItemTypeCombinations = {
				{ Item.Type.IMAGE, Item.Type.TEXT },
				{ Item.Type.IMAGE, Item.Type.QUIZ },
				{ Item.Type.VIDEO, Item.Type.TEXT },
				{ Item.Type.VIDEO, Item.Type.QUIZ },
				{ Item.Type.TEXT, Item.Type.QUIZ } };

		public interface TitleChangedListener {
			public void titleChanged(String title);
		}

		public static class Item {
			public static enum Type {
				TEXT("Text"), QUIZ("Quiz"), IMAGE("Image"), VIDEO("Video");

				private final String name;

				private Type(String name) {
					this.name = name;
				}

				public String getName() {
					return name;
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

			public TextItem() {
				super(Type.TEXT);
			}

			public String getText() {
				return text;
			}

			public void setText(String text) {
				this.text = text;
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
			public ImageItem() {
				super(Type.IMAGE);
			}
		}

		public static class VideoItem extends ResourceItem {
			public VideoItem() {
				super(Type.VIDEO);
			}
		}

		public static class QuizItem extends Item {
			private Map<String, Boolean> questionMap;

			public QuizItem() {
				super(Type.QUIZ);
			}

			public Map<String, Boolean> getQuestionMap() {
				return questionMap;
			}

			public void setQuestionMap(Map<String, Boolean> questionMap) {
				this.questionMap = questionMap;
			}
		}

		private String title;

		private Collection<TitleChangedListener> titleChangedListeners = new ArrayList<TitleChangedListener>();

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
	}

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

	public static Lesson readXML(String url) {
		final Lesson lesson = new Lesson();

		lesson.add(new Page());
		lesson.get(0).setTitle("opa");
		lesson.add(new Page());
		lesson.get(1).setTitle("ouf");

		return lesson;
	}

	public static void writeXML(Lesson lesson) {

	}
}
