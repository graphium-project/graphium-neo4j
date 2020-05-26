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

import java.util.List;

import org.neo4j.graphdb.Node;

import com.vividsolutions.jts.geom.Point;

import at.srfg.graphium.model.IWaySegment;
import at.srfg.graphium.neo4j.persistence.INeo4jWayGraphReadDao;
import at.srfg.graphium.routing.algo.IPointToRoutingNodeResolver;
import at.srfg.graphium.routing.algo.ISegmentIdToRoutingNodeResolver;
import at.srfg.graphium.routing.algo.ISegmentToRoutingNodeResolver;

/**
 * @author mwimmer
 *
 */
public class Neo4jRoutingNodeResolverImpl<T extends IWaySegment>
	implements ISegmentToRoutingNodeResolver<T, Node>, IPointToRoutingNodeResolver<Node>, ISegmentIdToRoutingNodeResolver<Node> {
	
	protected INeo4jWayGraphReadDao graphReadDao;
	
	public Neo4jRoutingNodeResolverImpl(INeo4jWayGraphReadDao graphReadDao) {
		this.graphReadDao = graphReadDao;
	}
	
	@Override
	public Node resolveSegment(T segment, String graphName, String graphVersion) {
		Node startNode = graphReadDao.getSegmentNodeBySegmentId(graphName, graphVersion, segment.getId());
		return startNode;
	}
	
	@Override
	public Node resolveSegment(Point point, double searchDistance, String graphName, String graphVersion) {
		List<Node> nodes = graphReadDao.findNearestNodes(graphName, graphVersion, point, searchDistance, 1);
		return !nodes.isEmpty() ? nodes.get(0) : null; 
	}

	@Override
	public Node resolveSegment(Long segmentId, String graphName, String graphVersion) {
		Node startNode = graphReadDao.getSegmentNodeBySegmentId(graphName, graphVersion, segmentId);
		return startNode;
	}

}
