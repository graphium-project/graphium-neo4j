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
package at.srfg.graphium.mapmatching.model;

import java.util.List;

public interface IMatchedBranch extends Comparable<IMatchedBranch> {

	List<IMatchedWaySegment> getMatchedWaySegments();
	void addMatchedWaySegment(IMatchedWaySegment segment);
	void removeLastMatchedWaySegment();
	void removeMatchedWaySegments(List<Integer> segmentIndicesToRemove);
	
	void setWeightingStrategy(IWeightingStrategy weightingStrategy);
	
	int getStep();
	void incrementStep();
	
	double getMatchedFactor();
	void setMatchedFactor(double matchedFactor);

	int getMatchedPoints();
	
	void setFinished(boolean finished);
	boolean isFinished();
	
	int getNrOfUTurns();
	void setNrOfUTurns(int nrOfUTurns);
	void incrementNrOfUTurns();
	
	int getNrOfShortestPathSearches();
	void setNrOfShortestPathSearches(int nrOfShortestPathSearches);
	void incrementNrOfShortestPathSearches();
	
	double getLength();

	int getNrOfTotalTrackPoints();
	void setNrOfTotalTrackPoints(int nrOfTotalTrackPoints);

	/**
	 * Empty segments are segments which do not match a track point (short segments or segments calculated by shortest path search)
	 */
	int getNrOfEmptySegments();

	/**
	 * certainPathEndSegment defines the matched segment where the convergence point between all possible paths could be found.
	 * All possible paths show the same sub-path until this segment, after it sub-paths will be uncertain.
	 * certainPathEndSegment can be used for online map matching for identifying the first track point has to be sent to 
	 * the map matcher. 
	 */
	IMatchedWaySegment getCertainPathEndSegment();
	void setCertainPathEndSegment(IMatchedWaySegment certainPathEndSegment);

	Object clone() throws CloneNotSupportedException;
	void recalculate();

}