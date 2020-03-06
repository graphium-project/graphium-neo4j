/**
 * Graphium Neo4j - Module of Graphium for routing services via Neo4j
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
package at.srfg.graphium.routing.neo4j.service.impl;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;

import at.srfg.graphium.core.exception.GraphAlreadyExistException;
import at.srfg.graphium.core.exception.GraphImportException;
import at.srfg.graphium.core.persistence.IWayGraphVersionMetadataDao;
import at.srfg.graphium.core.service.IGraphVersionImportService;
import at.srfg.graphium.model.Access;
import at.srfg.graphium.model.FormOfWay;
import at.srfg.graphium.model.FuncRoadClass;
import at.srfg.graphium.model.ISegmentXInfo;
import at.srfg.graphium.model.IWayGraphVersionMetadata;
import at.srfg.graphium.model.IWaySegment;
import at.srfg.graphium.model.IWaySegmentConnection;
import at.srfg.graphium.model.OneWay;
import at.srfg.graphium.model.State;
import at.srfg.graphium.model.impl.WaySegment;
import at.srfg.graphium.routing.exception.RoutingException;
import at.srfg.graphium.routing.exception.UnkownRoutingAlgoException;
import at.srfg.graphium.routing.model.IRoute;
import at.srfg.graphium.routing.model.IRoutingOptions;
import at.srfg.graphium.routing.model.impl.RoutingAlgorithms;
import at.srfg.graphium.routing.model.impl.RoutingCriteria;
import at.srfg.graphium.routing.model.impl.RoutingMode;
import at.srfg.graphium.routing.model.impl.RoutingOptionsImpl;
import at.srfg.graphium.routing.service.IRoutingService;

/**
 * @author mwimmer
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:/application-context-graphium-routing-neo4j_test.xml",
		"classpath:/application-context-graphium-core.xml",
		"classpath:/application-context-graphium-model.xml"})
public class TestNeo4jRoutingServiceImpl {
	
	private static Logger log = LoggerFactory.getLogger(TestNeo4jRoutingServiceImpl.class);
	
	@Resource(name="neo4jRoutingService")
	private IRoutingService<IWaySegment, Double, IRoutingOptions> routingService;
	
	@Resource(name="neo4jWayGraphVersionMetadataDao")
	private IWayGraphVersionMetadataDao metadataDao;
	
	@Resource(name="neo4jQueuingGraphVersionImportService")
	private IGraphVersionImportService<? extends IWaySegment> importService;

	@Value("${db.graphName}")
	String graphName;
	@Value("${db.version1}")
	String version1;
	@Value("#{new java.text.SimpleDateFormat(\"${db.dateFormat}\").parse(\"${db.validFrom1}\")}")
	Date validFrom1;
	@Value("#{new java.text.SimpleDateFormat(\"${db.dateFormat}\").parse(\"${db.validTo1}\")}")
	Date validTo1;
	@Value("${db.version2}")
	String version2;
	@Value("#{new java.text.SimpleDateFormat(\"${db.dateFormat}\").parse(\"${db.validFrom2}\")}")
	Date validFrom2;
	@Value("#{new java.text.SimpleDateFormat(\"${db.dateFormat}\").parse(\"${db.validTo2}\")}")
	Date validTo2;
	@Value("${db.inputFileName}")
	String inputFileName;
	
	//@BeforeClass
	@PostConstruct
	public void setup() throws IOException {
		// import first version
//		importGraphVersion(graphName, version1, validFrom1, validTo1);
		
		// import second version
		IWayGraphVersionMetadata metadata = metadataDao.getWayGraphVersionMetadata(graphName, version2);
		if (metadata == null) {
			importGraphVersion(graphName, version2, validFrom2, null);
		}
		
	}
	
	private void importGraphVersion(String graphName, String version, Date validFrom, Date validTo) {
		InputStream stream = null;
		try 
		{
			stream = new FileInputStream(inputFileName);
			importService.importGraphVersion(graphName, version, stream, true);
			
			IWayGraphVersionMetadata metadata = metadataDao.getWayGraphVersionMetadata(graphName, version);
			metadata.setValidFrom(validFrom);
			if (validTo != null) {
				metadata.setValidTo(validTo);
			}
			metadata.setState(State.ACTIVE);
			
			metadataDao.updateGraphVersion(metadata);
			
		} catch (FileNotFoundException e) {
			log.error("file not found", e);
		} catch (GraphImportException e) {
			log.error("error importing graph", e);
		} catch (GraphAlreadyExistException e) {
			log.error("error, graph already exists", e);
		} finally {
		}
		
		log.info("Import finished");
	}

	//@Test
	public void testRouteWithWaySegmentsInLoop() throws UnkownRoutingAlgoException {
		log.info("Testing testRouteWithWaySegmentsInLoop()...");
		for (int i=0; i<10; i++) {
			testRouteWithWaySegments();
		}
	}
	
	@Test
//	@Ignore
	public void testRouteWithWaySegments() throws UnkownRoutingAlgoException {
		log.info("Testing testRouteWithWaySegments()...");

		long startSegmentId = 4586021;
		long endSegmentId = 51772367;
		
		StopWatch stopWatch = new StopWatch();
		
		IRoutingOptions options = new RoutingOptionsImpl(graphName, version2);
		options.setCriteria(RoutingCriteria.LENGTH);
		options.setMode(RoutingMode.CAR);

//		IRoutingOptions options = new RoutingOptionsImpl(graphName, version, RoutingAlgorithms.DIJKSTRA, RoutingCriteria.LENGTH, RoutingMode.CAR);
//		IRoutingOptions options = new RoutingOptionsImpl(graphName, version, RoutingCriteria.MIN_DURATION, RoutingMode.CAR);
		
//		IRoutingOptions options = new RoutingOptionsImpl(graphName, version, RoutingCriteria.MIN_DURATION, RoutingMode.PEDESTRIAN);
//		IRoutingOptions options = new RoutingOptionsImpl(graphName, version, RoutingCriteria.MIN_DURATION, null);
		IWaySegment startSegment = new WaySegment();
		startSegment.setId(startSegmentId);
		IWaySegment endSegment = new WaySegment();
		endSegment.setId(endSegmentId);
		List<IWaySegment> segments = new ArrayList<>();
		segments.add(startSegment);
		segments.add(endSegment);
		
		stopWatch.start();
		IRoute<IWaySegment, Double> route = routingService.routePerSegments(options, segments);
		stopWatch.stop();
		
		Assert.assertNotNull(route);
		
		printRoute(route);
		log.info("Routing took " + stopWatch.getTime() + " ms");
	}

	
	@Test
//	@Ignore
	public void testRouteWithWaySegmentsForCurrentVersion() throws UnkownRoutingAlgoException {
		log.info("Testing testRouteWithWaySegmentsForCurrentVersion()...");
		
		long startSegmentId = 4586021;
		long endSegmentId = 51772367;
		
		StopWatch stopWatch = new StopWatch();
		
		IRoutingOptions options = new RoutingOptionsImpl(graphName, null);
		options.setCriteria(RoutingCriteria.MIN_DURATION);
		options.setMode(RoutingMode.CAR);

		IWaySegment startSegment = new WaySegment();
		startSegment.setId(startSegmentId);
		IWaySegment endSegment = new WaySegment();
		endSegment.setId(endSegmentId);
		List<IWaySegment> segments = new ArrayList<>();
		segments.add(startSegment);
		segments.add(endSegment);
		
		stopWatch.start();
		IRoute<IWaySegment, Double> route = routingService.routePerSegments(options, segments);
		stopWatch.stop();
		
		Assert.assertNotNull(route);
		
		printRoute(route);
		log.info("Routing took " + stopWatch.getTime() + " ms");
	}

	@Test
//	@Ignore
	public void testRouteWithValidFilters() throws UnkownRoutingAlgoException {
		log.info("Testing testRouteWithValidFilters()...");
		
		long startSegmentId = 4586021;
		long endSegmentId = 51772367;
		
		StopWatch stopWatch = new StopWatch();
		
		IRoutingOptions options = new RoutingOptionsImpl(graphName, null);
		options.setCriteria(RoutingCriteria.MIN_DURATION);
		options.setMode(RoutingMode.CAR);
		
		Map<String, Set<Object>> tagFilters = new HashMap<>();
//		Set<Object> lanesFilter = new HashSet<>();
//		lanesFilter.add((short)1);
//		tagFilters.put("lanes_tow", lanesFilter);
		
		Set<Object> frcFilter = new HashSet<>();
		frcFilter.add((short)0);
		frcFilter.add((short)1);
		frcFilter.add((short)2);
		frcFilter.add((short)3);
		frcFilter.add((short)4);
		tagFilters.put("frc", frcFilter);
		
		options.setTagValueFilters(tagFilters);
		
		IWaySegment startSegment = new WaySegment();
		startSegment.setId(startSegmentId);
		IWaySegment endSegment = new WaySegment();
		endSegment.setId(endSegmentId);
		List<IWaySegment> segments = new ArrayList<>();
		segments.add(startSegment);
		segments.add(endSegment);
		
		stopWatch.start();
		IRoute<IWaySegment, Double> route = routingService.routePerSegments(options, segments);
		stopWatch.stop();
		
		Assert.assertNotNull(route);
		
		printRoute(route);
		log.info("Routing took " + stopWatch.getTime() + " ms");
	}

	@Test
//	@Ignore
	public void testRouteWithInValidFilters() throws UnkownRoutingAlgoException {
		log.info("Testing testRouteWithInValidFilters()...");
		
		long startSegmentId = 4586021;
		long endSegmentId = 51772367;

		StopWatch stopWatch = new StopWatch();
		
		IRoutingOptions options = new RoutingOptionsImpl(graphName, null);
		options.setCriteria(RoutingCriteria.MIN_DURATION);
		options.setMode(RoutingMode.CAR);

		Map<String, Set<Object>> tagFilters = new HashMap<>();
		Set<Object> lanesFilter = new HashSet<>();
		
		// there are no segments with 3 lanes!!!
		lanesFilter.add((short)3);
		tagFilters.put("lanes_tow", lanesFilter);
		
		Set<Object> frcFilter = new HashSet<>();
		
		// frc values are stored as shorts - not Strings!!!
		frcFilter.add("0");
		frcFilter.add("1");
		frcFilter.add("2");
		frcFilter.add("3");
		frcFilter.add("4");
		tagFilters.put("frc", frcFilter);
		
		options.setTagValueFilters(tagFilters);
		
		IWaySegment startSegment = new WaySegment();
		startSegment.setId(startSegmentId);
		IWaySegment endSegment = new WaySegment();
		endSegment.setId(endSegmentId);
		List<IWaySegment> segments = new ArrayList<>();
		segments.add(startSegment);
		segments.add(endSegment);
		
		stopWatch.start();
		IRoute<IWaySegment, Double> route = routingService.routePerSegments(options, segments);
		stopWatch.stop();
		
		Assert.assertNotNull(route);
		Assert.assertNull(route.getSegments());

		log.info("Routing took " + stopWatch.getTime() + " ms");
	}

	@Test
//	@Ignore
	public void testRouteWithWaySegmentsForValidTimestamp() throws UnkownRoutingAlgoException {
		log.info("Testing testRouteWithWaySegmentsForValidTimestamp()...");

		long startSegmentId = 4586021;
		long endSegmentId = 51772367;

		Calendar cal = Calendar.getInstance();
		cal.set(2018, 03, 22); // 22. April 2018
		
		StopWatch stopWatch = new StopWatch();
		
		IRoutingOptions options = new RoutingOptionsImpl(graphName, version2);
		options.setCriteria(RoutingCriteria.MIN_DURATION);
		options.setMode(RoutingMode.CAR);

		IWaySegment startSegment = new WaySegment();
		startSegment.setId(startSegmentId);
		IWaySegment endSegment = new WaySegment();
		endSegment.setId(endSegmentId);
		List<IWaySegment> segments = new ArrayList<>();
		segments.add(startSegment);
		segments.add(endSegment);
		
		stopWatch.start();
		IRoute<IWaySegment, Double> route = routingService.routePerSegments(options, segments);
		stopWatch.stop();
		
		Assert.assertNotNull(route);
		
		printRoute(route);
		log.info("Routing took " + stopWatch.getTime() + " ms");
	}

	@Test
	@Ignore
	public void testRouteWithWaySegmentsForInvalidTimestamp() throws UnkownRoutingAlgoException {
		log.info("Testing testRouteWithWaySegmentsForInvalidTimestamp()...");
		
		long startSegmentId = 4586021;
		long endSegmentId = 51772367;

		Calendar cal = Calendar.getInstance();
		cal.set(2016, 02, 02); // 2. März 2016
		
		StopWatch stopWatch = new StopWatch();
		
		IRoutingOptions options = new RoutingOptionsImpl(graphName, version2);
		options.setAlgorithm(RoutingAlgorithms.ASTAR);
		options.setCriteria(RoutingCriteria.MIN_DURATION);
		options.setMode(RoutingMode.CAR);

		IWaySegment startSegment = new WaySegment();
		startSegment.setId(startSegmentId);
		IWaySegment endSegment = new WaySegment();
		endSegment.setId(endSegmentId);
		List<IWaySegment> segments = new ArrayList<>();
		segments.add(startSegment);
		segments.add(endSegment);
		
		stopWatch.start();
		IRoute<IWaySegment, Double> route = routingService.routePerSegments(options, segments);
		stopWatch.stop();
		
		Assert.assertNotNull(route);
		
		printRoute(route);
		log.info("Routing took " + stopWatch.getTime() + " ms");
	}

	@Test
//	@Ignore
	public void testRouteWithCoordinates() {
		log.info("Testing testRouteWithCoordinates()...");

		double startY = 55.638616;
		double startX = 9.561346;
		double endY = 56.33339;
		double endX = 10.12442;
		
		List<Coordinate> coords = new ArrayList<Coordinate>(2);
		coords.add(new Coordinate(startX, startY));
		coords.add(new Coordinate(endX, endY));
		
		IRoutingOptions options = new RoutingOptionsImpl(graphName, version2, coords); //, RoutingCriteria.MIN_DURATION, RoutingMode.CAR);
		options.setCriteria(RoutingCriteria.LENGTH);
		options.setMode(RoutingMode.CAR);

		// by default segments will be cut!
		IRoute<IWaySegment, Double> route = null;
		try {
			route = routingService.route(options);
		} catch (UnkownRoutingAlgoException | RoutingException e) {
			log.error("Routing not successful!", e);
		}
		
		Assert.assertNotNull(route);
		
		printRoute(route);
		printRouteCSV(route);
		printRouteGeom(route);
		log.info("SRID=4326;POINT (" + startX + " " + startY + ")");
		log.info("SRID=4326;POINT (" + endX + " " + endY + ")");
		
//		options = new RoutingOptionsImpl(graphName, version, RoutingCriteria.LENGTH, RoutingMode.CAR);
//		
//		route = routingService.route(options, startX, startY, endX, endY);
//		
//		Assert.assertNotNull(route);
//		
//		log.info("Route for LENGTH:");
//		printRoute(route);
		
	}

	@Test
//	@Ignore
	public void testRouteWithCoordinatesForCurrentVersion() throws UnkownRoutingAlgoException, RoutingException {
		log.info("Testing testRouteWithCoordinatesForCurrentVersion()...");
		
		double startY = 55.3948;
		double startX = 9.4495;
		double endY = 55.41020;
		double endX = 10.06664;

		List<Coordinate> coords = new ArrayList<Coordinate>(2);
		coords.add(new Coordinate(startX, startY));
		coords.add(new Coordinate(endX, endY));
		
		IRoutingOptions options = new RoutingOptionsImpl(graphName, null, coords);
		options.setCriteria(RoutingCriteria.MIN_DURATION);
		options.setMode(RoutingMode.CAR);
		options.setRoutingTimestamp(LocalDate.now());

		// by default segments will be cut!
		IRoute<IWaySegment, Double> route = routingService.route(options);
		
		Assert.assertNotNull(route);
		
		printRoute(route);
		printRouteCSV(route);
		printRouteGeom(route);
		log.info("SRID=4326;POINT (" + startX + " " + startY + ")");
		log.info("SRID=4326;POINT (" + endX + " " + endY + ")");
		
		
	}

	@Test
	@Ignore
	public void testRouteWithCoordinatesCompareRoutingCriterias() throws UnkownRoutingAlgoException, RoutingException {
		log.info("Testing testRouteWithCoordinatesCompareRoutingCriterias()...");
		
		double startX = 13.02678;
		double startY = 47.82557;
		double endX = 13.01637;
		double endY = 47.82050;

		List<Coordinate> coords = new ArrayList<Coordinate>(2);
		coords.add(new Coordinate(startX, startY));
		coords.add(new Coordinate(endX, endY));
		
		IRoutingOptions options = new RoutingOptionsImpl(graphName, version2, coords);
		options.setCriteria(RoutingCriteria.MIN_DURATION);
		options.setMode(RoutingMode.CAR);

		// by default segments will be cut!
		IRoute<IWaySegment, Double> route = routingService.route(options);
		
		Assert.assertNotNull(route);
		
		log.info("Route for MIN_DURATION:");
		printRoute(route);
		
		RouteSegment[] expectedMinDurationRoute = new RouteSegment[] {
			new RouteSegment(901456797L, 890264283L, 890356197L, false, 14, 114.010605f), 
			new RouteSegment(901456725L, 890279283L, 890264283L, false, 2, 18.177055f), 
			new RouteSegment(901456728L, 890264282L, 890279283L, false, 2, 15.829121f), 
			new RouteSegment(901456727L, 890292015L, 890264282L, false, 11, 90.399704f), 
			new RouteSegment(901418541L, 890292015L, 890292542L, true, 1, 19.613031f), 
			new RouteSegment(901419398L, 460035347L, 890292542L, false, 0, 1.9944953f), 
			new RouteSegment(461014306L, 460037803L, 460035347L, false, 0, 1.5803671f), 
			new RouteSegment(461014305L, 460037794L, 460037803L, false, 10, 168.61662f), 
			new RouteSegment(461014299L, 460013641L, 460037794L, false, 4, 68.56605f), 
			new RouteSegment(461005492L, 460013641L, 460013632L, true, 25, 694.96497f), 
			new RouteSegment(461014278L, 460037761L, 460013632L, false, 5, 78.62679f), 
			new RouteSegment(461014277L, 460009772L, 460037761L, false, 10, 169.11105f), 
			new RouteSegment(461014275L, 460009772L, 460037758L, true, 4, 59.726955f), 
			new RouteSegment(461014276L, 460037758L, 460009773L, true, 0, 6.604948f), 
			new RouteSegment(901404956L, 460009773L, 890357382L, true, 0, 1.2801849f), 
			new RouteSegment(901457185L, 890357382L, 890000479L, true, 9, 64.559074f), 
			new RouteSegment(901457191L, 890357394L, 890000479L, false, 9, 59.561806f), 
			new RouteSegment(901457190L, 890264306L, 890357394L, false, 7, 46.27283f), 
			new RouteSegment(901457189L, 890357390L, 890264306L, false, 5, 36.25508f), 
			new RouteSegment(901457188L, 890352368L, 890357390L, false, 10, 68.28709f), 
			new RouteSegment(901454838L, 890000470L, 890352368L, false, 0, 1.282293f), 
			new RouteSegment(901404901L, 890267164L, 890000470L, false, 4, 49.11939f), 
			new RouteSegment(901404900L, 890266977L, 890267164L, false, 0, 4.8305664f), 
			new RouteSegment(901558922L, 890357442L, 890266977L, false, 12, 168.91676f), 
			new RouteSegment(901457161L, 890281264L, 890357442L, false, 1, 8.53974f), 
			new RouteSegment(901409896L, 890291708L, 890281264L, false, 3, 36.94618f), 
			new RouteSegment(901457193L, 890357450L, 890291708L, false, 3, 37.581577f), 
			new RouteSegment(901457192L, 890283463L, 890357450L, false, 1, 9.441535f), 
			new RouteSegment(901457195L, 890357463L, 890283463L, false, 3, 48.383465f), 
			new RouteSegment(901457194L, 890285746L, 890357463L, false, 2, 34.445896f)
		};

		compareRoutes(expectedMinDurationRoute, route);
		
		options = new RoutingOptionsImpl(graphName, version2, coords);
		options.setCriteria(RoutingCriteria.LENGTH);
		options.setMode(RoutingMode.CAR);
		
		route = routingService.route(options);
		
		Assert.assertNotNull(route);
		
		log.info("Route for criteria LENGTH:");
		
		printRoute(route);
		
		RouteSegment[] expectedMinLengthRoute = new RouteSegment[] {
			new RouteSegment(901456797L, 890264283L, 890356197L, true, 3, 28.381134f), 
			new RouteSegment(901456853L, 890356197L, 890356283L, true, 8, 64.83828f), 
			new RouteSegment(901456854L, 890356283L, 890278133L, true, 3, 28.114983f), 
			new RouteSegment(901456896L, 890356350L, 890278133L, false, 5, 38.134865f), 
			new RouteSegment(901456895L, 890287015L, 890356350L, false, 2, 12.980286f), 
			new RouteSegment(901456868L, 890356306L, 890287015L, false, 12, 97.701294f), 
			new RouteSegment(901456867L, 890288495L, 890356306L, false, 16, 135.80621f), 
			new RouteSegment(901415701L, 890278488L, 890288495L, false, 12, 99.08791f), 
			new RouteSegment(901410515L, 890278487L, 890278488L, false, 10, 85.65574f), 
			new RouteSegment(901410548L, 890278579L, 890278487L, false, 2, 19.233297f), 
			new RouteSegment(901416512L, 890277831L, 890278579L, false, 9, 71.55877f), 
			new RouteSegment(901410279L, 890277830L, 890277831L, false, 7, 58.60049f), 
			new RouteSegment(901413285L, 890284753L, 890277830L, false, 8, 64.5698f), 
			new RouteSegment(901415546L, 890280417L, 890284753L, false, 12, 96.267204f), 
			new RouteSegment(901411245L, 890280416L, 890280417L, false, 4, 34.417664f), 
			new RouteSegment(901415681L, 890278278L, 890280416L, false, 11, 89.696434f), 
			new RouteSegment(901414034L, 890286039L, 890278278L, false, 5, 38.851063f), 
			new RouteSegment(901416900L, 890281749L, 890286039L, false, 3, 24.949482f), 
			new RouteSegment(901411813L, 890277787L, 890281749L, false, 10, 87.10053f), 
			new RouteSegment(901410264L, 890004424L, 890277787L, false, 4, 30.047768f), 
			new RouteSegment(901393451L, 890004424L, 890010012L, true, 6, 77.44593f), 
			new RouteSegment(901409891L, 890010012L, 890285746L, true, 4, 49.796986f), 
			new RouteSegment(901457194L, 890285746L, 890357463L, true, 7, 95.00039f)
		};
		
		compareRoutes(expectedMinLengthRoute, route);

		System.out.println("SRID=4326;POINT (" + startX + " " + startY + ")");
		System.out.println("SRID=4326;POINT (" + endX + " " + endY + ")");
	}

	private void compareRoutes(RouteSegment[] expectedMinDurationRoute, IRoute<IWaySegment, Double> route) {
		
		Assert.assertEquals(expectedMinDurationRoute.length, route.getSegments().size());
		List<IWaySegment> segments = route.getSegments();
		
		long duration = 0;
		float length = 0;

		IWaySegment prevSeg = null;
		for (int i = 0; i < expectedMinDurationRoute.length; i++) {
			IWaySegment seg = segments.get(i);
			Assert.assertEquals("RouteSegment has not the same id!", expectedMinDurationRoute[i].getWayId(), seg.getId());
			Assert.assertEquals("RouteSegment has not the same startNodeid!", expectedMinDurationRoute[i].getStartNodeId(), seg.getStartNodeId());
			Assert.assertEquals("RouteSegment has not the same endNodeid!", expectedMinDurationRoute[i].getEndNodeId(), seg.getEndNodeId());
			boolean startToEnd = false;
			if (prevSeg == null) {				
				startToEnd = !isStartToEnd(seg, route.getSegments().get(1));				
			} else {
				startToEnd = isStartToEnd(seg, prevSeg);
			}
			
			Assert.assertEquals("RouteSegment has not the same startToEnd!", expectedMinDurationRoute[i].isStartToEnd(), startToEnd);
			Assert.assertEquals("RouteSegment has not the same duration!", expectedMinDurationRoute[i].getDuration(), seg.getDuration(startToEnd));
			duration += seg.getDuration(startToEnd);
			Assert.assertEquals("RouteSegment has not the same duration!", expectedMinDurationRoute[i].getLength(), seg.getLength(), 0.00000001);
			length += seg.getLength();
			
			prevSeg = seg;			
		}
		System.out.println("route.getLength() " + route.getLength() + ", length= " + length);
		System.out.println("route.getLength() " + route.getDuration() + ", duration= " + duration);
		// routes must not differ very much from expected because length of start and end segment are cut, too.
		Assert.assertTrue(route.getLength() <= length);
		Assert.assertTrue(route.getDuration() <= duration);
	}
	
//	@Test
//	@Ignore
//	public void testRouteWithCoordinatesAndWithoutCutting() {
//		log.info("Testing testRouteWithCoordinatesAndWithoutCutting()...");
//
//		IRoutingOptions options = new RoutingOptionsImpl(graphName, version2, RoutingCriteria.MIN_DURATION, RoutingMode.CAR);
//		
//		double startY = 55.638616;
//		double startX = 9.561346;
//		double endY = 56.33339;
//		double endX = 10.12442;
//
//		IRoute<IWaySegment> route = routingService.route(options, startX, startY, endX, endY, false);
//		
//		Assert.assertNotNull(route);
//		
//		printRoute(route);
//		System.out.println("SRID=4326;POINT (" + startX + " " + startY + ")");
//		System.out.println("SRID=4326;POINT (" + endX + " " + endY + ")");
//	}

	private void printRouteCSV(IRoute<IWaySegment, Double> route) {
		log.info("Route found for Graph " + route.getGraphName() + " in Version " + route.getGraphVersion());
		log.info("Path: " + route.getPath());
		log.info("Length: " + route.getLength()); 
		log.info("Duration: " + route.getDuration());

		
		System.out.println("WAY_ID;STARTNODE_ID;ENDNODE_ID;STARTTOEND;DURATION;LENGTH;FRC;SPEEDCALC;MAXSPEED");
		// print segment's attributes
		if (route.getSegments() != null) {
			IWaySegment prevSeg = null;
			for (IWaySegment seg : route.getSegments()) {
//				System.out.println(seg.getId() + ": length = " + seg.getLength() 
//						+ ", speedTow = " + seg.getMaxSpeedTow()
//						+ ", speedBkw = " + seg.getMaxSpeedBkw() 
//						+ ", duration_tow = " + seg.getDuration(true) + "(" + seg.getMinDuration(true) + ")s"
//						+ ", duration_bkw = " + seg.getDuration(false) + "(" + seg.getMinDuration(false) + ")s");
				
				boolean startToEnd = false;
				if (prevSeg == null) {
					if (seg.getEndNodeId() == route.getSegments().get(1).getStartNodeId() ||
						seg.getEndNodeId() == route.getSegments().get(1).getEndNodeId()) {
						startToEnd = false;
					}	
				} else {
					if (seg.getStartNodeId() == prevSeg.getStartNodeId() ||
						seg.getStartNodeId() == prevSeg.getEndNodeId()) {
						startToEnd = true;
					}
				}
				
				System.out.println(seg.getId() + "; " + seg.getStartNodeId() + "; " + seg.getEndNodeId() + "; " + startToEnd 
						+ "; " + seg.getDuration(startToEnd) + "; " + seg.getLength() + "; " + (seg.getFrc() == null ? "" : seg.getFrc().getValue())
						+ "; " + (startToEnd ? seg.getSpeedCalcTow() : seg.getSpeedCalcBkw()) + "; " + (startToEnd ? seg.getMaxSpeedTow() : seg.getMaxSpeedBkw()));				
				prevSeg = seg;
			}
		} else {
			System.out.println("Route segments are null!");
		}
	}
	
	private void printRoute(IRoute<IWaySegment, Double> route) {
		log.info("Route found for Graph " + route.getGraphName() + " in Version " + route.getGraphVersion());
		log.info("Path: " + route.getPath());
		log.info("Length: " + route.getLength()); 
		log.info("Duration: " + route.getDuration());

		// print segment's attributes
		if (route.getSegments() != null) {
			IWaySegment prevSeg = null;
			for (IWaySegment seg : route.getSegments()) {
//				System.out.println(seg.getId() + ": length = " + seg.getLength() 
//						+ ", speedTow = " + seg.getMaxSpeedTow()
//						+ ", speedBkw = " + seg.getMaxSpeedBkw() 
//						+ ", duration_tow = " + seg.getDuration(true) + "(" + seg.getMinDuration(true) + ")s"
//						+ ", duration_bkw = " + seg.getDuration(false) + "(" + seg.getMinDuration(false) + ")s");
				
				boolean startToEnd = false;
				if (prevSeg == null) {
					startToEnd = !isStartToEnd(seg, route.getSegments().get(1));
				} else {
					startToEnd = isStartToEnd(seg, prevSeg);
				}				
				System.out.println("new RouteSegment(" + seg.getId() + "L, " + seg.getStartNodeId() + "L, " + seg.getEndNodeId() + "L, " + startToEnd 
						+ ", " + seg.getDuration(startToEnd) + ", " + seg.getLength() + "f), ");				
				prevSeg = seg;
			}
		} else {
			System.out.println("Route segments are null!");
		}
	}
	
	private void printRouteGeom(IRoute<IWaySegment, Double> route) {
		if (route.getGeometry() != null) {
			System.out.println("Route's Geometry: " + route.getGeometry().toText());
			
		} else
		if (route.getSegments() != null && !route.getSegments().isEmpty()) {
			
			LineString[] lineStrings = new LineString[route.getSegments().size()];
			for (int i = 0; i < route.getSegments().size(); i++) {
				lineStrings[i] = route.getSegments().get(i).getGeometry();
			}
			MultiLineString mls = new MultiLineString(lineStrings, lineStrings[0].getFactory());

			System.out.println("Route's Geometry: " + mls.toText());
		}
	}

	
	private boolean isStartToEnd(IWaySegment seg, IWaySegment prevSeg) {
		
		boolean startToEnd = false;
		if (prevSeg == null) {
			throw new IllegalArgumentException("isStartToEnd() prevSeg is null");
		} else {
			if (seg.getStartNodeId() == prevSeg.getStartNodeId() ||
				seg.getStartNodeId() == prevSeg.getEndNodeId()) {
				startToEnd = true;
			}
		}
		return startToEnd;
	}
	
	private class RouteSegment implements IWaySegment {
		
		private long id;
		private long startNodeId;
		private long endNodeId;
		private boolean startToEnd;
		private float length;
		private int duration; // Duration in seconds
		
		public RouteSegment(long id, long startNodeId, long endNodeId, boolean startToEnd, 
				int duration, float length) {
			
			super();
			this.id = id;
			this.startNodeId = startNodeId;
			this.endNodeId = endNodeId;
			this.startToEnd = startToEnd;
			this.duration = duration;
			this.length = length;
		}

		@Override
		public LineString getGeometry() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void setGeometry(LineString geometry) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public float getLength() {
			return length;
		}

		@Override
		public void setLength(float length) {
			this.length = length;
			
		}

		@Override
		public String getName() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void setName(String name) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public String getStreetType() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void setStreetType(String streetType) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public long getWayId() {
			return id;
		}

		@Override
		public void setWayId(long wayId) {
			id = wayId;
			
		}

		@Override
		public long getStartNodeId() {

			return startNodeId;
		}

		@Override
		public void setStartNodeId(long startNodeId) {
			this.startNodeId = startNodeId;
			
		}

		@Override
		public int getStartNodeIndex() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public void setStartNodeIndex(int startNodeIndex) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public long getEndNodeId() {
			return endNodeId;
		}

		@Override
		public void setEndNodeId(long endNodeId) {
			this.endNodeId = endNodeId;
			
		}

		@Override
		public int getEndNodeIndex() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public void setEndNodeIndex(int endNodeIndex) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public Map<String, String> getTags() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void setTags(Map<String, String> tags) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public long getId() {
			return id;
		}

		@Override
		public void setId(long id) {
			this.id = id;			
		}

		@Override
		public List<ISegmentXInfo> getXInfo() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public List<ISegmentXInfo> getXInfo(String type) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void setXInfo(List<ISegmentXInfo> xInfo) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void addXInfo(ISegmentXInfo xInfo) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void addXInfo(List<ISegmentXInfo> xInfo) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public List<IWaySegmentConnection> getCons() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void setCons(List<IWaySegmentConnection> cons) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void addCons(List<IWaySegmentConnection> connections) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public short getMaxSpeedTow() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public void setMaxSpeedTow(short maxSpeedTow) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public short getMaxSpeedBkw() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public void setMaxSpeedBkw(short maxSpeedBkw) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public Short getSpeedCalcTow() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void setSpeedCalcTow(Short speedCalcTow) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public Short getSpeedCalcBkw() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void setSpeedCalcBkw(Short speedCalcBkw) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public short getLanesTow() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public void setLanesTow(short lanesTow) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public short getLanesBkw() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public void setLanesBkw(short lanesBkw) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public FuncRoadClass getFrc() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void setFrc(FuncRoadClass frc) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public FormOfWay getFormOfWay() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void setFormOfWay(FormOfWay formOfWay) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public Set<Access> getAccessTow() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void setAccessTow(Set<Access> accessTow) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public Set<Access> getAccessBkw() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void setAccessBkw(Set<Access> accessBkw) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public Boolean isTunnel() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void setTunnel(Boolean tunnel) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public Boolean isBridge() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void setBridge(Boolean bridge) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public Boolean isUrban() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void setUrban(Boolean urban) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public Date getTimestamp() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void setTimestamp(Date timestamp) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public int getDuration(boolean directionTow) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public int getMinDuration(boolean directionTow) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public OneWay isOneway() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public List<IWaySegmentConnection> getStartNodeCons() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public List<IWaySegmentConnection> getEndNodeCons() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void setStartNodeCons(List<IWaySegmentConnection> startNodeCons) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void setEndNodeCons(List<IWaySegmentConnection> endNodeCons) {
			// TODO Auto-generated method stub
			
		}
		
		public boolean isStartToEnd() {
			return startToEnd;
		}
		
		/**
		 * 
		 * @return duration in routing direction
		 */
		public int getDuration() {
			return duration;
		}
		
		public Object clone() {
			
			throw new RuntimeException("clone not supported");

		}
		
	}
	
}
