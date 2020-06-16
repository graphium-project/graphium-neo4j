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
package at.srfg.graphium.mapmatching.weighting.distances.impl;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import at.srfg.graphium.mapmatching.model.IMatchedWaySegment;
import at.srfg.graphium.mapmatching.model.IMatchedWaySegment.IDistancesCache;
import at.srfg.graphium.mapmatching.model.ITrack;
import at.srfg.graphium.mapmatching.weighting.distances.IDistanceCalculator;
import at.srfg.graphium.mapmatching.weighting.impl.RouteDistanceWeightingStrategy;

/**
 * This class serves as base class for various distance calculation types. It takes
 * care of caching the distance values calculated by inheriting classes.
 * 
 * A two level caching-strategy is used: 
 * 	-	First every {@link IMatchedWaySegment} instance has its own local cache (one for every different
 * 		distance type).
 *  -	Then there is a global cache for every distance type. The purpose of these caches is
 *  	to share the distance values among all paths. Otherwise distances for a segment might have to be 
 *  	calculated again and again for every different path. 
 *  
 */
public abstract class DistanceCalculator implements IDistanceCalculator<Double> {
	
	/**
	 * Is supposed to return a distance value for routing segments. The segments between 
	 * the first and last segment in {@code segments} were obtained from shortest path search.
	 * 
	 * This methods gets called from {@link DistanceCalculator#getRoutingSegmentsDistance(List)}.
	 */
	protected abstract double getRoutingSegmentsDistanceValue(ITrack track, List<IMatchedWaySegment> segments);
	
	/**
	 * Is supposed to return a distance value for the track points {@code trackIndexFrom} and
	 * {@code trackIndexTo} in relation to the given {@code segment}.
	 * 
	 * This methods gets called from {@link DistanceCalculator#getSegmentPointsDistance(IMatchedWaySegment, List, List)}.
	 */
	protected abstract Double getSegmentPointsDistanceValue(IMatchedWaySegment segment, int trackIndexFrom, int trackIndexTo);
	
	/**
	 * See {@link RouteDistanceWeightingStrategy#addPenaltyForPseudoSkippedParts()}.
	 */
	public abstract Double getPenaltyForPseudoSkippedParts(IMatchedWaySegment segment);
	
	/**
	 * Is supposed to return the cache on a {@code segment} in which the distance values
	 * should be stored.
	 */
	public abstract IDistancesCache getDistancesCache(IMatchedWaySegment segment);
	
	/**
	 * Global caches
	 */
	@VisibleForTesting
	protected final Cache<SegmentKey<SegmentPointsCacheKey>, Double> globalSegmentPointsCache;
	@VisibleForTesting
	protected final Cache<SegmentKey<RoutingSegmentsCacheKey>, Double> globalRoutingSegmentsCache;
		
	public DistanceCalculator() {
		globalSegmentPointsCache = CacheBuilder.newBuilder()
				.maximumSize(200)
				.concurrencyLevel(1).build();
		globalRoutingSegmentsCache = CacheBuilder.newBuilder()
				.maximumSize(200)
				.concurrencyLevel(1).build();
	}

	/**
	 * Wrapper around {@link DistanceCalculator#getRoutingSegmentsDistanceValue(List)} which
	 * takes care of caching.
	 */
	public Double getRoutingSegmentsDistance(List<IMatchedWaySegment> segments, ITrack track) {
		IMatchedWaySegment lastSegment = segments.get(segments.size() - 1);
		
		final RoutingSegmentsCacheKey cacheKey = getRoutingSegmentsCacheKey(segments);
		final SegmentKey<RoutingSegmentsCacheKey> globalCacheKey = getGlobalCacheKey(lastSegment, cacheKey);
		
		if (hasRoutingSegmentsValue(lastSegment, cacheKey)) {
			// distance is cached in the local segment cache
			return getRoutingSegmentsValue(lastSegment);
		} else {
			// check if the distance was already calculated for another path
			Double cacheValue = globalRoutingSegmentsCache.getIfPresent(globalCacheKey);
			
			if (cacheValue != null) {
				// got distance from global cache, also store it in the segment cache,
				// because the value might get discarded from the global cache
				putRoutingSegmentsCacheValue(lastSegment, cacheKey, cacheValue);
				return cacheValue;
			}
		}
		
		// the distance has not been calculated yet, do it now
		double distance = getRoutingSegmentsDistanceValue(track, segments);
		
		// then store it in the caches
		putRoutingSegmentsCacheValue(lastSegment, cacheKey, distance);
		globalRoutingSegmentsCache.put(globalCacheKey, distance);
		
		return distance;
	}
	
