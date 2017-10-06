/**
 * Graphium Neo4j - Module of Graphserver for Neo4j extension
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
package at.srfg.graphium.neo4j.persistence.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

import org.apache.commons.lang3.time.StopWatch;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import at.srfg.graphium.core.service.impl.GraphReadOrder;
import at.srfg.graphium.io.exception.WaySegmentSerializationException;
import at.srfg.graphium.io.outputformat.ISegmentOutputFormat;
import at.srfg.graphium.model.IWaySegment;
import at.srfg.graphium.model.IWaySegmentConnection;
import at.srfg.graphium.model.view.IWayGraphView;
import at.srfg.graphium.neo4j.persistence.INeo4jWayGraphReadDao;
import at.srfg.graphium.neo4j.persistence.nodemapper.INeo4jXInfoNodeMapper;
import at.srfg.graphium.neo4j.service.impl.STRTreeService;

/**
 * @author mwimmer
 *
 */
public class Neo4jWayGraphReadDaoImpl extends AbstractNeo4jDaoImpl implements INeo4jWayGraphReadDao {

	private static Logger log = LoggerFactory.getLogger(Neo4jWayGraphReadDaoImpl.class);
	
	// TODO: Performance-Tuning: Caching von GeometryEncoder zu GraphVersionName
	// TODO: Performance-Tuning: Caching von SpatialLayer zu GraphVersionName => macht das Sinn?
	
	private INeo4jXInfoNodeMapper<IWaySegment> segmentMapper;
	private INeo4jXInfoNodeMapper<List<IWaySegmentConnection>> neo4jWaySegmentConnectionsNodeMapper;
	private STRTreeService treeIndexService = null;
	
