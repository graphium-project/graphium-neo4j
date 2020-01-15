/**
 * Graphium Neo4j - Module of Graphium for routing services via Neo4j
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
/**
 * (C) 2016 Salzburg Research Forschungsgesellschaft m.b.H.
 *
 * All rights reserved.
 *
 */
package at.srfg.graphium.routing.neo4j.evaluators.impl;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import at.srfg.graphium.neo4j.model.WayGraphConstants;
import at.srfg.graphium.neo4j.model.WaySegmentRelationshipType;

/**
 * @author mwimmer
 */
public abstract class AbstractSegmentEvaluator {
	
	private float laneChangeCostFactor = 0.1f;
	private int forbiddenLaneChangeCostValue = Integer.MAX_VALUE;
	
	/**
	 * @param relationship
	 * @return
	 */
	protected Object getCurrentDurationCosts(Node node, Relationship relationship) {
		Integer duration = null;
		if (((long)relationship.getProperty(WayGraphConstants.CONNECTION_NODE_ID)) ==
				((long)node.getProperty(WayGraphConstants.SEGMENT_STARTNODE_ID))) {
			if (relationship.getEndNode().getId() == node.getId()) {
				// node is end node of relationship
				if (node.hasProperty(WayGraphConstants.SEGMENT_CURRENT_DURATION_TOW)) {
					duration = (Integer) node.getProperty(WayGraphConstants.SEGMENT_CURRENT_DURATION_TOW);
				} else {
					duration = (Integer) node.getProperty(WayGraphConstants.SEGMENT_MIN_DURATION_TOW);
				}
			} else if (relationship.getStartNode().getId() == node.getId()) {
				// node is start node of relationship
				if (node.hasProperty(WayGraphConstants.SEGMENT_CURRENT_DURATION_BKW)) {
					duration = (Integer) node.getProperty(WayGraphConstants.SEGMENT_CURRENT_DURATION_BKW);
				} else {
					duration = (Integer) node.getProperty(WayGraphConstants.SEGMENT_MIN_DURATION_BKW);
				}
			}
		} else if (((long) relationship.getProperty(WayGraphConstants.CONNECTION_NODE_ID)) == 
				((long) node.getProperty(WayGraphConstants.SEGMENT_ENDNODE_ID))) {
			if (relationship.getEndNode().getId() == node.getId()) {
				// node is end node of relationship
				if (node.hasProperty(WayGraphConstants.SEGMENT_CURRENT_DURATION_BKW)) {
					duration = (Integer) node.getProperty(WayGraphConstants.SEGMENT_CURRENT_DURATION_BKW);
				} else {
					duration = (Integer) node.getProperty(WayGraphConstants.SEGMENT_MIN_DURATION_BKW);
				}
			} else if (relationship.getStartNode().getId() == node.getId()) {
				// node is start node of relationship
				if (node.hasProperty(WayGraphConstants.SEGMENT_CURRENT_DURATION_TOW)) {
					duration = (Integer) node.getProperty(WayGraphConstants.SEGMENT_CURRENT_DURATION_TOW);
				} else {
					duration = (Integer) node.getProperty(WayGraphConstants.SEGMENT_MIN_DURATION_TOW);
				}
			}
		} else {
			if (relationship.isType(WaySegmentRelationshipType.SEGMENT_CONNECTION_WITHOUT_NODE)) {
				if (relationship.getProperty(WayGraphConstants.CONNECTION_TAG_PREFIX.concat(WayGraphConstants.CONNECTION_TYPE)) == null
						|| !relationship.getProperty(WayGraphConstants.CONNECTION_TAG_PREFIX.concat(WayGraphConstants.CONNECTION_TYPE))
							.equals(WayGraphConstants.CONNECTION_TYPE_CONNECTS_FORBIDDEN)) {
					// low duration for lane changes
					if (node.hasProperty(WayGraphConstants.SEGMENT_CURRENT_DURATION_TOW)) {
						duration = (int)((double)((Integer) node.getProperty(WayGraphConstants.SEGMENT_CURRENT_DURATION_TOW)) * laneChangeCostFactor);
					} else {
						duration = (int)((double)((Integer) node.getProperty(WayGraphConstants.SEGMENT_MIN_DURATION_TOW)) * laneChangeCostFactor);
					}
				} else if (relationship.getProperty(WayGraphConstants.CONNECTION_TAG_PREFIX.concat(WayGraphConstants.CONNECTION_TYPE)) == null
						|| relationship.getProperty(WayGraphConstants.CONNECTION_TAG_PREFIX.concat(WayGraphConstants.CONNECTION_TYPE))
							.equals(WayGraphConstants.CONNECTION_TYPE_CONNECTS_FORBIDDEN)) {
					// high duration for forbidden lane changes
					duration = forbiddenLaneChangeCostValue;
				}
			}
		}
		return duration;
	}

