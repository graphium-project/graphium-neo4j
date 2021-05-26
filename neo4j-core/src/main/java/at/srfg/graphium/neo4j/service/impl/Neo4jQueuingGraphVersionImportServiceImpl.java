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

import java.util.List;

import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.srfg.graphium.core.exception.GraphAlreadyExistException;
import at.srfg.graphium.core.exception.GraphImportException;
import at.srfg.graphium.core.exception.GraphNotExistsException;
import at.srfg.graphium.core.helper.GraphVersionHelper;
import at.srfg.graphium.core.service.impl.QueuingGraphVersionImportServiceImpl;
import at.srfg.graphium.model.IWayGraph;
import at.srfg.graphium.model.IWaySegment;
import at.srfg.graphium.model.IWaySegmentConnection;
import at.srfg.graphium.neo4j.persistence.INeo4jWayGraphIndexDao;
import at.srfg.graphium.neo4j.persistence.configuration.IGraphDatabaseProvider;

/**
 * @author mwimmer
 */
public class Neo4jQueuingGraphVersionImportServiceImpl<T extends IWaySegment> extends QueuingGraphVersionImportServiceImpl<T> {

	private static Logger log = LoggerFactory.getLogger(Neo4jQueuingGraphVersionImportServiceImpl.class);
	
	private IGraphDatabaseProvider graphDatabaseProvider;
	private INeo4jWayGraphIndexDao indexDao;
	private int batchSizeForSpatialInsertion = 5000;

	@Override
	public void preImport(String graphName, String version) {
		super.preImport(graphName, version);
		
		// Drop Neo4j's internal index on segmentId (if exists).
		// This (schema update) has to be done before any data update!
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx()) {
			indexDao.dropNeosSegmentIndex(GraphVersionHelper.createGraphVersionName(graphName, version));
			
			tx.success();
		}
	}
	
	private void deleteGraphVersion(String graphName, String version) throws GraphNotExistsException {
		writeDao.deleteSegments(graphName, version);
		metadataDao.deleteWayGraphVersionMetadata(graphName, version, false);
	}

	@Override
	protected void saveBatch(List<T> segmentsToSave, List<IWaySegmentConnection> connectionsToSave,
			String graphName, String version) throws GraphImportException {
		Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx();
		try {
			super.saveBatch(segmentsToSave, connectionsToSave, graphName, version);
			tx.success();
		} catch (Exception e) {
			e.printStackTrace();
			tx.failure();
			
			// Exception werfen, damit der Import abgebrochen wird
			throw new GraphImportException("Error during saving batch", e);
			
		} finally {
			tx.close();
		}
	}

	@Override
	protected void handleImportError(String graphName, String version, Exception e) throws GraphImportException {
		log.error(e.toString(), e);
		// Hier muss manuell ein Rollback getriggered werden => Löschen der Graphversion + Metadata + Spatial Layer
		try {
			if (e instanceof GraphAlreadyExistException) {
				// we must not delete the graph version
			} else {
				deleteGraphVersion(graphName, version);
			}
		} catch (GraphNotExistsException e1) {
			// do nothing
		}
		throw new GraphImportException(e.getMessage(), e);
	}

	@Override
	public void postImport(IWayGraph wayGraph, String version, boolean graphVersionAlreadyExisted) {
		super.postImport(wayGraph, version, graphVersionAlreadyExisted);
		
		String graphName = wayGraph.getName();
		log.info("Creating indexes...");
		writeDao.createIndexes(graphName, version);
		log.info("Indexes created successfully");
	}

	public IGraphDatabaseProvider getGraphDatabaseProvider() {
		return graphDatabaseProvider;
	}

	public void setGraphDatabaseProvider(IGraphDatabaseProvider graphDatabaseProvider) {
		this.graphDatabaseProvider = graphDatabaseProvider;
	}

	public int getBatchSizeForSpatialInsertion() {
		return batchSizeForSpatialInsertion;
	}

	public void setBatchSizeForSpatialInsertion(int batchSizeForSpatialInsertion) {
		this.batchSizeForSpatialInsertion = batchSizeForSpatialInsertion;
	}

	public INeo4jWayGraphIndexDao getIndexDao() {
		return indexDao;
	}

	public void setIndexDao(INeo4jWayGraphIndexDao indexDao) {
		this.indexDao = indexDao;
	}

}