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
import java.io.InputStream;
import java.util.Date;
import java.util.Map;

import javax.annotation.Resource;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.vividsolutions.jts.geom.Polygon;

import at.srfg.graphium.core.exception.GraphAlreadyExistException;
import at.srfg.graphium.core.exception.GraphImportException;
import at.srfg.graphium.core.service.IGraphVersionImportService;
import at.srfg.graphium.model.IWayGraph;
import at.srfg.graphium.model.IWayGraphVersionMetadata;
import at.srfg.graphium.model.impl.WayGraph;
import at.srfg.graphium.model.impl.WayGraphVersionMetadata;
import at.srfg.graphium.model.management.impl.Source;
import at.srfg.graphium.neo4j.persistence.configuration.GraphDatabaseProvider;

/**
 * @author mwimmer
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:/application-context-graphium-core.xml",
		"classpath:application-context-graphium-neo4j_test.xml"})
public class TestNeo4jQueuingGraphVersionImportServiceImpl {

	private static Logger log = LoggerFactory.getLogger(TestNeo4jQueuingGraphVersionImportServiceImpl.class);

	private final static String INPUTFILE = "/development/project_data/evis/gip/1702/gip_at_frc_0_2_17_02_170327_no_pc.json";
	
	@Resource(name="neo4jQueuingGraphVersionImportService")
	private IGraphVersionImportService importService;

	@Test
	public void testImportGraphVersion() {
	// benötigt 4GB Heap, dauert etwa 18min (frc 0-4)
		String graphName = "gip_at_frc_0_4";
		String version = "16_02_160519";
//		String version = "limited_500_15_02_150507";
		String originGraphName = "gip_at";
		String originVersion = "test";
		Date validFrom = new Date();
		Date validTo = null;
		Map<String, String> tags = null;
		int sourceId = 1;
		String sourceName = "GIP";
		String type = "graph";
		String description = "die GIP";
		String creator = "ich";
		String originUrl = "http://gip.at";

		
		
		Polygon coveredArea = null;
		InputStream stream = null;
		
		//Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx();
		try 
		{
			stream = new FileInputStream(INPUTFILE);
			
			IWayGraphVersionMetadata metadata = new WayGraphVersionMetadata();
			metadata.setGraphName(graphName);
			metadata.setVersion(version);
			metadata.setOriginGraphName(originGraphName);
			metadata.setOriginVersion(originVersion);
			metadata.setValidFrom(validFrom);
			metadata.setValidTo(validTo);
			metadata.setType(type);
			metadata.setCreator(creator);
			metadata.setCreationTimestamp(new Date());
			metadata.setCoveredArea(coveredArea);
			metadata.setSource(new Source(sourceId, sourceName));
			metadata.setOriginUrl(originUrl);
			metadata.setDescription(description);

			importService.importGraphVersion(graphName, version, stream, true);
			
	//		tx.success();
			
		} catch (FileNotFoundException e) {
			log.error("file not found", e);
	//		tx.failure();
		} catch (GraphImportException e) {
			log.error("error importing graph", e);
	//		tx.failure();
		} catch (GraphAlreadyExistException e) {
			log.error("error, graph already exists", e);
	//		tx.failure();
		} finally {
//			tx.close();
		}
		
		log.info("Import finished");
		
	}
	
	@Ignore
	@Test
	public void testPostImport() {
		String graphName = "gip_at_frc_0_4";
		String version = "16_02_160318";
		IWayGraph wayGraph = new WayGraph(0, graphName);
		((Neo4jQueuingGraphVersionImportServiceImpl)importService).postImport(wayGraph, version, false);
	}
	
}
