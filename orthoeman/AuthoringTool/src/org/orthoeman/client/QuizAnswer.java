package org.orthoeman.client;

import java.util.Map;

import org.orthoeman.shared.Lesson.Page.QuizItem;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.SimpleCheckBox;
import com.google.gwt.user.client.ui.TextBox;

public class QuizAnswer extends FlowPanel {
	public QuizAnswer(final QuizItem quiz_item, final int id) {
		final Map<Integer, QuizItem.Answer> answer_map = quiz_item
				.getAnswerMap();
		final QuizItem.Answer answer = answer_map.get(id);
		final String answer_text = answer.getText();
		final boolean correct = answer.isCorrect();

		final Button remove_button = new Button("Remove");
		final SimpleCheckBox correct_cb = new SimpleCheckBox();
		final TextBox answer_textbox = new TextBox();

		addStyleName("row");
		addStyleName("border");

		final FlowPanel left_cell = new FlowPanel();
		left_cell.addStyleName("cell");
		left_cell.addStyleName("nowrap");

		final FlowPanel right_cell = new FlowPanel();
		right_cell.addStyleName("cell");
		right_cell.addStyleName("fill_x");

		answer_textbox.addStyleName("noborder");
		answer_textbox.addStyleName("fill_x");

		remove_button.setTitle("Click to remove the current row");
		remove_button.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				answer_map.remove(id);
				removeFromParent();
			}
		});

		correct_cb.setTitle("Marks the answer as correct");
		correct_cb.setValue(correct);
		correct_cb.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				answer.setCorrect(correct_cb.getValue());
			}
		});

		answer_textbox.setTitle("Type in a possbile answer for the quiz");
		answer_textbox.setText(answer_text);
		answer_textbox.addValueChangeHandler(new ValueChangeHandler<String>() {
			@Override
			public void onValueChange(ValueChangeEvent<String> event) {
				answer.setText(event.getValue());
			}
		});

		left_cell.add(remove_button);
		left_cell.add(correct_cb);
		right_cell.add(answer_textbox);

		add(left_cell);
		add(right_cell);
	}

	public QuizAnswer(QuizItem quiz_item) {
		this(quiz_item, quiz_item.createAnswer());
	}
}
