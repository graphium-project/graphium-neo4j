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
 * (C) 2017 Salzburg Research Forschungsgesellschaft m.b.H.
 *
 * All rights reserved.
 *
 */
package at.srfg.graphium.neo4j.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.index.strtree.STRtree;
import com.vividsolutions.jts.io.ParseException;

import at.srfg.graphium.core.helper.GraphVersionHelper;
import at.srfg.graphium.core.observer.IGraphVersionStateModifiedObserver;
import at.srfg.graphium.core.observer.impl.AbstractGraphVersionStateModifiedObserver;
import at.srfg.graphium.core.service.IGraphVersionMetadataService;
import at.srfg.graphium.model.IWayGraphVersionMetadata;
import at.srfg.graphium.model.State;
import at.srfg.graphium.neo4j.model.index.STRTreeEntity;
import at.srfg.graphium.neo4j.persistence.configuration.IGraphDatabaseProvider;
import at.srfg.graphium.neo4j.persistence.impl.Neo4jWaySegmentHelperImpl;

/**
 * @author mwimmer
 */
public class STRTreeCacheManager extends AbstractGraphVersionStateModifiedObserver
implements IGraphVersionStateModifiedObserver {
	private static Logger log = LoggerFactory.getLogger(STRTreeCacheManager.class);

	private IGraphVersionMetadataService metadataService;
	private IGraphDatabaseProvider graphDatabaseProvider;
	
	// the current (active) version of each graph 
	private Map<String, STRTreeCacheEntry> activeIndexCache = null;
	// the last n requested historic (active) graph versions
	private Cache<String, STRtree> historicIndexCache = null;
	private long expirationTime = 60;
	private int maximumHistoricCachSize = 3;
	private Map<String, STRtree> lastBuiltTree = new HashMap<>(1);
	
	@PostConstruct
	public void setup() {
		historicIndexCache = CacheBuilder.newBuilder()
										 .expireAfterAccess(expirationTime, TimeUnit.SECONDS)
										 .maximumSize(maximumHistoricCachSize)
										 .build();
		
		List<String> graphNamesToIndex = metadataService.getGraphs();
		
		if (graphNamesToIndex != null && !graphNamesToIndex.isEmpty()) {
		
			activeIndexCache = new HashMap<>(graphNamesToIndex.size());
			
			for (String graphName : graphNamesToIndex) {
				IWayGraphVersionMetadata metadata = metadataService.getCurrentWayGraphVersionMetadata(graphName);
				if (metadata != null) {
					String graphVersionName = GraphVersionHelper.createGraphVersionName(metadata.getGraphName(), metadata.getVersion());
					activeIndexCache.put(graphName, new STRTreeCacheEntry(metadata, buildTree(graphVersionName)));
				} else {
					log.warn("No current version found for graph " + graphName);
				}
			}
		}
	}
	
	private synchronized STRtree buildTree(String graphVersionName) {
		if (lastBuiltTree.containsKey(graphVersionName)) {
			log.info("Already built STR-Tree found for graph version " + graphVersionName);
			return lastBuiltTree.get(graphVersionName);
		}
		
		log.info("Building STR-Tree for graph version " + graphVersionName + " ...");
		lastBuiltTree.clear();
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

		
		
		// TODO: count object bytes and log memory usage
		
		
		printMemoryUsage();
		
		lastBuiltTree.put(graphVersionName, tree);
		
		log.info("STR-Tree built");
		
		return tree;
	}

	public STRtree getIndex(String graphName, String version) {
		STRtree index = null;
		String graphVersionName = GraphVersionHelper.createGraphVersionName(graphName, version);
		
		if (activeIndexCache != null && activeIndexCache.containsKey(graphName)) {
			// requested graph version is an active one
			if (activeIndexCache.get(graphName).getMetadata().getVersion().equals(version)) {
				index = activeIndexCache.get(graphName).getTree();
			}
		} 
		if (index == null) {
			// requested graph version is an historic one
			index = historicIndexCache.getIfPresent(graphVersionName);
			if (index == null) {
				// requested graph version is not in cache => load, build and cache tree index 
				IWayGraphVersionMetadata metadata = metadataService.getWayGraphVersionMetadata(graphName, version);
				if (metadata != null && metadata.getState().equals(State.ACTIVE)) {
					// if requested version was null take version of metadata (should be current version)
					graphVersionName = GraphVersionHelper.createGraphVersionName(graphName, metadata.getVersion());
					// maybe now we can find the right version in index cache?
					if (activeIndexCache != null && activeIndexCache.containsKey(graphName) &&
						activeIndexCache.get(graphName).getMetadata().getVersion().equals(metadata.getVersion())) {
						index = activeIndexCache.get(graphName).getTree();
					} else {
						historicIndexCache.put(graphVersionName, buildTree(graphVersionName));
						index = historicIndexCache.getIfPresent(graphVersionName);
					}
				}
			}
		}
		return index;
	}

	@Override
	public void update(Observable observable, Object metadataObj) {
		if (metadataObj != null) {
			if (metadataObj instanceof IWayGraphVersionMetadata) {
				IWayGraphVersionMetadata metadata = (IWayGraphVersionMetadata) metadataObj;
				if (((IWayGraphVersionMetadata) metadata).getState().equals(State.ACTIVE)) {
					// if a new graph version has been activated STRTree has to be built and cached
					if (activeIndexCache == null || !activeIndexCache.containsKey(metadata.getGraphName()) ||
						(activeIndexCache.containsKey(metadata.getGraphName()) && 
						 activeIndexCache.get(metadata.getGraphName()).getMetadata().getValidFrom().before(metadata.getValidFrom()))) {
						log.info("Got update to rebuild STR-Tree");
						String graphVersionName = GraphVersionHelper.createGraphVersionName(metadata.getGraphName(), metadata.getVersion());
						activeIndexCache.put(metadata.getGraphName(), new STRTreeCacheEntry(metadata, buildTree(graphVersionName)));
					}
				} else if (metadata.getState().equals(State.DELETED)) {
					log.info("Got update to remove graph version " + metadata.getGraphName() + "_" + 
							metadata.getVersion() + " from STR-Tree");
					removeFromTree(metadata.getGraphName(), metadata.getVersion());
				}
			} else {
				log.warn("Got update to rebuild STR-Tree, but argument object was no instance of IWayGraphVersionMetadata");
			}
		} else {
			log.warn("Got update to rebuild STR-Tree, but metadata object was null");
		}
	}

	private void removeFromTree(String graphName, String version) {
		if (activeIndexCache != null && activeIndexCache.containsKey(graphName) && 
			activeIndexCache.get(graphName).getMetadata().getVersion().equals(version)) {
			activeIndexCache.remove(graphName);
		} else {
			String graphVersionName = GraphVersionHelper.createGraphVersionName(graphName, version);
			historicIndexCache.invalidate(graphVersionName);
		}
	}

	private void printMemoryUsage() {
		Runtime runtime = Runtime.getRuntime();
		log.info("Memory Usage: " + ((runtime.totalMemory() - runtime.freeMemory())/(1024*1024)) + " MB");
	}

	public IGraphVersionMetadataService getMetadataService() {
		return metadataService;
	}

	public void setMetadataService(IGraphVersionMetadataService metadataService) {
		this.metadataService = metadataService;
	}

	public IGraphDatabaseProvider getGraphDatabaseProvider() {
		return graphDatabaseProvider;
	}

	public void setGraphDatabaseProvider(IGraphDatabaseProvider graphDatabaseProvider) {
		this.graphDatabaseProvider = graphDatabaseProvider;
	}

	public long getExpirationTime() {
		return expirationTime;
	}

	public void setExpirationTime(long expirationTime) {
		this.expirationTime = expirationTime;
	}

	public int getMaximumHistoricCachSize() {
		return maximumHistoricCachSize;
	}

	public void setMaximumHistoricCachSize(int maximumHistoricCachSize) {
		this.maximumHistoricCachSize = maximumHistoricCachSize;
	}

	public class STRTreeCacheEntry {
		private IWayGraphVersionMetadata metadata;
		private STRtree tree;
		
		public STRTreeCacheEntry(IWayGraphVersionMetadata metadata, STRtree tree) {
			super();
			this.metadata = metadata;
			this.tree = tree;
		}
		
		public IWayGraphVersionMetadata getMetadata() {
			return metadata;
		}
		public void setMetadata(IWayGraphVersionMetadata metadata) {
			this.metadata = metadata;
		}
		public STRtree getTree() {
			return tree;
		}
		public void setTree(STRtree tree) {
			this.tree = tree;
		}
		
	}
	
}