	/**
	 * @param relationship
	 * @return
	 */
	protected Object getMinDurationCosts(Node node, Relationship relationship) {
		Integer duration = null;
		if (((long)relationship.getProperty(WayGraphConstants.CONNECTION_NODE_ID)) ==
				((long)node.getProperty(WayGraphConstants.SEGMENT_STARTNODE_ID))) {
			if (relationship.getEndNode().getId() == node.getId()) {
				// node is end node of relationship
				duration = (Integer) node.getProperty(WayGraphConstants.SEGMENT_MIN_DURATION_TOW);
			} else {
				// node is start node of relationship
				duration = (Integer) node.getProperty(WayGraphConstants.SEGMENT_MIN_DURATION_BKW);
			}
		} else if (((long)relationship.getProperty(WayGraphConstants.CONNECTION_NODE_ID)) ==
				((long)node.getProperty(WayGraphConstants.SEGMENT_ENDNODE_ID))) {
			if (relationship.getEndNode().getId() == node.getId()) {
				// node is end node of relationship
				duration = (Integer) node.getProperty(WayGraphConstants.SEGMENT_MIN_DURATION_BKW);
			} else {
				// node is start node of relationship
				duration = (Integer) node.getProperty(WayGraphConstants.SEGMENT_MIN_DURATION_TOW);
			}
		} else {
			if (relationship.isType(WaySegmentRelationshipType.SEGMENT_CONNECTION_WITHOUT_NODE)) {
				if (relationship.getProperty(WayGraphConstants.CONNECTION_TAG_PREFIX.concat(WayGraphConstants.CONNECTION_TYPE)) == null
						|| !relationship.getProperty(WayGraphConstants.CONNECTION_TAG_PREFIX.concat(WayGraphConstants.CONNECTION_TYPE))
							.equals(WayGraphConstants.CONNECTION_TYPE_CONNECTS_FORBIDDEN)) {
					// low duration for lane changes
					duration = (int)((double)((Integer) node.getProperty(WayGraphConstants.SEGMENT_MIN_DURATION_TOW)) * laneChangeCostFactor);
				} else if (relationship.getProperty(WayGraphConstants.CONNECTION_TAG_PREFIX.concat(WayGraphConstants.CONNECTION_TYPE)) == null
						|| relationship.getProperty(WayGraphConstants.CONNECTION_TAG_PREFIX.concat(WayGraphConstants.CONNECTION_TYPE))
								.equals(WayGraphConstants.CONNECTION_TYPE_CONNECTS_FORBIDDEN)) {
					// high duration for forbidden lane changes
					duration = forbiddenLaneChangeCostValue;
				}
			}
		}
		return duration;
	}

	public float getLaneChangeCostFactor() {
		return laneChangeCostFactor;
	}
	public void setLaneChangeCostFactor(float laneChangeCostFactor) {
		this.laneChangeCostFactor = laneChangeCostFactor;
	}

	public int getForbiddenLaneChangeCostValue() {
		return forbiddenLaneChangeCostValue;
	}
	public void setForbiddenLaneChangeCostValue(int forbiddenLaneChangeCostValue) {
		this.forbiddenLaneChangeCostValue = forbiddenLaneChangeCostValue;
	}

}