/**
 * Graphium Neo4j - Module of Graphserver for Neo4j extension
 * Copyright © 2019 Salzburg Research Forschungsgesellschaft (graphium@salzburgresearch.at)
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
package at.srfg.graphium.neo4j.model.cache;

import java.util.HashMap;
import java.util.Map;

import at.srfg.graphium.model.IWayGraphVersionMetadata;

/**
 * @author mwimmer
 *
 */
public class GraphSegmentsCacheEntry {

	private IWayGraphVersionMetadata metadata;
	private Map<Long, SegmentCacheEntry> segmentsCache;
	
	public GraphSegmentsCacheEntry(IWayGraphVersionMetadata metadata, Map<Long, SegmentCacheEntry> segmentsCache) {
		super();
		this.metadata = metadata;
		this.segmentsCache = segmentsCache;
	}
	
	public GraphSegmentsCacheEntry(IWayGraphVersionMetadata metadata) {
		super();
		this.metadata = metadata;
		this.segmentsCache = new HashMap<>();
	}
	
	public IWayGraphVersionMetadata getMetadata() {
		return metadata;
	}
	
	public void setMetadata(IWayGraphVersionMetadata metadata) {
		this.metadata = metadata;
	}
	
	public SegmentCacheEntry getSegmentsCacheEntry(long id) {
		SegmentCacheEntry entry = null;
		if (segmentsCache != null) {
			entry = segmentsCache.get(id);
		}
		return entry;
	}
	
	public Map<Long, SegmentCacheEntry> getSegmentsCache() {
		return segmentsCache;
	}
	
	public void addSegmentsCacheEntry(Long id, SegmentCacheEntry entry) {
		segmentsCache.put(id, entry);
	}
	
	public void setSegmentsCache(Map<Long, SegmentCacheEntry> segmentsCache) {
		this.segmentsCache = segmentsCache;
	}
	
}
