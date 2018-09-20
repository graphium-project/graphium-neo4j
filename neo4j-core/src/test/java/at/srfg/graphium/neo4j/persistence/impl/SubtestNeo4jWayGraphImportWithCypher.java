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

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.junit.Ignore;
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

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

import at.srfg.graphium.core.persistence.IWayGraphWriteDao;
import at.srfg.graphium.model.IWaySegment;
import at.srfg.graphium.neo4j.ITestGraphiumNeo4j;
import at.srfg.graphium.neo4j.model.WayGraphConstants;
import at.srfg.graphium.neo4j.persistence.configuration.GraphDatabaseProvider;

/**
 * @author mwimmer
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:/application-context-graphium-neo4j_test.xml",
		"classpath:/application-context-graphium-core.xml"})
public class SubtestNeo4jWayGraphImportWithCypher implements ITestGraphiumNeo4j {

	private static Logger log = LoggerFactory.getLogger(SubtestNeo4jWayGraphImportWithCypher.class);
	
	@Autowired
	private GraphDatabaseProvider graphDatabaseProvider;

	@Resource(name="neo4jWayGraphWriteDao")
	private IWayGraphWriteDao<IWaySegment> neo4jGraphWriteDao;
	
	private String graphName = "gip_at";
	private String versionName = "test";
	private int batchSizeForSpatialInsertion = 5000;
	
	@Ignore
	@Test
	public void testCreateGraph() {
		String createNodes = "USING PERIODIC COMMIT 10000 " +
							 "LOAD CSV WITH HEADERS FROM \"file:///D:\\\\development\\\\project_data\\\\graphserver\\\\download\\\\gip_at_frc_0_4_16_02_160229.csv\" AS row " +
							 "CREATE (:waysegment {segment_id:row.id, name:row.name, maxspeed_tow: row.maxspeed_tow, "
							 + "maxspeed_bkw: row.maxspeed_bkw, speed_calc_tow: row.speed_calc_tow, speed_calc_bkw: row.speed_calc_bkw, "
							 + "lanes_tow: row.lanes_tow, lanes_bkw: row.lanes_bkw, frc: row.frc, fow: row.fow, streettype: row.streettype, "
							 + "way_id: row.way_id, startnode_id: row.startnode_id, startnode_index: row.startnode_index, "
							 + "endnode_id: row.endnode_id, endnode_index: row.endnode_index, tunnel: row.tunnel, bridge: row.bridge, "
							 + "urban: row.urban, timestamp: row.timestamp, access_tow: row.access_tow, access_bkw: row.access_bkw, wkt: row.wkt});";
	
		String createIndex = "CREATE INDEX ON :waysegment(segment_id);";
	
		String createRelationships = "USING PERIODIC COMMIT 5000 " + 
							 		 "LOAD CSV WITH HEADERS FROM \"file:///D:\\\\development\\\\project_data\\\\graphserver\\\\download\\\\gip_at_frc_0_4_16_02_160229_conns.csv\" AS row " +
							 		 "MERGE (startSeg:waysegment {segment_id: row.from_segment_id}) " +
							 		 "MERGE (endSeg:waysegment {segment_id: row.to_segment_id}) " +
							 		 "MERGE (startSeg)-[:SEGMENT_CONNECTION_ON_STARTNODE]->(endSeg);";
		
		log.info("create index...");
		Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx();
		try {
			graphDatabaseProvider.getGraphDatabase().execute(createIndex); // 2sec
			tx.success();
		} catch (Exception e) {
			e.printStackTrace();
			tx.failure();
		} finally {
			tx.close();
		}

		log.info("create nodes...");
		graphDatabaseProvider.getGraphDatabase().execute(createNodes); // 30sec

		log.info("create relationships...");
		graphDatabaseProvider.getGraphDatabase().execute(createRelationships);
//		LogProvider lp = NullLogProvider.getInstance();
////		LogProvider lp = FormattedLogProvider.getInstance();
//		ExecutionEngine engine = new ExecutionEngine(graphDatabaseProvider.getGraphDatabase(), lp);
//		engine.execute(createRelationships);
		
		// COMMIT 10000 => Dauer: 4:23 => Heap Space Error!
		// COMMIT 10000 für insert und 5000 für rels => Dauer: 1:02
		// COMMIT 20000 für insert und 10000 für rels => Dauer: 1:02
		// COMMIT 10000 für insert und 1000 für rels => Dauer: 1:03

		// COMMIT 10000 für insert und 1000 für rels + Hinzufügen zu Spatial Layer (5000) => Dauer: 3:46

		// ACHTUNG: Heap Size sollte > 2,5GB sein!!!
		
		log.info("create spatial layer...");
		
//
		tx = graphDatabaseProvider.getGraphDatabase().beginTx();
		try {
//			WKTReader wktReader = new WKTReader();
			neo4jGraphWriteDao.createGraph(graphName, versionName, true);
//			Layer layer = graphDatabaseProvider.getSpatialDatabaseService().getOrCreateEditableLayer(graphVersionName);
			ResourceIterator<Node> segmentNodes = graphDatabaseProvider.getGraphDatabase().findNodes(Label.label(WayGraphConstants.SEGMENT_LABEL));
			int i = 1;
			List<Node> nodes = new ArrayList<>();
			while (segmentNodes.hasNext()) {
				nodes.add(segmentNodes.next());
				
				if (i >= batchSizeForSpatialInsertion && i % batchSizeForSpatialInsertion == 0) {
					nodes = new ArrayList<>();
				}
//				Node seg = segmentNodes.next();
//				layer.getGeometryEncoder().encodeGeometry(wktReader.read((String)seg.getProperty("wkt")), seg);
//				layer.add(seg);
//				if (i % 10000 == 0) {
//					log.info(i + "nodes added to spatial layer");
//				}
				i++;
			}

			if (nodes.size() > batchSizeForSpatialInsertion) {
				nodes = new ArrayList<>();
			}

//			IndexManager indexManager = graphDatabaseProvider.getGraphDatabase().index();
//			String[] indexNames = indexManager.nodeIndexNames();
//			Index<Node> segmentIndex = indexManager.forNodes("waysegment_index");
//			IndexHits<Node> segmentNodes = segmentIndex.query("*.*");
//			int i = 0;
//			for (Node segmentNode : segmentNodes) {
//				layer.add(segmentNode);
//				if (i % 10000 == 0) {
//					log.info(i + "nodes added to spatial layer");
//				}
//				i++;
//			}
			
			tx.success();
		} catch (Exception e) {
			e.printStackTrace();
			tx.failure();
		} finally {
			tx.close();
		}
	}

	@Ignore
	@Test
	public void testWktReader() {
		String wkt = "SRID=4326;LINESTRING(15.3042924 48.7563767,15.3043378 48.7562181,15.3043472 48.7560717,15.3043087 48.7558942,15.3042776 48.7557628,15.3042394 48.7556648)";
		WKTReader wktReader = new WKTReader();
		try {
			Geometry geom = wktReader.read(wkt);
			log.info(geom.toString());
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		String wkt2 = "LINESTRING(15.3042924 48.7563767,15.3043378 48.7562181,15.3043472 48.7560717,15.3043087 48.7558942,15.3042776 48.7557628,15.3042394 48.7556648)";
		try {
			Geometry geom = wktReader.read(wkt2);
			log.info(geom.toString());
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public void run() {
	}
	
}