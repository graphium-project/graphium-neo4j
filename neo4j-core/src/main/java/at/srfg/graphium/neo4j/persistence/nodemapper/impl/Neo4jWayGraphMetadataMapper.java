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
import java.util.Map;

import org.neo4j.graphdb.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKBReader;

import at.srfg.graphium.model.IWayGraphVersionMetadata;
import at.srfg.graphium.model.State;
import at.srfg.graphium.model.impl.WayGraphVersionMetadata;
import at.srfg.graphium.model.management.impl.Source;
import at.srfg.graphium.neo4j.model.WayGraphConstants;
import at.srfg.graphium.neo4j.persistence.impl.Neo4jWaySegmentHelperImpl;
import at.srfg.graphium.neo4j.persistence.nodemapper.INeo4jNodeMapper;

/**
 * @author mwimmer
 *
 */
public class Neo4jWayGraphMetadataMapper implements INeo4jNodeMapper<IWayGraphVersionMetadata> {

	private static Logger log = LoggerFactory.getLogger(Neo4jWayGraphMetadataMapper.class);
	
	@Override
	public IWayGraphVersionMetadata map(Node node) {
		IWayGraphVersionMetadata metadata = new WayGraphVersionMetadata();
		Map<String, Object> properties = node.getAllProperties();
		if (properties.containsKey(WayGraphConstants.METADATA_ACCESSTYPES)) {
			metadata.setAccessTypes(Neo4jWaySegmentHelperImpl.parseAccessTypes((byte[]) properties.get(WayGraphConstants.METADATA_ACCESSTYPES)));
		}
		if (properties.containsKey(WayGraphConstants.METADATA_CONNECTIONS_COUNT)) {
			metadata.setConnectionsCount((int) properties.get(WayGraphConstants.METADATA_CONNECTIONS_COUNT));
		}
		if (properties.containsKey(WayGraphConstants.METADATA_CREATION_TIMESTAMP)) {
			metadata.setCreationTimestamp(new Date((long) properties.get(WayGraphConstants.METADATA_CREATION_TIMESTAMP)));
		}
		if (properties.containsKey(WayGraphConstants.METADATA_CREATOR)) {
			metadata.setCreator((String) properties.get(WayGraphConstants.METADATA_CREATOR));
		}
		if (properties.containsKey(WayGraphConstants.METADATA_DESCRIPTION)) {
			metadata.setDescription((String) properties.get(WayGraphConstants.METADATA_DESCRIPTION));
		}
		if (properties.containsKey(WayGraphConstants.METADATA_GRAPHNAME)) {
			metadata.setGraphName((String) properties.get(WayGraphConstants.METADATA_GRAPHNAME));
		}
		if (properties.containsKey(WayGraphConstants.METADATA_ORIGIN_GRAPHNAME)) {
			metadata.setOriginGraphName((String) properties.get(WayGraphConstants.METADATA_ORIGIN_GRAPHNAME));
		}
		if (properties.containsKey(WayGraphConstants.METADATA_ORIGIN_VERSION)) {
			metadata.setOriginVersion((String) properties.get(WayGraphConstants.METADATA_ORIGIN_VERSION));
		}
		if (properties.containsKey(WayGraphConstants.METADATA_ORIGIN_URL)) {
			metadata.setOriginUrl((String) properties.get(WayGraphConstants.METADATA_ORIGIN_URL));
		}
		if (properties.containsKey(WayGraphConstants.METADATA_SEGMENTS_COUNT)) {
			metadata.setSegmentsCount((int) properties.get(WayGraphConstants.METADATA_SEGMENTS_COUNT));
		}
		if (properties.containsKey(WayGraphConstants.METADATA_SOURCE)) {
			metadata.setSource(new Source(0, (String) properties.get(WayGraphConstants.METADATA_SOURCE)));
		}
		if (properties.containsKey(WayGraphConstants.METADATA_STATE)) {
			metadata.setState(State.valueOf((String) properties.get(WayGraphConstants.METADATA_STATE)));
		}
		if (properties.containsKey(WayGraphConstants.METADATA_STORAGE_TIMESTAMP)) {
			metadata.setStorageTimestamp(new Date((long) properties.get(WayGraphConstants.METADATA_STORAGE_TIMESTAMP)));
		}
		
		// TODO => siehe Neo4jTagMappingUtils
		
//		if (properties.containsKey(WayGraphConstants.METADATA_TAGS)) {
//			metadata.setConnectionsCount((int) properties.get(WayGraphConstants.METADATA_TAGS));
//		}
		if (properties.containsKey(WayGraphConstants.METADATA_TYPE)) {
			metadata.setType((String) properties.get(WayGraphConstants.METADATA_TYPE));
		}
		if (properties.containsKey(WayGraphConstants.METADATA_VALID_FROM)) {
			metadata.setValidFrom(new Date((long) properties.get(WayGraphConstants.METADATA_VALID_FROM)));
		}
		if (properties.containsKey(WayGraphConstants.METADATA_VALID_TO)) {
			metadata.setValidTo(new Date((long) properties.get(WayGraphConstants.METADATA_VALID_TO)));
		}
		if (properties.containsKey(WayGraphConstants.METADATA_VERSION)) {
			metadata.setVersion((String) properties.get(WayGraphConstants.METADATA_VERSION));
		}
		if (properties.containsKey(WayGraphConstants.METADATA_COVERED_AREA)) {
			try {
				WKBReader wkbReader = new WKBReader();
				metadata.setCoveredArea((Polygon) wkbReader.read((byte[]) properties.get(WayGraphConstants.METADATA_COVERED_AREA)));
			} catch (ParseException e) {
				log.error("Could not parse covered area from metadata " + (metadata.getGraphName() == null ? "" : metadata.getGraphName()), e);
			}
		}

		return metadata;
	}

}