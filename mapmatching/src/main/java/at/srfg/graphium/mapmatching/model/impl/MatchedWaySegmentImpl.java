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
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.operation.distance.DistanceOp;

import at.srfg.graphium.geomutils.GeometryUtils;
import at.srfg.graphium.mapmatching.model.Direction;
import at.srfg.graphium.mapmatching.model.IMatchedWaySegment;
import at.srfg.graphium.mapmatching.model.ITrack;
import at.srfg.graphium.model.Access;
import at.srfg.graphium.model.FormOfWay;
import at.srfg.graphium.model.FuncRoadClass;
import at.srfg.graphium.model.ISegmentXInfo;
import at.srfg.graphium.model.IWaySegment;
import at.srfg.graphium.model.IWaySegmentConnection;
import at.srfg.graphium.model.OneWay;

public class MatchedWaySegmentImpl implements IMatchedWaySegment, Cloneable, Serializable {

	private static final long serialVersionUID = 1248829342095910035L;

	protected static final Logger log = LoggerFactory.getLogger(MatchedWaySegmentImpl.class);
	
	private IWaySegment segment;
	private int startPointIndex = 0;	// inclusive
	private int endPointIndex = 0;		// exclusive
	
	private List<Double> distances = new ArrayList<Double>();
	private List<Point> matchedPoints = new ArrayList<Point>();
	
	private Direction direction;
	private boolean startSegment = false;
	private boolean fromPathSearch = false;
	private boolean uTurnSegment = false;
	private boolean afterSkippedPart = false;
	private double weight;
	private boolean certain;
	
	private DistancesCache matchedPointDistanceCache;
	private DistancesCache straightLineDistanceCache;
	private DistancesCache routeDistanceCache;

	public MatchedWaySegmentImpl() {
		this(null);
	}
	
	public MatchedWaySegmentImpl(IWaySegment segment) {
		this(segment, false);
	}

	public MatchedWaySegmentImpl(IWaySegment segment, boolean startSegment) {
		this.startSegment = startSegment;
		this.segment = segment;
		
		this.matchedPointDistanceCache = new DistancesCache();
		this.straightLineDistanceCache = new DistancesCache();
		this.routeDistanceCache = new DistancesCache();
	}

	public double getMatchedFactor() {
		double totalMatchedFactor = 0;
		
		if (distances.isEmpty()) {
			throw new RuntimeException("distances were not initialized correctly for segment " + toString());
		} else {
			for (Double d : distances) {
				totalMatchedFactor += d;
			}
			totalMatchedFactor = totalMatchedFactor / distances.size();
		}
		
		return totalMatchedFactor;
	}

	@Override
	public void removeLastDistance() {
		if (distances.size() > 0) {
			distances.remove(distances.size() - 1);
			
			if (getMatchedPoints() > 0) {
				matchedPoints.remove(matchedPoints.size() - 1);
			}
		} else {
			throw new IndexOutOfBoundsException("trying to remove the last element from an empty 'distances' list");
		}
	}

	/**
	 *  Calculates the distances of the given segment to its assigned points and
	 *  also stores the matched points (on the segment).
	 */
	@Override
	public void calculateDistances(ITrack track) {
		List<Double> newDistances = new ArrayList<Double>();
		List<Point> newMatchedPoints = new ArrayList<Point>();
		
		if (getStartPointIndex() == getEndPointIndex()) {
			newDistances.add(getDistanceForEmptySegment(track));
		} else {
			for (int i = getStartPointIndex(); i < getEndPointIndex(); i++) {
				
				newDistances.add(GeometryUtils.distanceMeters(getGeometry(), track.getTrackPoints().get(i).getPoint()));

				// TODO the inaccurate distance calculation is still used to find the matched points,
				// it would be better to reproject the geometries and then use DistanceOp
				DistanceOp distanceOp = new DistanceOp(getGeometry(), track.getTrackPoints().get(i).getPoint());
				Coordinate matchedCoordinate = distanceOp.nearestLocations()[0].getCoordinate();
				Point matchedPoint = GeometryUtils.createPoint(matchedCoordinate, getGeometry().getSRID());
				newMatchedPoints.add(matchedPoint);
			}
		}
		
		this.distances = newDistances;
		this.matchedPoints = newMatchedPoints;
	}

