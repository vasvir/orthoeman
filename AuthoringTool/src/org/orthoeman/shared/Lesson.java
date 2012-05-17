package org.orthoeman.shared;

import java.util.ArrayList;
import java.util.Collection;

public class Lesson {
	/*
	 * Image Text Image Quiz Video Text Video Quiz Text Quiz
	 */

	public static class Page extends ArrayList<Page.Item> {

		public static class Item {
			public static enum Type {
				TEXT, QUIZ, IMAGE, VIDEO;
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
	    
	    public static class TextItem extends Item{
			public TextItem() {
				super(Type.TEXT);
			}
	    	
	    }

	}

	private String title;
	private String author;
	private Collection<Page> pages;

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

	public Collection<Page> getPages() {
		return pages;
	}

	public void setPages(Collection<Page> pages) {
		this.pages = pages;
	}
}
