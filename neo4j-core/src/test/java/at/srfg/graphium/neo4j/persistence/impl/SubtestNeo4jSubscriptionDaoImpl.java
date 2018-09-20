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

import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.annotation.Resource;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.vividsolutions.jts.geom.Polygon;

import at.srfg.graphium.core.persistence.ISubscriptionDao;
import at.srfg.graphium.core.persistence.IWayGraphVersionMetadataDao;
import at.srfg.graphium.core.persistence.IWayGraphViewDao;
import at.srfg.graphium.model.Access;
import at.srfg.graphium.model.ISource;
import at.srfg.graphium.model.IWayGraphVersionMetadata;
import at.srfg.graphium.model.State;
import at.srfg.graphium.model.impl.WayGraph;
import at.srfg.graphium.model.impl.WayGraphVersionMetadata;
import at.srfg.graphium.model.management.ISubscription;
import at.srfg.graphium.model.management.ISubscriptionGroup;
import at.srfg.graphium.model.management.impl.Subscription;
import at.srfg.graphium.model.management.impl.SubscriptionGroup;
import at.srfg.graphium.model.view.IWayGraphView;
import at.srfg.graphium.model.view.impl.WayGraphView;
import at.srfg.graphium.neo4j.ITestGraphiumNeo4j;
import at.srfg.graphium.neo4j.persistence.configuration.GraphDatabaseProvider;

