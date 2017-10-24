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

import java.util.List;

import org.apache.commons.lang3.time.StopWatch;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.index.strtree.GeometryItemDistance;
import com.vividsolutions.jts.index.strtree.STRtree;
import com.vividsolutions.jts.io.ParseException;

import at.srfg.graphium.geomutils.GeometryUtils;
import at.srfg.graphium.neo4j.model.WayGraphConstants;
import at.srfg.graphium.neo4j.model.index.STRTreeEntity;
import at.srfg.graphium.neo4j.persistence.configuration.GraphDatabaseProvider;
import at.srfg.graphium.neo4j.persistence.index.STRTreeIndex;

/**
 * @author mwimmer
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:/application-context-graphium-neo4j_test.xml",
		"classpath:/application-context-graphium-core.xml"})
public class TestJtsSTRTree {
	
	private static Logger log = LoggerFactory.getLogger(TestJtsSTRTree.class);
	
	@Autowired
	private GraphDatabaseProvider graphDatabaseProvider;
	
	@Autowired
	private STRTreeIndex treeIndex;
	
	@Test
	public void testJtsSTRTreeV1() {
//		String graphName = "gip_at_frc_0_4";
//		String version = "16_02_160406";
		String graphName = "gip_sbg_city_frc_0_4";
		String version = "16_02_160406";
		
		printMemoryUsage();
		
		STRtree tree = new STRtree();
		
		String graphVersionName = graphName + "_" + version;
		
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
					long segmentId = (long) segmentNode.getProperty(WayGraphConstants.SEGMENT_ID);
					STRTreeEntity entity = new STRTreeEntity(geom.getCoordinateSequence(), geom.getFactory(), segmentId);
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
		
		double x = 13.043516;
		double y = 47.815719;
//		double searchDistance   = 0.0000904776810466969;
		
		Point point = GeometryUtils.createPoint2D(x, y, 4326);
		GeometryItemDistance distance = new GeometryItemDistance();
		STRTreeEntity entity = null;
		if (!tree.isEmpty()) {
			entity = (STRTreeEntity) tree.nearestNeighbour(point.getEnvelopeInternal(), point, distance);
		}
	
		if (entity == null)  {
			log.error("Entity is null");
		} else {
			log.info("Nearest segment is " + entity.getSegmentId());
		}
		
		printMemoryUsage();
		
	}

	@Test
	public void testJtsSTRTreeV2() {
//		String graphName = "gip_at_frc_0_8";
//		String version = "16_04_161103_2";
		String graphName = "gip_at_frc_0_4";
		String version = "16_02_160722";
//		String graphName = "gip_sbg_city_frc_0_4";
//		String version = "16_02_160406";
		String graphVersionName = graphName + "_" + version;
		
		printMemoryUsage();
		
		StopWatch watch = new StopWatch();
		watch.start();
		
		treeIndex.init(graphVersionName);
		
		watch.stop();
		log.info("Building STRTree took " + watch.getTime() + "ms");
		
		double x = 13.043516;
		double y = 47.815719;
		double searchDistance   = 0.000904776810466969;
		
		Point point = GeometryUtils.createPoint2D(x, y, 4326);

		long startTime = System.nanoTime();
		
		List<Long> segmentIds = treeIndex.findNearestSegmentIds(graphVersionName, point, searchDistance, 10);
		
		log.info("findNearestSegmentIds took " + (System.nanoTime() - startTime) + "ns");
		
		if (segmentIds == null || segmentIds.isEmpty()) {
			log.error("No segments found!");
		} else {
			log.info("SegmentIds:");
			for (Long id : segmentIds) {
				log.info("" + id);
			}
		}
		
		printMemoryUsage();
		
	}

	@Test
	public void testSerializeJtsTree() {
		String graphName = "gip_at_frc_0_8";
		String version = "16_04_161103_2";
		String graphVersionName = graphName + "_" + version;
		
		printMemoryUsage();
		
		StopWatch watch = new StopWatch();
		watch.start();
		
		treeIndex.init(graphVersionName);
		
		watch.stop();
		log.info("Building STRTree took " + watch.getTime() + "ms");
	}
	
	private void printMemoryUsage() {
		Runtime runtime = Runtime.getRuntime();
		long allocatedMemory = runtime.totalMemory();
		log.info("Memory Usage: " + (allocatedMemory/(1024*1024)) + " MB");
	}
	
}