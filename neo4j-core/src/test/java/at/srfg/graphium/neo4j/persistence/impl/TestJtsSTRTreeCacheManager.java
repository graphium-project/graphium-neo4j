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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.vividsolutions.jts.geom.Point;

import at.srfg.geomutils.GeometryUtils;
import at.srfg.graphium.neo4j.service.impl.STRTreeService;

/**
 * @author mwimmer
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:/application-context-graphium-neo4j_test.xml",
		"classpath:/application-context-graphium-core.xml"})
public class TestJtsSTRTreeCacheManager {
	
	private static Logger log = LoggerFactory.getLogger(TestJtsSTRTreeCacheManager.class);
	
	@Autowired
	private STRTreeService indexService;
	
	@Test
	public void testCacheManager() {
//		String graphName = "gip_at_frc_0_8";
//		String version = "16_04_161103_2";
//		String graphName = "gip_at_frc_0_4";
//		String version = "16_10_170411";
//		String version2 = "16_10_170410";
//		String graphName = "osm_at";
//		String version = "170407";
//		String version2 = "161223";
		String graphName = "gip_at_mm";
		String version = "14_11_170906";
		String version2 = "15_02_170906";

		double x = 13.043516;
		double y = 47.815719;
		double searchDistance   = 0.000904776810466969;
		
		Point point = GeometryUtils.createPoint2D(x, y, 4326);

		long startTime = System.nanoTime();
		
		List<Long> segmentIds = indexService.findNearestSegmentIds(graphName, version, point, searchDistance, 10);
		
		log.info("findNearestSegmentIds took " + (System.nanoTime() - startTime) + "ns");
		
		startTime = System.nanoTime();
		
		segmentIds = indexService.findNearestSegmentIds(graphName, version2, point, searchDistance, 10);
		
		log.info("findNearestSegmentIds took " + (System.nanoTime() - startTime) + "ns");
		
		log.info("waiting for 1 sec... - index should be kept in cache");
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
		}
		
		startTime = System.nanoTime();
		
		segmentIds = indexService.findNearestSegmentIds(graphName, version2, point, searchDistance, 10);
		
		log.info("findNearestSegmentIds took " + (System.nanoTime() - startTime) + "ns");
		
		log.info("waiting for 3 sec... - index should be evicted from cache");
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
		}
		
		startTime = System.nanoTime();
		
		segmentIds = indexService.findNearestSegmentIds(graphName, version2, point, searchDistance, 10);
		
		log.info("findNearestSegmentIds took " + (System.nanoTime() - startTime) + "ns");

		if (segmentIds == null || segmentIds.isEmpty()) {
			log.error("No segments found!");
		} else {
			log.info("SegmentIds:");
			for (Long id : segmentIds) {
				log.info("" + id);
			}
		}
		
	}
	
}