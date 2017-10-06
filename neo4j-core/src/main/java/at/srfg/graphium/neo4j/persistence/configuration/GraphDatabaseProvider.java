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
package at.srfg.graphium.neo4j.persistence.configuration;

import java.io.File;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

/**
 * @author mwimmer
 */
public class GraphDatabaseProvider implements IGraphDatabaseProvider {
	
	private GraphDatabaseService graphDb;
	private String neo4jPropertiesFile;
	private String neo4jGraphDBDirectory;
	
	@PostConstruct
	public void setup() {
		File graphDbDirectory = new File(neo4jGraphDBDirectory);
		graphDb = new GraphDatabaseFactory()
			    .newEmbeddedDatabaseBuilder(graphDbDirectory)
			    .loadPropertiesFromFile(neo4jPropertiesFile)
			    .newGraphDatabase();
	}
	
	@PreDestroy
	public void shutdown() {
		graphDb.shutdown();
	}
	
	@Override	
	public GraphDatabaseService getGraphDatabase() {
		return graphDb;
	}

	public String getNeo4jPropertiesFile() {
		return neo4jPropertiesFile;
	}

	public void setNeo4jPropertiesFile(String neo4jPropertiesFile) {
		this.neo4jPropertiesFile = neo4jPropertiesFile;
	}

	public String getNeo4jGraphDBDirectory() {
		return neo4jGraphDBDirectory;
	}

	public void setNeo4jGraphDBDirectory(String neo4jGraphDBDirectory) {
		this.neo4jGraphDBDirectory = neo4jGraphDBDirectory;
	}

}
