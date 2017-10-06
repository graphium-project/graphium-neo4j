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

import com.vividsolutions.jts.geom.Point;

import at.srfg.graphium.model.IWaySegment;

public interface IMatchedWaySegment extends IWaySegment {

	IWaySegment getSegment();
	void setSegment(IWaySegment segment);
	
	int getStartPointIndex();
	void setStartPointIndex(int index);
	
	int getEndPointIndex();
	void setEndPointIndex(int index);

	List<Double> getDistances();
	void calculateDistances(ITrack track);
	void removeLastDistance();
	
	double getMatchedFactor();
	
	double getWeight();
	void setWeight(double weight);
	
	int getMatchedPoints();

	/**
	 * The direction with which the segment was traversed.
	 */
	Direction getDirection();
	void setDirection(Direction direction);
	
	boolean isStartSegment();
	void setStartSegment(boolean startSegment);

	boolean isFromPathSearch();
	void setFromPathSearch(boolean fromPathSearch);
	
	boolean isUTurnSegment();
	void setUTurnSegment(boolean uTurnSegment);
	
	/**
	 * Indicates if the segment is the first segment after a skipped track part.
	 */
	boolean isAfterSkippedPart();
	void setAfterSkippedPart(boolean afterSkippedPart);
	
	Point getMatchedPoint(int trackPointIndex);
	double getDistance(int trackPointIndex);

	IDistancesCache getMatchedPointDistancesCache();
	IDistancesCache getStraightLineDistancesCache();
	IDistancesCache getRouteDistancesCache();
	
	public static interface IDistancesCache {
		IDistanceValueCache getSegmentPointsCache();
		IDistanceValueCache getRoutingSegmentsCache();
	}
	
	/**
	 * Container for a single cache value with a {@code key}.
	 */
	public static interface IDistanceValueCache {
		Double getValue();
		boolean hasValue(Object key);
		void put(Object key, Double value);
	}

	boolean isCertain();
	void setCertain(boolean certain);
}
