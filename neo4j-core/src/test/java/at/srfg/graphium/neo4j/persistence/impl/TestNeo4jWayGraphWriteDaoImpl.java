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

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import at.srfg.graphium.core.exception.GraphAlreadyExistException;
import at.srfg.graphium.core.exception.GraphNotExistsException;
import at.srfg.graphium.core.persistence.IWayGraphReadDao;
import at.srfg.graphium.core.persistence.IWayGraphWriteDao;
import at.srfg.graphium.io.inputformat.IQueuingGraphInputFormat;
import at.srfg.graphium.model.IDefaultConnectionXInfo;
import at.srfg.graphium.model.IDefaultSegmentXInfo;
import at.srfg.graphium.model.IWayGraphVersionMetadata;
import at.srfg.graphium.model.IWaySegment;
import at.srfg.graphium.model.IWaySegmentConnection;
import at.srfg.graphium.model.impl.DefaultConnectionXInfo;
import at.srfg.graphium.model.impl.DefaultSegmentXInfo;
import at.srfg.graphium.neo4j.persistence.configuration.GraphDatabaseProvider;

/**
 * @author mwimmer
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:/application-context-graphium-neo4j_test.xml",
		"classpath:/application-context-graphium-core.xml"})
public class TestNeo4jWayGraphWriteDaoImpl {

	private static Logger log = LoggerFactory.getLogger(TestNeo4jWayGraphWriteDaoImpl.class);
	
	@Resource(name="neo4jWayGraphWriteDao")
	private IWayGraphWriteDao<IWaySegment> neo4jGraphWriteDao;
	
	@Resource(name="neo4jWayGraphReadDao")
	private IWayGraphReadDao<IWaySegment> neo4jGraphReadDao;

	@Resource(name="jacksonQueuingGraphInputFormat")
	private IQueuingGraphInputFormat<IWaySegment> inputFormat;
	
	@Autowired
	private GraphDatabaseProvider graphDatabaseProvider;
	
//	private String graphVersionName = "gip_at_16_02_160420";
	private String graphName = "gip_at";
	private String versionName = "16_02_160426";
//	private String graphVersionName = "gip_at_test";
//	private String graphName = "gip_at";
//	private String versionName = "test";
	
	@Test
	public void testCreateGraph() {
		Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx();
		try {
//		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx()) {
			neo4jGraphWriteDao.createGraph(graphName, versionName, true);
			tx.success();
		} catch (GraphAlreadyExistException | GraphNotExistsException e) {
			e.printStackTrace();
			tx.failure();
		} finally {
			tx.close();
		}
	}

	@Test
	public void testParseAndSaveSgments() {
		int batchSize = 1000;
		String jsonFile = "D:/development/project_data/graphserver/upload/gip_at_frc_0_4_limited_500_15_10_151222a.json";
		//String jsonFile = "C:/development/Graphserver/working_data/central_server/upload/gip_at_frc_0_4_limited_500_15_02_150507.json";
		BlockingQueue<IWaySegment> segmentsQueue = new ArrayBlockingQueue<IWaySegment>(10000);
		BlockingQueue<IWayGraphVersionMetadata> metadataQueue = new ArrayBlockingQueue<IWayGraphVersionMetadata>(1);

		Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx();
		try {
			InputStream stream = null;
			
			stream = new FileInputStream(jsonFile);
			inputFormat.deserialize(stream, segmentsQueue, metadataQueue);
			
			neo4jGraphWriteDao.createGraphVersion(graphName, versionName, true, true);

			// save segments via view
			List<IWaySegment> segmentsToSave = new ArrayList<IWaySegment>();
			Map<Long, List<IWaySegmentConnection>> connectionIntegrityMap = new HashMap<Long, List<IWaySegmentConnection>>();
			List<IWaySegmentConnection> connectionsToSave = new ArrayList<IWaySegmentConnection>();
			List<Long> segmentIds = new ArrayList<Long>();
			while (!segmentsQueue.isEmpty()) {
				IWaySegment segment;
				try {
					segment = segmentsQueue.poll(5000, TimeUnit.MILLISECONDS);
					
					if (segment != null) {
						segmentsToSave.add(segment);
						segmentIds.add(segment.getId());
						addConnectionsToIntegrityList(segment, connectionIntegrityMap);
						
						if (segmentsToSave.size() == batchSize) {
							neo4jGraphWriteDao.saveSegments(segmentsToSave, graphName, versionName);
							connectionsToSave = getValidConnections(connectionIntegrityMap, segmentIds);
							neo4jGraphWriteDao.saveConnections(connectionsToSave, graphName, versionName);
							segmentsToSave.clear();
							connectionsToSave.clear();
						}
					}

				} catch (InterruptedException e) {
					log.error("error during thread sleep", e);
				}
			}

			if (!segmentsToSave.isEmpty()) {
				neo4jGraphWriteDao.saveSegments(segmentsToSave, graphName, versionName);
				connectionsToSave = getValidConnections(connectionIntegrityMap, segmentIds);
				neo4jGraphWriteDao.saveConnections(connectionsToSave, graphName, versionName);
				segmentsToSave.clear();
				connectionsToSave.clear();
			}
			
			tx.success();

		} catch (Exception e) {
			e.printStackTrace();
			tx.failure();
		} finally {
			tx.close();
		}
		
//		testPrintNodes();
	}
	
	@Test
	public void testUpdateSegmentAttributes() {
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx();) 
		{
//			long segmentId = 101166657;
			long segmentId = 33035975;
			IWaySegment segment = neo4jGraphReadDao.getSegmentById(graphName, versionName, segmentId, false);
			List<IWaySegment> segments = new ArrayList<>(1);
			segment.setSpeedCalcTow((short) 50);
			segments.add(segment);
			
			neo4jGraphWriteDao.updateSegmentAttributes(segments, graphName, versionName);
			tx.success();
		} catch (Exception e) {
			log.error(e.toString(), e);
		}
	}

	/**
	 * @param connectionIntegrityList
	 * @return
	 */
	private List<IWaySegmentConnection> getValidConnections(
			Map<Long, List<IWaySegmentConnection>> connectionsMap, List<Long> segmentIds) {
		List<IWaySegmentConnection> validConnections = new ArrayList<IWaySegmentConnection>();
		List<IWaySegmentConnection> connectionsList;
		for (Long segmentId : segmentIds) {
			connectionsList = connectionsMap.get(segmentId);
			if (connectionsList != null) {
				validConnections.addAll(connectionsList);
				connectionsMap.remove(segmentId);
			}
		}

		if (log.isDebugEnabled()) {
			int waitingConnectionsSize = 0;
			for (List<IWaySegmentConnection> conns : connectionsMap.values()) {
				waitingConnectionsSize += conns.size();
			}
			log.debug(validConnections.size() + " valid connections found to save (" + 
					waitingConnectionsSize + " connections wait for saving...)");
		}

		return validConnections;
	}

	/**
	 * @param segment
	 * @param connectionIntegrityList
	 */
	private void addConnectionsToIntegrityList(IWaySegment segment,
			Map<Long, List<IWaySegmentConnection>> connectionIntegrityMap) {
		if (segment.getStartNodeCons() != null) {
			for (IWaySegmentConnection conn : segment.getStartNodeCons()) {
				if (!connectionIntegrityMap.containsKey(conn.getToSegmentId())) {
					connectionIntegrityMap.put(conn.getToSegmentId(), new ArrayList<>());
				}
				connectionIntegrityMap.get(conn.getToSegmentId()).add(conn);
			}
		}
		if (segment.getEndNodeCons() != null) {
			for (IWaySegmentConnection conn : segment.getEndNodeCons()) {
				if (!connectionIntegrityMap.containsKey(conn.getToSegmentId())) {
					connectionIntegrityMap.put(conn.getToSegmentId(), new ArrayList<>());
				}
				connectionIntegrityMap.get(conn.getToSegmentId()).add(conn);
			}
		}
	}

	@Test
	public void testUpdateSegmentWithXInfo() {
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx();) 
		{
//			long segmentId = 101166657;
			long segmentId = 901551174;
			IWaySegment segment = neo4jGraphReadDao.getSegmentById(graphName, versionName, segmentId, true);
			List<IWaySegment> segments = new ArrayList<>(1);
			segments.add(segment);
			
			IDefaultSegmentXInfo xInfo = new DefaultSegmentXInfo();
			Map<String, Object> values = new HashMap<>();
			values.put("dummyFloat", 3.9f);
			xInfo.setValues(values);
			xInfo.setSegmentId(segmentId);
			xInfo.setDirectionTow(true);
			segment.addXInfo(xInfo);
			
			IDefaultConnectionXInfo connXInfo = new DefaultConnectionXInfo();
			connXInfo.setValue("dummyDouble", 4.3d);
			if (segment.getStartNodeCons() != null && !segment.getStartNodeCons().isEmpty()) {
				segment.getStartNodeCons().get(0).addXInfo(connXInfo);
			} else if (segment.getEndNodeCons() != null && !segment.getEndNodeCons().isEmpty()) {
				segment.getEndNodeCons().get(0).addXInfo(connXInfo);
			}
			
			neo4jGraphWriteDao.updateSegments(segments, graphName, versionName);
			tx.success();
		} catch (Exception e) {
			log.error(e.toString(), e);
		}
	}
	
	@Test
	public void deleteSegments() {
		String graphName = "gip_at_frc_0_8";
		String versionName = "16_04_161103_2";
		try {
			neo4jGraphWriteDao.deleteSegments(graphName, versionName);
		} catch (GraphNotExistsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		log.info("Fertig");
	}

}
