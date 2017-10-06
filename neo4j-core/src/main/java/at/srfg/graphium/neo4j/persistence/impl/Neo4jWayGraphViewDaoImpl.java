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
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.NotImplementedException;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.io.WKBWriter;

import at.srfg.graphium.core.exception.GraphViewNotExistsException;
import at.srfg.graphium.model.IWayGraph;
import at.srfg.graphium.model.view.IWayGraphView;
import at.srfg.graphium.model.view.impl.WayGraphView;
import at.srfg.graphium.neo4j.model.WayGraphConstants;
import at.srfg.graphium.neo4j.model.WayGraphRelationshipType;
import at.srfg.graphium.neo4j.persistence.INeo4jWayGraphViewDao;
import at.srfg.graphium.neo4j.persistence.nodemapper.INeo4jNodeMapper;

/**
 * @author mwimmer
 *
 */
public class Neo4jWayGraphViewDaoImpl extends AbstractNeo4jDaoImpl implements INeo4jWayGraphViewDao {

	private static Logger log = LoggerFactory.getLogger(Neo4jWayGraphViewDaoImpl.class);
	
	private INeo4jNodeMapper<IWayGraphView> nodeMapper;
	
	@Override
	public List<IWayGraphView> getViewsForGraph(String graphName) {
		List<IWayGraphView> views = null;
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx()) {
			ResourceIterator<Node> viewNodes = getGraphDatabase().findNodes(Label.label(WayGraphConstants.VIEW_LABEL), WayGraphConstants.VIEW_GRAPH_NAME, graphName);
			if (viewNodes != null && viewNodes.hasNext()) {
				views = new ArrayList<>();
				do {
					views.add(nodeMapper.map(viewNodes.next()));
				} while (viewNodes.hasNext());
			}
			tx.success();
			return views;
		}
	}

	@Override
	public void saveView(IWayGraphView view) {
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx()) {
			// create new node for view
			Node viewNode = getGraphDatabase().createNode(Label.label(WayGraphConstants.VIEW_LABEL));
			Date now = Calendar.getInstance().getTime();
			viewNode.setProperty(WayGraphConstants.VIEW_CONNECTIONS_COUNT, view.getConnectionsCount());
			if (view.getCoveredArea() != null) {
				WKBWriter wkbWriter = new WKBWriter();
				viewNode.setProperty(WayGraphConstants.VIEW_COVERED_AREA, wkbWriter.write(view.getCoveredArea()));
			}
			viewNode.setProperty(WayGraphConstants.VIEW_CREATION_TIMESTAMP, now.getTime());
			viewNode.setProperty(WayGraphConstants.VIEW_GRAPH_NAME, view.getGraph().getName());
			viewNode.setProperty(WayGraphConstants.VIEW_NAME, view.getViewName());
			viewNode.setProperty(WayGraphConstants.VIEW_SEGMENTS_COUNT, view.getSegmentsCount());
			// TODO: find appropriate structure for Map<String, String>
			//viewNode.setProperty(WayGraphConstants.VIEW_TAGS, view.getConnectionsCount());
	
			// read metadata nodes for graph name (using index) and create relationships
			createRelationshipsToMetadataNodes(viewNode, view.getGraph().getName());
			
			tx.success();
		}
	}

	private void createRelationshipsToMetadataNodes(Node viewNode, String graphName) {
		ResourceIterator<Node> nodeIterator = getGraphDatabase().findNodes(Label.label(WayGraphConstants.METADATA_LABEL), 
				WayGraphConstants.METADATA_GRAPHNAME, graphName);
		Node metadataNode = null;
		if (nodeIterator.hasNext()) {
			Iterable<Relationship> viewRels = viewNode.getRelationships(WayGraphRelationshipType.GRAPH_VERSION);
			Set<Node> viewRelatedMetadataNodes = new HashSet<Node>();
			for (Relationship rel : viewRels) {
				viewRelatedMetadataNodes.add(rel.getEndNode());
			}
			
			// create relationship from view node to metadata node
			while (nodeIterator.hasNext()) {
				metadataNode = nodeIterator.next();
				if (!viewRelatedMetadataNodes.contains(metadataNode)) {
					viewNode.createRelationshipTo(metadataNode, WayGraphRelationshipType.GRAPH_VERSION);
				}
			}
			nodeIterator.close();
		} else {
			nodeIterator.close();
			throw new RuntimeException("Error in save process: metadata node has to be persisted first");
		}
		nodeIterator.close();
	}

	@Override
	public void saveDefaultView(IWayGraph wayGraph) {
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx()) {
			IWayGraphView view = new WayGraphView(wayGraph.getName(), wayGraph, null, true, null, 0, 0, null);
			
			// TODO: read segmentsCount from metadata
			
			// check if default view already exists
			if (!defaultViewExists(wayGraph)) {
				saveView(view);
			} else {
				Node viewNode = getGraphDatabase().findNode(Label.label(WayGraphConstants.VIEW_LABEL), WayGraphConstants.VIEW_NAME, view.getViewName());
				if (viewNode != null) {
					// read metadata nodes for graph name (using index) and create relationships - only for non related metadata node (new metadata)
					createRelationshipsToMetadataNodes(viewNode, view.getGraph().getName());
				} else {
					log.error("Could not create relationship between default view and new metadata");
				}
			}
			
			tx.success();
		}
		
	}

	private boolean defaultViewExists(IWayGraph wayGraph) {
		return getViewNode(wayGraph.getName()) != null;
	}

	@Override
	public Node getViewNode(String viewName) {
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx()) {
			Node node = getGraphDatabase().findNode(Label.label(WayGraphConstants.VIEW_LABEL), WayGraphConstants.VIEW_NAME, viewName);
			
			tx.success();
			return node;
		}
	}
	
	@Override
	public boolean viewExists(String viewName) {
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx()) {
			Node viewNode = getViewNode(viewName);
			return viewNode != null;			
		}
	}
	
	@Override
	public IWayGraphView getView(String viewName) throws GraphViewNotExistsException {
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx()) {
			Node viewNode = getViewNode(viewName);
			IWayGraphView view = null;
			if (viewNode == null) {
				String msg = "View with name " + viewName + " does not exist";
				throw new GraphViewNotExistsException(msg, viewName);
			}
			view = nodeMapper.map(viewNode);
			tx.success();
			return view;
		}
	}

	@Override
	public int getSegmentsCount(IWayGraphView view, String graphVersion) {
		throw new NotImplementedException("");
	}

	@Override
	public boolean isDefaultView(String viewName) {
		// TODO: implement me
		throw new NotImplementedException("");
	}
	
	public INeo4jNodeMapper<IWayGraphView> getNodeMapper() {
		return nodeMapper;
	}

	public void setNodeMapper(INeo4jNodeMapper<IWayGraphView> nodeMapper) {
		this.nodeMapper = nodeMapper;
	}

	@Override
	public String getViewDefinition(IWayGraphView view) {
		return null;
	}

}