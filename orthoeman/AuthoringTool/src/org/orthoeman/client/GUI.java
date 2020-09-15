package org.orthoeman.client;

import elemental2.dom.DomGlobal;
import elemental2.dom.Element;
import elemental2.dom.Event;
import elemental2.dom.EventListener;
import elemental2.dom.ViewCSS;
import jsinterop.base.BvJs;
import jsinterop.base.Js;

public class GUI {
	public static class Web {
		public static class Html {
			public static class Tag {
				public static final String textarea = "textarea";
				public static final String div = "div";
				public static final String iframe = "iframe";
				public static final String a = "a";
				public static final String h1 = "h1";
				public static final String table = "table";
				public static final String link = "link";
				public static final String script = "script";
				public static final String svg = "svg";
				public static final String tbody = "tbody";
				public static final String thead = "thead";
				public static final String tfoot = "tfoot";
				public static final String tr = "tr";
				public static final String td = "td";
				public static final String th = "th";
			}

			public static class Attribute {
				public static final String id = "id";
				public static final String style = "style";
				public static final String type = "type";
				public static final String value = "value";
			}

			public static class Type {
				public static final String button = "button";
			}
		}

		public static class Svg {
			public static final String filterValue = "filter";

			public static class Tag {
				public static final String text = "text";
				public static final String g = "g";
				public static final String rect = "rect";
				public static final String circle = "circle";
				public static final String line = "line";
				public static final String defs = "defs";
				public static final String filter = filterValue;
				public static final String title = "title";
				public static final String feGaussianBlur = "feGaussianBlur";
			}

			public static class Attribute {
				public static final String style = Html.Attribute.style;
				public static final String r = "r";
				public static final String filter = filterValue;
				public static final String x = "x";
				public static final String y = "y";
				public static final String x1 = "x1";
				public static final String y1 = "y1";
				public static final String x2 = "x2";
				public static final String y2 = "y2";
				public static final String rx = "rx";
				public static final String ry = "ry";
				public static final String cx = "cx";
				public static final String cy = "cy";
				public static final String transform = "transform";
				public static final String stdDeviation = "stdDeviation";
			}

			public static class Css {
				public static class Attribute {
					public static final String fill = "fill";
					public static final String stroke = "stroke";
					public static final String strokeWidth = "stroke-width";
					public static final String fillOpacity = "fill-opacity";
					public static final String xLinkHref = "xlink:href";
				}
			}

			public static class Transform {
				public static final String translate = "translate";
				public static final String scale = "scale";
			}
		}

		public static class Css {
			public static final String noneValue = "none";
			public static final String nullValue = null;

			public static class Attribute {
				public static final String display = "display";
				public static final String visibility = "visibility";
				public static final String position = "position";
				public static final String width = "width";
				public static final String height = "height";
				public static final String left = "left";
				public static final String right = "right";
				public static final String top = "top";
				public static final String bottom = "bottom";
				public static final String margin = "margin";
				public static final String padding = "padding";
				public static final String paddingLeft = "padding-left";
				public static final String paddingRight = "padding-right";
				public static final String paddingTop = "padding-top";
				public static final String paddingBottom = "padding-bottom";
				public static final String borderRadius = "border-radius";
				public static final String boxShadow = "box-shadow";
				public static final String backgroundColor = "background-color";
				public static final String background = "background";
				public static final String color = "color";
				public static final String fontSize = "font-size";
				public static final String overflow = "overflow";
				public static final String overflowX = "overflowX";
				public static final String overflowY = "overflowY";
				public static final String pointerEvents = "pointer-event";
				public static final String userSelect = "user-select";
				public static final String[] userSelectAliases = { "-webkit-touch-callout", "-webkit-user-select",
						"-khtml-user-select", "-moz-user-select", "-ms-user-select", userSelect };
				public static final String cursor = "cursor";
			}

			public static class Size {
				public static final String full = "100%;";
			}

			public static class Display {
				public static final String none = noneValue;
				public static final String block = "block";
				public static final String inline = "inline";
				public static final String inlineBlock = "inline-block";
				public static final String reset = nullValue;
			}

			public static class Visibility {
				public static final String hidden = "hidden";
				public static final String reset = nullValue;
			}

			public static class Position {
				public static final String absolute = "absolute";
				public static final String relative = "relative";
			}

			public static class Overflow {
				public static final String visible = "visible";
				public static final String hidden = "hidden";
				public static final String scroll = "scroll";
				public static final String auto = "auto";
			}

