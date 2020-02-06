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
package at.srfg.graphium.neo4j.persistence;

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
	
	/**
	 * Builds default traversal description 
	 * @return Traversal description
	 */
	public TraversalDescription getTraverser() {
		return graphDatabaseProvider.getGraphDatabase().traversalDescription()
			.breadthFirst()
			.relationships(WaySegmentRelationshipType.SEGMENT_CONNECTION_ON_STARTNODE, org.neo4j.graphdb.Direction.OUTGOING)
			.relationships(WaySegmentRelationshipType.SEGMENT_CONNECTION_ON_ENDNODE, org.neo4j.graphdb.Direction.OUTGOING)
			.relationships(WaySegmentRelationshipType.SEGMENT_CONNECTION_WITHOUT_NODE, org.neo4j.graphdb.Direction.OUTGOING)
			.evaluator(Evaluators.toDepth(1))
			.evaluator(Evaluators.excludeStartPosition());
	}
	
	/**
	 * Builds default traversal description 
	 * @param allDirections If true, traverses through incoming and outgoing relationships. If false, traverses through outgoing relationships only
	 * @return Traversal description
	 */
	public TraversalDescription getTraverser(boolean allDirections) {
		if (allDirections) {
			return graphDatabaseProvider.getGraphDatabase().traversalDescription()
				.breadthFirst()
				.relationships(WaySegmentRelationshipType.SEGMENT_CONNECTION_ON_STARTNODE, org.neo4j.graphdb.Direction.BOTH)
				.relationships(WaySegmentRelationshipType.SEGMENT_CONNECTION_ON_ENDNODE, org.neo4j.graphdb.Direction.BOTH)
				.relationships(WaySegmentRelationshipType.SEGMENT_CONNECTION_WITHOUT_NODE, org.neo4j.graphdb.Direction.BOTH)
				.evaluator(Evaluators.toDepth(1))
				.evaluator(Evaluators.excludeStartPosition());
		} else {
			return getTraverser();
		}
	}

	/**
	 * Builds traversal description
	 * @param relationshipType
	 * @return Traversal description
	 */
	public TraversalDescription getTraverser(WaySegmentRelationshipType relationshipType) {
		return getTraverser(relationshipType, 1);
	}
	
	/**
	 * Builds traversal description
	 * @param relationshipType
	 * @param allDirections If true, traverses through incoming and outgoing relationships. If false, traverses through outgoing relationships only
	 * @return Traversal description
	 */
	public TraversalDescription getTraverser(WaySegmentRelationshipType relationshipType, boolean allDirections) {
		return getTraverser(relationshipType, 1, allDirections);
	}
	
	/**
	 * Builds traversal description
	 * @param relationshipType
	 * @param nrOfHops number of traversal steps
	 * @return Traversal description
	 */
	public TraversalDescription getTraverser(WaySegmentRelationshipType relationshipType, int nrOfHops) {
		return graphDatabaseProvider.getGraphDatabase().traversalDescription()
			.breadthFirst()
			.relationships(relationshipType, org.neo4j.graphdb.Direction.OUTGOING)
			.evaluator(Evaluators.excludeStartPosition())
			.evaluator(Evaluators.toDepth(nrOfHops));
	}
	
	/**
	 * Builds traversal description
	 * @param relationshipType
	 * @param nrOfHops number of traversal steps
	 * @param allDirections If true, traverses through incoming and outgoing relationships. If false, traverses through outgoing relationships only
	 * @return Traversal description
	 */
	public TraversalDescription getTraverser(WaySegmentRelationshipType relationshipType, int nrOfHops, boolean allDirections) {
		if (allDirections) {
			return graphDatabaseProvider.getGraphDatabase().traversalDescription()
				.breadthFirst()
				.relationships(relationshipType, org.neo4j.graphdb.Direction.BOTH)
				.evaluator(Evaluators.excludeStartPosition())
				.evaluator(Evaluators.toDepth(nrOfHops));
		} else {
			return getTraverser(relationshipType, nrOfHops);
		}
	}

	/**
	 * Builds traversal description
	 * @param relationshipType1
	 * @param relationshipType2
	 * @param nrOfHops number of traversal steps
	 * @return Traversal description
	 */
	public TraversalDescription getTraverser(WaySegmentRelationshipType relationshipType1, WaySegmentRelationshipType relationshipType2, int nrOfHops) {
		return graphDatabaseProvider.getGraphDatabase().traversalDescription()
			.breadthFirst()
			.relationships(relationshipType1, org.neo4j.graphdb.Direction.OUTGOING)
			.relationships(relationshipType2, org.neo4j.graphdb.Direction.OUTGOING)
			.evaluator(Evaluators.excludeStartPosition())
			.evaluator(Evaluators.toDepth(nrOfHops));
	}

	/**
	 * Builds traversal description
	 * @param relationshipType1
	 * @param relationshipType2
	 * @param nrOfHops number of traversal steps
	 * @param allDirections If true, traverses through incoming and outgoing relationships. If false, traverses through outgoing relationships only
	 * @return Traversal description
	 */
	public TraversalDescription getTraverser(WaySegmentRelationshipType relationshipType1, WaySegmentRelationshipType relationshipType2, int nrOfHops, boolean allDirections) {
		if (allDirections) {
			return graphDatabaseProvider.getGraphDatabase().traversalDescription()
				.breadthFirst()
				.relationships(relationshipType1, org.neo4j.graphdb.Direction.BOTH)
				.relationships(relationshipType2, org.neo4j.graphdb.Direction.BOTH)
				.evaluator(Evaluators.excludeStartPosition())
				.evaluator(Evaluators.toDepth(nrOfHops));
		} else {
			return getTraverser(relationshipType1, relationshipType2, nrOfHops);
		}
	}

	public IGraphDatabaseProvider getGraphDatabaseProvider() {
		return graphDatabaseProvider;
	}

	public void setGraphDatabaseProvider(IGraphDatabaseProvider graphDatabaseProvider) {
		this.graphDatabaseProvider = graphDatabaseProvider;
	}
	
}
