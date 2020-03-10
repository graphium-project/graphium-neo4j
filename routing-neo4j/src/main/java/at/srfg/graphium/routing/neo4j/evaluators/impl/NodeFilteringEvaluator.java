/**
 * Graphium Neo4j - Module of Graphium for routing services via Neo4j
 * Copyright © 2020 Salzburg Research Forschungsgesellschaft (graphium@salzburgresearch.at)
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
package at.srfg.graphium.routing.neo4j.evaluators.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;

/**
 *
 * @author mwimmer
 */
public class NodeFilteringEvaluator implements Evaluator {

	protected List<Predicate<? super Node>> nodeFilters = new ArrayList<>();
	protected List<Predicate<? super Relationship>> relationshipFilters = new ArrayList<>();
	
	public NodeFilteringEvaluator addNodeFilter(Predicate<? super Node> nodeFilter) {
		nodeFilters.add(nodeFilter);
		return this;
	}
	
	public NodeFilteringEvaluator addRelationshipFilter(Predicate<? super Relationship> relationshipFilter) {
		relationshipFilters.add(relationshipFilter);
		return this;
	}
	
	@Override
	public Evaluation evaluate(Path path) {
		Node endNode = path.endNode();
		Relationship lastRel = path.lastRelationship();
		
		for (Predicate<? super Node> nodeFilter : nodeFilters) {
			if (!nodeFilter.test(endNode)) {
				return Evaluation.EXCLUDE_AND_CONTINUE;
			}
		}
		
		if (lastRel != null) {
			for (Predicate<? super Relationship> relationshipFilter : relationshipFilters) {
				if (!relationshipFilter.test(lastRel)) {
					return Evaluation.EXCLUDE_AND_CONTINUE;
				}
			}
		}
		
		return Evaluation.INCLUDE_AND_CONTINUE;
	}

}
