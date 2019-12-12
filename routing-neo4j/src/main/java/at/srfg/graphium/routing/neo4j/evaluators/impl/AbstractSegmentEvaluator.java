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
import at.srfg.graphium.neo4j.model.cache.SegmentCacheEntry;
import at.srfg.graphium.neo4j.service.impl.STRTreeCacheManager;

/**
 * @author mwimmer
 */
public abstract class AbstractSegmentEvaluator {
	
	protected String graphName;
	protected String version;
	protected STRTreeCacheManager cache;

	public AbstractSegmentEvaluator(String graphName, String version, STRTreeCacheManager cache) {
		super();
		this.graphName = graphName;
		this.version = version;
		this.cache = cache;
	}

	/**
	 * @param relationship
	 * @return
	 */
	protected Object getCurrentDurationCosts(Node node, Relationship relationship) {
		Integer duration = null;
		if (((long)relationship.getProperty(WayGraphConstants.CONNECTION_NODE_ID)) == ((long)node.getProperty(WayGraphConstants.SEGMENT_STARTNODE_ID))) {
			if (relationship.getEndNode().getId() == node.getId()) {
				// node is end node of relationship
				if (node.hasProperty(WayGraphConstants.SEGMENT_CURRENT_DURATION_TOW)) {
					duration = (Integer) node.getProperty(WayGraphConstants.SEGMENT_CURRENT_DURATION_TOW);
				} else {
					duration = (Integer) getNodeProperty(node, WayGraphConstants.SEGMENT_MIN_DURATION_TOW);
				}
			} else {
				// node is start node of relationship
				if (node.hasProperty(WayGraphConstants.SEGMENT_CURRENT_DURATION_BKW)) {
					duration = (Integer) node.getProperty(WayGraphConstants.SEGMENT_CURRENT_DURATION_BKW);
				} else {
					duration = (Integer) getNodeProperty(node, WayGraphConstants.SEGMENT_MIN_DURATION_BKW);
				}
			}
		} else {
			if (relationship.getEndNode().getId() == node.getId()) {
				// node is end node of relationship
				if (node.hasProperty(WayGraphConstants.SEGMENT_CURRENT_DURATION_BKW)) {
					duration = (Integer) node.getProperty(WayGraphConstants.SEGMENT_CURRENT_DURATION_BKW);
				} else {
					duration = (Integer) getNodeProperty(node, WayGraphConstants.SEGMENT_MIN_DURATION_BKW);
				}
			} else {
				// node is start node of relationship
				if (node.hasProperty(WayGraphConstants.SEGMENT_CURRENT_DURATION_TOW)) {
					duration = (Integer) node.getProperty(WayGraphConstants.SEGMENT_CURRENT_DURATION_TOW);
				} else {
					duration = (Integer) getNodeProperty(node, WayGraphConstants.SEGMENT_MIN_DURATION_TOW);
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
		if (((long)relationship.getProperty(WayGraphConstants.CONNECTION_NODE_ID)) == ((long)node.getProperty(WayGraphConstants.SEGMENT_STARTNODE_ID))) {
			if (relationship.getEndNode().getId() == node.getId()) {
				// node is end node of relationship
				duration = (Integer) getNodeProperty(node, WayGraphConstants.SEGMENT_MIN_DURATION_TOW);
			} else {
				// node is start node of relationship
				duration = (Integer) getNodeProperty(node, WayGraphConstants.SEGMENT_MIN_DURATION_BKW);
			}
		} else {
			if (relationship.getEndNode().getId() == node.getId()) {
				// node is end node of relationship
				duration = (Integer) getNodeProperty(node, WayGraphConstants.SEGMENT_MIN_DURATION_BKW);
			} else {
				// node is start node of relationship
				duration = (Integer) getNodeProperty(node, WayGraphConstants.SEGMENT_MIN_DURATION_TOW);
			}
		}
		return duration;
	}
	
	protected Object getNodeProperty(Node node, String propertyName) {
		Object value = null;
		if (cache != null) {
			long nodeId = node.getId();
			SegmentCacheEntry cacheEntry = cache.getCacheEntryPerNodeId(graphName, version, nodeId);
			if (propertyName.equals(WayGraphConstants.SEGMENT_MIN_DURATION_BKW)) {
				value = cacheEntry.getDuration(false);
			} else if (propertyName.equals(WayGraphConstants.SEGMENT_MIN_DURATION_TOW)) {
				value = cacheEntry.getDuration(true);
			} else if (propertyName.equals(WayGraphConstants.SEGMENT_LENGTH)) {
				value = cacheEntry.getLength();
			}
		}
		if (value == null) {
			value = node.getProperty(propertyName);
		}
		return value;
	}

}