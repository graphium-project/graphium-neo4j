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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;

import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.WKBWriter;

import at.srfg.graphium.core.persistence.IWayGraphVersionMetadataDao;
import at.srfg.graphium.model.Access;
import at.srfg.graphium.model.ISource;
import at.srfg.graphium.model.IWayGraph;
import at.srfg.graphium.model.IWayGraphMetadataFactory;
import at.srfg.graphium.model.IWayGraphVersionMetadata;
import at.srfg.graphium.model.State;
import at.srfg.graphium.model.impl.WayGraph;
import at.srfg.graphium.neo4j.model.WayGraphConstants;
import at.srfg.graphium.neo4j.model.WayGraphRelationshipType;
import at.srfg.graphium.neo4j.persistence.INeo4jWayGraphViewDao;
import at.srfg.graphium.neo4j.persistence.nodemapper.INeo4jNodeMapper;

/**
 * @author mwimmer
 *
 */
public class Neo4jWayGraphVersionMetadataDaoImpl extends AbstractNeo4jDaoImpl implements IWayGraphVersionMetadataDao {

	private IWayGraphMetadataFactory metadataFactory;
	private INeo4jNodeMapper<IWayGraphVersionMetadata> nodeMapper;
	private INeo4jWayGraphViewDao viewDao;
	
	@Override
	public IWayGraphVersionMetadata newWayGraphVersionMetadata() {
		return metadataFactory.newWayGraphVersionMetadata();
	}

	@Override
	public IWayGraphVersionMetadata newWayGraphVersionMetadata(long id, long graphId, String graphName, String version,
			String originGraphName, String originVersion, State state, Date validFrom, Date validTo,
			Polygon coveredArea, int segmentsCount, int connectionsCount, Set<Access> accessTypes,
			Map<String, String> tags, ISource source, String type, String description, Date creationTimestamp,
			Date storageTimestamp, String creator, String originUrl) {
		return metadataFactory.newWayGraphVersionMetadata(id, graphId, graphName, version, originGraphName, originVersion, state, 
				validFrom, validTo, coveredArea, segmentsCount, connectionsCount, accessTypes, tags, source, type, description, 
				creationTimestamp, storageTimestamp, creator, originUrl);
	}

	@Override
	public IWayGraphVersionMetadata getWayGraphVersionMetadata(long id) {
		// wird derzeit nicht verwendet
		return null;
	}

