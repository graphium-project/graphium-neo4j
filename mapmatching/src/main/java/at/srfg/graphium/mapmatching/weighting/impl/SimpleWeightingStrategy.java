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

import java.util.Comparator;

import at.srfg.graphium.mapmatching.model.IMatchedBranch;
import at.srfg.graphium.mapmatching.model.IMatchedWaySegment;
import at.srfg.graphium.mapmatching.model.IWeightingStrategy;


/**
 * Original strategy which compares the paths by calculating
 * the distance between the track points and their assigned
 * street segments.
 */
public class SimpleWeightingStrategy implements IWeightingStrategy {
	
	private final Comparator<IMatchedBranch> comparator;
	
	private final double maxValidMatchedFactor;

	public SimpleWeightingStrategy(double minMatchingRadius) {
		this.comparator = new BranchComparator();
		
		this.maxValidMatchedFactor = 4 * minMatchingRadius; 
	}

	@Override
	public double calculateMatchedFactor(IMatchedBranch branch) {
		double totalMatchedFactor = 0;
		for (IMatchedWaySegment seg : branch.getMatchedWaySegments()) {
			if (seg.isStartSegment()) {
				totalMatchedFactor += seg.getMatchedFactor() * 0.5;
			} else {
				totalMatchedFactor += seg.getMatchedFactor();
			}
		}
		
		return totalMatchedFactor / branch.getMatchedPoints();
	}

	@Override
	public Comparator<IMatchedBranch> getComparator() {
		return comparator;
	}

	@Override
	public boolean branchIsValid(IMatchedBranch branch) {
		return branch.getMatchedFactor() <= getMaxValidMatchedFactor();
	}

	@Override
	public boolean branchIsValid(IMatchedBranch branch, int lastPartsToCheck) {
		return branchIsValid(branch);
	}

	@Override
	public double getMaxValidMatchedFactor() {
		return maxValidMatchedFactor;
	}

	protected final class BranchComparator implements Comparator<IMatchedBranch> {

		@Override
		public int compare(IMatchedBranch path1, IMatchedBranch path2) {		
			int matchedPointsPath1 = (path1.getNrOfTotalTrackPoints() - (
							(path1.getMatchedWaySegments().get(path1.getMatchedWaySegments().size()-1).getEndPointIndex() -
							 path1.getMatchedWaySegments().get(0).getStartPointIndex()) - path1.getMatchedPoints()));
			int matchedPointsPath2 = (path2.getNrOfTotalTrackPoints() - (
							(path2.getMatchedWaySegments().get(path2.getMatchedWaySegments().size()-1).getEndPointIndex() -
							 path2.getMatchedWaySegments().get(0).getStartPointIndex()) - path2.getMatchedPoints()));
			if (matchedPointsPath1 > matchedPointsPath2) {
				return -1;
			} else if (matchedPointsPath1 < matchedPointsPath2) {
				return 1;
			} else {
				return compareMatchedFactor(path1, path2);
			}
		}
		
		private int compareMatchedFactor(IMatchedBranch path1, IMatchedBranch path2) {
			if (path1.getMatchedFactor() < path2.getMatchedFactor()) {
				return -1;
			} else if (path1.getMatchedFactor() > path2.getMatchedFactor()) {
				return 1;
			} else {
				// both paths have the same matched-factor, prefer finished paths
				if (path1.isFinished() && path2.isFinished()) {
					// if both are finished, prefer the one with the fewest shortest path searches
					if (path1.getNrOfShortestPathSearches() < path2.getNrOfShortestPathSearches()) {
						return -1;
					} else if (path1.getNrOfShortestPathSearches() > path2.getNrOfShortestPathSearches()) {
						return 1;
					} else {
						// if both numbers of shortest path searches are equal, prefer the one with the maximum length
						if (path1.getLength() > path2.getLength()) {
							return -1;
						} else if (path1.getLength() < path2.getLength()) {
							return 1;
						} else {
							return 0;
						}
					}
				} else if (path1.isFinished()) {
					return -1;
				} else if (path2.isFinished()) {
					return 1;
				} else {
					return Double.compare(
							path1.getMatchedFactor(), 
							path2.getMatchedFactor());
				}
			}
		}
	}
	
}
