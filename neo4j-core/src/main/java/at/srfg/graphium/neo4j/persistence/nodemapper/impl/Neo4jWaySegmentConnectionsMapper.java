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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import at.srfg.graphium.model.*;
import at.srfg.graphium.neo4j.persistence.nodemapper.INeo4jXInfoNodeMapper;
import at.srfg.graphium.neo4j.persistence.propertyhandler.IConnectionXInfoPropertyHandler;
import at.srfg.graphium.neo4j.persistence.propertyhandler.impl.ConnectionXInfoPropertyHandlerRegistry;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import at.srfg.graphium.neo4j.model.WayGraphConstants;
import at.srfg.graphium.neo4j.model.WaySegmentRelationshipType;
import at.srfg.graphium.neo4j.persistence.impl.Neo4jWaySegmentHelperImpl;

/**
 * @author mwimmer
 *
 */
public class Neo4jWaySegmentConnectionsMapper implements INeo4jXInfoNodeMapper<List<IWaySegmentConnection>> {

	private IWayGraphModelFactory<IWaySegment> factory;

	private ConnectionXInfoPropertyHandlerRegistry connectionXInfoPropertyHandlerRegistry;

	@Override
	public List<IWaySegmentConnection> map(Node node) {
		return this.mapWithXInfoTypes(node);
	}

	private void addConnectionXInfos(Relationship relationship, IWaySegmentConnection connection, String... types) {
		if (types != null && types.length > 0) {
			//retrieve and set connection xinfos
			List<IConnectionXInfo> connectionXInfos = new ArrayList<>();
			for (String type : types) {
				IConnectionXInfoPropertyHandler<? extends IConnectionXInfo> handler =  connectionXInfoPropertyHandlerRegistry.get(type);
				connectionXInfos.addAll(handler.getXInfoProperties(relationship));
			}
			//connection xinfos
			connection.setXInfo(connectionXInfos);
		}
	}

	public List<IWaySegmentConnection> mapWithXInfoTypes(Node node, String... types) {
		List<IWaySegmentConnection> connections = new ArrayList<>();
		Iterable<Relationship> relationships = node.getRelationships(Direction.OUTGOING,
				WaySegmentRelationshipType.SEGMENT_CONNECTION_ON_STARTNODE,
				WaySegmentRelationshipType.SEGMENT_CONNECTION_ON_ENDNODE);
		long fromSegmentId = (long) node.getProperty(WayGraphConstants.SEGMENT_ID);
		for (Relationship relationship : relationships) {
			Set<Access> accesses = null;
			if (relationship.getAllProperties().containsKey(WayGraphConstants.CONNECTION_ACCESS)) {
				accesses = Neo4jWaySegmentHelperImpl.parseAccessTypes((byte[]) relationship.getProperty(WayGraphConstants.CONNECTION_ACCESS));
			}
			IWaySegmentConnection connection = factory.newWaySegmentConnection((long) relationship.getProperty(WayGraphConstants.CONNECTION_NODE_ID),
					fromSegmentId,
					(long) relationship.getEndNode().getProperty(WayGraphConstants.SEGMENT_ID),
					accesses);
			this.addConnectionXInfos(relationship,connection,types);
			connections.add(connection);
		}

		if (connections.isEmpty()) {
			return null;
		} else {
			return connections;
		}
	}

	public ConnectionXInfoPropertyHandlerRegistry getConnectionXInfoPropertyHandlerRegistry() {
		return connectionXInfoPropertyHandlerRegistry;
	}

	public void setConnectionXInfoPropertyHandlerRegistry(ConnectionXInfoPropertyHandlerRegistry connectionXInfoPropertyHandlerRegistry) {
		this.connectionXInfoPropertyHandlerRegistry = connectionXInfoPropertyHandlerRegistry;
	}

	public IWayGraphModelFactory<IWaySegment> getFactory() {
		return factory;
	}

	public void setFactory(IWayGraphModelFactory<IWaySegment> factory) {
		this.factory = factory;
	}
}
