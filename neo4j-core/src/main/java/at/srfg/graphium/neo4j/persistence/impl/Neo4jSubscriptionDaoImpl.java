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
import java.util.Iterator;
import java.util.List;

import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.srfg.graphium.core.persistence.ISubscriptionDao;
import at.srfg.graphium.model.impl.WayGraph;
import at.srfg.graphium.model.management.ISubscription;
import at.srfg.graphium.model.management.ISubscriptionGroup;
import at.srfg.graphium.model.management.impl.SubscriptionGroup;
import at.srfg.graphium.neo4j.model.WayGraphConstants;
import at.srfg.graphium.neo4j.model.WayGraphRelationshipType;
import at.srfg.graphium.neo4j.persistence.INeo4jWayGraphViewDao;
import at.srfg.graphium.neo4j.persistence.nodemapper.INeo4jNodeMapper;

/**
 * @author mwimmer
 *
 */
public class Neo4jSubscriptionDaoImpl extends AbstractNeo4jDaoImpl implements ISubscriptionDao {

	private static Logger log = LoggerFactory.getLogger(Neo4jSubscriptionDaoImpl.class);
	
	private INeo4jWayGraphViewDao viewDao;
	private INeo4jNodeMapper<ISubscription> nodeMapper;
	
	@Override
	public boolean subscribe(ISubscription subscription) {
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx()) {
			boolean insertOk = true;
			
			Label subscriptionLabel = Label.label(WayGraphConstants.SUBSCRIPTION_LABEL);
			Label subscriptionGroupLabel = Label.label(WayGraphConstants.SUBSCRIPTION_GROUP_LABEL);
			
			// lookup for subscriptionGroupNode for this graph name / view name
			Node subscriptionGroupNode = getGraphDatabase().findNode(subscriptionGroupLabel, WayGraphConstants.SUBSCRIPTION_GROUP_NAME, 
															subscription.getSubscriptionGroup().getName());
			
			// no subscriptionGroupNode found => create
			if (subscriptionGroupNode == null) {
				subscriptionGroupNode = getGraphDatabase().createNode(subscriptionGroupLabel);
				subscriptionGroupNode.setProperty(WayGraphConstants.SUBSCRIPTION_GROUP_NAME, subscription.getSubscriptionGroup().getName());
			}
			
			// lookup for subscriptionNode
			Node subscriptionNode = getSubscriptionNode(subscription.getViewName(), subscription.getServerName());
			
			if (subscriptionNode == null) {
				// no subscriptionGroupNode found => create new subscriptionNode
				subscriptionNode = getGraphDatabase().createNode(subscriptionLabel);
			}
			setSubscriptionProperties(subscriptionNode, subscription);
			
			// set relationships ...
			// subscriptionGroups <-> subscriptions
			if (!subscriptionNode.hasRelationship(WayGraphRelationshipType.SUBSCRIPTION_GROUP)) {
				subscriptionNode.createRelationshipTo(subscriptionGroupNode, WayGraphRelationshipType.SUBSCRIPTION_GROUP);
				subscriptionGroupNode.createRelationshipTo(subscriptionNode, WayGraphRelationshipType.SUBSCRIPTION);
			}
			
			// TODO: Wird die Relationship benötigt?
//			// subscriptions <-> views
//			if (!subscriptionNode.hasRelationship(WayGraphRelationshipType.SUBSCRIBED_VIEW)) {
//				Node viewNode = viewDao.getViewNode(subscription.getViewName());
//				if (viewNode != null) {
//					subscriptionNode.createRelationshipTo(viewNode, WayGraphRelationshipType.SUBSCRIBED_VIEW);
//				} else {
//					insertOk = false;
//				}
//			}
		
			tx.success();
			return insertOk;
		}
	}

	/**
	 * @param viewName
	 * @return
	 */
	private Node getSubscriptionNode(String viewName, String serverName) {
		Node subscriptionNode = null;
		ResourceIterator<Node> subscriptionNodesIterator = getGraphDatabase().findNodes(Label.label(WayGraphConstants.SUBSCRIPTION_LABEL), 
																WayGraphConstants.SUBSCRIPTION_VIEW_NAME, viewName);
		while (subscriptionNodesIterator.hasNext() && subscriptionNode == null) {
			Node subNode = subscriptionNodesIterator.next();
			if (subNode.getProperty(WayGraphConstants.SUBSCRIPTION_SERVER_NAME).equals(serverName)) {
				subscriptionNode = subNode;
			}
		}
		return subscriptionNode;
	}

	private void setSubscriptionProperties(Node subscriptionNode, ISubscription subscription) {
		subscriptionNode.setProperty(WayGraphConstants.SUBSCRIPTION_VIEW_NAME, subscription.getViewName());
		subscriptionNode.setProperty(WayGraphConstants.SUBSCRIPTION_GRAPH_NAME, subscription.getSubscriptionGroup().getGraph().getName());
		if (subscription.getUrl() != null) {
			subscriptionNode.setProperty(WayGraphConstants.SUBSCRIPTION_URL, subscription.getUrl());
		}
		if (subscription.getTimestamp() != null) {
			subscriptionNode.setProperty(WayGraphConstants.SUBSCRIPTION_TIMESTAMP, subscription.getTimestamp().getTime());
		}
		subscriptionNode.setProperty(WayGraphConstants.SUBSCRIPTION_SERVER_NAME, subscription.getServerName());
		if (subscription.getUser() != null) {
			subscriptionNode.setProperty(WayGraphConstants.SUBSCRIPTION_USERNAME, subscription.getUser());
		}
		if (subscription.getPassword() != null) {
			subscriptionNode.setProperty(WayGraphConstants.SUBSCRIPTION_PASSWORD, subscription.getPassword());
		}
	}
	
	@Override
	public boolean unsubscribe(String serverName, String graphName) {
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx()) {
			// lookup for subscriptionNode
			Node subscriptionNode = getSubscriptionNode(graphName, serverName);
			// lookup for subscriptionGroupNode
			Iterable<Relationship> subscriptionRelIt = subscriptionNode.getRelationships(WayGraphRelationshipType.SUBSCRIPTION);
			if (subscriptionRelIt != null && subscriptionRelIt.iterator().hasNext()) {
				Relationship subscriptionRel = subscriptionRelIt.iterator().next();
				// delete relationship to subscriptionGroupNode
				subscriptionRel.delete();
			}

			Iterable<Relationship> subscriptionGroupRelIt = subscriptionNode.getRelationships(WayGraphRelationshipType.SUBSCRIPTION_GROUP);
			if (subscriptionGroupRelIt != null && subscriptionGroupRelIt.iterator().hasNext()) {
				Relationship subscriptionGroupRel = subscriptionGroupRelIt.iterator().next();
				// delete relationship to subscriptionGroupNode
				subscriptionGroupRel.delete();
				
				// delete subscriptionGroupNode if there are no more subscriptions related
				Node subscriptionGroupNode = subscriptionGroupRel.getEndNode();
				if (!subscriptionGroupNode.getRelationships(WayGraphRelationshipType.SUBSCRIPTION).iterator().hasNext()) {
					Iterable<Relationship> subscriptionGroupOtherRelIt = subscriptionGroupNode.getRelationships();
					if (subscriptionGroupOtherRelIt != null) {
						for (Relationship rel : subscriptionGroupOtherRelIt) {
							rel.delete();
						}
					}
					subscriptionGroupNode.delete();
				}
			}
			
			subscriptionNode.delete();
		
			tx.success();
			return true;
		}
	}

	@Override
	public List<ISubscription> getSubscriptionsForGraph(String graphName) {
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx()) {
			List<ISubscription> subscriptionList = new ArrayList<>();
			ResourceIterator<Node> subscriptionNodesIterator = getGraphDatabase().findNodes(Label.label(WayGraphConstants.SUBSCRIPTION_LABEL), 
																	WayGraphConstants.SUBSCRIPTION_GRAPH_NAME, graphName);
			while (subscriptionNodesIterator.hasNext()) {
				Node subNode = subscriptionNodesIterator.next();
				subscriptionList.add(nodeMapper.map(subNode));
			}
	
			tx.success();
			return subscriptionList;
		}
	}

	@Override
	public List<ISubscription> getSubscriptionsForGraphAndServer(String serverName, String graphName) {
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx()) {
			List<ISubscription> subscriptions = new ArrayList<>();
			ResourceIterator<Node> subscriptionNodesIterator = getGraphDatabase().findNodes(Label.label(WayGraphConstants.SUBSCRIPTION_LABEL), 
																	WayGraphConstants.SUBSCRIPTION_GRAPH_NAME, graphName);
			while (subscriptionNodesIterator.hasNext()) {
				Node subNode = subscriptionNodesIterator.next();
				if (subNode.getProperty(WayGraphConstants.SUBSCRIPTION_SERVER_NAME).equals(serverName)) {
					subscriptions.add(nodeMapper.map(subNode));
				}
			}
			
			tx.success();
			return subscriptions;
		}
	}

	@Override
	public List<ISubscription> getSubscriptionsForView(String viewName) {
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx()) {
			// wird derzeit nicht verwendet
			List<ISubscription> subscriptionList = new ArrayList<>();
			ResourceIterator<Node> subscriptionNodesIterator = getGraphDatabase().findNodes(Label.label(WayGraphConstants.SUBSCRIPTION_LABEL), 
																	WayGraphConstants.SUBSCRIPTION_VIEW_NAME, viewName);
			while (subscriptionNodesIterator.hasNext()) {
				Node subNode = subscriptionNodesIterator.next();
				subscriptionList.add(nodeMapper.map(subNode));
			}
	
			tx.success();
			return subscriptionList;
		}
	}

	@Override
	public ISubscription getSubscriptionForViewAndServer(String serverName, String viewName) {
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx()) {
			ISubscription subscription = null;	
			Node subscriptionNode = getSubscriptionNode(viewName, serverName);
			if (subscriptionNode != null) {
				subscription = nodeMapper.map(subscriptionNode);
			}
			
			tx.success();
			return subscription;
		}
	}

	@Override
	public List<ISubscription> getSubscriptionsForGraph(String graphName, String groupName) {
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx()) {
			List<ISubscription> subscriptions = new ArrayList<>();
			ResourceIterator<Node> subscriptionNodesIterator = getGraphDatabase().findNodes(Label.label(WayGraphConstants.SUBSCRIPTION_LABEL), 
																	WayGraphConstants.SUBSCRIPTION_GRAPH_NAME, graphName);
			while (subscriptionNodesIterator.hasNext()) {
				Node subNode = subscriptionNodesIterator.next();
				Iterable<Relationship> subscriptionGroupRelIt = subNode.getRelationships(WayGraphRelationshipType.SUBSCRIPTION_GROUP);
				if (subscriptionGroupRelIt != null && subscriptionGroupRelIt.iterator().hasNext()) {
					if (subscriptionGroupRelIt.iterator().next().getEndNode().getAllProperties().get(WayGraphConstants.SUBSCRIPTION_GROUP_NAME).equals(groupName)) {
						subscriptions.add(nodeMapper.map(subNode));
					}
				}
			}
			
			tx.success();
			return subscriptions;
		}
	}

	@Override
	public List<ISubscription> getAllSubscriptions() {
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx()) {
			List<ISubscription> subscriptions = new ArrayList<>();
			ResourceIterator<Node> subscriptionNodesIterator = getGraphDatabase().findNodes(Label.label(WayGraphConstants.SUBSCRIPTION_LABEL));
			while (subscriptionNodesIterator.hasNext()) {
				subscriptions.add(nodeMapper.map(subscriptionNodesIterator.next()));
			}

			tx.success();
			return subscriptions;
		}
	}

	@Override
	public ISubscriptionGroup getSubscriptionGroup(String groupName) {
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx()) {
			ResourceIterator<Node> subscriptionNodesIterator = getGraphDatabase().findNodes(Label.label(WayGraphConstants.SUBSCRIPTION_GROUP_LABEL), 
																	WayGraphConstants.SUBSCRIPTION_GROUP_NAME, groupName);
			ISubscriptionGroup subscriptionGroup = null;
			if (subscriptionNodesIterator.hasNext()) {
				Node sgNode = subscriptionNodesIterator.next();
				Iterable<Relationship> relationShips = sgNode.getRelationships(WayGraphRelationshipType.SUBSCRIPTION);
				List<ISubscription> subscriptions = null;
				if (relationShips != null) {
					subscriptions = new ArrayList<>();
					Iterator<Relationship> itRel = relationShips.iterator();
					while (itRel.hasNext()) {
						subscriptions.add(nodeMapper.map(itRel.next().getEndNode()));
					}
					if (subscriptions != null) {
						subscriptionGroup = new SubscriptionGroup(0, groupName, 
															  	  new WayGraph(0, (String) subscriptions.get(0).getViewName()), 
															  	  subscriptions);
					}
				}
			}
			
			tx.success();
			
			return subscriptionGroup;
		}
	}

	public INeo4jWayGraphViewDao getViewDao() {
		return viewDao;
	}

	public void setViewDao(INeo4jWayGraphViewDao viewDao) {
		this.viewDao = viewDao;
	}

	public INeo4jNodeMapper<ISubscription> getNodeMapper() {
		return nodeMapper;
	}

	public void setNodeMapper(INeo4jNodeMapper<ISubscription> nodeMapper) {
		this.nodeMapper = nodeMapper;
	}

}