	@Override
	public Node getSegmentNodeBySegmentId(String graphName, String version, long segmentId) {
		
		StopWatch stopWatch = new StopWatch();
		if (log.isDebugEnabled()) {
			stopWatch.start();
		}
		
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx()) {
			String graphVersionName = createGraphVersionName(graphName, version);
			Node node = getSegmentNode(graphVersionName, segmentId);

			tx.success();
			
			if (log.isDebugEnabled()) {
				stopWatch.stop();
				log.debug("Node lookup took " + stopWatch.getTime() + " ms");
			}

			return node;
		}
	}
	
	@Override
	public IWaySegment getSegmentById(String graphName, String version, long segmentId, boolean includeConnections) {
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx()) {
			// wird derzeit nicht verwendet
			String graphVersionName = createGraphVersionName(graphName, version);
			Node node = getSegmentNode(graphVersionName, segmentId);
			IWaySegment segment = null;
			if (node != null) {
				segment = segmentMapper.map(node);
				if (includeConnections) {
					segment.setCons(neo4jWaySegmentConnectionsNodeMapper.map(node));
				}
			}
			
			tx.success();
			return segment;
		}
	}

	@Override
	public List<IWaySegment> getSegmentsById(String graphName, String version, List<Long> segmentIds,
			boolean includeConnections) {
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx()) {
			// wird derzeit nicht verwendet
			List<IWaySegment> segments = null;
			if (segmentIds != null) {
				segments = new ArrayList<>(segmentIds.size());
				for (Long id : segmentIds) {
					segments.add(getSegmentById(graphName, version, id, includeConnections));
				}
			}
			
			tx.success();
			return segments;
		}
	}

	@Override
	public void readStreetSegments(BlockingQueue<IWaySegment> queue, String viewName, String version) {
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx()) {
			
			// Derzeit wird das View-System nur mit Default-Views unterstützt. Zusätzliche Views mit Filterdefinitionen werden nicht berücksichtigt.
			// Der Name der Default-View entspricht dem Graphnamen. Daher kann mit dem View-Namen direkt auf den entsprechenden Graphversions-Layer
			// abgefragt werden.
			super.iterateSegmentNodes(this.getNodeIterator(viewName,version),
					queue,segmentMapper,neo4jWaySegmentConnectionsNodeMapper);
			
			tx.success();
		}
	}


	
	@Override
	public void readStreetSegments(BlockingQueue<IWaySegment> queue,
			String graphName, String version, GraphReadOrder order) {
		// TODO: order!
		log.error("info method currently not implemented!");
		log.info("info method currently not implemented!");
	}
	

	@Override
	public List<IWaySegment> getStreetSegments(String viewName, String version) {
		List<IWaySegment> segments = null;
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx()) {

			ResourceIterator<Node> segmentNodes = super.getNodeIterator(viewName, version);
			
			if (segmentNodes.hasNext()) {
				segments = new ArrayList<>();
				super.iterateSegmentNodes(segmentNodes,segments,segmentMapper,neo4jWaySegmentConnectionsNodeMapper);
			} else {
				log.info("no segments found");
			}
				
			tx.success();
		}
		return segments;
	}

	@Override
	public void streamSegments(ISegmentOutputFormat<IWaySegment> outputFormat, Polygon bounds, String viewName,
			Date timestamp) {
		// wird derzeit nicht verwendet
	}

	@Override
	public void streamSegments(ISegmentOutputFormat<IWaySegment> outputFormat, Polygon bounds, String viewName,
			String version) {
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx()) {
			super.iterateSegmentNodes(super.getNodeIterator(viewName,version),outputFormat,segmentMapper,neo4jWaySegmentConnectionsNodeMapper);
			
			tx.success();
		}
	}
	
	@Override
	public void streamSegments(ISegmentOutputFormat<IWaySegment> outputFormat,
			Polygon bounds, String viewName, String version,
			GraphReadOrder order) {
		log.error("info method currently not implemented!");
		log.info("info method currently not implemented!");
	}
	

	@Override
	public void streamSegments(ISegmentOutputFormat<IWaySegment> outputFormat, String viewName,
			String version, Set<Long> ids) {
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx()) {
			
			// Derzeit wird das View-System nur mit Default-Views unterstützt. Zusätzliche Views mit Filterdefinitionen werden nicht berücksichtigt.
			// Der Name der Default-View entspricht dem Graphnamen. Daher kann mit dem View-Namen direkt auf den entsprechenden Graphversions-Layer
			// abgefragt werden.
	
			if (ids != null) {
				IWaySegment segment = null;
				try {
					for (Long id : ids) {
						segment = getSegmentById(viewName, version, id, true);
						if (segment != null) {
							outputFormat.serialize(segment);
						}
					}
				} catch (WaySegmentSerializationException e) {
					log.error("error during streaming segments", e);
				}
			}
			
			tx.success();
		}
	}

	@Override
	public String getGraphVersion(IWayGraphView view, Date timestamp) {
		// wird derzeit nicht verwendet
		return null;
	}
	
	@Override
	public List<Node> findNearestNodes(String graphName, String version, Point referencePoint,
			double distance, int limit) {
		List<Node> nearestNodes = null;
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx()) {
			long time = System.nanoTime();

			List<Long> segmentIds = treeIndexService.findNearestSegmentIds(graphName, version, referencePoint, distance, limit);
			if (segmentIds != null) {
				nearestNodes = new ArrayList<Node>();
				for (Long id : segmentIds) {
					nearestNodes.add(graphDatabaseProvider.getGraphDatabase().getNodeById(id));
				}
			}
			
			if (log.isDebugEnabled()) {
				log.debug("look up of nearest segment took " + ((double) (System.nanoTime() - time) / 1000000) + " ms");
			}
			
			tx.success();
			
			return nearestNodes;
		}
	}

	@Override
	public List<IWaySegment> findNearestSegments(String graphName, String version, Point referencePoint,
			double radiusInKm, int maxNrOfSegments) {
		long startTime = 0;
		if (log.isDebugEnabled()) {
			startTime = System.nanoTime();
		}
		
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx()) {
			List<IWaySegment> segments = null;
			List<Node> nearestNodes = this.findNearestNodesWithOrthodromicDistance(
					graphName, version, referencePoint, radiusInKm, maxNrOfSegments);
			if (nearestNodes != null && !nearestNodes.isEmpty()) {
				segments = new ArrayList<IWaySegment>();
				IWaySegment segment;
				for (Node node : nearestNodes) {
					segment = segmentMapper.map(node);
					segments.add(segment);
				}
			}
			
			tx.success();
			
			if (log.isDebugEnabled()) {
				log.debug("findNearestSegments took " + (System.nanoTime() - startTime) + " ns");
			}
			
			return segments;
		}
	}

	@Override
	public List<Node> findNearestNodesWithOrthodromicDistance(String graphName, String version,
			Point referencePoint, double radiusInKm, int maxNrOfSegments) {
		long startTime = 0;
		if (log.isDebugEnabled()) {
			startTime = System.nanoTime();
		}
		
		List<Node> nearestNodes = null;
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx()) {
			List<Long> segmentIds = treeIndexService.findNearestSegmentIdsWithOrthodromicDistance(graphName, version, referencePoint, radiusInKm, maxNrOfSegments);
			if (segmentIds != null) {
				nearestNodes = new ArrayList<Node>();
				for (Long id : segmentIds) {
					nearestNodes.add(graphDatabaseProvider.getGraphDatabase().getNodeById(id));
				}
			}
			
			tx.success();

			if (log.isDebugEnabled()) {
				log.debug("findNearestNodesWithOrthodromicDistance took " + (System.nanoTime() - startTime) + " ns");
			}
			
			return nearestNodes;
		}
	}
	
	@Override
	public IWaySegment mapNode(String graphName, String version, Node node) {
		return segmentMapper.map(node);
	}


	public INeo4jXInfoNodeMapper<IWaySegment> getSegmentMapper() {
		return segmentMapper;
	}

	public void setSegmentMapper(INeo4jXInfoNodeMapper<IWaySegment> segmentMapper) {
		this.segmentMapper = segmentMapper;
	}

	public INeo4jXInfoNodeMapper<List<IWaySegmentConnection>> getNeo4jWaySegmentConnectionsNodeMapper() {
		return neo4jWaySegmentConnectionsNodeMapper;
	}

	public void setNeo4jWaySegmentConnectionsNodeMapper(INeo4jXInfoNodeMapper<List<IWaySegmentConnection>> neo4jWaySegmentConnectionsNodeMapper) {
		this.neo4jWaySegmentConnectionsNodeMapper = neo4jWaySegmentConnectionsNodeMapper;
	}

	public STRTreeService getTreeIndexService() {
		return treeIndexService;
	}

	public void setTreeIndexService(STRTreeService treeIndexService) {
		this.treeIndexService = treeIndexService;
	}

}
