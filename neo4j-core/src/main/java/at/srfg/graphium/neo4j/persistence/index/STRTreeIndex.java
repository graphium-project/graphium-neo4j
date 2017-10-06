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
/**
 * (C) 2016 Salzburg Research Forschungsgesellschaft m.b.H.
 *
 * All rights reserved.
 *
 */
package at.srfg.graphium.neo4j.persistence.index;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.index.strtree.STRtree;
import com.vividsolutions.jts.io.ParseException;

import at.srfg.geomutils.GeometryUtils;
import at.srfg.graphium.neo4j.model.index.STRTreeEntity;
import at.srfg.graphium.neo4j.persistence.configuration.IGraphDatabaseProvider;
import at.srfg.graphium.neo4j.persistence.impl.Neo4jWaySegmentHelperImpl;

/**
 * @author mwimmer
 */
public class STRTreeIndex {

	private static Logger log = LoggerFactory.getLogger(STRTreeIndex.class);
	
	private IGraphDatabaseProvider graphDatabaseProvider;
	
	private Map<String, STRtree> trees = new HashMap<>();
	
	public void init(String graphVersionName) {
		STRtree tree = new STRtree();
		
		printMemoryUsage();

		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx()) {
			ResourceIterator<Node> segmentNodes = graphDatabaseProvider.getGraphDatabase().findNodes(
					Label.label(Neo4jWaySegmentHelperImpl.createSegmentNodeLabel(graphVersionName)));
			
			int i=0;
			Node segmentNode = null;
			while (segmentNodes.hasNext()) {
				segmentNode = segmentNodes.next();
				LineString geom;
				try {
					geom = Neo4jWaySegmentHelperImpl.encodeLineString(segmentNode);
					
					// CAUTION: node IDs are only valid if nodes will not be deleted!
					STRTreeEntity entity = new STRTreeEntity(geom.getCoordinateSequence(), geom.getFactory(), segmentNode.getId());
					tree.insert(geom.getEnvelopeInternal(), entity);
				} catch (ParseException e) {
					log.error("Could not parse geometry", e);
				}
				i++;
			}

			log.info(i + " segments indexed");
			
			tx.success();
		}
		
		tree.build();
		trees.put(graphVersionName, tree);

		// TODO: log memory usage
		printMemoryUsage();
		
	}
	
	public boolean isIndexed(String graphVersionName) {
		return trees.containsKey(graphVersionName);
	}
	
	public void removeIndex(String graphVersionName) {
		trees.remove(graphVersionName);
	}
	
	public List<Long> findNearestSegmentIds(String graphVersionName, Point referencePoint,
			double distance, int limit) {
		Envelope env = GeometryUtils.createEnvelope(referencePoint, distance);
		return findNearestSegmentIds(graphVersionName, referencePoint, limit, env);
	}

	public List<Long> findNearestSegmentIdsWithOrthodromicDistance(String graphVersionName, Point referencePoint,
			double radiusInKm, int limit) {
		Envelope env = GeometryUtils.createEnvelopeInMeters(referencePoint, radiusInKm * 1000);
		return findNearestSegmentIds(graphVersionName, referencePoint, limit, env);
	}

	private List<Long> findNearestSegmentIds(String graphVersionName, Point referencePoint, int limit,
			Envelope env) {
		List<Long> segmentIds = null;
		STRtree tree = trees.get(graphVersionName);
		
		if (tree == null) {
			log.warn("no STR-Tree found for graph version name " + graphVersionName);
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

	private void printMemoryUsage() {
		Runtime runtime = Runtime.getRuntime();
		log.info("Memory Usage: " + ((runtime.totalMemory() - runtime.freeMemory())/(1024*1024)) + " MB");
	}

	public IGraphDatabaseProvider getGraphDatabaseProvider() {
		return graphDatabaseProvider;
	}

	public void setGraphDatabaseProvider(IGraphDatabaseProvider graphDatabaseProvider) {
		this.graphDatabaseProvider = graphDatabaseProvider;
	}
	
}
