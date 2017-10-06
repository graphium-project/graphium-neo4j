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
package at.srfg.graphium.neo4j.persistence.nodemapper.utils;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.neo4j.graphdb.Node;

import at.srfg.graphium.neo4j.model.WayGraphConstants;

public class Neo4jTagMappingUtils {

	public static void createTagProperties(Node node, Map<String,String> tags, String prefix) {
		for (String key : tags.keySet()) {
			node.setProperty(prefix+key, tags.get(key));
		}
	}
	
	public static Map<String, String> mapTagProperties(Node node) {
		Map<String, String> tags = new HashMap<>();
		
		for (String key : node.getPropertyKeys()) {			
			if (key.startsWith(WayGraphConstants.SEGMENT_TAG_PREFIX)) {
				tags.put(StringUtils.removeStart(
						key, WayGraphConstants.SEGMENT_TAG_PREFIX), (String) node.getProperty(key));
			}
		}
		
		if (tags.isEmpty()) {
			tags = null;
		}
		return tags;
	}
	
}
