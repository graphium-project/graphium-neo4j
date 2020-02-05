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
package at.srfg.graphium.routing.service.neo4j.impl;

import java.util.List;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.io.ParseException;

import at.srfg.graphium.model.IWaySegment;
import at.srfg.graphium.neo4j.persistence.configuration.IGraphDatabaseProvider;
import at.srfg.graphium.neo4j.persistence.impl.Neo4jWaySegmentHelperImpl;
import at.srfg.graphium.routing.exception.RoutingException;
import at.srfg.graphium.routing.exception.UnkownRoutingAlgoException;
import at.srfg.graphium.routing.model.IRoute;
import at.srfg.graphium.routing.model.IRoutingOptions;
import at.srfg.graphium.routing.service.IRoutingService;
import at.srfg.graphium.routing.service.impl.GenericRoutingServiceImpl;

/**
 * @author mwimmer
 *
 */
public class Neo4jRoutingServiceImpl extends GenericRoutingServiceImpl<IWaySegment, Node, Double, IRoutingOptions>
	implements IRoutingService<IWaySegment, Double, IRoutingOptions> {
	
	protected IGraphDatabaseProvider graphDatabaseProvider;
	
	@Override
	public IRoute<IWaySegment, Double> route(IRoutingOptions options)
			throws UnkownRoutingAlgoException, RoutingException {
		IRoute<IWaySegment, Double> route = null;
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx()) {
			route = super.route(options);
			tx.success();
		}
		return route;
	}

	@Override
	public IRoute<IWaySegment, Double> routePerSegments(IRoutingOptions options, List<IWaySegment> segments) throws UnkownRoutingAlgoException {
		IRoute<IWaySegment, Double> route = null;
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx()) {
			route = super.routePerSegments(options, segments);
			tx.success();
		}
		return route;
	}

	@Override
	public IRoute<IWaySegment, Double> routePerSegmentIds(IRoutingOptions options, List<Long> segmentIds) throws UnkownRoutingAlgoException {
		IRoute<IWaySegment, Double> route = null;
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx()) {
			route = super.routePerSegmentIds(options, segmentIds);
			tx.success();
		}
		return route;
	}

	@Override
	protected LineString getNodeGeometry(Node node) {
		try {
			return Neo4jWaySegmentHelperImpl.encodeLineString(node);
		} catch (ParseException e) {
			return null;
		}
	}
	
	@Override
	protected Double sumWeights(Double weight1, Double weight2) {
		return weight1 + weight2;
	}

	public IGraphDatabaseProvider getGraphDatabaseProvider() {
		return graphDatabaseProvider;
	}

	public void setGraphDatabaseProvider(IGraphDatabaseProvider graphDatabaseProvider) {
		this.graphDatabaseProvider = graphDatabaseProvider;
	}

}