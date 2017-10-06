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
package at.srfg.graphium.routing.neo4j.evaluators.impl;

import org.apache.commons.lang3.math.NumberUtils;
import org.neo4j.graphalgo.CostEvaluator;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.srfg.graphium.neo4j.model.WayGraphConstants;
import at.srfg.graphium.routing.model.impl.RoutingCriteria;

public class NodeBasedCostEvaluator extends AbstractSegmentEvaluator implements CostEvaluator<Double>{

	private static Logger log = LoggerFactory.getLogger(NodeBasedCostEvaluator.class);
	
	private final static double LOWERCOSTTRESH = 0.0000001;
	protected String costProperty;
	
	public NodeBasedCostEvaluator(String costProperty) {
		super();
		this.costProperty = costProperty;
	}
	
	@Override
	public Double getCost(Relationship relationship, Direction direction) {		
		if (log.isDebugEnabled()) {
			long targetSegmentId = (long) relationship.getEndNode().getProperty(WayGraphConstants.SEGMENT_ID);
			log.debug("Relationship: " + relationship.getStartNode().getProperty(WayGraphConstants.SEGMENT_ID) + 
					  " => " + targetSegmentId);
		}
		
		return getCostValue(relationship, costProperty);
	}

	protected double getCostValue(Relationship relationship, String propertyName) {
		return getCostValue(relationship.getStartNode(), relationship, propertyName);
	}
	
	protected double getCostValue(Node node, Relationship relationship, String propertyName) {
		Object costObject = null;
		if (propertyName.equals(RoutingCriteria.MIN_DURATION.getValue())) {
			costObject = getMinDurationCosts(node, relationship);
		} else if (propertyName.equals(RoutingCriteria.CURRENT_DURATION.getValue())) {
			costObject = getCurrentDurationCosts(node, relationship);
			if (costObject == null) {
				// costObject could be null in case of inconsistency
				costObject = getMinDurationCosts(node, relationship);
			}
		} else {
			costObject = node.getProperty(propertyName);
		}
		
		Number cost;
		if (costObject instanceof Number) {
			cost = (Number)costObject;
		} else {
			cost = NumberUtils.toDouble(costObject.toString(), 0.0);
		}
		if (cost.doubleValue() < LOWERCOSTTRESH) {
			return LOWERCOSTTRESH;
		}
		
		return cost.doubleValue();
	}

}
