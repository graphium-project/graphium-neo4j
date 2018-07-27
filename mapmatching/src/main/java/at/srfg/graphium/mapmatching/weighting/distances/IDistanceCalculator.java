/**
 * Graphium Neo4j - Map Matching module of Graphium
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
package at.srfg.graphium.mapmatching.weighting.distances;

import java.util.List;

import at.srfg.graphium.mapmatching.model.IMatchedWaySegment;
import at.srfg.graphium.mapmatching.model.ITrack;

public interface IDistanceCalculator<T> {

	T getPenaltyForPseudoSkippedParts(IMatchedWaySegment segment);

	T getRoutingSegmentsDistance(List<IMatchedWaySegment> previousSegments, ITrack track);

	T getDistanceForEmptyEndSegments(List<IMatchedWaySegment> previousSegments);
	
	void getSegmentPointsDistance(IMatchedWaySegment segment, List<T> distances);

	T getPenaltyForSwitchedFrc(IMatchedWaySegment segment, IMatchedWaySegment previousSegment);

	T getPenaltyForBikesAgainstOneWay(IMatchedWaySegment segment, IMatchedWaySegment previousSegment);

	T getPenaltyForBikesOnWalkways(IMatchedWaySegment segment);

}
