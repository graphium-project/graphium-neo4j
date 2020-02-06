/**
 * Graphium Neo4j - Map Matching module of Graphium
 * Copyright © 2017 Salzburg Research Forschungsgesellschaft (graphium@salzburgresearch.at)
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
package at.srfg.graphium.mapmatching.model;

/**
 * The direction with which a matched segment was traversed.
 */
public enum Direction {
	
	/**
	 * The way was entered at the start node and left at the end node.
	 */
	START_TO_END(true, false, false, true),

	/**
	 * The way was entered at the end node and left at the start node.
	 */
	END_TO_START(false, true, true, false),
	
	/**
	 * The way was entered at the start node and left again at the start node (u-turn).
	 */
	START_TO_START(true, true, false, false),
	
	/**
	 * The way was entered at the end node and left again at the end node (u-turn).
	 */
	END_TO_END(false, false, true, true),
	
	/**
	 * The way was entered without a node and left at the start node (lane change)
	 */
	CENTER_TO_START(false, true, false, false),
	
	/**
	 * The way was entered without a node and left at the end node (lane change)
	 */
	CENTER_TO_END(false, false, false, true),
	
	/**
	 * The way was entered at the start node and left without a node (lane change)
	 */
	START_TO_CENTER(true, false, false, false),
	
	/**
	 * The way was entered at the end node and left without a node (lane change)
	 */
	END_TO_CENTER(false, false, true, false),
	
	/**
	 * The way was entered and left without a node (lane change).
	 */
	CENTER_TO_CENTER(false, false, false, false);

	private boolean enteringThroughStartNode;
	private boolean leavingThroughStartNode;
	private boolean enteringThroughEndNode;
	private boolean leavingThroughEndNode;
	
	private Direction(boolean enteringThroughStartNode,
			boolean leavingThroughStartNode,
			boolean enteringThroughEndNode,
			boolean leavingThroughEndNode) {
		this.enteringThroughStartNode = enteringThroughStartNode;
		this.leavingThroughStartNode = leavingThroughStartNode;
		this.enteringThroughEndNode = enteringThroughEndNode;
		this.leavingThroughEndNode = leavingThroughEndNode;
	}
	
	public boolean isEnteringThroughStartNode() {
		return enteringThroughStartNode;
	}
	
	public boolean isEnteringThroughEndNode() {
		return enteringThroughEndNode;
	}
	
	public boolean isLeavingThroughStartNode() {
		return leavingThroughStartNode;
	}
	
	public boolean isLeavingThroughEndNode() {
		return leavingThroughEndNode;
	}
}
