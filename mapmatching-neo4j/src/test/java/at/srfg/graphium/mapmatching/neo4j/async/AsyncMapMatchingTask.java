package at.srfg.graphium.mapmatching.neo4j.async;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.srfg.graphium.core.exception.GraphNotExistsException;
import at.srfg.graphium.mapmatching.matcher.IMapMatcherTask;
import at.srfg.graphium.mapmatching.model.IMatchedBranch;
import at.srfg.graphium.mapmatching.model.ITrack;
import at.srfg.graphium.mapmatching.neo4j.matcher.impl.Neo4jMapMatcher;
import at.srfg.graphium.routing.exception.RoutingParameterException;

/**
 * @author mwimmer
 *
 */
public class AsyncMapMatchingTask implements Runnable {

	private static Logger log = LoggerFactory.getLogger(AsyncMapMatchingTask.class);
	private Neo4jMapMatcher mapMatcher;
	private ITrack track;
	private String graphName;
	private String routingMode = "car";
	
	public AsyncMapMatchingTask(Neo4jMapMatcher mapMatcher, ITrack track, String graphName) {
		super();
		this.mapMatcher = mapMatcher;
		this.track = track;
		this.graphName = graphName;
	}

	@Override
	public void run() {
		try {
			matchTrack(track, graphName);
		} catch (GraphNotExistsException | RoutingParameterException e) {
			log.error(e.getMessage());
		}
	}

	private List<IMatchedBranch> matchTrack(ITrack track, String graphName) throws GraphNotExistsException, RoutingParameterException {
		long startTime = System.nanoTime();
		IMapMatcherTask task = mapMatcher.getTask(graphName, track, routingMode);
		List<IMatchedBranch> branches = task.matchTrack();
		log.info("Map matching took " +  + (System.nanoTime() - startTime) + "ns = " + ((System.nanoTime() - startTime) / 1000000) + "ms");
		return branches;
	}

}
