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
/**
 * (C) 2016 Salzburg Research Forschungsgesellschaft m.b.H.
 *
 * All rights reserved.
 *
 */
package at.srfg.graphium.neo4j.persistence.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;

import at.srfg.graphium.core.exception.GraphAlreadyExistException;
import at.srfg.graphium.core.exception.GraphNotExistsException;
import at.srfg.graphium.core.exception.GraphStorageException;
import at.srfg.graphium.core.helper.GraphVersionHelper;
import at.srfg.graphium.core.persistence.IWayGraphWriteDao;
import at.srfg.graphium.model.IBaseSegment;
import at.srfg.graphium.model.IConnectionXInfo;
import at.srfg.graphium.model.ISegmentXInfo;
import at.srfg.graphium.model.IWayGraphVersionMetadata;
import at.srfg.graphium.model.IWaySegment;
import at.srfg.graphium.model.IWaySegmentConnection;
import at.srfg.graphium.neo4j.model.WayGraphConstants;
import at.srfg.graphium.neo4j.model.WaySegmentRelationshipType;
import at.srfg.graphium.neo4j.persistence.INeo4jWayGraphIndexDao;
import at.srfg.graphium.neo4j.persistence.INeo4jWaySegmentHelper;
import at.srfg.graphium.neo4j.persistence.configuration.IGraphDatabaseProvider;
import at.srfg.graphium.neo4j.persistence.nodemapper.utils.Neo4jTagMappingUtils;
import at.srfg.graphium.neo4j.persistence.propertyhandler.IConnectionXInfoPropertyHandler;
import at.srfg.graphium.neo4j.persistence.propertyhandler.ISegmentXInfoPropertyHandler;
import at.srfg.graphium.neo4j.persistence.propertyhandler.impl.ConnectionXInfoPropertyHandlerRegistry;
import at.srfg.graphium.neo4j.persistence.propertyhandler.impl.SegmentXInfoPropertyHandlerRegistry;

/**
 * @author mwimmer
 */
