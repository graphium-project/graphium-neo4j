/**
 * Graphium Neo4j - Module of Graphium for routing services via Neo4j
 * Copyright © 2020 Salzburg Research Forschungsgesellschaft (graphium@salzburgresearch.at)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package at.srfg.graphium.routing.neo4j.model.impl;

import java.util.Iterator;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;

/**
 *
 * @author mwimmer
 */
public class SimplePathImpl implements Path {

	private Node startNode;
	private Node endNode;
	private Relationship lastRelationship;
	
	public SimplePathImpl(Node startNode, Node endNode, Relationship lastRelationship) {
		super();
		this.startNode = startNode;
		this.endNode = endNode;
		this.lastRelationship = lastRelationship;
	}

	@Override
	public Node startNode() {
		return startNode;
	}

	@Override
	public Node endNode() {
		return endNode;
	}

	@Override
	public Relationship lastRelationship() {
		return lastRelationship;
	}

	@Override
	public Iterable<Relationship> relationships() {
		return null;
	}

	@Override
	public Iterable<Relationship> reverseRelationships() {
		return null;
	}

	@Override
	public Iterable<Node> nodes() {
		return null;
	}

	@Override
	public Iterable<Node> reverseNodes() {
		return null;
	}

	@Override
	public int length() {
		return 0;
	}

	@Override
	public Iterator<PropertyContainer> iterator() {
		return null;
	}

}
