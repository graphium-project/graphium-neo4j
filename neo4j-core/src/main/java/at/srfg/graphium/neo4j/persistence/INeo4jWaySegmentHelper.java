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
package at.srfg.graphium.neo4j.persistence;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;

import at.srfg.graphium.model.IWaySegment;

/**
 * @author mwimmer
 */
public interface INeo4jWaySegmentHelper<T extends IWaySegment> {

	/**
	 * @param graphDb GraphDatabaseService
	 * @param encoder GeometryEncoder of spatial layer
	 * @param segment Way segment object
	 * @return newly created Node
	 */
	Node createNode(GraphDatabaseService graphDb, T segment, String graphVersionName);

	/**
	 * @param graphDb GraphDatabaseService
	 * @param encoder GeometryEncoder of spatial layer
	 * @param segment Way segment object
	 * @param node Node to update
	 */
	void updateNodeProperties(GraphDatabaseService graphDb, IWaySegment segment, Node node);

}
