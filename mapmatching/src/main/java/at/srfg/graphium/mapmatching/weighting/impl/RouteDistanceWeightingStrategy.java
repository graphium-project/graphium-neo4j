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
package at.srfg.graphium.mapmatching.weighting.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;

import at.srfg.graphium.mapmatching.model.IMatchedBranch;
import at.srfg.graphium.mapmatching.model.IMatchedWaySegment;
import at.srfg.graphium.mapmatching.model.ITrack;
import at.srfg.graphium.mapmatching.properties.IMapMatchingProperties;
import at.srfg.graphium.mapmatching.weighting.distances.IDistanceCalculator;
import at.srfg.graphium.mapmatching.weighting.distances.impl.DistanceCalculator;
import at.srfg.graphium.mapmatching.weighting.distances.impl.MatchedPointDistanceCalculator;
import at.srfg.graphium.mapmatching.weighting.distances.impl.RouteDistanceCalculator;
import at.srfg.graphium.mapmatching.weighting.distances.impl.StraightLineDistanceCalculator;


/**
 * Weighting strategy which uses two different factors:
 * 
 * 	- 	the matched-point factor like {@link SimpleWeightingStrategy}, and
 * 	- 	the route-distance factor, that is the difference between the straight-line
 * 		distance between the track points and the route distance between the matched
 * 		points on the street segments.
 * 
 */
