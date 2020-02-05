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

import java.util.Set;
import java.util.function.Predicate;

import org.neo4j.graphalgo.CostEvaluator;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.srfg.graphium.model.Access;
import at.srfg.graphium.model.IWaySegment;
import at.srfg.graphium.neo4j.model.WayGraphConstants;
import at.srfg.graphium.neo4j.persistence.impl.Neo4jWaySegmentHelperImpl;
import at.srfg.graphium.neo4j.service.impl.STRTreeCacheManager;
import at.srfg.graphium.neo4j.traversal.DirectedIncomingConnectionPathExpander;
import at.srfg.graphium.neo4j.traversal.DirectedOutgoingConnectionPathExpander;
import at.srfg.graphium.neo4j.traversal.IDirectedConnectionPathExpander;
import at.srfg.graphium.routing.algo.IRoutingAlgo;
import at.srfg.graphium.routing.algo.IRoutingAlgoFactory;
import at.srfg.graphium.routing.exception.UnkownRoutingAlgoException;
import at.srfg.graphium.routing.model.IRoutingOptions;
import at.srfg.graphium.routing.model.impl.RoutingAlgorithms;
import at.srfg.graphium.routing.model.impl.RoutingMode;
import at.srfg.graphium.routing.neo4j.evaluators.impl.NodeBasedCostEvaluator;
import at.srfg.graphium.routing.neo4j.evaluators.impl.OffsetAwareNodeBasedCostEvaluator;
/**
 * @author mwimmer
 *
 */
public class Neo4jRoutingAlgoFactoryImpl<T extends IWaySegment> implements IRoutingAlgoFactory<IRoutingOptions, Node, Double> {

	private static Logger log = LoggerFactory.getLogger(Neo4jRoutingAlgoFactoryImpl.class);
	
	private STRTreeCacheManager cache;

	@Override
	public IRoutingAlgo<IRoutingOptions, Node, Double> createInstance(IRoutingOptions routeOptions, Node startNode, 
			Float percentageStartWeight, Node endNode, Float percentageEndWeight)
			throws UnkownRoutingAlgoException {
		
		// create expander und costEvaluator based on routeOptions
		PathExpander<Object> expander = getOutgoingExpander(routeOptions, Direction.OUTGOING); 
		CostEvaluator<Double> costEvaluator = createCostEvaluator(routeOptions, startNode, percentageStartWeight, endNode, percentageEndWeight);
		
		switch ((RoutingAlgorithms)routeOptions.getAlgorithm()) {
		case DIJKSTRA:
			return new Dijkstra<T>(expander, costEvaluator, routeOptions);
		case BIDIRECTIONAL_DIJKSTRA:
			// TODO: return instance of custom astar
			PathExpander<Object> expanderIncomings = getIncomingExpander(routeOptions, Direction.INCOMING);
			return new BidirectionalDijkstra<T>(expander, expanderIncomings, costEvaluator, routeOptions);
		default:
			throw new UnkownRoutingAlgoException((RoutingAlgorithms) routeOptions.getAlgorithm());
		}
			
	}

	protected PathExpander<Object> getOutgoingExpander(IRoutingOptions options, Direction direction) {
		DirectedOutgoingConnectionPathExpander expander = new DirectedOutgoingConnectionPathExpander(null);
		
		addOptions(expander, options);

		return expander;
	}

	protected PathExpander<Object> getIncomingExpander(IRoutingOptions options, Direction direction) {
		DirectedIncomingConnectionPathExpander expander = new DirectedIncomingConnectionPathExpander(null);
		
		addOptions(expander, options);

		return expander;
	}
	
	private void addOptions(IDirectedConnectionPathExpander expander, IRoutingOptions options) {
		// access type restriction on routing mode
		if (options.getMode() != null) {
			Access access = null;
			if (options.getMode().equals(RoutingMode.CAR)) {
				access = Access.PRIVATE_CAR;
			}
			if (access != null) {
				final Access accessToFilter = access;
//				expander.addNodeFilter(getNodePredicate(accessToFilter));
				expander.addRelationshipFilter(getRelationshipPredicate(accessToFilter));
			}
		}
		
		if (options.getTagValueFilters() != null && !options.getTagValueFilters().isEmpty()) {
			for (String filterKey : options.getTagValueFilters().keySet()) {
				Set<Object> filterValues = options.getTagValueFilters().get(filterKey);
				if (filterValues != null && !filterValues.isEmpty()) {
					expander.addNodeFilter(n -> ((PropertyContainer) n).hasProperty(filterKey) 
							&& filterValues.contains(((PropertyContainer) n).getProperty(filterKey)));
				}
			}
		} 
	}

	protected CostEvaluator<Double> createCostEvaluator(IRoutingOptions options, Node startNode, 
			Float percentageStartWeight, Node endNode, Float percentageEndWeight) {
		if (percentageStartWeight == null || percentageEndWeight == null) {
			return new NodeBasedCostEvaluator(options.getGraphName(), options.getGraphVersion(), cache, options.getCriteria().getValue());
		} else {
			return new OffsetAwareNodeBasedCostEvaluator(options.getCriteria().getValue(), 
														 startNode.getId(),		// id of Neo4j node!
														 endNode.getId(), 		// id of Neo4j node!
														 percentageStartWeight, 
														 percentageEndWeight,
														 options.getGraphName(),
														 options.getGraphVersion(),
														 cache);
//			return new OffsetAwareNodeBasedCostEvaluator(options.getCriteria().getValue(), 
//														 startNode.getId(),		// id of Neo4j node!
//														 endNode.getId(), 		// id of Neo4j node!
//														 percentageStartWeight, 
//														 percentageEndWeight,
//														 segmentLengthCache);
		}
	}

	protected Predicate<? super Node> getNodePredicate(Access mode) {
		return n -> (n.hasProperty(WayGraphConstants.SEGMENT_ACCESS_BKW) &&
				Neo4jWaySegmentHelperImpl.parseAccessTypes(
				(byte[]) n.getProperty(WayGraphConstants.SEGMENT_ACCESS_BKW)
				).contains(mode)) ||
				(n.hasProperty(WayGraphConstants.SEGMENT_ACCESS_TOW) &&
				Neo4jWaySegmentHelperImpl.parseAccessTypes(
				(byte[]) n.getProperty(WayGraphConstants.SEGMENT_ACCESS_TOW)
				).contains(mode));
	}

//	protected Predicate<? super Relationship> getRelationshipPredicate(Access mode) {
//		return r -> r.hasProperty(WayGraphConstants.CONNECTION_ACCESS) &&
//				Neo4jWaySegmentHelperImpl.parseAccessTypes(
//				(byte[]) r.getProperty(WayGraphConstants.CONNECTION_ACCESS)
//				).contains(mode);
//	}

	protected Predicate<? super Relationship> getRelationshipPredicate(Access mode) {
		// see: https://www.javahabit.com/2016/06/16/understanding-java-8-lambda-final-finally-variable/
		Object[] access = new Object[1];
		return r -> (access[0] = r.getProperty(WayGraphConstants.CONNECTION_ACCESS)) != null && 
				Neo4jWaySegmentHelperImpl.parseAccessTypes((byte[]) access[0]).contains(mode);
	}
	
	public STRTreeCacheManager getCache() {
		return cache;
	}

	public void setCache(STRTreeCacheManager cache) {
		this.cache = cache;
	}

}