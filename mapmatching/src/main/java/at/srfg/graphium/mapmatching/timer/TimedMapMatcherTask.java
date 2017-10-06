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
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;

import at.srfg.graphium.mapmatching.matcher.IMapMatcherTask;
import at.srfg.graphium.mapmatching.model.IMatchedBranch;

/**
 * @author mwimmer
 *
 */
public class TimedMapMatcherTask implements Callable<List<IMatchedBranch>> {

	private IMapMatcherTask mapMatcherTask;
	private MapMatcherTimerService service;
	private Long startSegmentId;
	private List<IMatchedBranch> previousBranches;
	
	public TimedMapMatcherTask(IMapMatcherTask mapMatcherTask, Long startSegmentId, List<IMatchedBranch> previousBranches, 
			int timeoutInMs, MapMatcherTimerService service) {
		this.mapMatcherTask = mapMatcherTask;
		this.service = service;
		this.startSegmentId = startSegmentId;
		this.previousBranches = previousBranches;
		service.addMapMatcherTask(mapMatcherTask, timeoutInMs);
	}

	public List<IMatchedBranch> matchTrack() {
		List<IMatchedBranch> branches = null;
		try {
			if (startSegmentId != null) {
				branches = mapMatcherTask.matchTrack(startSegmentId);
			} else if (previousBranches != null) {
				branches = mapMatcherTask.matchTrack(previousBranches);
			} else {
				branches = mapMatcherTask.matchTrack();
			}
		} catch (CancellationException e) {
			throw new CancellationException(e.getMessage());
		} finally {	
			// remove task from map
			evict();
		}
		return branches;
	}

	private void evict() {
		service.getMapMatcherTasks().remove(mapMatcherTask);
	}

	@Override
	public List<IMatchedBranch> call() throws Exception {
		return matchTrack();
	}
	
}
