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
package at.srfg.graphium.mapmatching.model.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.common.annotations.VisibleForTesting;

import at.srfg.graphium.mapmatching.model.IMatchedBranch;
import at.srfg.graphium.mapmatching.model.IMatchedWaySegment;
import at.srfg.graphium.mapmatching.model.IWeightingStrategy;

public class MatchedBranchImpl implements IMatchedBranch, Cloneable, Serializable {

	private static final long serialVersionUID = -5050363598919572570L;

	private List<IMatchedWaySegment> segments = new ArrayList<IMatchedWaySegment>();
	private int step = 0;
	private boolean finished = false;
	private int nrOfUTurns = 0;
	private int nrOfShortestPathSearches;
	private double length = 0;
	private double matchedFactor = 0;
	private boolean matchedFactorValid = false;
	private int nrOfTotalTrackPoints = 0; 		// optional; number of total trackpoints, not matched trackpoints
	private boolean nrOfTotalTrackPointsValid = false;
	private int matchedPoints = 0;
	private boolean matchedPointsValid = false;
	private int endPointIndexLastStep = 0;
	private IMatchedWaySegment certainPathEndSegment = null;
	
	private IWeightingStrategy weightingStrategy;
	
	public MatchedBranchImpl(IWeightingStrategy weightingStrategy) {
		this.weightingStrategy = weightingStrategy;
	}
	
	@Override
	public void addMatchedWaySegment(IMatchedWaySegment segment) {
		segments.add(segment);
		length += segment.getLength();
		matchedFactorValid = false;
		matchedPointsValid = false;
		nrOfTotalTrackPointsValid = false;
	}

	@Override
	public void removeLastMatchedWaySegment() {
		removeMatchedWaySegment(segments.size() - 1);
	}

	private void removeMatchedWaySegment(int i) {
		IMatchedWaySegment segmentToRemove = segments.get(i);
		segments.remove(i);
//		step--;
		length -= segmentToRemove.getGeometry().getLength();
		matchedFactorValid = false;
		matchedPointsValid = false;
		nrOfTotalTrackPointsValid = false;
		updateEndPointIndexLastStep();
	}

	@Override
	public void removeMatchedWaySegments(List<Integer> segmentIndicesToRemove) {
		// remove the segments with the given indices starting with the highest index
		Collections.sort(segmentIndicesToRemove);
		Collections.reverse(segmentIndicesToRemove);
		
		for (int segmentIndex : segmentIndicesToRemove) {
			removeMatchedWaySegment(segmentIndex);
		}
	}

	@Override
	public void setMatchedFactor(double matchedFactor) {
		this.matchedFactor = matchedFactor;
		this.matchedFactorValid = true;
	}

	@Override
	public double getMatchedFactor() {
		if (!matchedFactorValid) {
			calcMatchedFactor();
			matchedFactorValid = true;
		}
		return this.matchedFactor;
	}

	private void calcMatchedFactor() {
		this.matchedFactor = weightingStrategy.calculateMatchedFactor(this);
	}

	@Override
	public int getMatchedPoints() {
		if (!matchedPointsValid) {
			matchedPoints = 0;
			for (IMatchedWaySegment seg : segments) {
				matchedPoints += seg.getMatchedPoints();
			}
			matchedPointsValid = true;
		}
		return matchedPoints;
	}

	@Override
	public List<IMatchedWaySegment> getMatchedWaySegments() {
		matchedFactorValid = false;
		matchedPointsValid = false;
		nrOfTotalTrackPointsValid = false;
		return segments;
	}

	public void setSegments(List<IMatchedWaySegment> segments) {
		matchedFactorValid = false;
		matchedPointsValid = false;
		nrOfTotalTrackPointsValid = false;
		this.segments = segments;
		
		for (IMatchedWaySegment seg : segments) {
			length += seg.getGeometry().getLength();
		}
		
		updateEndPointIndexLastStep();

	}

	@Override
	public int getStep() {
		return step;
	}

	@Override
	public void incrementStep() {
		if (getNrOfTotalTrackPoints() != endPointIndexLastStep) {
			step++;
			updateEndPointIndexLastStep();
		}
	}

	private void updateEndPointIndexLastStep() {
		endPointIndexLastStep = getNrOfTotalTrackPoints();
	}
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		MatchedBranchImpl cloneObj = (MatchedBranchImpl)super.clone();
		List<IMatchedWaySegment> clonedSegs = new ArrayList<IMatchedWaySegment>(segments.size());
		for (IMatchedWaySegment seg : segments) {
			clonedSegs.add((IMatchedWaySegment) seg.clone());
		}
		cloneObj.setSegments(clonedSegs);
		return cloneObj;
	}

	@Override
	public boolean isFinished() {
		return finished;
	}

	@Override
	public void setFinished(boolean finished) {
		this.finished = finished;
	}

	@Override
	public int getNrOfUTurns() {
		return nrOfUTurns;
	}

	@Override
	public void setNrOfUTurns(int nrOfUTurns) {
		this.nrOfUTurns = nrOfUTurns;
	}

	@Override
	public void incrementNrOfUTurns() {
		nrOfUTurns++;
	}

	@Override
	public int getNrOfShortestPathSearches() {
		return nrOfShortestPathSearches;
	}

	@Override
	public void incrementNrOfShortestPathSearches() {
		nrOfShortestPathSearches++;
	}

	@Override
	public void setNrOfShortestPathSearches(int nrOfShortestPathSearches) {
		this.nrOfShortestPathSearches = nrOfShortestPathSearches;
	}
	
	/**
	 * Empty segments are segments which do not match a track point (short segments or segments calculated by shortest path search)
	 * @return
	 */
	@Override
	public int getNrOfEmptySegments() {
		int emptySegs = 0;
		for (IMatchedWaySegment seg : segments) {
			if (seg.getStartPointIndex() == seg.getEndPointIndex()) {
				emptySegs++;
			}
		}
		return emptySegs;
	}

	@Override
	public int compareTo(IMatchedBranch o) {
		return weightingStrategy.getComparator().compare(this, o);
	}

	@Override
	public double getLength() {
		return length;
	}
	
	@VisibleForTesting
	public IWeightingStrategy getWeightingStrategy() {
		return weightingStrategy;
	}

	@Override
	public void setWeightingStrategy(IWeightingStrategy weightingStrategy) {
		this.weightingStrategy = weightingStrategy;
		matchedFactorValid = false;
	}

	@Override
	public int getNrOfTotalTrackPoints() {
		if (!nrOfTotalTrackPointsValid) {
			if (segments != null && !segments.isEmpty()) {
				nrOfTotalTrackPoints = segments.get(segments.size()-1).getEndPointIndex();
			} else {
				nrOfTotalTrackPoints = 0;
			}
			nrOfTotalTrackPointsValid = true;
		}
		return nrOfTotalTrackPoints;
	}

	@Override
	public void setNrOfTotalTrackPoints(int nrOfTotalTrackPoints) {
		this.nrOfTotalTrackPoints = nrOfTotalTrackPoints;
		nrOfTotalTrackPointsValid = true;
	}

	@Override
	public IMatchedWaySegment getCertainPathEndSegment() {
		return certainPathEndSegment;
	}

	@Override
	public void setCertainPathEndSegment(IMatchedWaySegment certainPathEndSegment) {
		this.certainPathEndSegment = certainPathEndSegment;
	}

	@Override
	public void recalculate() {
		matchedFactorValid = false;
		matchedPointsValid = false;
		nrOfTotalTrackPointsValid = false;
	}

}