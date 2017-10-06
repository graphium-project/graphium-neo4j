/**
 * Graphium Neo4j - Server integration for Graphium modules in Neo4j Standalone server as unmanaged Extensions
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
package at.srfg.graphium.neo4j.configuration;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.neo4j.graphdb.GraphDatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import at.srfg.graphium.neo4j.persistence.configuration.IGraphDatabaseProvider;

/**
 * @author mwimmer
 *
 */
public class GraphiumNeo4jDatabaseProvider implements IGraphDatabaseProvider {
	private static Logger log = LoggerFactory.getLogger(GraphiumNeo4jDatabaseProvider.class);
	
	@Autowired
	private GraphDatabaseService graphDb;
	
	public GraphiumNeo4jDatabaseProvider() {}
	
	public GraphiumNeo4jDatabaseProvider(GraphDatabaseService graphDb) {
		this.graphDb = graphDb;
	}
	
	@PostConstruct
	public void setup() {
		log.info("graph database provided setup and spatial database service created");
	}
	
	@PreDestroy
	public void shutdown() {
	}
	
	@Override	
	public GraphDatabaseService getGraphDatabase() {
		return graphDb;
	}
	
}
