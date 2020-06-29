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
package at.srfg.graphium.routing.service.neo4j.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import at.srfg.graphium.core.exception.GraphNotExistsException;
import at.srfg.graphium.core.persistence.IWayGraphReadDao;
import at.srfg.graphium.model.FuncRoadClass;
import at.srfg.graphium.model.IWaySegment;
import at.srfg.graphium.model.impl.WaySegment;
import at.srfg.graphium.neo4j.model.cache.SegmentCacheEntry;
import at.srfg.graphium.neo4j.service.impl.STRTreeCacheManager;
import at.srfg.graphium.routing.service.impl.WaySegmentsByIdLoaderImpl;

/**
 * @author mwimmer
 *
 */
public class Neo4jWaySegmentsByIdLoaderImpl<T extends IWaySegment> extends WaySegmentsByIdLoaderImpl<T> {

	// FIXME: Cache ist zwar nett, aber leider hält er nicht alle Attribute, die beim Map Matching benötigt werden (z.B. FormOfWay)
	private STRTreeCacheManager cache;

	public Neo4jWaySegmentsByIdLoaderImpl(IWayGraphReadDao<T> graphDao) {
		super(graphDao);
	}

	public Neo4jWaySegmentsByIdLoaderImpl(IWayGraphReadDao<T> graphDao, STRTreeCacheManager cache) {
		super(graphDao);
		this.cache = cache;
	}

	@Override
	protected List<T> getSegments(String graphName, String graphVersion, Set<Long> segmentIds)
			throws GraphNotExistsException {
		List<T> segments = null;
		
		// use cache
		if (cache != null) {
			segments = new ArrayList<>();
			SegmentCacheEntry cacheEntry;
			for (Long segmentId : segmentIds) {
				cacheEntry = cache.getCacheEntryPerSegmentId(graphName, graphVersion, segmentId);
				if (cacheEntry != null) {
					segments.add(adapt(cacheEntry));
				}
			}
		}
		
		if (segments == null || segments.isEmpty()) {
			segments = super.getSegments(graphName, graphVersion, segmentIds);
		}
		
		return segments;
	}

	private T adapt(SegmentCacheEntry cacheEntry) {
		T segment = (T) new WaySegment();
		segment.setId(cacheEntry.getId());
		segment.setGeometry(cacheEntry.getGeometry());
		segment.setLength(cacheEntry.getLength());
		segment.setMaxSpeedTow(cacheEntry.getMaxSpeedTow());
		segment.setMaxSpeedBkw(cacheEntry.getMaxSpeedBkw());
		segment.setFrc(FuncRoadClass.getFuncRoadClassForValue(cacheEntry.getFrc()));
		return segment;
	}

	public STRTreeCacheManager getCache() {
		return cache;
	}

	public void setCache(STRTreeCacheManager cache) {
		this.cache = cache;
	}

}
