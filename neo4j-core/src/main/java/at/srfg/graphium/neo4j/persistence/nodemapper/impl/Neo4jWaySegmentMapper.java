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

import java.util.Map;

import org.neo4j.graphdb.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.io.ParseException;

import at.srfg.graphium.model.FormOfWay;
import at.srfg.graphium.model.FuncRoadClass;
import at.srfg.graphium.model.IWayGraphModelFactory;
import at.srfg.graphium.model.IWaySegment;
import at.srfg.graphium.neo4j.model.WayGraphConstants;
import at.srfg.graphium.neo4j.model.cache.SegmentCacheEntry;
import at.srfg.graphium.neo4j.persistence.impl.Neo4jWayGraphWriteDaoImpl;
import at.srfg.graphium.neo4j.persistence.impl.Neo4jWaySegmentHelperImpl;
import at.srfg.graphium.neo4j.persistence.nodemapper.INeo4jXInfoNodeMapper;
import at.srfg.graphium.neo4j.persistence.nodemapper.utils.Neo4jTagMappingUtils;
import at.srfg.graphium.neo4j.service.impl.STRTreeCacheManager;

/**
 * @author mwimmer
 *
 */
public class Neo4jWaySegmentMapper implements INeo4jXInfoNodeMapper<IWaySegment> {

	private static Logger log = LoggerFactory.getLogger(Neo4jWayGraphWriteDaoImpl.class);
	
	private IWayGraphModelFactory<IWaySegment> factory;
	private STRTreeCacheManager cache;
	
	@Override
	public IWaySegment map(Node node) {
		return this.mapWithXInfoTypes(node, null, null);
	}