	@Override
	public IWayGraphVersionMetadata getWayGraphVersionMetadata(String graphName, String version) {
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx()) {
			Node metadataNode = getMetadataNode(graphName, version);
	
			IWayGraphVersionMetadata metadata = null;
			if (metadataNode != null) {
				metadata = nodeMapper.map(metadataNode);
			}
			
			tx.success();
			return metadata;
		}
	}

	private Node getMetadataNode(String graphName, String version) {
		return getGraphDatabase().findNode(Label.label(WayGraphConstants.METADATA_LABEL), 
								WayGraphConstants.METADATA_GRAPHVERSIONNAME, createGraphVersionName(graphName, version));
	}

	@Override
	public List<IWayGraphVersionMetadata> getWayGraphVersionMetadataList(String graphName) {
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx()) {
			List<IWayGraphVersionMetadata> metadataList = new ArrayList<>();
			ResourceIterator<Node> nodeIterator = getGraphDatabase().findNodes(Label.label(WayGraphConstants.METADATA_LABEL), 
					WayGraphConstants.METADATA_GRAPHNAME, graphName);
			while (nodeIterator.hasNext()) {
				metadataList.add(nodeMapper.map(nodeIterator.next()));
			}
			nodeIterator.close();
			
			tx.success();
			return metadataList;
		}
	}

	@Override
	public List<IWayGraphVersionMetadata> getWayGraphVersionMetadataListForOriginGraphname(String originGraphName) {
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx()) {
			List<IWayGraphVersionMetadata> metadataList = new ArrayList<>();
			ResourceIterator<Node> nodeIterator = getGraphDatabase().findNodes(Label.label(WayGraphConstants.METADATA_LABEL), 
													WayGraphConstants.METADATA_ORIGIN_GRAPHNAME, originGraphName);
			while (nodeIterator.hasNext()) {
				metadataList.add(nodeMapper.map(nodeIterator.next()));
			}
			nodeIterator.close();
			
			tx.success();
			return metadataList;
		}
	}

	@Override
	public List<IWayGraphVersionMetadata> getWayGraphVersionMetadataList(String graphName, State state, Date validFrom,
			Date validTo, Set<Access> accessTypes) {
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx()) {
			List<IWayGraphVersionMetadata> metadataList = new ArrayList<>();
			ResourceIterator<Node> nodeIterator = getGraphDatabase().findNodes(Label.label(WayGraphConstants.METADATA_LABEL), 
					WayGraphConstants.METADATA_GRAPHNAME, graphName);
			Node metadataNode = null;
			boolean valid = true;
			if (nodeIterator.hasNext()) {
				// create relationship from view node to metadata node
				while (nodeIterator.hasNext()) {
					valid = true;
					metadataNode = nodeIterator.next();
					
					if (state != null && !State.valueOf((String)metadataNode.getProperty(WayGraphConstants.METADATA_STATE)).equals(state)) {
						valid = false;
					}
					if (valid) {
						Long validFromTime = (Long) metadataNode.getProperty(WayGraphConstants.METADATA_VALID_FROM);
						Long validToTime = null;
						if (metadataNode.hasProperty(WayGraphConstants.METADATA_VALID_TO)) {
							validToTime = (long) metadataNode.getProperty(WayGraphConstants.METADATA_VALID_TO);
						
						}
						if (validFrom != null && validTo != null) {
							if (validFrom.getTime() <= validTo.getTime() &&
								validFromTime <= validTo.getTime() &&
								(validToTime == null || validToTime >= validFrom.getTime())) {
								// OK
							} else {
								valid = false;
							}
						} else if (validFrom != null) {
							if (validToTime == null || validToTime >= validFrom.getTime()) {
								// OK
							} else {
								valid = false;
							}
						} else if (validTo != null) {
							if (validFromTime >= validTo.getTime()) {
								// OK
							} else {
								valid = false;
							}
						}
					}
					if (valid && accessTypes != null) {
						byte[] accessTypeIds = (byte[]) metadataNode.getProperty(WayGraphConstants.METADATA_ACCESSTYPES);
						if (accessTypeIds == null) {
							valid = false;
						} else {
							Set<Access> accesses = Neo4jWaySegmentHelperImpl.parseAccessTypes(accessTypeIds);
							Set<Access> overlappingAccesses = Neo4jWaySegmentHelperImpl.getAccessOverlappings(accessTypes, accesses);
							if (overlappingAccesses.isEmpty()) {
								valid = false;
							}
						}
					}
	
					if (valid) {
						metadataList.add(nodeMapper.map(metadataNode));
					}
				}
			}
			nodeIterator.close();
			
			tx.success();
			return metadataList;
		}
	}

	@Override
	public void saveGraphVersion(IWayGraphVersionMetadata graphMetadata) {
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx()) {
			Node wayGraphNode = getGraphNode(graphMetadata.getGraphName());
			if (wayGraphNode != null) {
				Node metadataNode = getGraphDatabase().createNode(Label.label(WayGraphConstants.METADATA_LABEL));
				updateMetadataNodeProperties(graphMetadata, metadataNode);
				wayGraphNode.createRelationshipTo(metadataNode, WayGraphRelationshipType.GRAPH_METADATA);
				metadataNode.createRelationshipTo(wayGraphNode, WayGraphRelationshipType.GRAPH);
			}
			
			tx.success();
		}
	}

	private void updateMetadataNodeProperties(IWayGraphVersionMetadata graphMetadata, Node metadataNode) {
		if (graphMetadata.getAccessTypes() != null) {
			metadataNode.setProperty(WayGraphConstants.METADATA_ACCESSTYPES, Neo4jWaySegmentHelperImpl.createAccessArray(graphMetadata.getAccessTypes()));
		}
		metadataNode.setProperty(WayGraphConstants.METADATA_CONNECTIONS_COUNT, graphMetadata.getConnectionsCount());
		if (graphMetadata.getCoveredArea() != null) {
			WKBWriter wkbWriter = new WKBWriter();
			metadataNode.setProperty(WayGraphConstants.METADATA_COVERED_AREA, wkbWriter.write(graphMetadata.getCoveredArea()));
		}
		if (graphMetadata.getCreationTimestamp() != null) {
			metadataNode.setProperty(WayGraphConstants.METADATA_CREATION_TIMESTAMP, graphMetadata.getCreationTimestamp().getTime());
		} else {
			removeMetadataProperty(metadataNode, WayGraphConstants.METADATA_CREATION_TIMESTAMP);
		}
		if (graphMetadata.getCreator() != null) {
			metadataNode.setProperty(WayGraphConstants.METADATA_CREATOR, graphMetadata.getCreator());
		} else {
			removeMetadataProperty(metadataNode, WayGraphConstants.METADATA_CREATOR);
		}
		if (graphMetadata.getDescription() != null) {
			metadataNode.setProperty(WayGraphConstants.METADATA_DESCRIPTION, graphMetadata.getDescription());
		} else {
			removeMetadataProperty(metadataNode, WayGraphConstants.METADATA_DESCRIPTION);
		}
		if (graphMetadata.getGraphName() != null) {
			metadataNode.setProperty(WayGraphConstants.METADATA_GRAPHNAME, graphMetadata.getGraphName());
		}
		if (graphMetadata.getOriginGraphName() != null) {
			metadataNode.setProperty(WayGraphConstants.METADATA_ORIGIN_GRAPHNAME, graphMetadata.getOriginGraphName());
		} else {
			removeMetadataProperty(metadataNode, WayGraphConstants.METADATA_ORIGIN_GRAPHNAME);
		}
		if (graphMetadata.getOriginUrl() != null) {
			metadataNode.setProperty(WayGraphConstants.METADATA_ORIGIN_URL, graphMetadata.getOriginUrl());
		} else {
			removeMetadataProperty(metadataNode, WayGraphConstants.METADATA_ORIGIN_URL);
		}
		if (graphMetadata.getOriginVersion() != null) {
			metadataNode.setProperty(WayGraphConstants.METADATA_ORIGIN_VERSION, graphMetadata.getOriginVersion());
		} else {
			removeMetadataProperty(metadataNode, WayGraphConstants.METADATA_ORIGIN_VERSION);
		}
		metadataNode.setProperty(WayGraphConstants.METADATA_SEGMENTS_COUNT, graphMetadata.getSegmentsCount());
		if (graphMetadata.getSource() != null) {
			metadataNode.setProperty(WayGraphConstants.METADATA_SOURCE, graphMetadata.getSource().getName());
		} else {
			removeMetadataProperty(metadataNode, WayGraphConstants.METADATA_SOURCE);
		}
		if (graphMetadata.getState() != null) {
			metadataNode.setProperty(WayGraphConstants.METADATA_STATE, graphMetadata.getState().name());
		}
		if (graphMetadata.getStorageTimestamp() != null) {
			metadataNode.setProperty(WayGraphConstants.METADATA_STORAGE_TIMESTAMP, graphMetadata.getStorageTimestamp().getTime());
		}
		// TODO
//		if (graphMetadata.getTags() != null) {
//			metadataNode.setProperty(WayGraphConstants.METADATA_TAGS, graphMetadata.getTags());
//		}
		if (graphMetadata.getType() != null) {
			metadataNode.setProperty(WayGraphConstants.METADATA_TYPE, graphMetadata.getType());
		} else {
			removeMetadataProperty(metadataNode, WayGraphConstants.METADATA_TYPE);
		}
		if (graphMetadata.getValidFrom() != null) {
			metadataNode.setProperty(WayGraphConstants.METADATA_VALID_FROM, graphMetadata.getValidFrom().getTime());
		}
		if (graphMetadata.getValidTo() != null) {
			metadataNode.setProperty(WayGraphConstants.METADATA_VALID_TO, graphMetadata.getValidTo().getTime());
		} else {
			removeMetadataProperty(metadataNode, WayGraphConstants.METADATA_VALID_TO);
		}
		if (graphMetadata.getVersion() != null) {
			metadataNode.setProperty(WayGraphConstants.METADATA_VERSION, graphMetadata.getVersion());
		}
		if (graphMetadata.getVersion() != null && graphMetadata.getVersion() != null) {
			metadataNode.setProperty(WayGraphConstants.METADATA_GRAPHVERSIONNAME, createGraphVersionName(graphMetadata.getGraphName(), graphMetadata.getVersion()));
		}
	}

	private void removeMetadataProperty(Node metadataNode, String property) {
		metadataNode.removeProperty(property);
	}
	
	private void updateWayGraphNodeProperties(String graphName, Node wayGraphNode) {
		wayGraphNode.setProperty(WayGraphConstants.WAYGRAPH_NAME, graphName);
	}
	
	@Override
	public void updateGraphVersion(IWayGraphVersionMetadata graphMetadata) {
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx()) {
			Node metadataNode = getMetadataNode(graphMetadata.getGraphName(), graphMetadata.getVersion());
			updateMetadataNodeProperties(graphMetadata, metadataNode);
			
			tx.success();
		}
	}

	@Override
	public void setGraphVersionState(String graphName, String version, State state) {
		if (state != null) {
			try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx()) {
				Node metadataNode = getMetadataNode(graphName, version);
				metadataNode.setProperty(WayGraphConstants.METADATA_STATE, state.name());
				
				tx.success();
			}
		}
	}

	@Override
	public void setValidToTimestampOfPredecessorGraphVersion(IWayGraphVersionMetadata graphMetadata) {
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx()) {
			// read previous metadata node => metadata node where valid_to is null and valid_from < new valid_from and state is ACTIVE
	
			ResourceIterator<Node> nodeIterator = getGraphDatabase().findNodes(Label.label(WayGraphConstants.METADATA_LABEL),
					WayGraphConstants.METADATA_GRAPHNAME, graphMetadata.getGraphName());
			Node metadataNode = null;
			boolean found = false;
			while (nodeIterator.hasNext() && !found) {
				metadataNode = nodeIterator.next();
				if (metadataNode.getProperty(WayGraphConstants.METADATA_STATE).equals(State.ACTIVE.name()) &&
					!metadataNode.hasProperty(WayGraphConstants.METADATA_VALID_TO) &&
					(Long)metadataNode.getProperty(WayGraphConstants.METADATA_VALID_FROM) < graphMetadata.getValidFrom().getTime()) {
					found = true;
				}
			}
			nodeIterator.close();
			if (found) {
				metadataNode.setProperty(WayGraphConstants.METADATA_VALID_TO, graphMetadata.getValidFrom().getTime());
			}
			
			tx.success();
		}
	}

	@Override
	public boolean checkIfGraphExists(String graphName) {
		// wird derzeit nicht verwendet
		return false;
	}

	@Override
	public IWayGraph getGraph(String graphName) {
		IWayGraph graph = null;
		Node metadataNode = getGraphNode(graphName);
		if (metadataNode != null) {
			graph = new WayGraph(0, graphName);
		}
		return graph;
	}

	private Node getGraphNode(String graphName) {
		Node metadataNode = null;
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx()) {
			metadataNode = getGraphDatabase().findNode(Label.label(WayGraphConstants.WAYGRAPH_LABEL), WayGraphConstants.WAYGRAPH_NAME, graphName);

			tx.success();
		}
		return metadataNode;
	}
	
	@Override
	public IWayGraph getGraph(long graphId) {
		// wird derzeit nicht verwendet
		return null;
	}

	@Override
	public long saveGraph(String graphName) {
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx()) {
			Node wayGraphNode = getGraphDatabase().createNode(Label.label(WayGraphConstants.WAYGRAPH_LABEL));
			updateWayGraphNodeProperties(graphName, wayGraphNode);
			
			tx.success();
		}
		return 0;
	}

	@Override
	public List<String> getGraphs() {
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx()) {
			Set<String> graphNames = new HashSet<>();
			ResourceIterator<Node> nodeIterator = getGraphDatabase().findNodes(Label.label(WayGraphConstants.METADATA_LABEL));
			Node metadataNode = null;
			while (nodeIterator.hasNext()) {
				metadataNode = nodeIterator.next();
				graphNames.add((String) metadataNode.getProperty(WayGraphConstants.METADATA_GRAPHNAME));
			}
			nodeIterator.close();
			
			tx.success();
			return new ArrayList<>(graphNames);
		}
	}

	@Override
	public String checkNewerVersionAvailable(String viewName, String version) {
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx()) {
			String versionFound = null;
	
			long requestedValidFrom = 0;
			if (version != null) {
				IWayGraphVersionMetadata metadata = getWayGraphVersionMetadataForView(viewName, version);
				if (metadata != null) {
					requestedValidFrom = metadata.getValidFrom().getTime();
				}
			}
			
			Node viewNode = viewDao.getViewNode(viewName);
			
			if (viewNode != null) {
				Set<String> states = new HashSet<String>();
				states.add(State.PUBLISH.name());
				states.add(State.SYNCHRONIZED.name());
				states.add(State.ACTIVE.name());
				
				// get all related metadata nodes and select the current version
				Iterator<Relationship> rels = viewNode.getRelationships(WayGraphRelationshipType.GRAPH_VERSION).iterator();
				Node metadataNode = null;
				Long validFrom = null;
				Long lastValidFrom = null;
				while (rels.hasNext()) {
					metadataNode = rels.next().getEndNode();
					validFrom = (long) metadataNode.getProperty(WayGraphConstants.METADATA_VALID_FROM);
					if (requestedValidFrom < validFrom &&
						states.contains(metadataNode.getProperty(WayGraphConstants.METADATA_STATE))) {
						if (lastValidFrom == null || lastValidFrom < validFrom) {
							versionFound = (String) metadataNode.getProperty(WayGraphConstants.METADATA_VERSION);
						}
						lastValidFrom = validFrom;
					}
				}
			}
			
			tx.success();
			return versionFound;
		}
	}

	@Override
	public IWayGraphVersionMetadata getCurrentWayGraphVersionMetadata(String graphName) {
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx()) {
			IWayGraphVersionMetadata metadata = getCurrentActiveWayGraphVersionMetadata(graphName);
			
			tx.success();
			return metadata;
		}
	}

	protected IWayGraphVersionMetadata getCurrentActiveWayGraphVersionMetadata(String graphName) {
		Set<State> states = new HashSet<>();
		states.add(State.ACTIVE);
		
		Node metadataNode = getCurrentWayGraphVersionMetadataNode(graphName, states);
		
		IWayGraphVersionMetadata metadata = null;
		if (metadataNode != null) {
			metadata = nodeMapper.map(metadataNode);
		}
		return metadata;
	}

	@Override
	public IWayGraphVersionMetadata getCurrentWayGraphVersionMetadataForView(String viewName) {
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx()) {
			Node metadataNode = null;
			boolean found = false;
			
			// get view node
			Node viewNode = viewDao.getViewNode(viewName);
			
			if (viewNode != null) {
				// get all related metadata nodes and select the current version
				Iterator<Relationship> rels = viewNode.getRelationships(WayGraphRelationshipType.GRAPH_VERSION).iterator();
				while (rels.hasNext() && !found) {
					metadataNode = rels.next().getEndNode();
					if (metadataNode.getProperty(WayGraphConstants.METADATA_STATE).equals(State.ACTIVE.name()) &&
						!metadataNode.hasProperty(WayGraphConstants.METADATA_VALID_TO)) {
						found = true;
					}
				}
			}
			
			IWayGraphVersionMetadata metadata = null;
			if (found) {
				metadata = nodeMapper.map(metadataNode);
			}
			
			tx.success();
			return metadata;
		}
	}

	@Override
	public IWayGraphVersionMetadata getWayGraphVersionMetadataForView(String viewName, String version) {
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx()) {
			Node metadataNode = null;
			boolean found = false;
			
			// get view node
			Node viewNode = viewDao.getViewNode(viewName);
			
			if (viewNode != null) {
				// get all related metadata nodes and select the current version
				Iterator<Relationship> rels = viewNode.getRelationships(WayGraphRelationshipType.GRAPH_VERSION).iterator();
				while (rels.hasNext() && !found) {
					metadataNode = rels.next().getEndNode();
					if (metadataNode.getProperty(WayGraphConstants.METADATA_VERSION).equals(version)) {
						found = true;
					}
				}
			}
			
			IWayGraphVersionMetadata metadata = null;
			if (found) {
				metadata = nodeMapper.map(metadataNode);
			}
			
			tx.success();
			return metadata;
		}
	}

	@Override
	public IWayGraphVersionMetadata getCurrentWayGraphVersionMetadata(String graphName, Set<State> states) {
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx()) {
			Node metadataNode = getCurrentWayGraphVersionMetadataNode(graphName, states);
			IWayGraphVersionMetadata metadata = null;
			if (metadataNode != null) {
				metadata = nodeMapper.map(metadataNode);
			}
			
			tx.success();
			return metadata;
		}
	}

	@Override
	public void deleteWayGraphVersionMetadata(String graphName, String version, boolean keepMetadata) {
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx()) {
			Node metadataNode = getMetadataNode(graphName, version);
			if (metadataNode != null) {
				if (keepMetadata) {
					metadataNode.setProperty(WayGraphConstants.METADATA_STATE, State.DELETED.name());
				} else {
					Iterable<Relationship> allRelationships = metadataNode.getRelationships();
				    for (Relationship relationship : allRelationships) {
				        relationship.delete();
				    }
				    metadataNode.delete();
				}
			}
			
			tx.success();
		}
	}

	public IWayGraphMetadataFactory getMetadataFactory() {
		return metadataFactory;
	}

	public void setMetadataFactory(IWayGraphMetadataFactory metadataFactory) {
		this.metadataFactory = metadataFactory;
	}

	public INeo4jNodeMapper<IWayGraphVersionMetadata> getNodeMapper() {
		return nodeMapper;
	}

	public void setNodeMapper(INeo4jNodeMapper<IWayGraphVersionMetadata> nodeMapper) {
		this.nodeMapper = nodeMapper;
	}

	public INeo4jWayGraphViewDao getViewDao() {
		return viewDao;
	}

	public void setViewDao(INeo4jWayGraphViewDao viewDao) {
		this.viewDao = viewDao;
	}

}
