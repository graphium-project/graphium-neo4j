/**
 * Graphium Neo4j - Module of Graphserver for Neo4j extension
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
package at.srfg.graphium.neo4j.service.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.index.strtree.STRtree;

import at.srfg.graphium.geomutils.GeometryUtils;
import at.srfg.graphium.neo4j.model.index.STRTreeEntity;
import at.srfg.graphium.neo4j.persistence.index.STRTreeEntityComparator;

/**
 * @author mwimmer
 */
public class STRTreeService {

	private static Logger log = LoggerFactory.getLogger(STRTreeService.class);

	private STRTreeCacheManager cacheManager;
	
	public List<Long> findNearestSegmentIds(String graphName, String version, Point referencePoint,
			double distance, int limit) {
		Envelope env = GeometryUtils.createEnvelope(referencePoint, distance);
		return findNearestSegmentIds(graphName, version, referencePoint, limit, env);
	}

	public List<Long> findNearestSegmentIdsWithOrthodromicDistance(String graphName, String version, Point referencePoint,
			double radiusInKm, int limit) {
		Envelope env = GeometryUtils.createEnvelopeInMeters(referencePoint, radiusInKm * 1000);
		return findNearestSegmentIds(graphName, version, referencePoint, limit, env);
	}

	private List<Long> findNearestSegmentIds(String graphName, String version, Point referencePoint, int limit,
			Envelope env) {
		List<Long> segmentIds = null;
		STRtree tree = cacheManager.getIndex(graphName, version);
		
		if (tree == null) {
			log.warn("no STR-Tree found for graph name " + graphName + " and version " + version);
		} else {
			segmentIds = new ArrayList<>();
			
			List<STRTreeEntity> candidates = tree.query(env);
			if (candidates != null && !candidates.isEmpty()) {
				candidates.sort(new STRTreeEntityComparator(referencePoint));
				int lim;
				if (limit > 0) {
					lim = limit;
				} else {
					lim = candidates.size();
				}
				Iterator<STRTreeEntity> it = candidates.iterator();
				int i = 0;
				while (it.hasNext() && i < lim) {
					segmentIds.add(it.next().getSegmentId());
					i++;
				}
			}
		}
		
		return segmentIds;
	}

	public STRTreeCacheManager getCacheManager() {
		return cacheManager;
	}

	public void setCacheManager(STRTreeCacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}

}
