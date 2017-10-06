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
package at.srfg.graphium.neo4j.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.srfg.graphium.core.helper.GraphVersionHelper;
import at.srfg.graphium.core.observer.IGraphVersionStateModifiedObserver;
import at.srfg.graphium.core.observer.impl.AbstractGraphVersionStateModifiedObserver;
import at.srfg.graphium.core.persistence.IWayGraphVersionMetadataDao;
import at.srfg.graphium.model.IWayGraphVersionMetadata;
import at.srfg.graphium.model.State;
import at.srfg.graphium.neo4j.persistence.index.STRTreeIndex;

/**
 * Loads all nodes of current version for each graph name and builds an JTS STR-Tree in memory for spatial search.
 * 
 * @author mwimmer
 */
public class STRTreeIndexLoader extends AbstractGraphVersionStateModifiedObserver
	implements IGraphVersionStateModifiedObserver {
	
	private static Logger log = LoggerFactory.getLogger(STRTreeIndexLoader.class);

	private STRTreeIndex treeIndex;
	private IWayGraphVersionMetadataDao metadataDao;
	
	private List<String> graphNames = null;
	
	@PostConstruct
	public void setup() {
		List<String> graphNamesToIndex = new ArrayList<>();
		if (graphNames != null) {
			graphNamesToIndex.addAll(graphNames);
		} else {
			graphNamesToIndex = metadataDao.getGraphs();
		}
		
		for (String graphName : graphNamesToIndex) {
			IWayGraphVersionMetadata metadata = metadataDao.getCurrentWayGraphVersionMetadata(graphName);
			if (metadata != null) {
				buildTree(metadata);
			} else {
				log.warn("No current version found for graph " + graphName);
			}
		}
	}

	private void buildTree(IWayGraphVersionMetadata metadata) {
		String graphVersionName = GraphVersionHelper.createGraphVersionName(metadata.getGraphName(), metadata.getVersion());
		log.info("Building STR-Tree for graph " + metadata.getGraphName() + " in version " + metadata.getVersion() + "...");
		treeIndex.init(graphVersionName);
		log.info("STR-Tree built");
	}

	private void removeFromTree(IWayGraphVersionMetadata metadata) {
		String graphVersionName = GraphVersionHelper.createGraphVersionName(metadata.getGraphName(), metadata.getVersion());
		treeIndex.removeIndex(graphVersionName);
		log.info(graphVersionName + " removed from STR-Tree");
	}

	@Override
	public void update(Observable observable, Object metadata) {
		if (metadata != null) {
			if (metadata instanceof IWayGraphVersionMetadata) {
				if (((IWayGraphVersionMetadata) metadata).getState().equals(State.ACTIVE)) {
					log.info("Got update to rebuild STR-Tree");
					buildTree((IWayGraphVersionMetadata) metadata);
				} else if (((IWayGraphVersionMetadata) metadata).getState().equals(State.DELETED)) {
					log.info("Got update to remove graph version " + ((IWayGraphVersionMetadata) metadata).getGraphName() + "_" + 
							((IWayGraphVersionMetadata) metadata).getVersion() + " from STR-Tree");
					removeFromTree((IWayGraphVersionMetadata) metadata);
				}
			} else {
				log.warn("Got update to rebuild STR-Tree, but argument object was no instance of IWayGraphVersionMetadata");
			}
		} else {
			log.warn("Got update to rebuild STR-Tree, but metadata object was null");
		}
	}

	public STRTreeIndex getTreeIndex() {
		return treeIndex;
	}

	public void setTreeIndex(STRTreeIndex treeIndex) {
		this.treeIndex = treeIndex;
	}

	public IWayGraphVersionMetadataDao getMetadataDao() {
		return metadataDao;
	}

	public void setMetadataDao(IWayGraphVersionMetadataDao metadataDao) {
		this.metadataDao = metadataDao;
	}

	public List<String> getGraphNames() {
		return graphNames;
	}

	public void setGraphNames(List<String> graphNames) {
		this.graphNames = graphNames;
	}
	
}
