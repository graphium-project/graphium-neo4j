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

import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.linearref.LengthIndexedLine;

import at.srfg.graphium.neo4j.persistence.impl.Neo4jWaySegmentHelperImpl;
import at.srfg.graphium.routing.neo4j.evaluators.IPathAwareCostEvalutator;

/**
 *
 * @author mwimmer
 */
public abstract class AbstractCostEvaluator implements IPathAwareCostEvalutator<Double> {

	protected static Logger log = LoggerFactory.getLogger(AbstractCostEvaluator.class);

	protected long startNodeId;
	protected long endNodeId;
	protected Coordinate startCoord;
	protected Coordinate endCoord;
	protected boolean coordinateBased;

	public AbstractCostEvaluator(long startNodeId, long endNodeId,
			Coordinate startCoord, Coordinate endCoord) {
		this.startNodeId = startNodeId;
		this.endNodeId = endNodeId;
		this.startCoord = startCoord;
		this.endCoord = endCoord;
		
		if (startCoord != null && endCoord != null) {
			coordinateBased = true;
		} else {
			coordinateBased = false;
		}
	}

	/**
	 * @return Cost is traveltime of startNode (startSegment) in seconds
	 */
	@Override
	public Double getCost(Relationship relationship, Direction direction, WeightedPath path) {
		Double weight = null;
		if (path != null) {
			weight = path.weight();
		}
		return getCost(relationship, direction, weight);
	}
		
	/**
	 * @return Cost is traveltime of startNode (startSegment) in seconds
	 */
	@Override
	public Double getCost(Relationship relationship, Direction direction, Double weightSoFar) {
		Node startNode = relationship.getStartNode();
		Node endNode = relationship.getEndNode();
		double cost;
		long startNodeId = relationship.getStartNode().getId();
		long endNodeId = relationship.getEndNode().getId();
		double offset;
		double addSegCost;		
		
		if (coordinateBased && this.startNodeId == startNodeId) {
			offset = calculateOffset(relationship.getStartNode(), relationship.getEndNode(), startCoord, true);
			cost = getCost(startNode, relationship, weightSoFar);
			
			double endOffset = 1.0d;
			if (this.endNodeId == endNodeId) {
				// Route besitzt nur 2 Segmente => hier müssen wir den Offset auch auf dem Endsegment berücksichtigen
				endOffset = calculateOffset(relationship.getStartNode(), relationship.getEndNode(), endCoord, false);
			}
			addSegCost = getCost(endNode, relationship, weightSoFar) * endOffset;
			log.debug("startNodeId found");
		}
		
		else if (coordinateBased && this.endNodeId == endNodeId) {
			offset = calculateOffset(relationship.getStartNode(), relationship.getEndNode(), endCoord, false);
			cost = getCost(endNode, relationship, weightSoFar);
			addSegCost = 0;
			log.debug("endNodeId found");
		}
		
		else {
			offset = 1.0d;
			addSegCost = 0.0;
			cost = getCost(startNode, relationship, weightSoFar);
		}

		return cost * offset + addSegCost;
	}

	protected abstract double getCost(Node node, Relationship relationship, Double weightSoFar) ;

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

		// geohelper!
		LengthIndexedLine lengthIndexedGeom = new LengthIndexedLine(seg);
		double segEndLength =  lengthIndexedGeom.getEndIndex();	
		double startCoordLength = lengthIndexedGeom.project(coord);
		double percentageInLine = 100d / segEndLength * startCoordLength;
		
		if((start && !getDirection(startGeom, endGeom)) || (!start && !getDirectionEndSeg(startGeom, endGeom))) {
			percentageInLine = 100 - percentageInLine;
		}
	
		return percentageInLine / 100;
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
	
	@Override
	public Double getCost(Relationship relationship, Direction direction) {
		return getCost(relationship, direction, (Double) null);
	}

}
