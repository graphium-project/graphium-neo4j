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
package at.srfg.graphium.neo4j.persistence.impl;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.schema.IndexDefinition;

import at.srfg.graphium.neo4j.persistence.INeo4jWayGraphIndexDao;
import at.srfg.graphium.neo4j.persistence.configuration.IGraphDatabaseProvider;

/**
 * @author mwimmer
 *
 */
public class Neo4jWayGraphIndexDaoImpl implements INeo4jWayGraphIndexDao {

	private IGraphDatabaseProvider graphDatabaseProvider;
	private int awaitIndexesOnlineInSec = 0;
	
	/**
	 * drops an existing Neo4j (internal) index on the attribute "segmentId"
	 * @param graphVersionName
	 */
	@Override
	public void dropNeosSegmentIndex(String graphVersionName) {
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx()) {
			String indexLabel = Neo4jWaySegmentHelperImpl.createSegmentNodeLabel(graphVersionName);
			Iterable<IndexDefinition> indexDefs = graphDatabaseProvider.getGraphDatabase().schema().getIndexes(Label.label(indexLabel));
			Iterator<IndexDefinition> itIndexDefs = indexDefs.iterator();
			while (itIndexDefs.hasNext()) {
				itIndexDefs.next().drop();
			}
			tx.success();
		}
	}
	
	/**
	 * creates an Neo4j (internal) index
	 * @param graphVersionName
	 */
	@Override
	public void createIndexes(String graphVersionName) {
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx()) {
			String indexLabel = Neo4jWaySegmentHelperImpl.createSegmentNodeLabel(graphVersionName);
			graphDatabaseProvider.getGraphDatabase().execute("CREATE INDEX ON :" + indexLabel + "(segment_id)");
			if (awaitIndexesOnlineInSec > 0) {
				graphDatabaseProvider.getGraphDatabase().schema().awaitIndexesOnline(awaitIndexesOnlineInSec, TimeUnit.SECONDS);
			}
			
			tx.success();
		}
	}

	public IGraphDatabaseProvider getGraphDatabaseProvider() {
		return graphDatabaseProvider;
	}

	public void setGraphDatabaseProvider(IGraphDatabaseProvider graphDatabaseProvider) {
		this.graphDatabaseProvider = graphDatabaseProvider;
	}

	public int getAwaitIndexesOnlineInSec() {
		return awaitIndexesOnlineInSec;
	}

	public void setAwaitIndexesOnlineInSec(int awaitIndexesOnlineInSec) {
		this.awaitIndexesOnlineInSec = awaitIndexesOnlineInSec;
	}

}