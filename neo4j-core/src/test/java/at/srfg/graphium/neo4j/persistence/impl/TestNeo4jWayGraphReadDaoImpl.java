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

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import at.srfg.graphium.core.persistence.IWayGraphReadDao;
import at.srfg.graphium.io.outputformat.ISegmentOutputFormat;
import at.srfg.graphium.io.outputformat.ISegmentOutputFormatFactory;
import at.srfg.graphium.model.IWaySegment;
import at.srfg.graphium.neo4j.persistence.configuration.GraphDatabaseProvider;

/**
 * @author mwimmer
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:/application-context-graphium-neo4j_test.xml",
		"classpath:/application-context-graphium-core.xml"})
public class TestNeo4jWayGraphReadDaoImpl {

	private static Logger log = LoggerFactory.getLogger(TestNeo4jWayGraphReadDaoImpl.class);
	
	@Resource(name="neo4jWayGraphReadDao")
	private IWayGraphReadDao<IWaySegment> neo4jGraphReadDao;

	@Autowired
	private GraphDatabaseProvider graphDatabaseProvider;
	
	@Resource(name="jacksonSegmentOutputFormatFactory")
	private ISegmentOutputFormatFactory segmentOutputFormatFactory;

	private String graphName = "gip_at_frc_0_4";
	private String versionName = "16_02_160426";
	
	@Test
	public void testStreamSegments() {
		Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx();
		try {
			OutputStream os = new ByteArrayOutputStream();
			ISegmentOutputFormat<IWaySegment> graphOutputFormat;
			graphOutputFormat = (ISegmentOutputFormat<IWaySegment>) segmentOutputFormatFactory.getSegmentOutputFormat(os);
			neo4jGraphReadDao.streamSegments(graphOutputFormat, null, graphName, versionName);
			tx.success();
		} catch (Exception e) {
			e.printStackTrace();
			tx.failure();
		} finally {
			tx.close();
		}
	}

	@Test
	public void testStreamSegmentsWithIdFilter() {
		Set<Long> ids = new HashSet<>();
		ids.add(901551169L);
		ids.add(901551109L);
		Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx();
		try {
			OutputStream os = new ByteArrayOutputStream();
			ISegmentOutputFormat<IWaySegment> graphOutputFormat;
			graphOutputFormat = (ISegmentOutputFormat<IWaySegment>) segmentOutputFormatFactory.getSegmentOutputFormat(os);
			neo4jGraphReadDao.streamSegments(graphOutputFormat, graphName, versionName, ids);
			tx.success();
		} catch (Exception e) {
			e.printStackTrace();
			tx.failure();
		} finally {
			tx.close();
		}
	}

	@Test
	public void testGetSegmentById() {
		long segmentId = 901414292;
		IWaySegment segment = null;
		Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx();
		try {
			segment = neo4jGraphReadDao.getSegmentById(graphName, versionName, segmentId, true);
			tx.success();
		} catch (Exception e) {
			e.printStackTrace();
			tx.failure();
		} finally {
			tx.close();
		}
		
		Assert.assertNotNull(segment);
		Assert.assertEquals(segmentId, segment.getId());
		Assert.assertEquals(2, segment.getStartNodeCons().size());
		Assert.assertEquals(1, segment.getEndNodeCons().size());
		
	}
	
	@Test
	public void countSegments() {
		
		String graphName = "gip_at_frc_0_4";
		String versionName = "16_02_160519";
		
		Result result = graphDatabaseProvider.getGraphDatabase().execute(
				"MATCH (n:" + Neo4jWaySegmentHelperImpl.createSegmentNodeLabel(graphName + "_" + versionName) + ") RETURN count(n)");
		List<String> cols = result.columns();
		//result.columnAs("count(n)");
		log.info(StringUtils.join(cols));
		
		 while ( result.hasNext() )
		    {
		        Map<String,Object> row = result.next();
		        for ( Entry<String,Object> column : row.entrySet() )
		        {
		            log.info(column.getKey() + ": " + column.getValue());
		        }
		    }
		
	}

}