/**
 * @author mwimmer
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:/application-context-graphium-neo4j_test.xml",
		"classpath:/application-context-graphium-core.xml",
		"classpath:/application-context-graphium-model.xml"})
public class SubtestNeo4jSubscriptionDaoImpl implements ITestGraphiumNeo4j {

	@Resource(name="neo4jSubscriptionDao")
	private ISubscriptionDao dao;
	
	@Resource(name="neo4jWayGraphViewDao")
	private IWayGraphViewDao viewDao;

	@Resource(name="neo4jWayGraphVersionMetadataDao")
	private IWayGraphVersionMetadataDao metadataDao;
	
	@Autowired
	private GraphDatabaseProvider graphDatabaseProvider;
	
	@Value("${db.graphName}")
	String graphName;
	@Value("${db.version}")
	String version;
	@Value("${db.serverName}")
	String serverName;
	@Value("${db.viewName1}")
	String viewName1;
	@Value("${db.viewName2}")
	String viewName2;
	@Value("${db.groupName1}")
	String groupName1;
	@Value("${db.groupName2}")
	String groupName2;
	@Value("${db.serverUrl}")
	String url;
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

	@Test
	public void testSubscribe() {
		Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx();
		try {
			IWayGraphVersionMetadata metadata = metadataDao.getWayGraphVersionMetadata(graphName, version);
			if (metadata == null) {
				saveGraphVersion();
			}
			
			viewDao.saveDefaultView(new WayGraph(0, graphName));
			viewDao.saveView(new WayGraphView(viewName1, new WayGraph(0, graphName), viewName1, true, null, 0, 0, null));
			viewDao.saveView(new WayGraphView(viewName2, new WayGraph(0, graphName), viewName2, true, null, 0, 0, null));
			IWayGraphView view = viewDao.getView(graphName);
			
			ISubscriptionGroup subscriptionGroup1 = new SubscriptionGroup(0, groupName1, view.getGraph(), null);
			ISubscriptionGroup subscriptionGroup2 = new SubscriptionGroup(0, groupName2, view.getGraph(), null);
	
			ISubscription subscription = new Subscription(serverName, graphName, subscriptionGroup1, url, null, null, null);
			boolean subscribed = dao.subscribe(subscription);
//			System.out.println("\nSubscription (1. Versuch) war '" + (subscribed ? "erfolgreich" : "nicht erfolgreich"));
			Assert.assertEquals(subscribed, true);
			
			subscription = new Subscription(serverName, viewName1, subscriptionGroup1, url, null, null, null);
			subscribed = dao.subscribe(subscription);
//			System.out.println("\nSubscription (1. Versuch für View) war '" + (subscribed ? "erfolgreich" : "nicht erfolgreich"));
			Assert.assertEquals(subscribed, true);
			
			subscription = new Subscription(serverName, viewName2, subscriptionGroup2, url, null, null, null);
			subscribed = dao.subscribe(subscription);
//			System.out.println("\nSubscription (1. Versuch für View) war '" + (subscribed ? "erfolgreich" : "nicht erfolgreich"));
			Assert.assertEquals(subscribed, true);
			
			tx.success();
		} catch (Exception e) {
			e.printStackTrace();
			tx.failure(); // rollback
		} finally {
			tx.close();
		}
	}

	@Test
	public void testUnsubscribe() {
		Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx();
		try {
			boolean unsubscribed = dao.unsubscribe(serverName, viewName2);
			Assert.assertEquals(unsubscribed, true);
			
			tx.success();
		} catch (Exception e) {
			e.printStackTrace();
			tx.failure(); // rollback
		} finally {
			tx.close();
		}
	}

	private void saveGraphVersion() {
		IWayGraphVersionMetadata metadata = new WayGraphVersionMetadata(0, graphId, graphName, version, graphName, version, State.INITIAL, 
				validFrom, null, coveredArea, segmentsCount, connectionsCount, accessTypes, null, source, type, description, 
				now, now, creator, originUrl);
		metadataDao.saveGraphVersion(metadata);
	}

	@Test
	public void testGetSubscriptionsForViewAndServer() {
		Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx();
		try {
			testSubscribe();
			
			ISubscription subscriptionDefaultView = dao.getSubscriptionForViewAndServer(serverName, graphName);
			Assert.assertNotNull(subscriptionDefaultView);
			
			ISubscription subscriptionView1 = dao.getSubscriptionForViewAndServer(serverName, viewName1);
			Assert.assertNotNull(subscriptionView1);

			ISubscription subscriptionView2 = dao.getSubscriptionForViewAndServer(serverName, viewName2);
			Assert.assertNotNull(subscriptionView2);

			ISubscription subscriptionView3 = dao.getSubscriptionForViewAndServer(serverName, "HalliGalli");
			Assert.assertNull(subscriptionView3);
			
			tx.failure();
		} catch (Exception e) {
			e.printStackTrace();
			tx.failure(); // rollback
		} finally {
			tx.close();
		}
	}
	
	@Test
	public void testGetSubscriptionsForView() {
		Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx();
		try {
			testSubscribe();
			
			List<ISubscription> subscriptions = dao.getSubscriptionsForView(graphName);
			Assert.assertNotNull(subscriptions);
			Assert.assertEquals(subscriptions.size(), 1);
			Assert.assertEquals(subscriptions.get(0).getViewName(), graphName);
			
			subscriptions = dao.getSubscriptionsForView(viewName1);
			Assert.assertNotNull(subscriptions);
			Assert.assertEquals(subscriptions.size(), 1);
			Assert.assertEquals(subscriptions.get(0).getViewName(), viewName1);

			subscriptions = dao.getSubscriptionsForView("HalliGalli");
			Assert.assertNotNull(subscriptions);
			Assert.assertEquals(subscriptions.size(), 0);
			
			tx.failure();
		} catch (Exception e) {
			e.printStackTrace();
			tx.failure(); // rollback
		} finally {
			tx.close();
		}
	}
	
	@Test
	public void testGetSubscriptionsForGraph() {
		Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx();
		try {
			testSubscribe();
			
			List<ISubscription> subscriptions = dao.getSubscriptionsForGraph(graphName);
			Assert.assertNotNull(subscriptions);
			Assert.assertEquals(subscriptions.size(), 3);
			
			tx.failure();
		} catch (Exception e) {
			e.printStackTrace();
			tx.failure(); // rollback
		} finally {
			tx.close();
		}
	}
	
	@Test
	public void testGetSubscriptionsForGraphAndServer() {
		Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx();
		try {
			testSubscribe();
			
			List<ISubscription> subscriptions = dao.getSubscriptionsForGraphAndServer(serverName, graphName);
			Assert.assertNotNull(subscriptions);
			Assert.assertEquals(3, subscriptions.size());
			
			subscriptions = dao.getSubscriptionsForGraphAndServer("HalliGalliServer", graphName);
			Assert.assertNotNull(subscriptions);
			Assert.assertEquals(0, subscriptions.size());
			
			tx.failure();
		} catch (Exception e) {
			e.printStackTrace();
			tx.failure(); // rollback
		} finally {
			tx.close();
		}
	}
	
	@Test
	public void testGetSubscriptionsForGraphAndGroupName() {
		Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx();
		try {
			testSubscribe();
			
			List<ISubscription> subscriptions = dao.getSubscriptionsForGraph(graphName, groupName1);
			Assert.assertNotNull(subscriptions);
			Assert.assertEquals(2, subscriptions.size());
			
			subscriptions = dao.getSubscriptionsForGraph(graphName, groupName2);
			Assert.assertNotNull(subscriptions);
			Assert.assertEquals(1, subscriptions.size());
			
			subscriptions = dao.getSubscriptionsForGraphAndServer("HalliGalli", graphName);
			Assert.assertNotNull(subscriptions);
			Assert.assertEquals(0, subscriptions.size());
			
			tx.failure();
		} catch (Exception e) {
			e.printStackTrace();
			tx.failure(); // rollback
		} finally {
			tx.close();
		}
	}
	
	@Test
	public void testGetAllSubscriptions() {
		Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx();
		try {
			testSubscribe();
			
			List<ISubscription> subscriptions = dao.getAllSubscriptions();
			Assert.assertNotNull(subscriptions);
			Assert.assertEquals(3, subscriptions.size());
			
			tx.failure();
		} catch (Exception e) {
			e.printStackTrace();
			tx.failure(); // rollback
		} finally {
			tx.close();
		}
	}
	
	@Test
	public void testGetSubscriptionGroup() {
		Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx();
		try {
			testSubscribe();
			
			ISubscriptionGroup subscriptionGroup = dao.getSubscriptionGroup(groupName1);
			Assert.assertNotNull(subscriptionGroup);
			Assert.assertEquals(viewName1, subscriptionGroup.getGraph().getName());
			
			tx.failure();
		} catch (Exception e) {
			e.printStackTrace();
			tx.failure(); // rollback
		} finally {
			tx.close();
		}
	}

	@Override
	public void run() {
		testSubscribe();
		testGetSubscriptionsForGraphAndServer();
		testGetSubscriptionsForView();
		testGetSubscriptionsForGraph();
		testGetSubscriptionsForGraphAndServer();
		testGetSubscriptionsForGraphAndGroupName();
		testGetAllSubscriptions();
		testGetSubscriptionGroup();
		testUnsubscribe();
	}

}
