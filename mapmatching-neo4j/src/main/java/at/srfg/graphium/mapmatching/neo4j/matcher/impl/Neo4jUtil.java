/**
 * Graphium Neo4j - Module of Graphserver for Map Matching using Neo4j
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
package at.srfg.graphium.mapmatching.neo4j.matcher.impl;

import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;

import at.srfg.graphium.neo4j.model.WaySegmentRelationshipType;
import at.srfg.graphium.neo4j.persistence.configuration.IGraphDatabaseProvider;

/**
 * @author mwimmer
 *
 */
public class Neo4jUtil {
	
	private IGraphDatabaseProvider graphDatabaseProvider;
	
	public TraversalDescription getTraverser() {
		return graphDatabaseProvider.getGraphDatabase().traversalDescription()
			.breadthFirst()
			.relationships(WaySegmentRelationshipType.SEGMENT_CONNECTION_ON_STARTNODE, org.neo4j.graphdb.Direction.OUTGOING)
			.relationships(WaySegmentRelationshipType.SEGMENT_CONNECTION_ON_ENDNODE, org.neo4j.graphdb.Direction.OUTGOING)
			.evaluator(Evaluators.toDepth(1))
			.evaluator(Evaluators.excludeStartPosition());
	}

	public TraversalDescription getTraverser(WaySegmentRelationshipType relationshipType) {
		return getTraverser(relationshipType, 1);
	}
		
	public TraversalDescription getTraverser(WaySegmentRelationshipType relationshipType, int nrOfHops) {
		return graphDatabaseProvider.getGraphDatabase().traversalDescription()
			.breadthFirst()
			.relationships(relationshipType, org.neo4j.graphdb.Direction.OUTGOING)
			.evaluator(Evaluators.excludeStartPosition())
			.evaluator(Evaluators.toDepth(nrOfHops));
	}

	public TraversalDescription getTraverser(WaySegmentRelationshipType relationshipType1, WaySegmentRelationshipType relationshipType2, int nrOfHops) {
		return graphDatabaseProvider.getGraphDatabase().traversalDescription()
			.breadthFirst()
			.relationships(relationshipType1, org.neo4j.graphdb.Direction.OUTGOING)
			.relationships(relationshipType2, org.neo4j.graphdb.Direction.OUTGOING)
			.evaluator(Evaluators.excludeStartPosition())
			.evaluator(Evaluators.toDepth(nrOfHops));
	}

	public IGraphDatabaseProvider getGraphDatabaseProvider() {
		return graphDatabaseProvider;
	}

	public void setGraphDatabaseProvider(IGraphDatabaseProvider graphDatabaseProvider) {
		this.graphDatabaseProvider = graphDatabaseProvider;
	}
	
}
