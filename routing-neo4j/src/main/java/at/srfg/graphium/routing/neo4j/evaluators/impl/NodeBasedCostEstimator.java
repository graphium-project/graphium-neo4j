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

import org.neo4j.graphalgo.EstimateEvaluator;
import org.neo4j.graphdb.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;

import at.srfg.graphium.geomutils.GeometryUtils;
import at.srfg.graphium.neo4j.model.WayGraphConstants;
import at.srfg.graphium.routing.model.impl.RoutingCriteria;

public class NodeBasedCostEstimator implements EstimateEvaluator<Double> {

	private static Logger log = LoggerFactory.getLogger(NodeBasedCostEstimator.class);
	
	private final static double LOWERCOSTTRESH = 0.01;
	private RoutingCriteria routingCriteria;
	private float estimateFactor = 0.8f;
	
	public NodeBasedCostEstimator() {
		this.routingCriteria = RoutingCriteria.LENGTH;
	}
	
	public NodeBasedCostEstimator(RoutingCriteria routingCriteria) {
		this.routingCriteria = routingCriteria;
	}
	
	public NodeBasedCostEstimator(RoutingCriteria routingCriteria, float estimateFactor) {
		this(routingCriteria);
		this.estimateFactor = estimateFactor;
	}
	
    public Double getCost(final Node node, final Node goal) {
        double distance = Double.MAX_VALUE;
       	if (node != null) {
 			
			Coordinate nodeSCoord = new Coordinate((double)node.getProperty(WayGraphConstants.SEGMENT_START_X),
												   (double)node.getProperty(WayGraphConstants.SEGMENT_START_Y));
			Coordinate nodeECoord = new Coordinate((double)node.getProperty(WayGraphConstants.SEGMENT_END_X),
					   							   (double)node.getProperty(WayGraphConstants.SEGMENT_END_Y));
			
			Coordinate goalSCoord = new Coordinate((double)goal.getProperty(WayGraphConstants.SEGMENT_START_X),
					   							   (double)goal.getProperty(WayGraphConstants.SEGMENT_START_Y));
			Coordinate goalECoord = new Coordinate((double)goal.getProperty(WayGraphConstants.SEGMENT_END_X),
					   							   (double)goal.getProperty(WayGraphConstants.SEGMENT_END_Y));
			
			distance = getMinDistance(distance, nodeSCoord, goalSCoord);
			distance = getMinDistance(distance, nodeSCoord, goalECoord);
			distance = getMinDistance(distance, nodeECoord, goalSCoord);
			distance = getMinDistance(distance, nodeECoord, goalECoord);
			
			distance += (float)node.getProperty(WayGraphConstants.SEGMENT_LENGTH);
			
       	} else {
       		distance = 0;
       	}
       	
       	distance = distance * estimateFactor; // unterschätzen!

       	if (!routingCriteria.equals(RoutingCriteria.LENGTH)) {       	
	   		distance = distance / (130 / 3.6);
	   	}

       	if (distance == 0) {
       		distance = LOWERCOSTTRESH;
       	}
       	
       	if (log.isDebugEnabled()) {
       		log.debug("Segment " + node.getProperty(WayGraphConstants.SEGMENT_ID) + " => " + goal.getProperty(WayGraphConstants.SEGMENT_ID) 
       					+ ": distance = " + distance);
       	}
       	
       	return distance;
    }

	private double getMinDistance(double distance, Coordinate nodeCoord, Coordinate goalCoord) {
		return Math.min(distance, GeometryUtils.distanceAndoyer(nodeCoord, goalCoord));
	}
    
}