	/**
	 * Calculates the dummy/penalty distance for empty/short segments.
	 * 
	 * For large gaps (when the time span between the last matched point and
	 * the next point is greater than 15 seconds), 0 is assigned as distance.
	 * Otherwise routes that are close to the connection line, between the point
	 * before and after the gap, might get chosen. Even though these routes are
	 * not the fastest ones.
	 * 
	 * For short gaps, the idea is that empty segments are close to the track and often have
	 * a similar shape. A sub-track around the current position is built and then
	 * the distance to the segment end points is calculated. The minimum of these
	 * two distances is returned.
	 * 
	 * @param track
	 * @return
	 */
	private Double getDistanceForEmptySegment(ITrack track) {
		
		if (isShortGap(track)) {
			final int lastPointIndex = track.getTrackPoints().size() - 1;

			// build a sub-track with 2 points before and after the current position
			int indexFrom, indexTo;
			if (getStartPointIndex() <= 0) {
				indexFrom = 0;
				indexTo = Math.min(indexFrom + 2, lastPointIndex);
			} else if (getStartPointIndex() >= lastPointIndex) {
				indexTo = lastPointIndex;
				indexFrom = Math.max(indexTo - 2, 0);
			} else {
				indexFrom = Math.max(getStartPointIndex() - 2, 0);
				indexTo = Math.min(getStartPointIndex() + 2, lastPointIndex);
			}
			
			LineString trackSubLine = getSubTrack(indexFrom, indexTo, track);
			double distanceStartPoint = GeometryUtils.distanceMeters(trackSubLine, getGeometry().getStartPoint());
			double distanceEndPoint = GeometryUtils.distanceMeters(trackSubLine, getGeometry().getEndPoint());

			return Math.min(distanceStartPoint, distanceEndPoint);
		} else {
			return 0.0;
		}
	}
	
	/**
	 * For empty segments: Returns true, if the time difference between the last matched
	 * point and the next point is less than 15 seconds.
	 */
	private boolean isShortGap(ITrack track) {
		int pointBeforeGap = Math.max(0, getStartPointIndex() - 1);
		int pointAfterGap = Math.min(getEndPointIndex(), track.getTrackPoints().size() - 1);
		
		if (pointBeforeGap == pointAfterGap) {
			return true;
		}
		
		Date timeBeforeGap = track.getTrackPoints().get(pointBeforeGap).getTimestamp();
		Date timeAfterGap = track.getTrackPoints().get(pointAfterGap).getTimestamp();
		
		long differenceInSec = (timeAfterGap.getTime() - timeBeforeGap.getTime()) / 1000;
		
		return differenceInSec < 15;
	}

	/**
	 * Returns a sub-linestring of the track containing all track points
	 * between {@code indexFrom} and {@code indexTo}.
	 * 
	 * Note that the linestring is built directly with the track points
	 * instead of calling 'GeoHelper.subGeometry(track.getLineString(), ...)'. This
	 * is because the track linestring might have less points.
	 */
	private LineString getSubTrack(int indexFrom, int indexTo, ITrack track) {
		Coordinate[] coords = new Coordinate[indexTo + 1 - indexFrom];
		
		int indexCoords = 0;
		for (int index = indexFrom; index <= indexTo; index++) {
			coords[indexCoords] = track.getTrackPoints().get(index).getPoint().getCoordinate();
			indexCoords++;
		}
		
		return GeometryUtils.createLineString(coords, track.getLineString().getSRID());
	}

	@Override
	public Point getMatchedPoint(int trackPointIndex) {
		int internalIndex = trackPointIndex - getStartPointIndex();
		
		if (internalIndex < 0 || internalIndex >= matchedPoints.size()) {
			throw new IndexOutOfBoundsException("can not get the matched point for index " + trackPointIndex
					+ " at segment " + toString());
		}
		
		return matchedPoints.get(internalIndex);
	}

