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

import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKBReader;

import at.srfg.graphium.model.impl.WayGraph;
import at.srfg.graphium.model.view.IWayGraphView;
import at.srfg.graphium.model.view.impl.WayGraphView;
import at.srfg.graphium.neo4j.model.WayGraphConstants;
import at.srfg.graphium.neo4j.persistence.nodemapper.INeo4jNodeMapper;

/**
 * @author mwimmer
 *
 */
public class Neo4jWayGraphViewMapper implements INeo4jNodeMapper<IWayGraphView> {

	private static Logger log = LoggerFactory.getLogger(Neo4jWayGraphViewMapper.class);

	@Override
	public IWayGraphView map(Node node) {
		IWayGraphView view = new WayGraphView();
		Map<String, Object> properties = node.getAllProperties();
		if (properties.containsKey(WayGraphConstants.VIEW_CONNECTIONS_COUNT)) {
			view.setConnectionsCount((int) properties.get(WayGraphConstants.VIEW_CONNECTIONS_COUNT));
		}
//		if (properties.containsKey(WayGraphConstants.VIEW_FILTER)) {
//			view.setFilter((String) properties.get(WayGraphConstants.VIEW_FILTER));
//		}
		if (properties.containsKey(WayGraphConstants.VIEW_GRAPH_NAME)) {
			view.setGraph(new WayGraph(0, (String) properties.get(WayGraphConstants.VIEW_GRAPH_NAME)));
		}
		if (properties.containsKey(WayGraphConstants.VIEW_NAME)) {
			view.setViewName((String) properties.get(WayGraphConstants.VIEW_NAME));
		}
		if (properties.containsKey(WayGraphConstants.VIEW_SEGMENTS_COUNT)) {
			view.setSegmentsCount((int) properties.get(WayGraphConstants.VIEW_SEGMENTS_COUNT));
		}
		if (properties.containsKey(WayGraphConstants.VIEW_COVERED_AREA)) {
			try {
				WKBReader wkbReader = new WKBReader();
				view.setCoveredArea((Polygon) wkbReader.read((byte[]) properties.get(WayGraphConstants.VIEW_COVERED_AREA)));
			} catch (ParseException e) {
				log.error("Could not parse covered area from view " + (view.getViewName() == null ? "" : view.getViewName()));
			}
			view.setConnectionsCount((int) properties.get(WayGraphConstants.VIEW_CONNECTIONS_COUNT));
		}
//		if (properties.containsKey(WayGraphConstants.VIEW_TAGS)) {
//			view.setConnectionsCount((int) properties.get(WayGraphConstants.VIEW_CONNECTIONS_COUNT));
//		}
		return view;
	}

}