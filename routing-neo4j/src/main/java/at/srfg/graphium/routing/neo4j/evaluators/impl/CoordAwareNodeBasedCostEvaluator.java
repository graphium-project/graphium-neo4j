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

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.io.ParseException;

import at.srfg.graphium.geomutils.GeometryUtils;
import at.srfg.graphium.neo4j.persistence.impl.Neo4jWaySegmentHelperImpl;

public class CoordAwareNodeBasedCostEvaluator extends NodeBasedCostEvaluator {
	
	private static Logger log = LoggerFactory.getLogger(CoordAwareNodeBasedCostEvaluator.class);
	
	protected long startNodeId;
	protected long endNodeId;
	protected Coordinate startCoord;
	protected Coordinate endCoord;
	
	public CoordAwareNodeBasedCostEvaluator(String costProperty) {
		super(costProperty);
	}
			
	public CoordAwareNodeBasedCostEvaluator(String costProperty,
			long startNodeId, long endNodeId,
			Coordinate startCoord, Coordinate endCoord) {
		this(costProperty);
		this.startNodeId = startNodeId;
		this.endNodeId = endNodeId;
		this.startCoord = startCoord;
		this.endCoord = endCoord;
	}
	
	@Override
	protected double getCostValue(Relationship relationship, String propertyName) {
		long startNodeId = relationship.getStartNode().getId();
		long endNodeId = relationship.getEndNode().getId();
		double offset;
		double addSegCost;		
//		Object costObject;
		double cost;
		
		log.debug("startNodeId = " + startNodeId + " / endNodeId = " + endNodeId);
		
		if (this.startNodeId == startNodeId) {
			offset = calculateOffset(relationship.getStartNode(), relationship.getEndNode(), startCoord, true);
			cost = getCostValue(relationship.getStartNode(), relationship, propertyName);
			addSegCost = getCostValue(relationship.getEndNode(), relationship, propertyName);
			
			log.debug("startNodeId found");
		}
		else if (this.endNodeId == endNodeId) {
			offset = calculateOffset(relationship.getStartNode(), relationship.getEndNode(), endCoord, false);
			cost = getCostValue(relationship.getEndNode(), relationship, propertyName);
			addSegCost = 0;

			log.debug("endNodeId found");
		}
		else {
			offset = 1.0d;
			addSegCost = 0.0;
			cost = getCostValue(relationship.getEndNode(), relationship, propertyName);
		}
				 
		double segmentCost = cost * offset;
		double totalCost = segmentCost + addSegCost; 
		
		log.debug("cost = " + totalCost);
		
		return totalCost;
	}
	
	protected double calculateOffset(Node startNode, Node endNode, Coordinate coord, boolean start) {	
		LineString startGeom;
		LineString endGeom;
		try {
			startGeom = (LineString) Neo4jWaySegmentHelperImpl.encodeLineString(startNode);
			endGeom = (LineString) Neo4jWaySegmentHelperImpl.encodeLineString(endNode);
		} catch (ParseException e) {
			log.error("Could not parse geometry", e);
			return 0;
		}
		
		LineString seg;	
		if(start) {
			seg = startGeom;
		}
		else {
			seg = endGeom;
		}

		double offsetOnLine = GeometryUtils.offsetOnLineString(coord, seg);
		if((start && !getDirection(startGeom, endGeom)) || (!start && !getDirectionEndSeg(startGeom, endGeom))) {
			offsetOnLine = 1 - offsetOnLine;
		}
		return offsetOnLine;
	}
	
	protected boolean getDirection(LineString firstGeom,
			LineString secondGeom) {	
		// guard --> start / endpoint ident
		if(firstGeom.getStartPoint().equalsExact(secondGeom.getStartPoint())
				||firstGeom.getStartPoint().equalsExact(secondGeom.getEndPoint())) {
			return true;
		}
		else {
			return false;
		}		
	}
	
	protected boolean getDirectionEndSeg(LineString firstGeom,
			LineString secondGeom) {	
		if(firstGeom.getStartPoint().equalsExact(secondGeom.getStartPoint())
				||firstGeom.getEndPoint().equalsExact(secondGeom.getStartPoint())) {
			return true;
		}
		else {
			return false;
		}		
	}
}
