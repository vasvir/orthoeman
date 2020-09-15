package org.orthoeman.client;

import elemental2.dom.DomGlobal;
import elemental2.dom.Element;
import elemental2.dom.ViewCSS;
import jsinterop.base.Js;

public class GUI {
	/**
	 * @param el   - dom element
	 * @param prop - get the properties style, like width, fontFamily, fontSize,
	 *             fontSizeAdjust. property must be snake case: font-family
	 * @return
	 */
	public static String getStyleProperty(com.google.gwt.dom.client.Element el, String prop) {
		final Element elem = Js.cast(el);
		return Js.<ViewCSS>uncheckedCast(DomGlobal.window).getComputedStyle(elem, null).getPropertyValue(prop);
	}
}
