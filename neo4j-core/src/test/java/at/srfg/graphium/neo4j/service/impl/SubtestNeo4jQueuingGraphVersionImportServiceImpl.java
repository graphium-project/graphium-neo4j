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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Resource;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import at.srfg.graphium.core.exception.GraphAlreadyExistException;
import at.srfg.graphium.core.exception.GraphImportException;
import at.srfg.graphium.core.service.IGraphVersionImportService;
import at.srfg.graphium.model.IWayGraph;
import at.srfg.graphium.model.IWaySegment;
import at.srfg.graphium.model.impl.WayGraph;
import at.srfg.graphium.neo4j.ITestGraphiumNeo4j;

/**
 * @author mwimmer
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:/application-context-graphium-core.xml",
		"classpath:application-context-graphium-neo4j_test.xml"})
public class SubtestNeo4jQueuingGraphVersionImportServiceImpl implements ITestGraphiumNeo4j {

	private static Logger log = LoggerFactory.getLogger(SubtestNeo4jQueuingGraphVersionImportServiceImpl.class);

	@Value("${db.graphName}")
	String graphName;
	@Value("${db.version}")
	String version;
	@Value("${db.inputFileName}")
	String inputFileName;
	
	@Resource(name="neo4jQueuingGraphVersionImportService")
	private IGraphVersionImportService<? extends IWaySegment> importService;

	@Override
	public void run() {
		try {
			testImportGraphVersion();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	//@Test
	public void testImportGraphVersion() throws IOException {
		
		InputStream stream = null;
		
		try 
		{
			stream = new FileInputStream(inputFileName);

			importService.importGraphVersion(graphName, version, stream, true);
			
		} catch (FileNotFoundException e) {
			log.error("file not found", e);
		} catch (GraphImportException e) {
			log.error("error importing graph", e);
		} catch (GraphAlreadyExistException e) {
			log.error("error, graph already exists", e);
		} finally {
		}
		
		log.info("Import finished");
		
	}
	
	@SuppressWarnings("rawtypes")
	@Ignore
	@Test
	public void testPostImport() {
		IWayGraph wayGraph = new WayGraph(0, graphName);
		((Neo4jQueuingGraphVersionImportServiceImpl)importService).postImport(wayGraph, version, false);
	}
	
}
