package org.orthoeman.shared;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.orthoeman.shared.Lesson.Page.Item.Type;

public class Lesson extends ArrayList<Lesson.Page> {
	public interface PageListener {
		public void pageAdded(Page page);

		public void pageRemoved(Page page);
	}

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

		public Page(String title) {
			setTitle(title);
			addItem(validItemTypeCombinations[0][0]);
			addItem(validItemTypeCombinations[0][1]);
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

		public TextItem getTextItem() {
			for (final Item item : this) {
				if (item.getType() == Item.Type.TEXT)
					return (TextItem) item;
			}
			return null;
		}

		public void addItem(Type type) {
			switch (type) {
			case TEXT:
				add(new TextItem());
				break;
			case QUIZ:
				add(new QuizItem());
				break;
			case IMAGE:
				add(new ImageItem());
				break;
			case VIDEO:
				add(new VideoItem());
				break;
			default:
				break;
			}
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
		final Lesson lesson = new Lesson();

		lesson.add(new Page("opa"));
		lesson.get(0).add(new Page.TextItem("This is a very interesting opa"));
		lesson.get(0).add(new Page.ImageItem());

		lesson.add(new Page("ouf"));
		lesson.get(1).add(new Page.TextItem("oud is a nice concept"));
		lesson.get(1).add(new Page.ImageItem());

		return lesson;
	}

	public static void writeXML(Lesson lesson) {

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
