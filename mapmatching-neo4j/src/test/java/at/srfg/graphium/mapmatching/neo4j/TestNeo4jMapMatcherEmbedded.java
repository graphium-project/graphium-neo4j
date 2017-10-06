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
import at.srfg.graphium.io.adapter.IAdapter;
import at.srfg.graphium.mapmatching.dto.TrackDTO;
import at.srfg.graphium.mapmatching.matcher.IMapMatcherTask;
import at.srfg.graphium.mapmatching.model.IMatchedBranch;
import at.srfg.graphium.mapmatching.model.IMatchedWaySegment;
import at.srfg.graphium.mapmatching.model.ITrack;
import at.srfg.graphium.mapmatching.neo4j.matcher.impl.MapMatchingTask;
import at.srfg.graphium.mapmatching.neo4j.matcher.impl.Neo4jMapMatcher;
import at.srfg.graphium.mapmatching.neo4j.matcher.impl.Neo4jUtil;
import at.srfg.graphium.model.IWaySegment;
import at.srfg.graphium.neo4j.model.WayGraphConstants;
import at.srfg.graphium.neo4j.model.WaySegmentRelationshipType;
import at.srfg.graphium.neo4j.persistence.INeo4jWayGraphReadDao;

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
public class TestNeo4jMapMatcherEmbedded {

	private static Logger log = LoggerFactory.getLogger(TestNeo4jMapMatcherEmbedded.class);

	@Autowired
	private Neo4jMapMatcher mapMatcher;
	
	@Autowired
	private IAdapter<ITrack, TrackDTO> adapter;

	@Autowired
	private Neo4jUtil neo4jUtil;
	
	@Test
	public void testMatchTrack() {
		String graphName = "osm_at";
//		String graphName = "osm_at_with_lower_level_streets";
//		String graphName = "gip_at_frc_0_4";
//		String graphName = "gip_at_frc_0_8";
//		long trackId = 18394396;
//		long trackId = 19991780;
//		long trackId = 18241517;
		
		long trackId = 19233778L;
		
//		long trackId = 4893166;
		
		//long trackId = 12761079;
		//long trackId = 12774066;	// 2 Routings
		//long trackId = 12779916;	// kein Routing
		//long trackId = 12779918;	// 4 Routings

		matchTrack(trackId, graphName);
		
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
		}
		
		if (branches != null && !branches.isEmpty()) {
			IMatchedWaySegment safeSegment = branches.get(0).getCertainPathEndSegment();
			log.info("Certain path end segment is " + (safeSegment == null ? "NULL!!!" : safeSegment.getId()));
		}
		
	}		
	
	@Test
	public void testMatchTracks() {
		String graphName = "gip_at_frc_0_8";
		Long[] trackIds = new Long[] {
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

				5001494L,
				5006169L,
				4927281L,

		
		};
				
		for (Long trackId : trackIds) {
			matchTrack(trackId, graphName);
		}
		
	}

	private List<IMatchedBranch> matchTrack(long trackId, String graphName) {
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
		}
		
		return branches;
	}
	
	@Test
	public void testMatchTrackInLoop() {
		
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
	private List<IMatchedBranch> matchTrack(ITrack track, String graphName) throws GraphNotExistsException {
		long startTime = System.nanoTime();
		IMapMatcherTask task = mapMatcher.getTask(graphName, track);
		List<IMatchedBranch> branches = task.matchTrack();
		log.info("Map matching took " +  + (System.nanoTime() - startTime) + "ns = " + ((System.nanoTime() - startTime) / 1000000) + "ms");
		return branches;
	}

	private void printBranches(List<IMatchedBranch> branches) {
		for (IMatchedBranch branch : branches) {
			log.info("Branch with matched factor " + branch.getMatchedFactor());
			List<Long> segmentIds = new ArrayList<>(branch.getMatchedWaySegments().size());
			for (IMatchedWaySegment segment : branch.getMatchedWaySegments()) {
//				log.info("" + segment.getId());
				segmentIds.add(segment.getId());
			}
			log.info(StringUtils.join(segmentIds, ", "));
		}
	}
	
	@Test
	public void testTraverser() {
		
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

			MapMatchingTask task = (MapMatchingTask) mapMatcher.getTask(graphName, track);
			
			Transaction tx = task.getGraphDao().getGraphDatabaseProvider().getGraphDatabase().beginTx();
			try {
				
				IWaySegment segment = task.getGraphDao().getSegmentById(graphName, version, segmentId, false);
				
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
	
}