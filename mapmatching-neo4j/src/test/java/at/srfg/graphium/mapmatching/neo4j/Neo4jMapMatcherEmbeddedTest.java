/**
 * Graphium Neo4j - Module of Graphserver for Map Matching using Neo4j
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
package at.srfg.graphium.mapmatching.neo4j;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CancellationException;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.TransactionTerminatedException;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import at.srfg.graphium.core.exception.GraphNotExistsException;
import at.srfg.graphium.geomutils.GeometryUtils;
import at.srfg.graphium.io.adapter.IAdapter;
import at.srfg.graphium.mapmatching.dto.TrackDTO;
import at.srfg.graphium.mapmatching.matcher.IMapMatcherTask;
import at.srfg.graphium.mapmatching.matcher.impl.MapMatchingServiceImpl;
import at.srfg.graphium.mapmatching.model.IMatchedBranch;
import at.srfg.graphium.mapmatching.model.IMatchedWaySegment;
import at.srfg.graphium.mapmatching.model.ITrack;
import at.srfg.graphium.mapmatching.model.ITrackMetadata;
import at.srfg.graphium.mapmatching.model.ITrackPoint;
import at.srfg.graphium.mapmatching.model.impl.TrackImpl;
import at.srfg.graphium.mapmatching.model.impl.TrackMetadataImpl;
import at.srfg.graphium.mapmatching.model.impl.TrackPointImpl;
import at.srfg.graphium.mapmatching.neo4j.async.AsyncMapMatchingTask;
import at.srfg.graphium.mapmatching.neo4j.matcher.impl.MapMatchingTask;
import at.srfg.graphium.mapmatching.neo4j.matcher.impl.Neo4jMapMatcher;
import at.srfg.graphium.model.IWaySegment;
import at.srfg.graphium.neo4j.model.WayGraphConstants;
import at.srfg.graphium.neo4j.model.WaySegmentRelationshipType;
import at.srfg.graphium.neo4j.persistence.INeo4jWayGraphReadDao;
import at.srfg.graphium.neo4j.persistence.Neo4jUtil;
import at.srfg.graphium.routing.exception.RoutingParameterException;

/**
 * @author mwimmer
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:/application-context-graphium-core.xml",
		"classpath:/application-context-graphium-neo4j-persistence.xml",
		"classpath:/application-context-graphium-neo4j-aliasing.xml",
		"classpath:/application-context-graphium-mapmatching.xml",
		"classpath:/application-context-graphium-mapmatching-neo4j.xml",
		"classpath:/application-context-graphium-routing-neo4j.xml",
		"classpath:/application-context-graphium-neo4j_test.xml"})
public class Neo4jMapMatcherEmbeddedTest {

	private static Logger log = LoggerFactory.getLogger(Neo4jMapMatcherEmbeddedTest.class);

	@Autowired
	private Neo4jMapMatcher mapMatcher;
	
	@Autowired
	private IAdapter<ITrack, TrackDTO> adapter;

	@Autowired
	private Neo4jUtil neo4jUtil;
	
	@Autowired
	private MapMatchingServiceImpl mapMatchingService;
	
//	private String routingMode = "bike";
	private String routingMode = "car";
	
	@Test
	public void testMatchTrack() {
//		String graphName = "osm_at";
		String graphName = "gip_at_miv";
//		String graphName = "osm_at_with_lower_level_streets";
//		String graphName = "gip_at_frc_0_4";
//		String graphName = "osm_biobs";
//		String graphName = "gip_at_frc_0_8";
//		long trackId = 18394396;
//		long trackId = 19991780;
//		long trackId = 18241517;
		
		String trackId = "97488203";
//		String trackId = "98921998";
//		String trackId = "89412876";
//		String trackId = "105868664";
		
//		long trackId = 4893166;
		
		//long trackId = 12761079;
		//long trackId = 12774066;	// 2 Routings
		//long trackId = 12779916;	// kein Routing
		//long trackId = 12779918;	// 4 Routings

		matchTrack(trackId, graphName, false);
		
	}		
	
	@Test
	public void testMatchTrackAsync() {
		String graphName = "gip_at_miv";
		String trackId = "63541687";
		String fileName = "C:/development/project_data/mapmatcher/testsuite/tracks_json/" + trackId + ".json";
		int nrOfThreads = 8;
		int nrOfRuns = 100;

		ObjectMapper mapper = new ObjectMapper();
		TrackDTO trackDto;
		try {
			log.info("Matching Track " + trackId);

			trackDto = mapper.readValue(new File(fileName), TrackDTO.class);
			ITrack track = adapter.adapt(trackDto);
			
			for (int i=0; i<nrOfRuns; i++) {
				runAsync(track, graphName, nrOfThreads);
			}
			
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}
		
		log.info("Matching Track " + trackId + " finished");

	}		
	
	private void runAsync(ITrack track, String graphName, int nrOfThreads) {
		List<Thread> threads = new ArrayList<>(nrOfThreads);
		
		for (int i=0; i<nrOfThreads; i++) {
			AsyncMapMatchingTask mapMatchingTask = new AsyncMapMatchingTask(mapMatcher, track, graphName);
		
			Thread thread = new Thread(mapMatchingTask);
			threads.add(thread);
			thread.start();
		}
		
		while (!checkThreadsFinished(threads)) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}

	private boolean checkThreadsFinished(List<Thread> threads) {
		for (Thread thread : threads) {
			if (thread.isAlive()) {
				return false;
			}
		}
		return true;
	}

	@Test
	public void testMatchTrackWithTimeoutGuard() {
//		String graphName = "osm_at";
//		String graphName = "osm_at_with_lower_level_streets";
//		String graphName = "gip_at_frc_0_4";
		String graphName = "gip_at_miv";
//		String graphName = "osm_biobs";
//		String graphName = "gip_at_frc_0_8";
		
		String trackId = "97488203";
		
		matchTrack(trackId, graphName, true);
		
	}		
	
	@Test
	public void testMatchTrackOnline() {
		String graphName = "osm_at";
		long trackId = 20353522;

		mapMatcher.getProperties().setOnline(true);

		String fileName = "D:/development/project_data/graphserver/tests/tracks/json/" + trackId + ".json";

		ObjectMapper mapper = new ObjectMapper();
		TrackDTO trackDto;
		List<IMatchedBranch> branches = null;
		try {
			trackDto = mapper.readValue(new File(fileName), TrackDTO.class);
			ITrack track = adapter.adapt(trackDto);
			
			branches = matchTrack(track, graphName);
			
			printBranches(branches);
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		} catch (GraphNotExistsException e) {
			log.error(e.getMessage(), e);
		} catch (RoutingParameterException e) {
			log.error(e.getMessage(), e);
		}
		
		if (branches != null && !branches.isEmpty()) {
			IMatchedWaySegment safeSegment = branches.get(0).getCertainPathEndSegment();
			log.info("Certain path end segment is " + (safeSegment == null ? "NULL!!!" : safeSegment.getId()));
		}
		
	}		
	
	@Test
	public void testMatchTracks() {
		String graphName = "gip_at_frc_0_8";
		String[] trackIds = new String[] {
//				10230393L,
//				6379588L,
//				5541664L,
//				};
				
//				5277778L,
//				17907122L,
//				14922302L,
//				9921823L,
////				};
//				
//				19126609L,
//				18501705L,
//				18394396L,
//				18241517L,
////				};
//		
//				4994362L,
//				4976334L,
//				4863513L,
//				5001494L,
//				5006169L,
//				4927281L,
//				4893166L,
//				4993115L,
////				};
//
////				22948755L,
////				22948732L,
////				22948726L,
////				22948759L
////		};
//				
//				22948293L,
//				22948301L,
//				22948325L,
//				22948418L,
//				22948425L,
//				22948537L,
//				22948692L,
//				22948693L,
//				22948726L,
//				22948732L,
//				22948736L,
//				22948755L,
//				22948759L

				"5001494",
				"5006169",
				"4927281"

		
		};
				
		for (String trackId : trackIds) {
			matchTrack(trackId, graphName, false);
		}
		
	}

	private List<IMatchedBranch> matchTrack(String trackId, String graphName, boolean considerTimeout) {
//		String fileName = "C:/development/project_data/evis/tracks/json/" + trackId + ".json";
		String fileName = "C:/development/project_data/mapmatcher/testsuite/tracks_json/" + trackId + ".json";

		ObjectMapper mapper = new ObjectMapper();
		TrackDTO trackDto;
		List<IMatchedBranch> branches = null;
		try {
			trackDto = mapper.readValue(new File(fileName), TrackDTO.class);
			ITrack track = adapter.adapt(trackDto);
			
			log.info("Matching Track " + trackId);
			
			if (considerTimeout) {
				int timeoutInMs = 1000;
				branches = matchTrackWithTimeoutGuard(track, graphName, timeoutInMs);
			} else {
				branches = matchTrack(track, graphName);
			}
			
			printBranches(branches);
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		} catch (GraphNotExistsException e) {
			log.error(e.getMessage(), e);
		} catch (RoutingParameterException e) {
			log.error(e.getMessage(), e);
		}
		
		return branches;
	}
	
	@Test
	public void testMatchTrackInLoop() throws RoutingParameterException {
		
//		try {
//			System.in.read();
//		} catch (IOException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
//		
		
//		String graphName = "gip_at_frc_0_4";
		String graphName = "gip_at_frc_0_8";
		
//		long trackId = 19130214;
//		long trackId = 191302142;
//		long trackId = 5006169;
		
		
		long trackId = 19114064;
		
		
//		long trackId = 5006169;
		
		
//		long trackId = 15187779;
//		long trackId = 19091471;
//		long trackId = 4994362;
		
		
		//long trackId = 12761079;
		//long trackId = 12774066;	// 2 Routings
		//long trackId = 12779916;	// kein Routing
		//long trackId = 12779918;	// 4 Routings

		String fileName = "D:/development/project_data/graphserver/tests/tracks/json/" + trackId + ".json";

		ObjectMapper mapper = new ObjectMapper();
		TrackDTO trackDto;
		try {
			trackDto = mapper.readValue(new File(fileName), TrackDTO.class);
			ITrack track = adapter.adapt(trackDto);
			
			for (int i=0; i<5; i++) {
				List<IMatchedBranch> branches = matchTrack(track, graphName);
				
				printBranches(branches);
			}
			
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		} catch (GraphNotExistsException e) {
			log.error(e.getMessage(), e);
		}
		
	}

	/**
	 * @param track
	 * @return
	 * @throws GraphNotExistsException 
	 */
	private List<IMatchedBranch> matchTrack(ITrack track, String graphName) throws GraphNotExistsException, RoutingParameterException {
		long startTime = System.nanoTime();
		IMapMatcherTask task = mapMatcher.getTask(graphName, track, routingMode);
		List<IMatchedBranch> branches = task.matchTrack();
		log.info("Map matching took " +  + (System.nanoTime() - startTime) + "ns = " + ((System.nanoTime() - startTime) / 1000000) + "ms");
		return branches;
	}

	/**
	 * @param track
	 * @return
	 * @throws GraphNotExistsException 
	 */
	private List<IMatchedBranch> matchTrackAysnc(ITrack track, String graphName) throws GraphNotExistsException, RoutingParameterException {
		long startTime = System.nanoTime();
		IMapMatcherTask task = mapMatcher.getTask(graphName, track, routingMode);
		List<IMatchedBranch> branches = task.matchTrack();
		log.info("Map matching took " +  + (System.nanoTime() - startTime) + "ns = " + ((System.nanoTime() - startTime) / 1000000) + "ms");
		return branches;
	}

	/**
	 * @param track
	 * @return
	 * @throws GraphNotExistsException 
	 */
	private List<IMatchedBranch> matchTrackWithTimeoutGuard(ITrack track, String graphName, int timeoutInMs) throws GraphNotExistsException, RoutingParameterException {
		long startTime = System.nanoTime();
		List<IMatchedBranch> branches = mapMatchingService.matchTrack(graphName, null, track, null, null, timeoutInMs, true, routingMode);
		log.info("Map matching took " +  + (System.nanoTime() - startTime) + "ns = " + ((System.nanoTime() - startTime) / 1000000) + "ms");
		return branches;
	}

	private void printBranches(List<IMatchedBranch> branches) {
		for (IMatchedBranch branch : branches) {
			log.info("Branch with matched factor " + branch.getMatchedFactor());

			List<String> segmentIdsAndIndices = new ArrayList<>(branch.getMatchedWaySegments().size());
			List<String> segmentIds = new ArrayList<>(branch.getMatchedWaySegments().size());
			for (IMatchedWaySegment segment : branch.getMatchedWaySegments()) {
				segmentIdsAndIndices.add(segment.getId() + "(" + segment.getStartPointIndex() + "-" + segment.getEndPointIndex() + ")");
				segmentIds.add("" + segment.getId());
			}

			log.info(StringUtils.join(segmentIdsAndIndices, ", "));
			log.info(StringUtils.join(segmentIds, ", "));
		}
	}
	
	@Test
	public void testTraverser() throws RoutingParameterException {
		
		String graphName = "gip_at_frc_0_8";
		String version = "16_04_161103_2";
		long segmentId = 901269836;
		long trackId = 5006169;
		
		String fileName = "D:/development/project_data/graphserver/tests/tracks/json/" + trackId + ".json";

		ObjectMapper mapper = new ObjectMapper();
		TrackDTO trackDto;
		try {
			trackDto = mapper.readValue(new File(fileName), TrackDTO.class);
			ITrack track = adapter.adapt(trackDto);

			MapMatchingTask task = (MapMatchingTask) mapMatcher.getTask(graphName, track, routingMode);
			
			Transaction tx = task.getGraphDao().getGraphDatabaseProvider().getGraphDatabase().beginTx();
			try {
				
				IWaySegment segment = (IWaySegment) task.getGraphDao().getSegmentById(graphName, version, segmentId, false);
				
				Traverser traverser = getTraverser(
						segment, 
						WaySegmentRelationshipType.SEGMENT_CONNECTION_ON_STARTNODE, 
						WaySegmentRelationshipType.SEGMENT_CONNECTION_ON_ENDNODE, 
						3,
						task.getGraphDao(),
						task.getGraphName(),
						task.getGraphVersion());
				
				if (traverser != null) {
					Iterator<Path> connectedPaths = traverser.iterator();
					
					while (connectedPaths.hasNext()) {
						Path connectedPath = connectedPaths.next();
	
						List<Long> ids = new ArrayList<>();
						for (Node connectedSegmentNode : connectedPath.nodes()) {
							ids.add((Long)connectedSegmentNode.getProperty(WayGraphConstants.SEGMENT_ID));
						}
						
						log.info(StringUtils.join(ids, " -> "));
					}
				}

				tx.success();
			} catch (CancellationException | TransactionTerminatedException e) {
				tx.failure();
			} finally {
				tx.close();
			}
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		} catch (GraphNotExistsException e) {
			log.error(e.getMessage(), e);
		}
		
	}
	
	private Traverser getTraverser(IWaySegment segment, WaySegmentRelationshipType relationshipType1, WaySegmentRelationshipType relationshipType2, int nrOfHops,
			INeo4jWayGraphReadDao graphDao, String graphName, String version) {
		Node node = graphDao.getSegmentNodeBySegmentId(graphName, version, segment.getId());
		
		TraversalDescription traversalDescription = neo4jUtil.getTraverser(relationshipType1, relationshipType2, nrOfHops);

		return traversalDescription.traverse(node);
	}
	
	@Test
	public void testExtendedPathFinding() throws RoutingParameterException {
		String graphName = "osm_dk";
		
		try {
			ITrack track = createTempTrack();
			
			List<IMatchedBranch> branches = matchTrack(track, graphName);
				
			printBranches(branches);
			
		} catch (GraphNotExistsException e) {
			log.error(e.getMessage(), e);
		}

	}

	private ITrack createTempTrack() {
		int srid = 4326;
		
		ITrackMetadata metadata = new TrackMetadataImpl();
		metadata.setNumberOfPoints(4);
		
		Calendar cal = Calendar.getInstance();
		cal.set(2019, Calendar.AUGUST, 20, 10, 0, 0);
		
		List<ITrackPoint> trackPoints = new ArrayList<>();
		ITrackPoint tp1 = new TrackPointImpl();
		tp1.setPoint(GeometryUtils.createPoint2D(12.52916, 55.74783, srid));
		tp1.setTimestamp(cal.getTime());
		trackPoints.add(tp1);
		
		cal.add(Calendar.SECOND, 3);
		ITrackPoint tp2 = new TrackPointImpl();
		tp2.setPoint(GeometryUtils.createPoint2D(12.52832, 55.74851, srid));
		tp2.setTimestamp(cal.getTime());
		trackPoints.add(tp2);
		
		cal.add(Calendar.SECOND, 5);
		ITrackPoint tp3 = new TrackPointImpl();
		tp3.setPoint(GeometryUtils.createPoint2D(12.52559, 55.75068, srid));
		tp3.setTimestamp(cal.getTime());
		trackPoints.add(tp3);
		
		cal.add(Calendar.SECOND, 50);
		ITrackPoint tp4 = new TrackPointImpl();
		tp4.setPoint(GeometryUtils.createPoint2D(12.51508, 55.762, srid));
		tp4.setTimestamp(cal.getTime());
		trackPoints.add(tp4);

		cal.add(Calendar.SECOND, 25);
		ITrackPoint tp5 = new TrackPointImpl();
		tp5.setPoint(GeometryUtils.createPoint2D(12.50423, 55.76817, srid));
		tp5.setTimestamp(cal.getTime());
		trackPoints.add(tp5);
		
		ITrack track = new TrackImpl();
		track.setId(1);
		track.setMetadata(metadata);
		track.setTrackPoints(trackPoints);
		track.calculateLineString();
		track.calculateTrackPointValues();
		return track;
	}
	
}