	/**
	 * Wrapper around {@link DistanceCalculator#getSegmentPointsDistanceValue(IMatchedWaySegment, int, int)}
	 * which takes care of caching.
	 */
	@Override
	public void getSegmentPointsDistance(IMatchedWaySegment segment, List<Double> distances) {
		int trackIndexFrom = segment.getStartPointIndex();
		int trackIndexTo = segment.getEndPointIndex();
		
		final SegmentPointsCacheKey cacheKey = getSegmentPointsCacheKey(trackIndexFrom, trackIndexTo);
		final SegmentKey<SegmentPointsCacheKey> globalCacheKey = getGlobalCacheKey(segment, cacheKey);
		
		boolean hasCachedValue = checkSegmentCache(segment, distances, cacheKey, globalCacheKey);
		if (hasCachedValue) {
			return;
		}
		
		List<Double> distancesForSegment = new LinkedList<Double>();
		for (int indexFrom = segment.getStartPointIndex(); indexFrom < segment.getEndPointIndex(); indexFrom++) {
			Double distance = getSegmentPointsDistanceValue(segment, indexFrom, indexFrom + 1);
			distancesForSegment.add(distance);
		}
		
		double distanceForSegment = reduceDistancesForSegment(distancesForSegment);
		distances.add(distanceForSegment);
		
		storeInSegmentCache(segment, cacheKey, globalCacheKey, distanceForSegment);
	}

	/**
	 * Checks if the distance is stored in one of the caches. If so, the distance
	 * value is added to the {@code distances} result list.
	 */
	private boolean checkSegmentCache(IMatchedWaySegment segment, List<Double> distances, 
			final SegmentPointsCacheKey cacheKey, final SegmentKey<SegmentPointsCacheKey> globalCacheKey) {
		boolean hasCachedValue = false;
		
		if (hasSegmentPointsCacheValue(segment, cacheKey)) {
			// distance is cached in the local segment cache
			hasCachedValue = true;
			
			Double distance = getSegmentPointsCacheValue(segment);
			distances.add(distance);
		} else {
			// check if the distance was already calculated for another path
			Double cachedDistance = globalSegmentPointsCache.getIfPresent(globalCacheKey);
			
			if (cachedDistance != null) {
				hasCachedValue = true;
				putSegmentPointsCacheValue(segment, cacheKey, cachedDistance);
				
				distances.add(cachedDistance);
			}
		}
		
		return hasCachedValue;
	}
	
	protected double reduceDistancesForSegment(List<Double> distancesForSegment) {
		double distanceForSegment = 0;
		
		for (double distance : distancesForSegment) {
			distanceForSegment += distance;
		}
		
		return distanceForSegment;
	}
	
	private void storeInSegmentCache(IMatchedWaySegment segment, 
			final SegmentPointsCacheKey cacheKey, final SegmentKey<SegmentPointsCacheKey> globalCacheKey,
			double distancesForSegment) {
		putSegmentPointsCacheValue(segment, cacheKey, distancesForSegment);
		globalSegmentPointsCache.put(globalCacheKey, distancesForSegment);
	}

	public Double getDistanceForEmptyEndSegments(List<IMatchedWaySegment> previousSegments) {
		return 0.0;
	}
	
	protected <T> SegmentKey<T> getGlobalCacheKey(IMatchedWaySegment lastSegment, T cacheKey) {
		return new SegmentKey<T>(lastSegment.getId(), cacheKey);
	}
	
	protected SegmentPointsCacheKey getSegmentPointsCacheKey(int trackIndexFrom, int trackIndexTo) {
		return new SegmentPointsCacheKey(trackIndexFrom, trackIndexTo);
	}
	
	protected RoutingSegmentsCacheKey getRoutingSegmentsCacheKey(List<IMatchedWaySegment> segments) {
		return new RoutingSegmentsCacheKey(segments);
	}
	
	protected boolean hasSegmentPointsCacheValue(IMatchedWaySegment segment, Object key) {
		return getDistancesCache(segment).getSegmentPointsCache().hasValue(key);
	}
	
	protected boolean hasRoutingSegmentsValue(IMatchedWaySegment segment, Object key) {
		return getDistancesCache(segment).getRoutingSegmentsCache().hasValue(key);
	}
	
