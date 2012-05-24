package org.orthoeman.client;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.orthoeman.shared.Lesson;
import org.orthoeman.shared.Lesson.Page;

import gwtupload.client.IUploadStatus.Status;
import gwtupload.client.IUploader;
import gwtupload.client.PreloadedImage;
import gwtupload.client.PreloadedImage.OnLoadPreloadedImageHandler;
import gwtupload.client.SingleUploader;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.canvas.dom.client.CssColor;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.touch.client.Point;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class AuthoringTool implements EntryPoint {
	private static final String itemTypeSeparator = " - ";

	private Lesson lesson = null;
	private Lesson.Page currentPage = null;

	private Map<Lesson.Page, Button> page_button_map = new HashMap<Lesson.Page, Button>();

	private TextBox title_tb;
	private TextArea text_area;
	private RootPanel quizContainer;
	private RootPanel canvasContainer;
	private RootPanel videoContainer;
	private ListBox combobox;

	/**
	 * This is the entry point method.
	 */
	@Override
	public void onModuleLoad() {
		final Label splashScreenLabel = getLabel("splashScreenLabel");
		final Label errorLabel = getLabel("errorLabel");

		combobox = getListBox("itemCombobox");
		combobox.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				final Lesson.Page page = getCurrentPage();
				final Lesson.Page.Item old_item1 = page.get(0);
				final Lesson.Page.Item old_item2 = page.get(1);

				page.clear();
				final String[] type_names_a = combobox.getItemText(
						combobox.getSelectedIndex()).split(itemTypeSeparator);
				final Lesson.Page.Item.Type item1_type = Lesson.Page.Item.Type
						.getTypeByName(type_names_a[0]);
				final Lesson.Page.Item.Type item2_type = Lesson.Page.Item.Type
						.getTypeByName(type_names_a[1]);

				// keep old elements
				if (old_item1.getType() == item1_type) {
					page.add(old_item1);
				}
				if (old_item2.getType() == item1_type) {
					page.add(old_item2);
				}
				if (page.isEmpty()) {
					page.addItem(item1_type);
				}
				if (old_item1.getType() == item2_type) {
					page.add(old_item1);
				}
				if (old_item2.getType() == item2_type) {
					page.add(old_item2);
				}
				if (page.size() == 1) {
					page.addItem(item2_type);
				}

				setCurrentPage(page);
			}
		});
		for (final Lesson.Page.Item.Type[] item_type_combination : Lesson.Page.validItemTypeCombinations) {
			combobox.addItem(getComboboxOptionText(item_type_combination[0],
					item_type_combination[1]));
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
		help_menu.addItem(new MenuItem("Report a bug...", command));
		help_menu.addItem(new MenuItem("About", command));

		final MenuBar menu_bar = new MenuBar();
		menu_bar.addItem("File", file_menu);
		menu_bar.addItem("Edit", edit_menu);
		menu_bar.addItem("Help", help_menu);
		RootPanel.get("htmlMenuBar").setVisible(false);
		RootPanel.get("menuBarContainer").add(menu_bar);

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

				setCurrentPage(new_page);

				lesson.remove(page);
				removePageButton(page);
			}
		});

		title_tb = getTextBox("titleTextBox");
		title_tb.addValueChangeHandler(new ValueChangeHandler<String>() {
			@Override
			public void onValueChange(ValueChangeEvent<String> event) {
				getCurrentPage().setTitle(event.getValue());
			}
		});

		text_area = getTextArea("textArea");
		text_area.addValueChangeHandler(new ValueChangeHandler<String>() {
			@Override
			public void onValueChange(ValueChangeEvent<String> event) {
				getCurrentPage().getTextItem().setText(event.getValue());
			}
		});

		quizContainer = getQuizContainer();
		videoContainer = getVideoContainer();

		final Canvas canvas = Canvas.createIfSupported();
		if (canvas == null) {
			RootPanel.get("errorLabelContainer").add(
					new Label("No canvas, get a proper browser!"));
			return;
		}
		final Canvas back_canvas = Canvas.createIfSupported();

		canvasContainer = getCanvasContainer();
		canvasContainer.add(canvas);

		final int width = 800;
		final int height = 600;
		canvas.setWidth(width + "px");
		canvas.setHeight(height + "px");
		canvas.setCoordinateSpaceWidth(width);
		canvas.setCoordinateSpaceHeight(height);

		back_canvas.setWidth(width + "px");
		back_canvas.setHeight(height + "px");
		back_canvas.setCoordinateSpaceWidth(width);
		back_canvas.setCoordinateSpaceHeight(height);

		final Context2d context = canvas.getContext2d();
		context.setFillStyle(CssColor.make("yellow"));
		context.fillRect(0, 0, width, height);

		back_canvas.getContext2d().drawImage(canvas.getCanvasElement(), 0, 0);

		class Point {
			double x;
			double y;
			boolean valid;
		}
		final Point start_point = new Point();
		final Point old_point = new Point();

		canvas.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				if (start_point.valid) {
					start_point.valid = false;
					old_point.valid = false;

					back_canvas.getContext2d().drawImage(
							canvas.getCanvasElement(), 0, 0);

					return;
				}
				final int x = event.getRelativeX(canvas.getElement());
				final int y = event.getRelativeY(canvas.getElement());
				errorLabel.setText("Point " + x + " " + y);
				start_point.x = x;
				start_point.y = y;
				start_point.valid = true;

			}
		});

		canvas.addMouseMoveHandler(new MouseMoveHandler() {

			@Override
			public void onMouseMove(MouseMoveEvent event) {
				if (!start_point.valid)
					return;
				final int x = event.getRelativeX(canvas.getElement());
				final int y = event.getRelativeY(canvas.getElement());

				if (old_point.valid) {
					context.drawImage(back_canvas.getCanvasElement(), 0, 0);
				}

				context.beginPath();
				context.moveTo(start_point.x, start_point.y);
				context.lineTo(x, y);
				context.closePath();
				context.stroke();

				old_point.x = x;
				old_point.y = y;
				old_point.valid = true;
			}
		});

		final OnLoadPreloadedImageHandler showImageHandler = new OnLoadPreloadedImageHandler() {
			@Override
			public void onLoad(PreloadedImage img) {
				context.drawImage((ImageElement) (Object) img.getElement(), 0,
						0);
				back_canvas.getContext2d().drawImage(canvas.getCanvasElement(),
						0, 0);
			}
		};

		// protected UploaderConstants i18nStrs;

		final IUploader.OnFinishUploaderHandler onFinishUploaderHandler = new IUploader.OnFinishUploaderHandler() {
			@Override
			public void onFinish(IUploader uploader) {
				if (uploader.getStatus() == Status.SUCCESS) {
					final PreloadedImage preloadedImage = new PreloadedImage(
							uploader.fileUrl(), showImageHandler);
					preloadedImage.setTitle("Ttile to set");
				}
			}
		};

		final SingleUploader upload = new SingleUploader();
		upload.addOnFinishUploadHandler(onFinishUploaderHandler);
		// RootPanel.get("uploadContainer").add(upload);

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

	private static TextArea getTextArea(String id) {
		return TextArea.wrap(DOM.getElementById(id));
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

	private static RootPanel getCanvasContainer() {
		return RootPanel.get("canvasContainer");
	}

	private static RootPanel getVideoContainer() {
		return RootPanel.get("videoContainer");
	}

	private static RootPanel getQuizContainer() {
		return RootPanel.get("quizContainer");
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

	private void setCurrentPage(Lesson.Page page) {
		this.currentPage = page;
		final RootPanel pageContainer = RootPanel.get("pageContainer");

		if (page == null) {
			pageContainer.setVisible(false);
			return;
		}

		text_area.setVisible(false);
		quizContainer.setVisible(false);
		canvasContainer.setVisible(false);
		videoContainer.setVisible(false);

		combobox.setSelectedIndex(getComboboxOptionIndex(page.get(0).getType(),
				page.get(1).getType()));

		title_tb.setText(page.getTitle());
		for (final Lesson.Page.Item item : page) {
			switch (item.getType()) {
			case TEXT:
				final Lesson.Page.TextItem text_item = (Lesson.Page.TextItem) item;
				text_area.setText(text_item.getText());
				text_area.setVisible(true);
				break;
			case QUIZ:
				quizContainer.setVisible(true);
				break;
			case IMAGE:
				canvasContainer.setVisible(true);
				break;
			case VIDEO:
				videoContainer.setVisible(true);
				break;
			}
		}
		pageContainer.setVisible(true);
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

	private static String getComboboxOptionText(Lesson.Page.Item.Type type1,
			Lesson.Page.Item.Type type2) {
		return type1.getName() + itemTypeSeparator + type2.getName();
	}

	private int getComboboxOptionIndex(Lesson.Page.Item.Type type1,
			Lesson.Page.Item.Type type2) {
		final String option = getComboboxOptionText(type1, type2);
		final int total = combobox.getItemCount();
		for (int i = 0; i < total; i++)
			if (option.equals(combobox.getItemText(i)))
				return i;
		return -1;
	}
}