public class RouteDistanceWeightingStrategy
		extends SimpleWeightingStrategy {
	
	private static Logger log = LoggerFactory.getLogger(RouteDistanceWeightingStrategy.class);
	
	private final String ROUTING_MODE_CAR = "car";
	private final String ROUTING_MODE_BIKE = "bike";
	
	private final IMapMatchingProperties properties;
	
	private final DistanceCalculator routeDistanceCalculator;
	private final DistanceCalculator straightLineDistanceCalculator;
	private final MatchedPointDistanceCalculator matchedPointDistanceCalculator;

	private ITrack track = null;
	
	// empirisch ermittelt
	// TODO: sollte da nicht anders skaliert werden???
	private static final double MaxMatchedPointDistanceMeter = 14.75601603;
	private static final double MaxMatchedPointDistanceMeterLowSampling = 62.09845481;
	private double maxMatchedPointDistanceMeter;
		
	private static final double MaxRouteDistance = 0.439670898;

	private static final double WeightMatchedPointDistance = 0.6;
	private static final double WeightRouteDistance = 0.4;

	private static final double MaxValidMatchedFactor = 10.0;
	
	private static final double MaxValidRouteDistanceFactor = 10.0;
	
	private static final double SmallDistancesThresholdMeter = 75.0;

	private static final double PenaltyForPseudoSkippedParts = 5 * MaxMatchedPointDistanceMeterLowSampling;
	
	public RouteDistanceWeightingStrategy(ITrack track, IMapMatchingProperties properties) {
		super(Double.NaN);
		this.track = track;
		this.properties = properties;
		maxMatchedPointDistanceMeter = properties.getMaxMatchingRadiusMeter();
		
		this.routeDistanceCalculator = new RouteDistanceCalculator();
		this.straightLineDistanceCalculator = new StraightLineDistanceCalculator(track);
		this.matchedPointDistanceCalculator = new MatchedPointDistanceCalculator(PenaltyForPseudoSkippedParts);
	}
	
	@Override
	public boolean branchIsValid(IMatchedBranch branch) {
		return branchIsValid(branch, 10);
	}

	/**
	 * Checks if the matched factor for the whole path is not too bad and also
	 * checks if the weighting factors for the last {@code lastPartsToCheck} parts
	 * are not too high.
	 * 
	 * @param lastPartsToCheck Number of last parts to check.
	 */
	@Override
	public boolean branchIsValid(IMatchedBranch branch, int lastPartsToCheck) {
		List<Double> routeDistanceFactors = new ArrayList<Double>();
		List<Integer> segmentDistanceIndexes = new ArrayList<Integer>();
		List<Double> weightsForParts = getWeightsForParts(branch, routeDistanceFactors, segmentDistanceIndexes);
		
		double matchedFactor = calculateMatchedFactor(branch, weightsForParts);
		branch.setMatchedFactor(matchedFactor);
		
		int segIndex = 0;
		for (Integer i : segmentDistanceIndexes) {
			branch.getMatchedWaySegments().get(segIndex++).setWeight(weightsForParts.get(i));
		}
		
		if (matchedFactor > getMaxValidMatchedFactor()) {
			return false;
		}
		
		return lastPartsAreValid(routeDistanceFactors, lastPartsToCheck);
	}
	
	/**
	 * Checks if one of the last sections is a bad match.
	 * 
	 * If the route-distance factor of a section exceeds a certain threshold, 
	 * the branch is considered invalid.
	 */
	private boolean lastPartsAreValid(List<Double> routeDistanceFactors, int lastPartsToCheck) {
		int endIndex = Math.max(0, routeDistanceFactors.size() - lastPartsToCheck);
		
		for (int i = routeDistanceFactors.size() - 1; i >= endIndex; i--) {
			if (routeDistanceFactors.get(i) > MaxValidRouteDistanceFactor) {
				return false;
			}
		}
		
		return true;
	}

	@Override
	public double getMaxValidMatchedFactor() {
		return MaxValidMatchedFactor;
	}
	
	/**
	 * Calculates the matched-factor for a path.
	 * 
	 * First three different distances (straight-line distance, route distance
	 * and matched-point distance) are calculated for every path part (segments, routed empty segments).
	 * Then a matched-factor is calculated for every part and the weighted sum of these
	 * factors is returned.
	 */
	@Override
	public double calculateMatchedFactor(IMatchedBranch branch) {
		List<Double> weightsForParts = getWeightsForParts(branch, null, new ArrayList<Integer>());
		
		return calculateMatchedFactor(branch, weightsForParts);
	}
	
	private double calculateMatchedFactor(IMatchedBranch branch, List<Double> weightsForParts) {
		double totalWeight = sum(weightsForParts);
		return (totalWeight / branch.getMatchedPoints());
	}

	private List<Double> getWeightsForParts(IMatchedBranch branch, List<Double> routeDistanceFactors, List<Integer> segmentDistanceIndexes) {
		// calculate the different distances for path track part
		List<Double> routeDistances = calculateRouteDistances(branch, segmentDistanceIndexes);
		List<Double> straightLineDistances = calculateStraightLineDistances(branch, segmentDistanceIndexes);
		List<Double> matchedPointDistances = calculateMatchedPointDistances(branch, segmentDistanceIndexes);
		
		List<Double> weightsForParts = getWeightsForParts(
				routeDistances, straightLineDistances, matchedPointDistances, routeDistanceFactors);
		
		printDistances(branch.getMatchedWaySegments(), routeDistances, straightLineDistances, matchedPointDistances, weightsForParts);
		
		return weightsForParts;
	}

	private void printDistances(List<IMatchedWaySegment> segments, List<Double> routeDistances, List<Double> straightLineDistances,
			List<Double> matchedPointDistances, List<Double> weightsForParts) {
		if (log.isDebugEnabled()) {
			log.debug("routeDistances.size()=" + routeDistances.size() + ", straightLineDistances.size()=" + straightLineDistances.size() + 
					", matchedPointDistances.size()=" + matchedPointDistances.size());
			for (int i=0; i<routeDistances.size(); i++) {
				log.debug(routeDistances.get(i) + ", " + straightLineDistances.get(i) +	", " + matchedPointDistances.get(i) + ", " + weightsForParts.get(i));
			}
		}
	}

	public List<Double> getWeightsForParts(List<Double> routeDistances,
			List<Double> straightLineDistances, List<Double> matchedPointDistances, List<Double> routeDistanceFactors) {
		List<Double> weightsForParts = new ArrayList<Double>(routeDistances.size());
		
		for (int i = 0; i < routeDistances.size(); i++) {
			// for every path part, calculate a matched-factor
			double routeDistance = routeDistances.get(i);
			double straightLineDistance = straightLineDistances.get(i);
			double matchedPointDistance = matchedPointDistances.get(i);

			double routeDistanceFactorScaled = getRouteDistanceFactor(routeDistance, straightLineDistance);
			double matchedPointDistanceFactorScaled = getMatchedPointDistanceFactor(matchedPointDistance);
			
			double weightForPart = WeightRouteDistance * routeDistanceFactorScaled + 
					WeightMatchedPointDistance * matchedPointDistanceFactorScaled;
			
			weightsForParts.add(weightForPart);
			
			if (routeDistanceFactors != null) {
				routeDistanceFactors.add(routeDistanceFactorScaled);
			}
		}
		
		return weightsForParts;
	}

	public double getMatchedPointDistanceFactor(double matchedPointDistance) {
		double matchedPointDistanceFactorScaled;
				
		matchedPointDistanceFactorScaled = scale(matchedPointDistance, maxMatchedPointDistanceMeter);
		
		return matchedPointDistanceFactorScaled;
	}

	public double getRouteDistanceFactor(double routeDistance, double straightLineDistance) {
		double routeDistanceFactor;
		
		if (straightLineDistance == 0.0) {
			routeDistanceFactor = 0.0;
		} else {
			double distanceDifference = (straightLineDistance - routeDistance) / straightLineDistance;
			routeDistanceFactor = Math.abs(distanceDifference);
		}
		
		double routeDistanceFactorScaled = scale(routeDistanceFactor, MaxRouteDistance);
		
		if (routeDistanceFactorScaled > MaxValidRouteDistanceFactor
				&& (straightLineDistance + routeDistance) < SmallDistancesThresholdMeter) {
			/* for small distances that would have a high route-distance factor which would
			 * result in an invalid branch, use a lower default factor instead
			 */
			return 1.0;
		}
		
		return routeDistanceFactorScaled;
	}

	private double scale(double factor, double maxFactorValue) {
		return factor/maxFactorValue;
	}

	/**
	 * Calculates the distances for every path part (segments, routed empty segments)
	 * with the given {@link DistanceCalculator}.
	 */
	public <D> List<D> calculateDistances(IMatchedBranch branch, IDistanceCalculator<D> distanceCalculator,
										  List<Integer> segmentDistanceIndexes) {
		List<IMatchedWaySegment> segments = branch.getMatchedWaySegments();
		segmentDistanceIndexes.clear();
		
		if (segments.isEmpty()) {
			return Collections.emptyList();
		}
		
		List<D> distances = new ArrayList<D>();
		List<IMatchedWaySegment> previousSegments = new ArrayList<IMatchedWaySegment>();
		int i = 0;
		for (IMatchedWaySegment segment : segments) {
			i++;

			if (segment.isAfterSkippedPart()) {
				addPenaltyForPseudoSkippedParts(track, segment, previousSegments, distances, distanceCalculator);
				
				// if a part was skipped, do not try to calculate distances to the segments before the skipped part
				previousSegments.clear();
			}
			
			if (i < segments.size()) {
				// no penalty on last segment
				if (properties.getRoutingMode().equals(ROUTING_MODE_CAR)) {
					addPossiblePenaltyForSwitchedFrc(segment, previousSegments, distances, distanceCalculator);
				} else if (properties.getRoutingMode().equals(ROUTING_MODE_BIKE)) {
					addPossiblePenaltyForBikesAgainstOneWays(segment, previousSegments, distances, distanceCalculator);
					addPossiblePenaltyForBikesOnWayways(segment, distances, distanceCalculator);
				}
			}
			
			if (segment.getMatchedPoints() <= 0) {
				// remember empty segments, they will be considered when the next matched point is found
				if (!previousSegments.isEmpty()) {
					// skip empty segments at the beginning
					previousSegments.add(segment);
				}
				continue;
			}
			
			if (!previousSegments.isEmpty()) {
				// calculate the distance to the last matched point of previous segments
				previousSegments.add(segment);
				D distance = distanceCalculator.getRoutingSegmentsDistance(previousSegments, track);
				distances.add(distance);
				previousSegments.clear();
			}
			
			distanceCalculator.getSegmentPointsDistance(segment, distances);
			previousSegments.add(segment);
			segmentDistanceIndexes.add(distances.size() - 1);
		}
		
		if (previousSegments.size() > 1) {
			// also get distance values for empty segments at the end of the path
			distances.add(distanceCalculator.getDistanceForEmptyEndSegments(previousSegments));
		}
		
		return distances;
	}

	/**
	 * Calculates penalty for FRC switches.

	 * Paths with the same FRC on each segment will be rated better than paths with FRC switches.
	 * 
	 * @param segment
	 * @param previousSegments
	 * @param distances
	 * @param distanceCalculator
	 */
	private <D> void addPossiblePenaltyForSwitchedFrc(IMatchedWaySegment segment, List<IMatchedWaySegment> previousSegments,
			List<D> distances, IDistanceCalculator<D> distanceCalculator) {
		if (!previousSegments.isEmpty()) {
			IMatchedWaySegment previousSegment = previousSegments.get(previousSegments.size() - 1);
			distances.add(distanceCalculator.getPenaltyForSwitchedFrc(segment, previousSegment));
		}
	}
	
	/**
	 * Calculates penalty for bikes driving against one ways.

	 * @param segment
	 * @param previousSegments
	 * @param distances
	 * @param distanceCalculator
	 */
	private <D> void addPossiblePenaltyForBikesAgainstOneWays(IMatchedWaySegment segment, List<IMatchedWaySegment> previousSegments,
			List<D> distances, IDistanceCalculator<D> distanceCalculator) {
		if (!previousSegments.isEmpty()) {
			IMatchedWaySegment previousSegment = previousSegments.get(previousSegments.size() - 1);
			distances.add(distanceCalculator.getPenaltyForBikesAgainstOneWay(segment, previousSegment));
		}
	}

	/**
	 * Calculates penalty for bikes driving on walkways.

	 * @param segment
	 * @param distances
	 * @param distanceCalculator
	 */
	private <D> void addPossiblePenaltyForBikesOnWayways(IMatchedWaySegment segment, List<D> distances, IDistanceCalculator<D> distanceCalculator) {
		distances.add(distanceCalculator.getPenaltyForBikesOnWalkways(segment));
	}

	/**
	 * Assigns a penalty to 'pseudo' skipped parts. 'Pseudo' skipped parts
	 * are skipped parts where the end point index of the segment before the
	 * skipped part is the same as the start point index of the segment after
	 * the skipped part, that is no points were actually skipped.
	 * This mostly occurs when 'alternative path search' is applied to 'bad'
	 * segments to get back to the good path. Because skipped paths lead to a
	 * better matched-factor (the 'routeDistanceFactor' is better), a penalty is
	 * assigned to these kind of skipped paths so that good paths without the
	 * 'bad' segments have a better matched-factor.
	 */
	private <D> void addPenaltyForPseudoSkippedParts(ITrack track, IMatchedWaySegment segmentAfterSkippedPart, List<IMatchedWaySegment> previousSegments,
			List<D> distances, IDistanceCalculator<D> distanceCalculator) {
		if (!previousSegments.isEmpty()) {
			IMatchedWaySegment segmentBeforeSkippedPart = previousSegments.get(previousSegments.size() - 1);
			
			if (segmentAfterSkippedPart.getStartPointIndex() == segmentBeforeSkippedPart.getEndPointIndex()) {
				distances.add(distanceCalculator.getPenaltyForPseudoSkippedParts(segmentBeforeSkippedPart));
			}
		}
	}

	@VisibleForTesting
	public List<Double> calculateMatchedPointDistances(IMatchedBranch branch, List<Integer> segmentDistanceIndexes) {
		return calculateDistances(branch, matchedPointDistanceCalculator, segmentDistanceIndexes);
	}

	@VisibleForTesting
	public List<Double> calculateStraightLineDistances(IMatchedBranch branch, List<Integer> segmentDistanceIndexes) {
		return calculateDistances(branch, straightLineDistanceCalculator, segmentDistanceIndexes);
	}

	@VisibleForTesting
	public List<Double> calculateRouteDistances(IMatchedBranch branch, List<Integer> segmentDistanceIndexes) {
		return calculateDistances(branch, routeDistanceCalculator, segmentDistanceIndexes);
	}

	private double sum(List<Double> weightsForParts) {
		double totalWeight = 0;
		
		for (double weightForPart : weightsForParts) {
			totalWeight += weightForPart;
		}
		
		return totalWeight;
	}
	
}