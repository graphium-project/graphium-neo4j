/**
 * Graphium Neo4j - Module of Graphserver for Neo4j extension
 * Copyright © 2019 Salzburg Research Forschungsgesellschaft (graphium@salzburgresearch.at)
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
package at.srfg.graphium.neo4j.persistence.nodemapper.hd.impl;

import java.util.Map;

import org.neo4j.graphdb.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.io.ParseException;

import at.srfg.graphium.model.hd.IHDWaySegment;
import at.srfg.graphium.neo4j.model.WayGraphConstants;
import at.srfg.graphium.neo4j.persistence.impl.Neo4jWaySegmentHelperImpl;
import at.srfg.graphium.neo4j.persistence.nodemapper.impl.Neo4jWaySegmentMapper;

/**
 * @author mwimmer
 *
 */
public class Neo4jHDWaySegmentMapper<W extends IHDWaySegment> extends Neo4jWaySegmentMapper<W> {

	private static Logger log = LoggerFactory.getLogger(Neo4jHDWaySegmentMapper.class);
	
	@Override
	public W mapWithXInfoTypes(Node node, String... types) {
		W segment = super.mapWithXInfoTypes(node, types);

		Map<String, Object> properties = node.getAllProperties();
		
		if (properties.containsKey(WayGraphConstants.HDSEGMENT_LEFT_BORDER_STARTNODE_ID)) {
			segment.setLeftBorderStartNodeId((long) properties.get(WayGraphConstants.HDSEGMENT_LEFT_BORDER_STARTNODE_ID));
		}
		if (properties.containsKey(WayGraphConstants.HDSEGMENT_LEFT_BORDER_ENDNODE_ID)) {
			segment.setLeftBorderEndNodeId((long) properties.get(WayGraphConstants.HDSEGMENT_LEFT_BORDER_ENDNODE_ID));
		}
		if (properties.containsKey(WayGraphConstants.HDSEGMENT_LEFT_BORDER_GEOM)) {
			try {
				segment.setLeftBorderGeometry(Neo4jWaySegmentHelperImpl.encodeLineString(node, WayGraphConstants.HDSEGMENT_LEFT_BORDER_GEOM));
			} catch (ParseException e) {
				log.error("Could not parse geometry", e);
			}
		}
		
		if (properties.containsKey(WayGraphConstants.HDSEGMENT_RIGHT_BORDER_STARTNODE_ID)) {
			segment.setRightBorderStartNodeId((long) properties.get(WayGraphConstants.HDSEGMENT_RIGHT_BORDER_STARTNODE_ID));
		}
		if (properties.containsKey(WayGraphConstants.HDSEGMENT_RIGHT_BORDER_ENDNODE_ID)) {
			segment.setRightBorderEndNodeId((long) properties.get(WayGraphConstants.HDSEGMENT_RIGHT_BORDER_ENDNODE_ID));
		}
		if (properties.containsKey(WayGraphConstants.HDSEGMENT_RIGHT_BORDER_GEOM)) {
			try {
				segment.setRightBorderGeometry(Neo4jWaySegmentHelperImpl.encodeLineString(node, WayGraphConstants.HDSEGMENT_RIGHT_BORDER_GEOM));
			} catch (ParseException e) {
				log.error("Could not parse geometry", e);
			}
		}
		
		return segment;
	}
	
}