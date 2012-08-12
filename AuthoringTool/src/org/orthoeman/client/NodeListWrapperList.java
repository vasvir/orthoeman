package org.orthoeman.client;

import java.util.AbstractList;

import com.google.gwt.xml.client.Node;
import com.google.gwt.xml.client.NodeList;

public class NodeListWrapperList extends AbstractList<Node> {
	private final NodeList nl;

	public NodeListWrapperList(NodeList nl) {
		this.nl = nl;
	}

	@Override
	public Node get(int index) {
		return nl.item(index);
	}

	@Override
	public int size() {
		return nl.getLength();
	}
}
