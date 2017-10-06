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

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

import at.srfg.graphium.core.persistence.IWayGraphVersionMetadataDao;
import at.srfg.graphium.core.persistence.IWayGraphViewDao;
import at.srfg.graphium.model.Access;
import at.srfg.graphium.model.ISource;
import at.srfg.graphium.model.IWayGraph;
import at.srfg.graphium.model.IWayGraphVersionMetadata;
import at.srfg.graphium.model.State;
import at.srfg.graphium.model.impl.WayGraph;
import at.srfg.graphium.model.impl.WayGraphVersionMetadata;
import at.srfg.graphium.model.management.impl.Source;
import at.srfg.graphium.neo4j.persistence.configuration.GraphDatabaseProvider;

/**
 * @author mwimmer
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:/application-context-graphium-neo4j_test.xml",
		"classpath:/application-context-graphium-core.xml",
		"classpath:/application-context-graphium-model.xml"})
public class TestNeo4jWayGraphVersionMetadataDaoImpl {

	private static Logger log = LoggerFactory.getLogger(TestNeo4jWayGraphVersionMetadataDaoImpl.class);
	
	@Resource(name="neo4jWayGraphVersionMetadataDao")
	private IWayGraphVersionMetadataDao metadataDao;
	
	@Resource(name="neo4jWayGraphViewDao")
	private IWayGraphViewDao viewDao;
	
	@Autowired
	private GraphDatabaseProvider graphDatabaseProvider;
	
	private String graphName = "gip_at";
	private String versionName = "test";
	
	private Date now;
	private Date validFrom;
	private Polygon coveredArea;
	private Set<Access> accessTypes;
	private ISource source;
	private String type;
	private String description;
	private String creator;
	private String originUrl;
	private long graphId;
	private int segmentsCount = 500;
	private int connectionsCount = 11;
	
	@PostConstruct
	public void setup() {
		Calendar cal = Calendar.getInstance();
		now = cal.getTime();
		cal.set(Calendar.DAY_OF_MONTH, 1);
		validFrom = cal.getTime();
		coveredArea = getBoundsAustria();
		accessTypes = new HashSet<Access>();
		accessTypes.add(Access.PRIVATE_CAR);
		source = new Source(0, "GIP");
		type = "Typ GIP AT";
		description = "Dies ist ein Testgraph";
		creator = "ich";
		originUrl = "http://0815.at";
		graphId = 1;
	}
	
	@Test
	public void testSaveGraphVersion() {
		
		Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx();
		try {
			saveGraphVersion();
			tx.failure();
		} catch (Exception e) {
			e.printStackTrace();
			tx.failure(); // rollback
		} finally {
			tx.close();
		}
	}
	
	private void saveGraphVersion() {
		metadataDao.saveGraph(graphName);
		IWayGraphVersionMetadata metadata = new WayGraphVersionMetadata(0, graphId, graphName, versionName, graphName, versionName, State.INITIAL, 
				validFrom, null, coveredArea, segmentsCount, connectionsCount, accessTypes, null, source, type, description, 
				now, now, creator, originUrl);
		metadataDao.saveGraphVersion(metadata);
	}
	
	private void assertMetadata(IWayGraphVersionMetadata metadata) {
		Assert.assertNotNull(metadata);
		
		Assert.assertEquals(metadata.getCreator(), creator);
		Assert.assertEquals(metadata.getDescription(), description);
		Assert.assertEquals(metadata.getGraphName(), graphName);
//		Assert.assertEquals(metadata.getGraphId(), graphId);
		Assert.assertEquals(metadata.getOriginGraphName(), graphName);
		Assert.assertEquals(metadata.getOriginUrl(), originUrl);
		Assert.assertEquals(metadata.getOriginVersion(), versionName);
		Assert.assertEquals(metadata.getType(), type);
		Assert.assertEquals(metadata.getVersion(), versionName);
		Set<Access> accessOverlappings = Neo4jWaySegmentHelperImpl.getAccessOverlappings(accessTypes, metadata.getAccessTypes());
		Assert.assertEquals(accessTypes.size(), accessOverlappings.size());
		Assert.assertEquals(getBoundsAustria(), metadata.getCoveredArea());
		Assert.assertEquals(metadata.getConnectionsCount(), connectionsCount);
		Assert.assertEquals(metadata.getCreationTimestamp(), now);
		Assert.assertEquals(metadata.getSegmentsCount(), segmentsCount);
		Assert.assertEquals(metadata.getSource().getName(), source.getName());
		Assert.assertEquals(metadata.getState(), State.INITIAL);
		Assert.assertEquals(metadata.getStorageTimestamp(), now);
		Assert.assertEquals(metadata.getValidFrom(), validFrom);
		Assert.assertNull(metadata.getValidTo());
	}

	@Test
	public void testGetWayGraphVersionMetadata() {
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx();)
		{
			saveGraphVersion();
			
			IWayGraphVersionMetadata metadata = metadataDao.getWayGraphVersionMetadata(graphName, versionName);
			
			tx.failure(); // rollback
			assertMetadata(metadata);
		}
	}

	@Test
	public void testGetWayGraphVersionMetadataList() {
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx();)
		{
			saveGraphVersion();
			
			List<IWayGraphVersionMetadata> metadataList = metadataDao.getWayGraphVersionMetadataList(graphName);
			
			tx.failure(); // rollback

			Assert.assertNotNull(metadataList);
			Assert.assertNotEquals(metadataList.size(), 0);
			
			assertMetadata(metadataList.get(0));
		}
	}

	@Test
	public void testGetWayGraphVersionMetadataListForOriginGraphname() {
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx();)
		{
			saveGraphVersion();
			
			List<IWayGraphVersionMetadata> metadataList = metadataDao.getWayGraphVersionMetadataListForOriginGraphname(graphName);

			tx.failure(); // rollback

			Assert.assertNotNull(metadataList);
			Assert.assertNotEquals(metadataList.size(), 0);
			
			assertMetadata(metadataList.get(0));
		}
	}

	@Test
	public void testGetWayGraphVersionMetadataListWithParams() {
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx();)
		{
			saveGraphVersion();
			
			List<IWayGraphVersionMetadata> metadataList = metadataDao.getWayGraphVersionMetadataList(graphName, State.INITIAL, validFrom, null, accessTypes);
			
			// muss 1 Treffer ergeben
			Assert.assertNotNull(metadataList);
			Assert.assertNotEquals(metadataList.size(), 0);
			
			assertMetadata(metadataList.get(0));

			// ==================================================

			Calendar cal = Calendar.getInstance();
			cal.setTime(validFrom);
			cal.add(Calendar.DAY_OF_MONTH, +1);
			Date validFromTest = cal.getTime();
			metadataList = metadataDao.getWayGraphVersionMetadataList(graphName, State.INITIAL, validFromTest, null, accessTypes);
			
			// muss 1 Treffer ergeben
			Assert.assertNotNull(metadataList);
			Assert.assertNotEquals(metadataList.size(), 0);
			
			assertMetadata(metadataList.get(0));

			// ==================================================

			cal.setTime(validFrom);
			cal.add(Calendar.DAY_OF_MONTH, +2);
			Date validToTest = cal.getTime();
			metadataList = metadataDao.getWayGraphVersionMetadataList(graphName, State.INITIAL, validFromTest, validToTest, accessTypes);
			
			// muss 1 Treffer ergeben
			Assert.assertNotNull(metadataList);
			Assert.assertNotEquals(metadataList.size(), 0);
			
			assertMetadata(metadataList.get(0));

			// ==================================================

			cal.setTime(validFrom);
			cal.add(Calendar.DAY_OF_MONTH, -2);
			Date validFromTest2 = cal.getTime();
			cal.add(Calendar.DAY_OF_MONTH, +1);
			Date validToTest2 = cal.getTime();
			metadataList = metadataDao.getWayGraphVersionMetadataList(graphName, State.INITIAL, validFromTest2, validToTest2, accessTypes);
			
			// muss 0 Treffer ergeben
			Assert.assertNotNull(metadataList);
			Assert.assertEquals(metadataList.size(), 0);
			
			// ==================================================

			metadataList = metadataDao.getWayGraphVersionMetadataList(graphName, State.ACTIVE, validFrom, null, accessTypes);
			
			// muss 0 Treffer ergeben
			Assert.assertNotNull(metadataList);
			Assert.assertEquals(metadataList.size(), 0);
			
			// ==================================================
			
			metadataList = metadataDao.getWayGraphVersionMetadataList(graphName + "Hallo", State.INITIAL, validFrom, null, accessTypes);
			
			// muss 0 Treffer ergeben
			Assert.assertNotNull(metadataList);
			Assert.assertEquals(metadataList.size(), 0);
			
			// ==================================================
			
			
			Set<Access> accessTypesTest = new HashSet<Access>();
			accessTypesTest.add(Access.PUBLIC_BUS);
			metadataList = metadataDao.getWayGraphVersionMetadataList(graphName, State.INITIAL, validFrom, null, accessTypesTest);
			
			// muss 0 Treffer ergeben
			Assert.assertNotNull(metadataList);
			Assert.assertEquals(metadataList.size(), 0);
			
			tx.failure(); // rollback
		}
	}

	@Test
	public void testUpdateGraphVersion() {
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx();)
		{
			saveGraphVersion();
			
			IWayGraphVersionMetadata metadata = metadataDao.getWayGraphVersionMetadata(graphName, versionName);
			
			int segmentsCountOld = metadata.getSegmentsCount();
			metadata.setSegmentsCount(segmentsCountOld + 100);
			metadataDao.updateGraphVersion(metadata);

			metadata = metadataDao.getWayGraphVersionMetadata(graphName, versionName);
			
			tx.failure(); // rollback

			Assert.assertEquals(metadata.getSegmentsCount(), segmentsCountOld + 100);
		}
	}

	@Test
	public void testSetGraphVersionState() {
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx();)
		{
			saveGraphVersion();
			
			IWayGraphVersionMetadata metadata = metadataDao.getWayGraphVersionMetadata(graphName, versionName);
			State stateOld = metadata.getState();
			metadataDao.setGraphVersionState(metadata.getGraphName(), metadata.getVersion(), State.ACTIVE);

			metadata = metadataDao.getWayGraphVersionMetadata(graphName, versionName);
			
			tx.failure(); // rollback

			Assert.assertNotEquals(metadata.getState(), stateOld);
		}
	}

	@Test
	public void testSetValidToTimestampOfPredecessorGraphVersion() {
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx();)
		{
			saveGraphVersion();
			
			IWayGraphVersionMetadata metadataVirtualNewcomer = metadataDao.getWayGraphVersionMetadata(graphName, versionName);
			metadataVirtualNewcomer.setState(State.ACTIVE);
			Calendar cal = Calendar.getInstance();
			cal.setTime(metadataVirtualNewcomer.getValidFrom());
			cal.add(Calendar.DAY_OF_MONTH, +1);
			metadataVirtualNewcomer.setValidFrom(cal.getTime());
			String newVersion = versionName + "_2";
			metadataVirtualNewcomer.setVersion(newVersion);
			metadataDao.setValidToTimestampOfPredecessorGraphVersion(metadataVirtualNewcomer);

			IWayGraphVersionMetadata metadata = metadataDao.getWayGraphVersionMetadata(graphName, versionName);
			
			// state sollte INITIAL sein, darum sollte auch kein Update erfolgen und valid_to daher null sein
			Assert.assertEquals(State.INITIAL, metadata.getState());
			Assert.assertNull(metadata.getValidTo());

			// ==================================================
			
			metadataDao.setGraphVersionState(metadata.getGraphName(), metadata.getVersion(), State.ACTIVE);
			
			metadataDao.setValidToTimestampOfPredecessorGraphVersion(metadataVirtualNewcomer);

			metadata = metadataDao.getWayGraphVersionMetadata(graphName, versionName);
			
			// state sollte ACTIVE sein, darum sollte valid_to nun angepasst worden sein an valid_from der neuen Metadata
			Assert.assertEquals(State.ACTIVE, metadata.getState());
			Assert.assertEquals(metadataVirtualNewcomer.getValidFrom(), metadata.getValidTo());
			
			// ==================================================
			
			metadataDao.saveGraphVersion(metadataVirtualNewcomer);
			
			metadataDao.setValidToTimestampOfPredecessorGraphVersion(metadataVirtualNewcomer);

			metadata = metadataDao.getWayGraphVersionMetadata(graphName, versionName);
			
			// state sollte ACTIVE sein, darum sollte valid_to nun angepasst worden sein an valid_from der neuen Metadata
			Assert.assertEquals(State.ACTIVE, metadata.getState());
			Assert.assertEquals(metadataVirtualNewcomer.getValidFrom(), metadata.getValidTo());

			// nun wird sicherheitshalber noch die neue Version gelesen...
			metadata = metadataDao.getWayGraphVersionMetadata(graphName, newVersion);
			
			// hier sollte valid_to null sein
			Assert.assertEquals(State.ACTIVE, metadata.getState());
			Assert.assertNull(metadata.getValidTo());

			tx.failure(); // rollback
		}
	}

	@Test
	public void testGetGraph() {
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx();)
		{
			saveGraphVersion();
			
			IWayGraph graph = metadataDao.getGraph(graphName);
			Assert.assertNotNull(graph);
			
			graph = metadataDao.getGraph(graphName + "Hallo");
			Assert.assertNull(graph);
			
			tx.failure(); // rollback

		}
	}

	@Test
	public void testGetGraphs() {
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx();)
		{
			saveGraphVersion();
			
			List<String> graphs = metadataDao.getGraphs();
			Assert.assertNotNull(graphs);
			Assert.assertEquals(graphs.size(), 1);
			Assert.assertEquals(graphs.get(0), graphName);
			
			tx.failure(); // rollback

		}
	}

	@Test
	public void testCheckNewerVersionAvailable() {
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx();)
		{
			saveGraphVersion();
			viewDao.saveDefaultView(new WayGraph(0, graphName));
			
//			IWayGraphVersionMetadata metadata1 = metadataDao.getCurrentWayGraphVersionMetadataForView(graphName);
			metadataDao.setGraphVersionState(graphName, versionName, State.ACTIVE);

			IWayGraphVersionMetadata metadata2 = metadataDao.getCurrentWayGraphVersionMetadataForView(graphName);
			Calendar cal = Calendar.getInstance();
			cal.setTime(metadata2.getValidFrom());
			cal.add(Calendar.DAY_OF_MONTH, +1);
			metadata2.setValidFrom(cal.getTime());
			metadata2.setVersion(versionName + "2");
			metadataDao.saveGraphVersion(metadata2);
			viewDao.saveDefaultView(new WayGraph(0, graphName)); // create relationship between new metadata and view

			String updateVersion = metadataDao.checkNewerVersionAvailable(graphName, versionName);
			
			Assert.assertNotNull(updateVersion);

			// ==================================================
			
			updateVersion = metadataDao.checkNewerVersionAvailable(graphName, versionName + "2");
			
			Assert.assertNotNull(updateVersion);

			tx.failure(); // rollback
		}
	}

	@Test
	public void testGetCurrentWayGraphVersionMetadata() {
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx();)
		{
			saveGraphVersion();
			
			IWayGraphVersionMetadata metadata = metadataDao.getCurrentWayGraphVersionMetadata(graphName);
			
			// state sollte INITIAL sein, darum sollte auch nichts gelesen werden
			Assert.assertNull(metadata);

			// ==================================================
			
			metadataDao.setGraphVersionState(graphName, versionName, State.ACTIVE);
			
			metadata = metadataDao.getCurrentWayGraphVersionMetadata(graphName);
			
			// state sollte ACTIVE sein
			Assert.assertNotNull(metadata);
			Assert.assertEquals(metadata.getState(), State.ACTIVE);

			tx.failure(); // rollback
		}
	}

	@Test
	public void testGetCurrentWayGraphVersionMetadataForView() {
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx();)
		{
			saveGraphVersion();
			viewDao.saveDefaultView(new WayGraph(0, graphName));
			
			IWayGraphVersionMetadata metadata = metadataDao.getCurrentWayGraphVersionMetadataForView(graphName);
			
			Assert.assertNull(metadata);

			// ==================================================
			
			metadataDao.setGraphVersionState(graphName, versionName, State.ACTIVE);
			
			metadata = metadataDao.getCurrentWayGraphVersionMetadataForView(graphName);
			
			Assert.assertNotNull(metadata);

			// ==================================================
			
			metadata = metadataDao.getCurrentWayGraphVersionMetadataForView(graphName + "Hallo");
			
			Assert.assertNull(metadata);

			tx.failure(); // rollback
		}
	}

	@Test
	public void testGetWayGraphVersionMetadataForView() {
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx();)
		{
			saveGraphVersion();
			viewDao.saveDefaultView(new WayGraph(0, graphName));
			
			IWayGraphVersionMetadata metadata = metadataDao.getWayGraphVersionMetadataForView(graphName, versionName);
			
			Assert.assertNotNull(metadata);

			// ==================================================
			
			metadata = metadataDao.getWayGraphVersionMetadataForView(graphName, versionName + "Hallo");
			
			Assert.assertNull(metadata);

			tx.failure(); // rollback
		}
	}

	@Test
	public void testGetCurrentWayGraphVersionMetadataWithStates() {
		try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx();)
		{
			saveGraphVersion();
			Set<State> states = new HashSet<>();
			states.add(State.INITIAL);
			
			IWayGraphVersionMetadata metadata = metadataDao.getCurrentWayGraphVersionMetadata(graphName, states);
			
			Assert.assertNotNull(metadata);

			// ==================================================
			
			states.clear();
			states.add(State.ACTIVE);
			
			metadata = metadataDao.getCurrentWayGraphVersionMetadata(graphName, states);
			
			Assert.assertNull(metadata);

			tx.failure(); // rollback
		}
	}
	
	private Polygon getBoundsAustria() {
		WKTReader reader = new WKTReader();
		String poly = "POLYGON((9.5282778 46.3704647,9.5282778 49.023522,17.1625438 49.023522,17.1625438 46.3704647,9.5282778 46.3704647))";
		Polygon bounds = null;
		try {
			bounds = (Polygon) reader.read(poly);
			bounds.setSRID(4326);
		} catch (ParseException e) {
			//log.error("error parsing geometry of reference point");
		}
		return bounds;
	}

}
