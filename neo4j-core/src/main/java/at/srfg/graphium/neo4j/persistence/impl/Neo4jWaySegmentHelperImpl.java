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

import java.util.HashSet;
import java.util.Set;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKBReader;
import com.vividsolutions.jts.io.WKBWriter;

import at.srfg.graphium.model.Access;
import at.srfg.graphium.model.IWaySegment;
import at.srfg.graphium.neo4j.model.WayGraphConstants;
import at.srfg.graphium.neo4j.persistence.INeo4jWaySegmentHelper;
import at.srfg.graphium.neo4j.persistence.nodemapper.utils.Neo4jTagMappingUtils;

/**
 * @author mwimmer
 */
public class Neo4jWaySegmentHelperImpl implements INeo4jWaySegmentHelper<IWaySegment> {
	
	private WKBWriter wkbWriter = new WKBWriter();
	private static WKBReader wkbReader = new WKBReader();
	
	@Override
	public Node createNode(GraphDatabaseService graphDb, IWaySegment segment, String graphVersionName) {
		Node node = graphDb.createNode(Label.label(createSegmentNodeLabel(graphVersionName)));
		updateNodeProperties(graphDb, segment, node);
		return node;
	}

	@Override
	public synchronized void updateNodeProperties(GraphDatabaseService graphDb, IWaySegment segment,
			Node node) {
		
		// AccessTypes dürfen nicht als HashSet gespeichert werden => short[]
		if (segment.getAccessBkw() != null) {
			node.setProperty(WayGraphConstants.SEGMENT_ACCESS_BKW, createAccessArray(segment.getAccessBkw()));
		}
		if (segment.getAccessTow() != null) {
			node.setProperty(WayGraphConstants.SEGMENT_ACCESS_TOW, createAccessArray(segment.getAccessTow()));
		}
		if (segment.isBridge()) {
			node.setProperty(WayGraphConstants.SEGMENT_BRIDGE, segment.isBridge());
		}
		node.setProperty(WayGraphConstants.SEGMENT_CURRENT_DURATION_TOW, segment.getDuration(true));
		node.setProperty(WayGraphConstants.SEGMENT_CURRENT_DURATION_BKW, segment.getDuration(false));
		node.setProperty(WayGraphConstants.SEGMENT_ENDNODE_ID, segment.getEndNodeId());
		node.setProperty(WayGraphConstants.SEGMENT_ENDNODE_INDEX, segment.getEndNodeIndex());
		if (segment.getFrc() != null) {
			node.setProperty(WayGraphConstants.SEGMENT_FRC, segment.getFrc().getValue());
		}
		if (segment.getFormOfWay() != null) {
			node.setProperty(WayGraphConstants.SEGMENT_FOW, segment.getFormOfWay().getValue());
		}
		node.setProperty(WayGraphConstants.SEGMENT_ID, segment.getId());
		node.setProperty(WayGraphConstants.SEGMENT_LANES_BKW, segment.getLanesBkw());
		node.setProperty(WayGraphConstants.SEGMENT_LANES_TOW, segment.getLanesTow());
		node.setProperty(WayGraphConstants.SEGMENT_LENGTH, segment.getLength());
		node.setProperty(WayGraphConstants.SEGMENT_MAXSPEED_BKW, segment.getMaxSpeedBkw());
		node.setProperty(WayGraphConstants.SEGMENT_MAXSPEED_TOW, segment.getMaxSpeedTow());
		node.setProperty(WayGraphConstants.SEGMENT_MIN_DURATION_TOW, segment.getMinDuration(true));
		node.setProperty(WayGraphConstants.SEGMENT_MIN_DURATION_BKW, segment.getMinDuration(false));
		if (segment.getName() != null) {
			node.setProperty(WayGraphConstants.SEGMENT_NAME, segment.getName());
		}
		if (segment.getSpeedCalcBkw() != null) {
			node.setProperty(WayGraphConstants.SEGMENT_SPEED_CALC_BKW, segment.getSpeedCalcBkw());
		}
		if (segment.getSpeedCalcTow() != null) {
			node.setProperty(WayGraphConstants.SEGMENT_SPEED_CALC_TOW, segment.getSpeedCalcTow());
		}
		node.setProperty(WayGraphConstants.SEGMENT_STARTNODE_ID, segment.getStartNodeId());
		node.setProperty(WayGraphConstants.SEGMENT_STARTNODE_INDEX, segment.getStartNodeIndex());
		if (segment.getStreetType() != null) {
			node.setProperty(WayGraphConstants.SEGMENT_STREETTYPE, segment.getStreetType());
		}
		if (segment.isTunnel()) {
			node.setProperty(WayGraphConstants.SEGMENT_TUNNEL, segment.isTunnel());
		}
		if (segment.isUrban()) {
			node.setProperty(WayGraphConstants.SEGMENT_URBAN, segment.isUrban());
		}
		if (segment.getId() != segment.getWayId()) {
			node.setProperty(WayGraphConstants.SEGMENT_WAY_ID, segment.getWayId());
		}
		if (segment.getGeometry() != null) {
			node.setProperty(WayGraphConstants.SEGMENT_GEOM, wkbWriter.write(segment.getGeometry()));
			Coordinate startCoord = segment.getGeometry().getCoordinates()[0];
			Coordinate endCoord = segment.getGeometry().getCoordinates()[segment.getGeometry().getCoordinates().length - 1];
			node.setProperty(WayGraphConstants.SEGMENT_START_X, startCoord.x);
			node.setProperty(WayGraphConstants.SEGMENT_START_Y, startCoord.y);
			node.setProperty(WayGraphConstants.SEGMENT_END_X, endCoord.x);
			node.setProperty(WayGraphConstants.SEGMENT_END_Y, endCoord.y);
		}
		if (segment.getTags() != null) {
			Neo4jTagMappingUtils.createTagProperties(node, segment.getTags(), WayGraphConstants.SEGMENT_TAG_PREFIX);
		}
	}

	/**
	 * @param accessTypes Set of access types 
	 * @return byte[] representing the ID of the requested access types
	 */
	public static byte[] createAccessArray(Set<Access> accessTypes) {
		byte[] accessIds = null;
		if (accessTypes != null) {
			accessIds = new byte[accessTypes.size()];
			int j = 0;
			for (Access access : accessTypes) {
				accessIds[j++] = (byte) access.getId();
			}
		}
		return accessIds;
	}
	
	/**
	 * @param accessTypes byte[] with IDs of access types 
	 * @return Set<Access> access types
	 */
	public static Set<Access> parseAccessTypes(byte[] accessTypes) {
		if (accessTypes != null) {
			int[] accesses = new int[accessTypes.length];
			for (int i=0; i<accessTypes.length; i++) {
				accesses[i] = accessTypes[i];
			}
			return Access.getAccessTypes(accesses);
		} else {
			return null;
		}
	}
	
	public static Set<Access> getAccessOverlappings(Set<Access> accesses1, Set<Access> accesses2) {
		Set<Access> overlappingAccesses = new HashSet<>();
		for (Access access1 : accesses1) {
			if (accesses2.contains(access1)) {
				overlappingAccesses.add(access1);
			}
		}
		return overlappingAccesses;
	}

	public static String createSegmentNodeLabel(String graphVersionName) {
		return WayGraphConstants.SEGMENT_LABEL + "_" + graphVersionName;
	}
	
	public static LineString encodeLineString(Node node) throws ParseException {
		return (LineString) wkbReader.read((byte[]) node.getProperty(WayGraphConstants.SEGMENT_GEOM));
	}
	
}
