package org.orthoeman.client;

import java.util.AbstractList;

import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;

public class DOMNodeListWrapperList extends AbstractList<Node> {
	private final NodeList<Node> nl;

	public DOMNodeListWrapperList(NodeList<Node> nl) {
		this.nl = nl;
	}

	@Override
	public Node get(int index) {
		return nl.getItem(index);
	}

	@Override
	public int size() {
		return nl.getLength();
	}
}
