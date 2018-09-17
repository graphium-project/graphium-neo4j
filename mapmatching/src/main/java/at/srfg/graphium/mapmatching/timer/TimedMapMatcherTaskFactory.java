/**
 * Graphium Neo4j - Map Matching module of Graphium
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
package at.srfg.graphium.mapmatching.timer;

import java.util.List;

import at.srfg.graphium.core.exception.GraphNotExistsException;
import at.srfg.graphium.mapmatching.matcher.IMapMatcher;
import at.srfg.graphium.mapmatching.matcher.IMapMatcherTask;
import at.srfg.graphium.mapmatching.model.IMatchedBranch;
import at.srfg.graphium.mapmatching.model.ITrack;

/**
 * @author mwimmer
 *
 */
public class TimedMapMatcherTaskFactory {
	
	private IMapMatcher mapMatcher;
	
	/**
	 * Creates a map matching task encapsulated within a TimedMapMatcherTask and creates an entry in the MapMatcherTimerService's map.
	 * @param graphName Graph's name; if not set map matching will be executed on the default graph (see Neo4jMapMatcher)
	 * @param graphVersion Graph's version; has to be version with state ACTIVE; if not set map matching will be executed on the current version 
	 * 					   (last version with state ACTIVE)
	 * @param track Track the map matching has to be executed for
	 * @param startSegmentId ID of the starting segment in the graph; optional; is used in case of iterative map matching
	 * @param previousBranches Branches of previous map matching iteration; optional; is used in case of iterative map matching
	 * @param timeoutInMs Timeout of map matching task in milliseconds; if map matching is not finished after timeout the map matcher's method cancel() 
	 * 					  will be called (the map matching will not return immediately in case of I/O operations (e.g. routing))
	 * @param service MapMatcherTimerService which manages a map with map matching tasks and their expire timestamps
	 * @param routingMode optional; possible values are "car" / "bike" / "pedestrian"
	 * @return TimedMapMatcherTask
	 * @throws GraphNotExistsException
	 */
	public TimedMapMatcherTask getTask(String graphName, String graphVersion, ITrack track, Long startSegmentId, List<IMatchedBranch> previousBranches, 
			int timeoutInMs, MapMatcherTimerService service, String routingMode) throws GraphNotExistsException {
		IMapMatcherTask mapMatcherTask;
		if (graphName != null) {
			if (graphVersion != null) {
				mapMatcherTask= mapMatcher.getTask(graphName, graphVersion, track, routingMode);
			} else {
				mapMatcherTask= mapMatcher.getTask(graphName, track, routingMode);
			}
		} else {
			mapMatcherTask= mapMatcher.getTask(track, routingMode);
		}
		return new TimedMapMatcherTask(mapMatcherTask, startSegmentId, previousBranches, timeoutInMs, service);
	}

	public IMapMatcher getMapMatcher() {
		return mapMatcher;
	}

	public void setMapMatcher(IMapMatcher mapMatcher) {
		this.mapMatcher = mapMatcher;
	}

}
