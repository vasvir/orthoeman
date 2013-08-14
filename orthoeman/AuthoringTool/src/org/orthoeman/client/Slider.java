package org.orthoeman.client;

import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.event.dom.client.ScrollEvent;
import com.google.gwt.event.dom.client.ScrollHandler;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

public class Slider extends ScrollPanel implements
		HasValueChangeHandlers<Double> {
	private final int total_ticks;
	private final double min;
	private final double max;
	private final double initial;
	private double value;

	public Slider(int total_ticks, double min, double max, double initial) {
		super(new SimplePanel());
		this.total_ticks = total_ticks;
		this.min = min;
		this.max = max;
		this.initial = initial;

		setAlwaysShowHorizontalScrollbar(true);
		setWidth(128);

		addScrollHandler(new ScrollHandler() {
			@Override
			public void onScroll(ScrollEvent event) {
				/*
				 * Log.trace("scroll event " + event + " value " +
				 * getHorizontalScrollPosition() + " min " +
				 * getMinimumHorizontalScrollPosition() + " max " +
				 * getMaximumHorizontalScrollPosition());
				 */
				value = Slider.this.min + (Slider.this.max - Slider.this.min)
						* getHorizontalScrollPosition()
						/ Slider.this.total_ticks;
				ValueChangeEvent.fire(Slider.this, value);
			}
		});

		addAttachHandler(new AttachEvent.Handler() {
			@Override
			public void onAttachOrDetach(AttachEvent event) {
				// Log.trace("Attached " + event.isAttached());
				if (event.isAttached())
					setValue(value);
			}
		});
		reset();
	}

	public double getValue() {
		return value;
	}

	public void setValue(double value) {
		this.value = value;
		final int position = (int) (((value - min) * total_ticks) / (max - min));
		setHorizontalScrollPosition(position);
		// Log.trace("Setting position to " + position + " read back "
		// + getHorizontalScrollPosition());
	}

	public void reset() {
		setValue(initial);
	}

	public void setAlwaysShowHorizontalScrollbar(boolean alwaysShow) {
		getScrollableElement().getStyle().setOverflowX(
				alwaysShow ? Overflow.SCROLL : Overflow.AUTO);
	}

	public void setWidth(int width) {
		final Widget sp = this.getWidget();
		sp.setWidth((width + total_ticks) + "px");
		sp.setHeight("1px");
		super.setWidth(width + "px");
		setValue(value);
	}

	@Override
	public HandlerRegistration addValueChangeHandler(
			ValueChangeHandler<Double> handler) {
		return addHandler(handler, ValueChangeEvent.getType());
	}
}