	@Override
	public double getDistance(int trackPointIndex) {
		int internalIndex = trackPointIndex - getStartPointIndex();
		
		if (internalIndex < 0 || internalIndex >= distances.size()) {
			throw new IndexOutOfBoundsException("can not get the distance for index " + trackPointIndex
					+ " at segment " + toString());
		}
		
		return distances.get(internalIndex);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof IMatchedWaySegment) {
			int startIndexMax = Math.max(this.getStartPointIndex(), ((IMatchedWaySegment)obj).getStartPointIndex());
			int endIndexMin = Math.min(this.getEndPointIndex(), ((IMatchedWaySegment)obj).getEndPointIndex());
			
			if (((IMatchedWaySegment)obj).getId() == this.getId() && 
				((IMatchedWaySegment)obj).getDirection().equals(this.getDirection()) &&
				startIndexMax <= endIndexMin) {	// fuzzy index compare => one segment could be match twice (UTurn); in this case indices are different
				return true;
			}
		}
		return false;
	}

	public Object clone() {
		try {
			MatchedWaySegmentImpl clonedSeg = (MatchedWaySegmentImpl) super.clone();
			clonedSeg.setSegment((IWaySegment) segment.clone());
			clonedSeg.distances = new ArrayList<Double>(distances);
			clonedSeg.matchedPoints = new ArrayList<Point>(matchedPoints);
			
			clonedSeg.matchedPointDistanceCache = matchedPointDistanceCache.clone();
			clonedSeg.straightLineDistanceCache = straightLineDistanceCache.clone();
			clonedSeg.routeDistanceCache = routeDistanceCache.clone();
			
			return clonedSeg;
		} catch (CloneNotSupportedException e) {
			log.error("error while cloning geometry", e);
		}
		return null;
	}
	
	@Override
	public String toString() {
		if (segment != null) {
			return "MatchedWaySegmentImpl{id=" + segment.getId() + ", dir= " + direction + ", " 
					+ startPointIndex + "-" + endPointIndex + "}";
		} else {
			return super.toString();
		}
	}
	
	public boolean isAfterSkippedPart() {
		return afterSkippedPart;
	}

	public IWaySegment getSegment() {
		return segment;
	}

	public void setSegment(IWaySegment segment) {
		this.segment = segment;
	}

	public int getStartPointIndex() {
		return startPointIndex;
	}

	public long getId() {
		return segment.getId();
	}

	public void setId(long id) {
		segment.setId(id);
	}

	public short getMaxSpeedTow() {
		return segment.getMaxSpeedTow();
	}

	public LineString getGeometry() {
		return segment.getGeometry();
	}

	public List<ISegmentXInfo> getXInfo() {
		return segment.getXInfo();
	}

	public void setMaxSpeedTow(short maxSpeedTow) {
		segment.setMaxSpeedTow(maxSpeedTow);
	}

	public void setGeometry(LineString geometry) {
		segment.setGeometry(geometry);
	}

	public short getMaxSpeedBkw() {
		return segment.getMaxSpeedBkw();
	}

	public float getLength() {
		return segment.getLength();
	}

	public void setXInfo(List<ISegmentXInfo> xInfo) {
		segment.setXInfo(xInfo);
	}

	public void setMaxSpeedBkw(short maxSpeedBkw) {
		segment.setMaxSpeedBkw(maxSpeedBkw);
	}

	public void addXInfo(ISegmentXInfo xInfo) {
		segment.addXInfo(xInfo);
	}

	public Short getSpeedCalcTow() {
		return segment.getSpeedCalcTow();
	}

	public void setLength(float length) {
		segment.setLength(length);
	}

	public void setSpeedCalcTow(Short speedCalcTow) {
		segment.setSpeedCalcTow(speedCalcTow);
	}

	public void addXInfo(List<ISegmentXInfo> xInfo) {
		segment.addXInfo(xInfo);
	}

	public String getName() {
		return segment.getName();
	}

	public Short getSpeedCalcBkw() {
		return segment.getSpeedCalcBkw();
	}

	public void setName(String name) {
		segment.setName(name);
	}

	public void setSpeedCalcBkw(Short speedCalcBkw) {
		segment.setSpeedCalcBkw(speedCalcBkw);
	}

	public String getStreetType() {
		return segment.getStreetType();
	}

	public short getLanesTow() {
		return segment.getLanesTow();
	}

	public void setStreetType(String streetType) {
		segment.setStreetType(streetType);
	}

	public void setLanesTow(short lanesTow) {
		segment.setLanesTow(lanesTow);
	}

	public long getWayId() {
		return segment.getWayId();
	}

	public short getLanesBkw() {
		return segment.getLanesBkw();
	}

	public void setWayId(long wayId) {
		segment.setWayId(wayId);
	}

	public void setLanesBkw(short lanesBkw) {
		segment.setLanesBkw(lanesBkw);
	}

	public long getStartNodeId() {
		return segment.getStartNodeId();
	}

	public FuncRoadClass getFrc() {
		return segment.getFrc();
	}

	public void setStartNodeId(long startNodeId) {
		segment.setStartNodeId(startNodeId);
	}

	public void setFrc(FuncRoadClass frc) {
		segment.setFrc(frc);
	}

	public int getStartNodeIndex() {
		return segment.getStartNodeIndex();
	}

	public FormOfWay getFormOfWay() {
		return segment.getFormOfWay();
	}

	public void setStartNodeIndex(int startNodeIndex) {
		segment.setStartNodeIndex(startNodeIndex);
	}

	public void setFormOfWay(FormOfWay formOfWay) {
		segment.setFormOfWay(formOfWay);
	}

	public long getEndNodeId() {
		return segment.getEndNodeId();
	}

	public Set<Access> getAccessTow() {
		return segment.getAccessTow();
	}

	public void setEndNodeId(long endNodeId) {
		segment.setEndNodeId(endNodeId);
	}

	public void setAccessTow(Set<Access> accessTow) {
		segment.setAccessTow(accessTow);
	}

	public int getEndNodeIndex() {
		return segment.getEndNodeIndex();
	}

	public Set<Access> getAccessBkw() {
		return segment.getAccessBkw();
	}

	public void setEndNodeIndex(int endNodeIndex) {
		segment.setEndNodeIndex(endNodeIndex);
	}

	public void setAccessBkw(Set<Access> accessBkw) {
		segment.setAccessBkw(accessBkw);
	}

	public List<IWaySegmentConnection> getStartNodeCons() {
		return segment.getStartNodeCons();
	}

	public Boolean isTunnel() {
		return segment.isTunnel();
	}

	public void setStartNodeCons(List<IWaySegmentConnection> startNodeCons) {
		segment.setStartNodeCons(startNodeCons);
	}

	public void setTunnel(Boolean tunnel) {
		segment.setTunnel(tunnel);
	}

	public Boolean isBridge() {
		return segment.isBridge();
	}

	public List<IWaySegmentConnection> getEndNodeCons() {
		return segment.getEndNodeCons();
	}

	public void setBridge(Boolean bridge) {
		segment.setBridge(bridge);
	}

	public void setEndNodeCons(List<IWaySegmentConnection> endNodeCons) {
		segment.setEndNodeCons(endNodeCons);
	}

	public Boolean isUrban() {
		return segment.isUrban();
	}

	public void setUrban(Boolean urban) {
		segment.setUrban(urban);
	}

	public Date getTimestamp() {
		return segment.getTimestamp();
	}

	public void setTimestamp(Date timestamp) {
		segment.setTimestamp(timestamp);
	}

	public int getDuration(boolean directionTow) {
		return segment.getDuration(directionTow);
	}

	public int getMinDuration(boolean directionTow) {
		return segment.getMinDuration(directionTow);
	}

	public OneWay isOneway() {
		return segment.isOneway();
	}

	public Map<String, String> getTags() {
		return segment.getTags();
	}

	public void setTags(Map<String, String> tags) {
		segment.setTags(tags);
	}
	
	public void setStartPointIndex(int startPointIndex) {
		this.startPointIndex = startPointIndex;
	}

	public int getEndPointIndex() {
		return endPointIndex;
	}

	public void setEndPointIndex(int endPointIndex) {
		this.endPointIndex = endPointIndex;
	}

	public List<Double> getDistances() {
		return distances;
	}

	@Override
	public int getMatchedPoints() {
		return endPointIndex - startPointIndex;
	}

	public Direction getDirection() {
		return direction;
	}

	public void setDirection(Direction direction) {
		this.direction = direction;
	}

	@Override
	public boolean isStartSegment() {
		return startSegment;
	}

	@Override
	public void setStartSegment(boolean startSegment) {
		this.startSegment = startSegment;
	}

	@Override
	public boolean isFromPathSearch() {
		return fromPathSearch;
	}

	@Override
	public void setFromPathSearch(boolean fromPathSearch) {
		this.fromPathSearch = fromPathSearch;
	}

	@Override
	public boolean isUTurnSegment() {
		return this.uTurnSegment;
	}

	@Override
	public void setUTurnSegment(boolean uTurnSegment) {
		this.uTurnSegment = uTurnSegment;
	}

	public void setAfterSkippedPart(boolean afterSkippedPart) {
		this.afterSkippedPart = afterSkippedPart;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(getId(), getDirection());
	}

	@Override
	public IDistancesCache getMatchedPointDistancesCache() {
		return matchedPointDistanceCache;
	}

	@Override
	public IDistancesCache getStraightLineDistancesCache() {
		return straightLineDistanceCache;
	}

	@Override
	public IDistancesCache getRouteDistancesCache() {
		return routeDistanceCache;
	}
	
	public static class DistancesCache implements IDistancesCache, Cloneable {

		private DistanceValueCache segmentPointsCache = new DistanceValueCache();
		private DistanceValueCache routingSegmentsCache = new DistanceValueCache();
		
		@Override
		public DistanceValueCache getSegmentPointsCache() {
			return segmentPointsCache;
		}

		@Override
		public DistanceValueCache getRoutingSegmentsCache() {
			return routingSegmentsCache;
		}
		
		@Override
		protected DistancesCache clone() {
			try {
				DistancesCache clonedCache = (DistancesCache) super.clone();
				
				clonedCache.segmentPointsCache = segmentPointsCache.clone();
				clonedCache.routingSegmentsCache = routingSegmentsCache.clone();
				
				return clonedCache;
			} catch (CloneNotSupportedException e) {}
			
			return null;
		}
	}
	
	public static class DistanceValueCache implements IDistanceValueCache, Cloneable {
		
		private Double value;
		private Object key;

		@Override
		public Double getValue() {
			return value;
		}

		@Override
		public boolean hasValue(Object otherKey) {
			return Objects.equal(key, otherKey);
		}

		@Override
		public void put(Object key, Double value) {
			this.key = key;
			this.value = value;
		}
		
		@Override
		protected DistanceValueCache clone() {
			try {
				return (DistanceValueCache) super.clone();
			} catch (CloneNotSupportedException e) {}
			
			return null;
		}
	}

	@Override
	public double getWeight() {
		return weight;
	}

	@Override
	public void setWeight(double weight) {
		this.weight = weight;
	}

	@Override
	public List<ISegmentXInfo> getXInfo(String type) {
		return segment.getXInfo(type);
	}

	@Override
	public List<IWaySegmentConnection> getCons() {
		return segment.getCons();
	}

	@Override
	public void setCons(List<IWaySegmentConnection> cons) {
		segment.setCons(cons);
	}

	@Override
	public void addCons(List<IWaySegmentConnection> connections) {
		segment.addCons(connections);
	}

	@Override
	public boolean isCertain() {
		return certain;
	}

	@Override
	public void setCertain(boolean certain) {
		this.certain = certain;
	}

	@Override
	public boolean isValid() {
		return matchedPoints.size() == endPointIndex - startPointIndex;
	}

}