public class Neo4jWayGraphWriteDaoImpl<W extends IWaySegment>
	extends AbstractNeo4jDaoImpl implements IWayGraphWriteDao<W> {

	private static Logger log = LoggerFactory.getLogger(Neo4jWayGraphWriteDaoImpl.class);
	
	protected INeo4jWaySegmentHelper<W> segmentHelper;
	protected INeo4jWayGraphIndexDao indexDao;
	protected SegmentXInfoPropertyHandlerRegistry segmentPropertyHandlerRegistry;
	protected ConnectionXInfoPropertyHandlerRegistry connectionPropertyHandlerRegistry;
	protected int batchSizeForNodeDeletion = 1000;
	
	@Override
	public void createGraph(String graphName, String version, boolean overrideGraphIfExsists) throws GraphAlreadyExistException {
		String graphVersionName = GraphVersionHelper.createGraphVersionName(graphName, version);
		if (checkIfMetadataExists(graphName, version)) {
			if (overrideGraphIfExsists) {
				// delete Nodes from Layer
				deleteGraphVersionSegmentNodes(graphVersionName);
			} else {
				throw new GraphAlreadyExistException("graph " + graphVersionName + " already exists");
			}
		}
	}
	
	private boolean checkIfMetadataExists(String graphName, String version) {
		return getWayGraphVersionMetadataNode(graphName, version) != null;
	}

	protected void deleteGraphVersionSegmentNodes(String graphVersionName) {
		log.info("deleting nodes ...");
		long count = 0;
		long totalDeleted = 0;
		String labelName = Neo4jWaySegmentHelperImpl.createSegmentNodeLabel(graphVersionName);

		do {
			try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx()) {
				Result result = getGraphDatabase().execute("MATCH (n:" + labelName + ") WITH n LIMIT " + batchSizeForNodeDeletion + " DETACH DELETE n RETURN count(*)");
				while (result.hasNext()) {
				    Map<String,Object> row = result.next();
				    for ( Entry<String,Object> column : row.entrySet() )
			        {
			            count = (long) column.getValue();
			        }
				    log.debug(count + " nodes deleted");
				    totalDeleted += count;
				}
				tx.success();
			}
		} while (count > 0);
		
		log.info("deleting index...");
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx()) {
			Index<Node> nodeIndex = getSegmentIdIndex(graphVersionName);
			nodeIndex.delete();
			tx.success();
		}
		log.info("index deleted");

		log.info(totalDeleted + " nodes deleted");
	}
	
	@Override
	public void createGraphVersion(String graphName, String version, boolean overrideGraphIfExsists,
			boolean createConnectionConstraint) throws GraphAlreadyExistException {
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx()) {
			createGraph(graphName, version, overrideGraphIfExsists);
			
			tx.success();
		}
	}

	@Override
	public void createConnectionContstraints(String graphVersionName) throws GraphNotExistsException {
		// nothing to do
	}

	@Override
	public void saveSegments(List<W> segments, String graphName, String version) {
		saveSegments(segments, graphName, version, null);
	}
	
	@Override
	public void saveSegments(List<W> segments, String graphName, String version, List<String> excludedXInfosList) {
		String graphVersionName = GraphVersionHelper.createGraphVersionName(graphName, version); 
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx()) {
			if (segments != null && !segments.isEmpty()) {
				
				Index<Node> indexSegmentId = getSegmentIdIndex(graphVersionName);
//				
				for (W segment : segments) {
					// create node
					Node segmentNode = segmentHelper.createNode(getGraphDatabase(), segment, graphVersionName);
	
					// create index on segmentId
					// use of segmenId index is much faster than Neo4j's segmentId lookup
					indexSegmentId.add(segmentNode, WayGraphConstants.SEGMENT_ID, segment.getId());
					
					// if XInfo exists for segment
					saveSegmentXInfo(getGraphDatabase(), segment, segmentNode, excludedXInfosList);
					
				}
				
				log.info("Saved " + segments.size() + " segments");
			}
			
			tx.success();
		}
	}

	@Override
	public void updateSegments(List<W> segments, String graphName, String version) {
		updateSegmentAttributes(segments, graphName, version);
		updateConnections(segments, graphName, version);
	}
	
	@Override
	public long updateSegmentAttributes(List<W> segments, String graphName, String version) {
		String graphVersionName = GraphVersionHelper.createGraphVersionName(graphName, version); 
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx()) {
			int updatedCount = 0;
			
			if (segments != null && !segments.isEmpty()) {
				
				for (W segment : segments) {
					// read Node from index
					Node node = getSegmentNode(graphVersionName, segment.getId());
					if (node != null) {
						// update node properties
						segmentHelper.updateNodeProperties(getGraphDatabase(), segment, node);
						
						// if XInfo exists for segment
						saveSegmentXInfo(getGraphDatabase(), segment, node, null);

						updatedCount++;
					}
				}
				
			}
			
			tx.success();
			return updatedCount;
		}
	}
	
	@Override
	public long updateConnections(List<W> segments, String graphName, String version) {
		String graphVersionName = GraphVersionHelper.createGraphVersionName(graphName, version);
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx()) {
			int updatedCount = 0;
			List<IWaySegmentConnection> connsWithXInfo = null;
			for (W segment : segments) {
				connsWithXInfo = new ArrayList<>();
				if (segment.getStartNodeCons() != null) {
					connsWithXInfo.addAll(segment.getStartNodeCons());
				}
				if (segment.getEndNodeCons() != null) {
					connsWithXInfo.addAll(segment.getEndNodeCons());
				}
				if (!connsWithXInfo.isEmpty()) {
					Node segmentNode = getSegmentNode(graphVersionName, segment.getId());
					for (IWaySegmentConnection conn : connsWithXInfo) {
						Relationship rel = getRelationShip(segmentNode, conn);
						if (rel != null) {
							saveConnectionXInfo(conn, rel, null);
						}
					}
				}
			}

			tx.success();
			return updatedCount;
		}
	}

	protected Map<IWaySegmentConnection,Relationship> mapRelationShips(Node segmentNode, List<IWaySegmentConnection> connections) {
		Map<IWaySegmentConnection,Relationship> result = new HashMap<>();
		if (segmentNode != null && connections != null && !connections.isEmpty()) {
			Iterable<Relationship> rels = segmentNode.getRelationships(Direction.OUTGOING,
					WaySegmentRelationshipType.SEGMENT_CONNECTION_ON_STARTNODE,
					WaySegmentRelationshipType.SEGMENT_CONNECTION_ON_ENDNODE,
					WaySegmentRelationshipType.SEGMENT_CONNECTION_WITHOUT_NODE);
			rels.forEach(rel -> {
				connections.forEach(conn -> {
					if ((long)(rel.getProperty(WayGraphConstants.CONNECTION_NODE_ID)) == conn.getNodeId()
							&& conn.getToSegmentId() == (long) (rel.getEndNode().getProperty(WayGraphConstants.SEGMENT_ID))) {
						result.put(conn,rel);
					}
				});
			});
		}
		return result;
	}

	protected Relationship getRelationShip(Node segmentNode, IWaySegmentConnection conn) {
		Relationship rel = null;
		if (segmentNode != null && conn != null) {
			Iterable<Relationship> rels = segmentNode.getRelationships(Direction.OUTGOING,
					WaySegmentRelationshipType.SEGMENT_CONNECTION_ON_STARTNODE,
					WaySegmentRelationshipType.SEGMENT_CONNECTION_ON_ENDNODE,
					WaySegmentRelationshipType.SEGMENT_CONNECTION_WITHOUT_NODE);
			Iterator<Relationship> relsIt = rels.iterator();
			boolean found = false;
			while (relsIt.hasNext() && !found) {
				rel = relsIt.next();
				long nodeId = (long) rel.getProperty(WayGraphConstants.CONNECTION_NODE_ID);
				if (nodeId == conn.getNodeId()) {
					long toSegmentId = (long) rel.getEndNode().getProperty(WayGraphConstants.SEGMENT_ID);
					if (conn.getToSegmentId() == toSegmentId) {
						found = true;
					}
				}
			}
		}
		return rel;
	}

	@Override
	public long saveConnectionsOnSegments(List<W> segmentsWithConnections, boolean saveSegments,
			String graphName, String version) {
		return 0;
	}

	@Override
	public long saveConnections(List<IWaySegmentConnection> connections, String graphName, String version) {
		String graphVersionName = GraphVersionHelper.createGraphVersionName(graphName, version); 
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx()) {
			int saveCount = 0;
			if (connections != null && !connections.isEmpty()) {
				
				for (IWaySegmentConnection connection : connections) {
					if (connection.getAccess() != null && !connection.getAccess().isEmpty()) {
						// read nodes from index
						Node fromSegmentNode = getSegmentNode(graphVersionName, connection.getFromSegmentId());
						Node toSegmentNode = getSegmentNode(graphVersionName, connection.getToSegmentId());
						// select relation type
						RelationshipType relType;
						if ((Long) fromSegmentNode.getProperty(WayGraphConstants.SEGMENT_STARTNODE_ID) == connection.getNodeId()) {
							relType = WaySegmentRelationshipType.SEGMENT_CONNECTION_ON_STARTNODE;
						} else if ((Long) fromSegmentNode.getProperty(WayGraphConstants.SEGMENT_ENDNODE_ID) == connection.getNodeId()) {
							relType = WaySegmentRelationshipType.SEGMENT_CONNECTION_ON_ENDNODE;
						} else {
							relType = WaySegmentRelationshipType.SEGMENT_CONNECTION_WITHOUT_NODE;
						}
						Relationship rel = fromSegmentNode.createRelationshipTo(toSegmentNode, relType);
						rel.setProperty(WayGraphConstants.CONNECTION_ACCESS, Neo4jWaySegmentHelperImpl.createAccessArray(connection.getAccess()));
						rel.setProperty(WayGraphConstants.CONNECTION_NODE_ID, connection.getNodeId());
						if (connection.getTags() != null) {
							Neo4jTagMappingUtils.createTagProperties(rel, connection.getTags(), WayGraphConstants.CONNECTION_TAG_PREFIX);
						}
						
						// if XInfo exists for connection
						saveConnectionXInfo(connection, rel, null);
					}
				}
			}
			
			tx.success();
			return saveCount;
		}
	}

	@Override
	public void createIndexes(String graphName, String version) {
		String graphVersionName = GraphVersionHelper.createGraphVersionName(graphName, version); 
		indexDao.createIndexes(graphVersionName);
	}

	@Override
	public void deleteSegments(String graphName, String version) {
		String graphVersionName = GraphVersionHelper.createGraphVersionName(graphName, version); 
		deleteGraphVersionSegmentNodes(graphVersionName);
	}

	@Override
	public void deleteSegmentXInfos(String graphName, String version, String... types) throws GraphStorageException, GraphNotExistsException {
		//TODO Implement ME
	}

	protected void saveSegmentXInfo(GraphDatabaseService graphDb, W segment, Node segmentNode, List<String> excludedXInfosList) {
		if (segment.getXInfo() != null && !segment.getXInfo().isEmpty()) {
			ISegmentXInfoPropertyHandler<ISegmentXInfo> propertySetter;
			List<ISegmentXInfo> xInfos = segment.getXInfo();
			for (ISegmentXInfo xInfo : xInfos) {
				if (excludedXInfosList == null || !excludedXInfosList.contains(xInfo.getXInfoType())) {
					propertySetter = segmentPropertyHandlerRegistry.get(xInfo.getXInfoType());
					if (propertySetter != null) {
						Node xInfoNode = propertySetter.getXInfoNode(xInfo, segmentNode);
						if (xInfoNode == null) {
							xInfoNode = propertySetter.setXInfoProperties(graphDb, xInfo, segmentNode);
						} else {
							throw new DuplicateKeyException("XInfo node of type '" + propertySetter.getResponsibleType() + "' already exists");
						}
					}
				}
			}
		}
	}

	protected void saveConnectionXInfo(IWaySegmentConnection connection, Relationship connectionRelationship, List<String> excludedXInfos) {
		if (connection.getXInfo() != null && !connection.getXInfo().isEmpty()) {
			IConnectionXInfoPropertyHandler propertyHandler;
			List<? extends IConnectionXInfo> xInfos = connection.getXInfo();
			for (IConnectionXInfo xInfo : xInfos) {
				if (excludedXInfos == null || !excludedXInfos.contains(xInfo.getXInfoType())) {
					propertyHandler = connectionPropertyHandlerRegistry.get(xInfo.getXInfoType());
					if (propertyHandler != null) {
						propertyHandler.setXInfoProperties(xInfo, xInfo.getGroupKey(),connectionRelationship);
					}
				}
			}
		}
	}

	@Override
	public void saveConnectionXInfos(List<? extends IBaseSegment> segments, String graphName, String version) throws GraphStorageException {
		saveConnectionXInfos(segments, graphName, version, null);
	}
	
	@Override
	public void saveConnectionXInfos(List<? extends IBaseSegment> segments, String graphName, String version, List<String> excludedXInfos) throws GraphStorageException {
		String graphVersionName = GraphVersionHelper.createGraphVersionName(graphName, version);
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx()) {
			if (segments != null && !segments.isEmpty()) {
				for (IBaseSegment segment : segments) {
					if (segment.getCons() != null) {
						Node node = this.getSegmentNode(graphVersionName,segment.getId());
						Map<IWaySegmentConnection,Relationship> connMapping = this.mapRelationShips(node,segment.getCons());
						connMapping.forEach((iWaySegmentConnection, relationship) -> {
                            if (relationship != null) {
                                saveConnectionXInfo(iWaySegmentConnection, relationship, excludedXInfos);
                            } else {
                                log.warn("Relationship not found for " +iWaySegmentConnection.getFromSegmentId() + "-(" + iWaySegmentConnection.getNodeId() + ")-" +  iWaySegmentConnection.getToSegmentId());
                            }
                        });
					}
				}
				log.info("Saved " + segments.size() + " connection xInfos");
			}
			tx.success();
		}
	}

	@Override
	public void deleteConnectionXInfos(String graphName, String version, final String... types) throws GraphStorageException, GraphNotExistsException {
		String graphVersionName = GraphVersionHelper.createGraphVersionName(graphName, version);
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx()) {
			if (types != null && types.length > 0) {
				log.info("deleting xinfos " + types);

				ResourceIterator<Node> nodes = getGraphDatabase().findNodes(Label.label(Neo4jWaySegmentHelperImpl.createSegmentNodeLabel(graphVersionName)));
				nodes.forEachRemaining(node -> {
					Iterable<Relationship> rels = node.getRelationships(Direction.OUTGOING,
							WaySegmentRelationshipType.SEGMENT_CONNECTION_ON_STARTNODE,
							WaySegmentRelationshipType.SEGMENT_CONNECTION_ON_ENDNODE,
							WaySegmentRelationshipType.SEGMENT_CONNECTION_WITHOUT_NODE);
					rels.forEach(relationship -> {
						for (String type : types) {
							IConnectionXInfoPropertyHandler<? extends IConnectionXInfo> propertyHandler = connectionPropertyHandlerRegistry.get(type);
							// TODO: nullcheck / Exception handling, there should be an exception up and not an null pointer exception if no property handler is found
							propertyHandler.deleteProperties(relationship,null);
						}
					});
				});

				log.info("xinfos delete");
			}
			tx.success();
		}
	}

	@Override
	public void saveSegmentXInfos(List<? extends IBaseSegment> segments, String graphName, String version) throws GraphStorageException {
		saveSegmentXInfos(segments, graphName, version, null);
	}

	@Override
	public void saveSegmentXInfos(List<? extends IBaseSegment> segments, String graphName, String version, List<String> excludedXInfosList) throws GraphStorageException {
		String graphVersionName = GraphVersionHelper.createGraphVersionName(graphName, version);
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx()) {
			if (segments != null && !segments.isEmpty()) {
//
				for (IBaseSegment segment : segments) {
					Node node = this.getSegmentNode(graphVersionName,segment.getId());
					//TODO implement me
				}

				log.info("Saved " + segments.size() + " segments");
			}

			tx.success();
		}
	}

	@Override
	public void updateConnectionXInfos(List<? extends IBaseSegment> segments, String graphName, String version) throws GraphStorageException {
		//TODO Implement me
	}

	@Override
	public void updateSegmentXInfos(List<? extends IBaseSegment> segments, String graphName, String version) throws GraphStorageException {
		//TODO Implement me
	}

	@Override
	public void postCreateGraph(IWayGraphVersionMetadata graphVersionMeta) {
		// nothing to do
	}
	
	public IGraphDatabaseProvider getGraphDatabaseProvider() {
		return graphDatabaseProvider;
	}

	public void setGraphDatabaseProvider(IGraphDatabaseProvider graphDatabaseProvider) {
		this.graphDatabaseProvider = graphDatabaseProvider;
	}

	public INeo4jWaySegmentHelper<W> getSegmentHelper() {
		return segmentHelper;
	}

	public void setSegmentHelper(INeo4jWaySegmentHelper<W> segmentHelper) {
		this.segmentHelper = segmentHelper;
	}

	public int getBatchSizeForNodeDeletion() {
		return batchSizeForNodeDeletion;
	}

	public void setBatchSizeForNodeDeletion(int batchSizeForNodeDeletion) {
		this.batchSizeForNodeDeletion = batchSizeForNodeDeletion;
	}

	public INeo4jWayGraphIndexDao getIndexDao() {
		return indexDao;
	}

	public void setIndexDao(INeo4jWayGraphIndexDao indexDao) {
		this.indexDao = indexDao;
	}

	public SegmentXInfoPropertyHandlerRegistry getSegmentPropertyHandlerRegistry() {
		return segmentPropertyHandlerRegistry;
	}

	public void setSegmentPropertyHandlerRegistry(SegmentXInfoPropertyHandlerRegistry segmentPropertyHandlerRegistry) {
		this.segmentPropertyHandlerRegistry = segmentPropertyHandlerRegistry;
	}

	public ConnectionXInfoPropertyHandlerRegistry getConnectionPropertyHandlerRegistry() {
		return connectionPropertyHandlerRegistry;
	}

	public void setConnectionPropertyHandlerRegistry(
			ConnectionXInfoPropertyHandlerRegistry connectionPropertyHandlerRegistry) {
		this.connectionPropertyHandlerRegistry = connectionPropertyHandlerRegistry;
	}
}