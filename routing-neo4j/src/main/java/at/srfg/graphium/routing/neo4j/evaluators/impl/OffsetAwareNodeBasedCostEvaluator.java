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
package at.srfg.graphium.routing.neo4j.evaluators.impl;

import org.neo4j.graphdb.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.srfg.graphium.neo4j.model.WayGraphConstants;
import at.srfg.graphium.neo4j.model.WaySegmentRelationshipType;
import at.srfg.graphium.neo4j.service.impl.STRTreeCacheManager;
/**
 *
 * @author mwimmer
 */
public class OffsetAwareNodeBasedCostEvaluator extends NodeBasedCostEvaluator  {

	private static Logger log = LoggerFactory.getLogger(CoordAwareNodeBasedCostEvaluator.class);
	
	protected long startNodeId;
	protected long endNodeId;
	protected float startNodeOffset;
	protected float endNodeOffset;

	public OffsetAwareNodeBasedCostEvaluator(String costProperty) {
		super(null, null, null, costProperty);
	}

	public OffsetAwareNodeBasedCostEvaluator(String costProperty, long startNodeId, long endNodeId,
			float startNodeOffset, float endNodeOffset,  String graphName, String version, STRTreeCacheManager cache) {
		super(graphName, version, cache, costProperty);
		this.startNodeId = startNodeId;
		this.endNodeId = endNodeId;
		this.startNodeOffset = startNodeOffset;
		this.endNodeOffset = endNodeOffset;
	}

	@Override
	protected double getCostValue(Relationship relationship, String propertyName) {
		long startNodeId = relationship.getStartNode().getId();
		long endNodeId = relationship.getEndNode().getId();
		double offset;
		double addSegCost;		
		double cost;
		
		if (this.startNodeId == startNodeId) {
			// given offset is based on geometry direction
			if (relationship.isType(WaySegmentRelationshipType.SEGMENT_CONNECTION_ON_ENDNODE)) {
				offset = startNodeOffset;
			} else {
				offset = 1 - startNodeOffset;
			}
			
			cost = getCostValue(relationship.getStartNode(), relationship, propertyName);
			addSegCost = getCostValue(relationship.getEndNode(), relationship, propertyName);
			
		}
		else if (this.endNodeId == endNodeId) {
			// given offset is based on geometry direction
			long connectionNodeId = (long) relationship.getProperty(WayGraphConstants.CONNECTION_NODE_ID);
			if (connectionNodeId == (long) relationship.getEndNode().getProperty(WayGraphConstants.SEGMENT_STARTNODE_ID)) {
				offset = endNodeOffset;
			} else {
				offset = 1 - endNodeOffset;
			}
			cost = getCostValue(relationship.getEndNode(), relationship, propertyName);
			addSegCost = 0;
		}
		else {
			offset = 1.0d;
			addSegCost = 0.0;
			cost = getCostValue(relationship.getEndNode(), relationship, propertyName);
		}
				 
		double segmentCost = cost * offset;
		double totalCost = segmentCost + addSegCost; 
		
		return totalCost;
	}
	
}