			public static class BorderStyle {
				public static final String none = noneValue;
			}

			public static class PointerEvents {
				public static final String none = noneValue;
			}

			public static class UserSelect {
				public static final String none = noneValue;
			}

			public static class Cursor {
				public static final String pointer = "pointer";
			}

			public static class FontSize {
				public static final String xSmall = "x-small";
				public static final String xxSmall = "xx-small";
			}

		}

		public static class Js {
			public static class Event {
				public static final String mouseEvents = "MouseEvents";

				public static final int BUTTON_LEFT = 0;
				public static final int BUTTON_MIDDLE = 1;
				public static final int BUTTON_RIGHT = 2;

				public static class Type {
					public static final String click = "click";
					public static final String visibilityChange = "visibilitychange";
					public static final String copy = "copy";
					public static final String cut = "cut";
					public static final String paste = "paste";
					public static final String select = "select";
					public static final String message = "message";
					public static final String fullscreenchange = "fullscreenchange";
					public static final String MSFullscreenChange = "MSFullscreenChange";
					public static final String transitionend = "transitionend";
					public static final String mousedown = "mousedown";
					public static final String mousemove = "mousemove";
					public static final String mouseup = "mouseup";
					public static final String mouseout = "mouseout";
					public static final String dragstart = "dragstart";
					public static final String selectstart = "selectstart";
					public static final String dblclick = "dblclick";
					public static final String wheel = "wheel";
					public static final String touchstart = "touchstart";
					public static final String touchmove = "touchmove";
					public static final String touchend = "touchend";
					public static final String touchcancel = "touchcancel";
					public static final String keypress = "keypress";
					public static final String keydown = "keydown";

					public static boolean isTouchEvent(String type) {
						return type.equals(touchmove) || type.equals(touchstart) || type.equals(touchend)
								|| type.equals(touchcancel);
					}
				}

				public static enum Key {
					ESCAPE(27, "Escape", "Esc");

					private int code;
					private String[] names;

					private Key(int code, String... names) {
						this.code = code;
						this.names = names;
					}

					public boolean match(Integer keycode) {
						if (keycode != null && keycode == this.code)
							return true;
						return false;
					}

					public boolean match(String keyname) {
						for (final String name : names) {
							if (name.equals(keyname))
								return true;
						}
						return false;
					}

					public boolean match(Integer keycode, String keyname) {
						return match(keycode) || match(keyname);
					}
				}
			}

			public static class Window {
				public static final String pageYOffset = "pageYOffset";
				public static final String scrollY = "scrollY";
			}

			public static class DataTransferFormatType {
				public static final String TextPlain = "text/plain";
				public static final String IETextPlain = "text";
			}

			public static class Document {
				public static class Command {
					public static final String clearAuthenticationCache = "ClearAuthenticationCache";
					public static final String copy = "Copy";
				}
			}
		}
	}

	private static boolean fullscreenListenerAdded;

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

	public static Element getFullScreenElement() {
		if (Js.isTruthy(DomGlobal.document.fullscreenElement))
			return DomGlobal.document.fullscreenElement;
		else if (Js.isTruthy(DomGlobal.document.msFullscreenElement))
			return DomGlobal.document.msFullscreenElement;
		return null;
	}

	public static boolean isFullScreen() {
		return Js.isTruthy(getFullScreenElement());
	}

	public static void toggleFullScreen(Element element) {
		if (!isFullScreen()) {
			if (BvJs.exists(element, "requestFullscreen"))
				element.requestFullscreen();
			else if (BvJs.exists(element, "msRequestFullscreen"))
				element.msRequestFullscreen();
		} else {
			if (BvJs.exists(DomGlobal.document, "exitFullscreen"))
				DomGlobal.document.exitFullscreen();
			else if (BvJs.exists(DomGlobal.document, "msExitFullscreen"))
				DomGlobal.document.msExitFullscreen();
		}
	}

	public static void addFullScreenEventListener(LastScheduledCommand lsc) {
		if (fullscreenListenerAdded)
			return;
		final EventListener li = new EventListener() {
			@Override
			public void handleEvent(Event evt) {
				lsc.schedule();
			}
		};
		DomGlobal.document.addEventListener(Web.Js.Event.Type.fullscreenchange, li);
		DomGlobal.document.addEventListener(Web.Js.Event.Type.MSFullscreenChange, li);
		fullscreenListenerAdded = true;
	}

	public static void log(String text, Object o) {
		DomGlobal.window.console.log(text, o);
	}
}
