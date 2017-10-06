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
package at.srfg.graphium.neo4j.persistence.nodemapper.impl;

import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import at.srfg.graphium.model.management.ISubscription;
import at.srfg.graphium.model.management.ISubscriptionGroup;
import at.srfg.graphium.model.management.impl.Subscription;
import at.srfg.graphium.model.management.impl.SubscriptionGroup;
import at.srfg.graphium.neo4j.model.WayGraphConstants;
import at.srfg.graphium.neo4j.model.WayGraphRelationshipType;
import at.srfg.graphium.neo4j.persistence.nodemapper.INeo4jNodeMapper;

/**
 * @author mwimmer
 *
 */
public class Neo4jSubscriptionMapper implements INeo4jNodeMapper<ISubscription> {

	@Override
	public ISubscription map(Node node) {
		ISubscription subscription = new Subscription();
		Map<String, Object> properties = node.getAllProperties();
		if (properties.containsKey(WayGraphConstants.SUBSCRIPTION_PASSWORD)) {
			subscription.setPassword((String)properties.get(WayGraphConstants.SUBSCRIPTION_PASSWORD));
		}
		if (properties.containsKey(WayGraphConstants.SUBSCRIPTION_SERVER_NAME)) {
			subscription.setServerName((String)properties.get(WayGraphConstants.SUBSCRIPTION_SERVER_NAME));
		}
		if (properties.containsKey(WayGraphConstants.SUBSCRIPTION_TIMESTAMP)) {
			subscription.setTimestamp(new Date((Long)properties.get(WayGraphConstants.SUBSCRIPTION_TIMESTAMP)));
		}
		if (properties.containsKey(WayGraphConstants.SUBSCRIPTION_URL)) {
			subscription.setUrl((String)properties.get(WayGraphConstants.SUBSCRIPTION_URL));
		}
		if (properties.containsKey(WayGraphConstants.SUBSCRIPTION_USERNAME)) {
			subscription.setUser((String)properties.get(WayGraphConstants.SUBSCRIPTION_USERNAME));
		}
		if (properties.containsKey(WayGraphConstants.SUBSCRIPTION_VIEW_NAME)) {
			subscription.setViewName((String)properties.get(WayGraphConstants.SUBSCRIPTION_VIEW_NAME));
		}
		
		Iterator<Relationship> subscriptionGroupIt =  node.getRelationships(WayGraphRelationshipType.SUBSCRIPTION_GROUP).iterator();
		if (subscriptionGroupIt != null && subscriptionGroupIt.hasNext()) {
			Node subscriptionGroupNode = subscriptionGroupIt.next().getEndNode();
			ISubscriptionGroup subscriptionGroup = new SubscriptionGroup();
			Map<String, Object> subscriptionGroupProperties = subscriptionGroupNode.getAllProperties();
			subscriptionGroup.setName((String)subscriptionGroupProperties.get(WayGraphConstants.SUBSCRIPTION_GROUP_NAME));
			
			// TODO: set subscriptions if necessary
			
			subscription.setSubscriptionGroup(subscriptionGroup);
		}
		
		return subscription;
	}

}
