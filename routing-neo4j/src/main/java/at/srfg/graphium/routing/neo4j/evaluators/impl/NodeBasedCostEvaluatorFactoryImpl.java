/**
 * Graphium Neo4j - Module of Graphium for routing services via Neo4j
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
 * (C) 2012 Salzburg Research Forschungsgesellschaft m.b.H.
 *
 * All rights reserved.
 *
 * @author anwagner
 **/
package at.srfg.graphium.routing.neo4j.evaluators.impl;

import org.neo4j.graphalgo.CostEvaluator;

import com.vividsolutions.jts.geom.Coordinate;

import at.srfg.graphium.neo4j.service.impl.STRTreeCacheManager;
import at.srfg.graphium.routing.model.IRoutingOptions;
import at.srfg.graphium.routing.neo4j.evaluators.INeo4jCostEvaluatorFactory;

public class NodeBasedCostEvaluatorFactoryImpl implements INeo4jCostEvaluatorFactory {

	@Override
	public CostEvaluator<Double> createCostEvaluator(STRTreeCacheManager cache, IRoutingOptions options) {	
		return new NodeBasedCostEvaluator(options.getGraphName(), options.getGraphVersion(), cache, options.getCriteria().getValue());
	}
	
	@Override
	public CostEvaluator<Double> createCoordAwareCostEvaluator(STRTreeCacheManager cache, IRoutingOptions options,
			long startNodeId, long endNodeId, Coordinate startCoord, Coordinate endCoord) {	
		return new CoordAwareNodeBasedCostEvaluator(options.getGraphName(), options.getGraphVersion(), cache, options.getCriteria().getValue(), 
				startNodeId, endNodeId, startCoord, endCoord);
	}
	
}
