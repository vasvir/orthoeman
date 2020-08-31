package com.google.gwt.core.linker;

import com.google.gwt.core.ext.LinkerContext;

public class BvCrossSiteIFrameLinker extends CrossSiteIframeLinker {
	@Override
	protected String getJsDevModeRedirectHookPermitted(LinkerContext context) {
		return "$wnd.location.protocol == \"http:\" || $wnd.location.protocol == \"file:\" "
				+ "|| $wnd.location.protocol == \"https:\"";
	}
}