	protected Double getSegmentPointsCacheValue(IMatchedWaySegment segment) {
		return getDistancesCache(segment).getSegmentPointsCache().getValue();
	}
	
	protected Double getRoutingSegmentsValue(IMatchedWaySegment segment) {
		return getDistancesCache(segment).getRoutingSegmentsCache().getValue();
	}
	
	protected void putSegmentPointsCacheValue(IMatchedWaySegment segment, Object key, Double value) {
		getDistancesCache(segment).getSegmentPointsCache().put(key, value);
	}
	
	protected void putRoutingSegmentsCacheValue(IMatchedWaySegment segment, Object key, Double value) {
		getDistancesCache(segment).getRoutingSegmentsCache().put(key, value);
	}
	
	protected static class SegmentKey<T> {
		private final long segmentId;
		private final T internalKey;
		
		public SegmentKey(long segmentId, T internalKey) {
			this.segmentId = segmentId;
			this.internalKey = internalKey;
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(segmentId, internalKey);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null || getClass() != obj.getClass())
				return false;
			
			@SuppressWarnings("unchecked")
			SegmentKey<T> other = (SegmentKey<T>) obj;

			return segmentId == other.segmentId &&
					Objects.equal(internalKey, other.internalKey);
		}
	}
	
	protected static class SegmentPointsCacheKey {
		private final int trackIndexFrom;
		private final int trackIndexTo;
		
		public SegmentPointsCacheKey(int trackIndexFrom, int trackIndexTo) {
			this.trackIndexFrom = trackIndexFrom;
			this.trackIndexTo = trackIndexTo;
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(trackIndexFrom, trackIndexTo);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null || getClass() != obj.getClass())
				return false;
			
			SegmentPointsCacheKey otherKey = (SegmentPointsCacheKey) obj;
			
			return trackIndexFrom == otherKey.trackIndexFrom &&
					trackIndexTo == otherKey.trackIndexTo;
		}
		
		@Override
		public String toString() {
			return MoreObjects.toStringHelper(this)
					.addValue(trackIndexFrom)
					.addValue(trackIndexTo).toString();
		}
	}
	
	protected static class RoutingSegmentsCacheKey {
		
		private final long firstSegmentId;
		private final int firstSegmentEndPointIndex;
		
		private final long lastSegmentId;
		private final int lastSegmentStartPointIndex;
		
		private final long[] segmentsInBetweenIds;
		
		public RoutingSegmentsCacheKey(List<IMatchedWaySegment> segments) {
			IMatchedWaySegment firstSegment = segments.get(0);
			IMatchedWaySegment lastSegment = segments.get(segments.size() - 1);
			
			firstSegmentId = firstSegment.getId();
			firstSegmentEndPointIndex = firstSegment.getEndPointIndex();
			lastSegmentId = lastSegment.getId();
			lastSegmentStartPointIndex = lastSegment.getStartPointIndex();
			
			List<IMatchedWaySegment> segmentsInBetween = RouteDistanceCalculator.getSegmentsInBetween(segments);
			segmentsInBetweenIds = new long[segmentsInBetween.size()];
			
			for (int i = 0; i < segmentsInBetween.size(); i++) {
				segmentsInBetweenIds[i] = segmentsInBetween.get(i).getId();
			}
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(firstSegmentId, firstSegmentEndPointIndex, 
					lastSegmentId, lastSegmentStartPointIndex, Arrays.hashCode(segmentsInBetweenIds));
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null || getClass() != obj.getClass())
				return false;
			
			RoutingSegmentsCacheKey otherKey = (RoutingSegmentsCacheKey) obj;
			
			return
					firstSegmentId == otherKey.firstSegmentId &&
					firstSegmentEndPointIndex == otherKey.firstSegmentEndPointIndex &&
					lastSegmentId == otherKey.lastSegmentId &&
					lastSegmentStartPointIndex == otherKey.lastSegmentStartPointIndex &&
					Arrays.equals(segmentsInBetweenIds, otherKey.segmentsInBetweenIds);
		}

		@Override
		public String toString() {
			return "RoutingSegmentsCacheKey [firstSegmentId=" + firstSegmentId + ", firstSegmentEndPointIndex="
					+ firstSegmentEndPointIndex + ", lastSegmentId=" + lastSegmentId + ", lastSegmentStartPointIndex="
					+ lastSegmentStartPointIndex + ", segmentsInBetweenIds=" + Arrays.toString(segmentsInBetweenIds)
					+ "]";
		}
		

	}
}