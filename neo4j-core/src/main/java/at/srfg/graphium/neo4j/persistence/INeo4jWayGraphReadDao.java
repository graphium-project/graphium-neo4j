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
package at.srfg.graphium.neo4j.persistence;

import java.util.List;

import org.neo4j.graphdb.Node;

import com.vividsolutions.jts.geom.Point;

import at.srfg.graphium.core.persistence.IWayGraphReadDao;
import at.srfg.graphium.model.IWaySegment;
import at.srfg.graphium.neo4j.persistence.configuration.IGraphDatabaseProvider;

/**
 * @author mwimmer
 *
 */
public interface INeo4jWayGraphReadDao<W extends IWaySegment> extends IWayGraphReadDao<W> {

	/**
	 * @param graphName
	 * @param version
	 * @param segmentId
	 * @return
	 */
	Node getSegmentNodeBySegmentId(String graphName, String version, long segmentId);

	List<Node> findNearestNodes(String graphName, String version, Point referencePoint, double distance, int limit);

	/**
	 * @param graphName
	 * @param version
	 * @param referencePoint
	 * @param radiusInKm
	 * @param maxNrOfSegments
	 * @return
	 */
	List<Node> findNearestNodesWithOrthodromicDistance(String graphName, String version, Point referencePoint,
			double radiusInKm, int maxNrOfSegments);

	/**
	 * @param graphName
	 * @param version
	 * @param node
	 * @return
	 */
	W mapNode(String graphName, String version, Node node);
	
	/**
	 * @return <code>IGraphDatabaseProvider</code>
	 */
	IGraphDatabaseProvider getGraphDatabaseProvider();
	
}
