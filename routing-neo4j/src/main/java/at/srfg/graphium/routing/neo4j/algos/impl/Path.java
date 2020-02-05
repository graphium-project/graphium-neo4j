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

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author mwimmer
 */
public class Path {

	private List<PathNode> nodes;
	private double totalCost;
	
	public List<PathNode> getNodes() {
		return nodes;
	}
	public void setNodes(List<PathNode> nodes) {
		this.nodes = nodes;
	}
	
	public void addNode(PathNode node) {
		if (nodes == null) {
			nodes = new ArrayList<PathNode>();
		}
		nodes.add(node);
	}

	public void addNodeAtStart(PathNode node) {
		if (nodes == null) {
			nodes = new ArrayList<PathNode>();
		}
		nodes.add(0, node);
	}
	
	public double getTotalCost() {
		return totalCost;
	}
	
	public void setTotalCost(double totalCost) {
		this.totalCost = totalCost;
	}
	
	public void addCost(double cost) {
		this.totalCost += cost;
	}
	
	public Path extendPath(PathNode node) {
		Path newPath = new Path();
		newPath.setTotalCost(totalCost + node.getCost());
		if (this.nodes != null) {
			List<PathNode> clonedNodes = new ArrayList<>(this.nodes.size());
			for(PathNode n : this.nodes) {
				clonedNodes.add(n);
			}
//			this.nodes.forEach(n -> clonedNodes.add(n));
			newPath.setNodes(clonedNodes);
		}
		newPath.addNode(node);
		return newPath;
	}
	
}