	@Override
	public IWaySegment mapWithXInfoTypes(Node node, String graphName, String version, String... types) {
		IWaySegment segment = factory.newSegment();

		Map<String, Object> properties = node.getAllProperties();
		segment.setId((long) properties.get(WayGraphConstants.SEGMENT_ID));
		segment.setName((String) properties.get(WayGraphConstants.SEGMENT_NAME));
		if (properties.containsKey(WayGraphConstants.SEGMENT_MAXSPEED_TOW)) {
			segment.setMaxSpeedTow((short) properties.get(WayGraphConstants.SEGMENT_MAXSPEED_TOW));
		}
		if (properties.containsKey(WayGraphConstants.SEGMENT_MAXSPEED_BKW)) {
			segment.setMaxSpeedBkw((short) properties.get(WayGraphConstants.SEGMENT_MAXSPEED_BKW));
		}
		if (properties.containsKey(WayGraphConstants.SEGMENT_SPEED_CALC_TOW)) {
			segment.setSpeedCalcTow((short) properties.get(WayGraphConstants.SEGMENT_SPEED_CALC_TOW));
		}
		if (properties.containsKey(WayGraphConstants.SEGMENT_SPEED_CALC_BKW)) {
			segment.setSpeedCalcBkw((short) properties.get(WayGraphConstants.SEGMENT_SPEED_CALC_BKW));
		}
		if (properties.containsKey(WayGraphConstants.SEGMENT_LANES_TOW)) {
			segment.setLanesTow((short) properties.get(WayGraphConstants.SEGMENT_LANES_TOW));
		}
		if (properties.containsKey(WayGraphConstants.SEGMENT_LANES_BKW)) {
			segment.setLanesBkw((short) properties.get(WayGraphConstants.SEGMENT_LANES_BKW));
		}
		if (properties.containsKey(WayGraphConstants.SEGMENT_LENGTH)) {
			segment.setLength((float)(properties.get(WayGraphConstants.SEGMENT_LENGTH)));
		}
		if (properties.containsKey(WayGraphConstants.SEGMENT_FRC)) {
			segment.setFrc(FuncRoadClass.getFuncRoadClassForValue((short)properties.get(WayGraphConstants.SEGMENT_FRC)));
		}
		if (properties.containsKey(WayGraphConstants.SEGMENT_FOW)) {
			segment.setFormOfWay(FormOfWay.getFormOfWayForValue((short) properties.get(WayGraphConstants.SEGMENT_FOW)));
		}
		if (properties.containsKey(WayGraphConstants.SEGMENT_STREETTYPE)) {
			segment.setStreetType((String) properties.get(WayGraphConstants.SEGMENT_STREETTYPE));
		}
		if (properties.containsKey(WayGraphConstants.SEGMENT_WAY_ID)) {
			segment.setWayId((long) properties.get(WayGraphConstants.SEGMENT_WAY_ID));
		}
		if (properties.containsKey(WayGraphConstants.SEGMENT_STARTNODE_ID)) {
			segment.setStartNodeId((long) properties.get(WayGraphConstants.SEGMENT_STARTNODE_ID));
		}
		if (properties.containsKey(WayGraphConstants.SEGMENT_STARTNODE_INDEX)) {
			segment.setStartNodeIndex((int) properties.get(WayGraphConstants.SEGMENT_STARTNODE_INDEX));
		}
		if (properties.containsKey(WayGraphConstants.SEGMENT_ENDNODE_ID)) {
			segment.setEndNodeId((long) properties.get(WayGraphConstants.SEGMENT_ENDNODE_ID));
		}
		if (properties.containsKey(WayGraphConstants.SEGMENT_ENDNODE_INDEX)) {
			segment.setEndNodeIndex((int) properties.get(WayGraphConstants.SEGMENT_ENDNODE_INDEX));
		}
		if (properties.containsKey(WayGraphConstants.SEGMENT_TUNNEL)) {
			segment.setTunnel(true);
		} else {
			segment.setTunnel(false);
		}
		if (properties.containsKey(WayGraphConstants.SEGMENT_BRIDGE)) {
			segment.setBridge(true);
		} else {
			segment.setBridge(false);
		}
		if (properties.containsKey(WayGraphConstants.SEGMENT_URBAN)) {
			segment.setUrban(true);
		} else {
			segment.setUrban(false);
		}
		//		segment.setId((long) properties.get(WayGraphConstants.SEGMENT_TIMESTAMP));
		if (properties.containsKey(WayGraphConstants.SEGMENT_ACCESS_TOW)) {
			segment.setAccessTow(Neo4jWaySegmentHelperImpl.parseAccessTypes((byte[]) properties.get(WayGraphConstants.SEGMENT_ACCESS_TOW)));
		}
		if (properties.containsKey(WayGraphConstants.SEGMENT_ACCESS_BKW)) {
			segment.setAccessBkw(Neo4jWaySegmentHelperImpl.parseAccessTypes((byte[]) properties.get(WayGraphConstants.SEGMENT_ACCESS_BKW)));
		}

		if (properties.containsKey(WayGraphConstants.SEGMENT_GEOM)) {
			LineString geometry = null;
			if (graphName != null && version != null && cache != null) {
				SegmentCacheEntry cacheEntry = cache.getCacheEntryPerNodeId(graphName, version, node.getId());
				if (cacheEntry != null) {
					geometry = cacheEntry.getGeometry();
				}
			}
			if (geometry == null) {
				try {
					geometry = Neo4jWaySegmentHelperImpl.encodeLineString(node);
				} catch (ParseException e) {
					log.error("Could not parse geometry", e);
				}
			}
			if (geometry != null) {
				segment.setGeometry(geometry);
			}
		}

		this.setSegmentXInfos(segment, types);

		segment.setTags(Neo4jTagMappingUtils.mapTagProperties(node));

		return segment;
	}

	private void setSegmentXInfos(IWaySegment segment, String... types) {
		if (types != null && types.length > 0) {
			//TODO implement if wanted in this context or override
		}
	}

	public IWayGraphModelFactory<IWaySegment> getFactory() {
		return factory;
	}

	public void setFactory(IWayGraphModelFactory<IWaySegment> factory) {
		this.factory = factory;
	}

	public STRTreeCacheManager getCache() {
		return cache;
	}

	public void setCache(STRTreeCacheManager cache) {
		this.cache = cache;
	}

}
