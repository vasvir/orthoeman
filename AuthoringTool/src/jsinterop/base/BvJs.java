package jsinterop.base;

import elemental2.core.Function;
import jsinterop.annotations.JsMethod;

public class BvJs {
	private final static String bvJsInteropUtil = "BvJsInteropUtil";

	@JsMethod(name = "bind", namespace = bvJsInteropUtil)
	private static native Function _bind(Object callback);

	public static final Function bind(Object callback) {
		return _bind(callback);
	}

	@JsMethod(name = "invoke", namespace = bvJsInteropUtil)
	private static native <V> V _invoke(Object callable, Object... args);

	public static final <V> V invoke(Object callable, Object... args) {
		return _invoke(callable, args);
	}

	@JsMethod(name = "wrapOneArgumentFunction", namespace = bvJsInteropUtil)
	private static native <V> V _wrapOneArgumentFunction(Object callable);

	public static final <V> V wrapOneArgumentFunction(Object callable) {
		return _wrapOneArgumentFunction(callable);
	}

	public static <T> String[] cast2StringArray(T data) {
		return Js.uncheckedCast(data);
	}

	public static <T> double[] cast2DoubleArray(T data) {
		return Js.uncheckedCast(data);
	}

	public static <T> String[][] cast2StringArray2D(T data) {
		return Js.uncheckedCast(data);
	}

	/**
	 * This emulates the common javascript idiom
	 * 
	 * <pre>
	 * var x = a || b;
	 * </pre>
	 * 
	 * @param <T>
	 * @param pm
	 * @param prop1
	 * @param prop2
	 * @return
	 */
	public static <T> T getPropertyAlt(JsPropertyMap<T> pm, String prop1, String prop2) {
		final T t1 = pm.get(prop1);
		if (t1 != null)
			return t1;
		final T t2 = pm.get(prop2);
		return t2;
	}

	// used for functions existence check
	// checks for non zero and non empty strings
	public static boolean exists(Object o, String propertyName) {
		return Js.isTruthy(Js.asPropertyMap(o).get(propertyName));
	}

	public static native boolean defined(Object o, String propertyName) /*-{
		return typeof o[propertyName] !== 'undefined';
	}-*/;
}
