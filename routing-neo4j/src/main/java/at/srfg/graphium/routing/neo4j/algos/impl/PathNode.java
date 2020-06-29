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
package at.srfg.graphium.routing.neo4j.algos.impl;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

/**
 *
 * @author mwimmer
 */
public class PathNode implements Comparable<PathNode> {

	private Long id;
	private Long segmentId; // used for debugging
	private Boolean startToEnd;
	private Node neo4jNode;
	private Relationship lastRelationship;
	private double cost;
	private PathNode parentNode;
	
	public PathNode(Long id, Boolean startToEnd, Node neo4jNode, Relationship lastRelationship, double cost, PathNode parentNode) {
		super();
		this.id = id;
		this.startToEnd = startToEnd;
		this.neo4jNode = neo4jNode;
		this.lastRelationship = lastRelationship;
		this.cost = cost;
		this.parentNode = parentNode;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Boolean isStartToEnd() {
		return startToEnd;
	}

	public void setStartToEnd(Boolean startToEnd) {
		this.startToEnd = startToEnd;
	}

	public double getCost() {
		return cost;
	}

	public void setCost(double cost) {
		this.cost = cost;
	}

	public PathNode getParentNode() {
		return parentNode;
	}

	public void setParentNode(PathNode parentNode) {
		this.parentNode = parentNode;
	}

	public Node getNeo4jNode() {
		return neo4jNode;
	}

	public void setNeo4jNode(Node neo4jNode) {
		this.neo4jNode = neo4jNode;
	}

	public Relationship getLastRelationship() {
		return lastRelationship;
	}

	public void setLastRelationship(Relationship lastRelationship) {
		this.lastRelationship = lastRelationship;
	}

	public Long getSegmentId() {
		return segmentId;
	}

	public void setSegmentId(Long segmentId) {
		this.segmentId = segmentId;
	}

	@Override
	public int compareTo(PathNode another) {
		double diff = getCost() - another.getCost();
		if (diff < 0) {
			return -1;
		} else if (diff > 0) {
			return 1;
		} else {
			return 0;
		}
	}
	
}