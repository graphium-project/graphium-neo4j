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
package at.srfg.graphium.neo4j.persistence.hd.impl;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;

import at.srfg.graphium.model.hd.IHDWaySegment;
import at.srfg.graphium.neo4j.model.WayGraphConstants;
import at.srfg.graphium.neo4j.persistence.impl.Neo4jWaySegmentHelperImpl;

/**
 * @author mwimmer
 *
 */
public class Neo4jHDWaySegmentHelperImpl<W extends IHDWaySegment> extends Neo4jWaySegmentHelperImpl<W> {
	
	@Override
	public synchronized void updateNodeProperties(GraphDatabaseService graphDb, W segment, Node node) {
		super.updateNodeProperties(graphDb, segment, node);
		node.setProperty(WayGraphConstants.HDSEGMENT_LEFT_BORDER_GEOM, wkbWriter.write(segment.getLeftBorderGeometry()));
		node.setProperty(WayGraphConstants.HDSEGMENT_LEFT_BORDER_STARTNODE_ID, segment.getLeftBorderStartNodeId());
		node.setProperty(WayGraphConstants.HDSEGMENT_LEFT_BORDER_ENDNODE_ID, segment.getLeftBorderEndNodeId());
		node.setProperty(WayGraphConstants.HDSEGMENT_RIGHT_BORDER_GEOM, wkbWriter.write(segment.getRightBorderGeometry()));
		node.setProperty(WayGraphConstants.HDSEGMENT_RIGHT_BORDER_STARTNODE_ID, segment.getRightBorderStartNodeId());
		node.setProperty(WayGraphConstants.HDSEGMENT_RIGHT_BORDER_ENDNODE_ID, segment.getRightBorderEndNodeId());
	}
	
}
