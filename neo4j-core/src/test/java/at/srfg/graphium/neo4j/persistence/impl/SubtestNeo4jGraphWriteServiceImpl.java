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
package at.srfg.graphium.neo4j.persistence.impl;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import at.srfg.graphium.core.exception.GraphNotExistsException;
import at.srfg.graphium.core.service.IGraphWriteService;
import at.srfg.graphium.model.IWaySegment;
import at.srfg.graphium.neo4j.ITestGraphiumNeo4j;

/**
 * @author mwimmer
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:/application-context-graphium-neo4j_test.xml",
		"classpath:/application-context-graphium-core.xml"})
public class SubtestNeo4jGraphWriteServiceImpl implements ITestGraphiumNeo4j {

	private static Logger log = LoggerFactory.getLogger(SubtestNeo4jGraphWriteServiceImpl.class);
	
	@Autowired
	private IGraphWriteService<IWaySegment> graphWriteService;
	
	@Value("${db.graphName}")
	String graphName;
	@Value("${db.version}")
	String version;

	@Test
	public void deleteSegments() {
		try {
			graphWriteService.deleteGraphVersion(graphName, version, true);
		} catch (GraphNotExistsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		log.info("Fertig");
	}

	@Override
	public void run() {
		deleteSegments();
	}

}
