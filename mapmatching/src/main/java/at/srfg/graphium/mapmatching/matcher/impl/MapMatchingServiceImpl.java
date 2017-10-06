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
/**
 * (C) 2016 Salzburg Research Forschungsgesellschaft m.b.H.
 *
 * All rights reserved.
 *
 */
package at.srfg.graphium.mapmatching.matcher.impl;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;

import at.srfg.graphium.core.exception.GraphNotExistsException;
import at.srfg.graphium.mapmatching.matcher.IMapMatchingService;
import at.srfg.graphium.mapmatching.model.IMatchedBranch;
import at.srfg.graphium.mapmatching.model.ITrack;
import at.srfg.graphium.mapmatching.timer.MapMatcherTimerService;
import at.srfg.graphium.mapmatching.timer.TimedMapMatcherTask;
import at.srfg.graphium.mapmatching.timer.TimedMapMatcherTaskFactory;

/**
 * @author mwimmer
 */
public class MapMatchingServiceImpl implements IMapMatchingService {
	
	private MapMatcherTimerService mapMatcherTimerService;
	private TimedMapMatcherTaskFactory mapMatcherTaskFactory;

	@Override
	public List<IMatchedBranch> matchTrack(String graphName, String graphVersion, ITrack track, Long startSegmentId, List<IMatchedBranch> previousBranches, 
			int timeoutInMs, boolean onlyBestResult) throws GraphNotExistsException, CancellationException {
		
		TimedMapMatcherTask task = mapMatcherTaskFactory.getTask(graphName, graphVersion, track, startSegmentId, previousBranches, timeoutInMs, mapMatcherTimerService);
		
		List<IMatchedBranch> branches = task.matchTrack();
		
		if (branches != null && !branches.isEmpty()) {
			if (onlyBestResult) {
				// return only first/best branch
				return Collections.singletonList(branches.get(0));
			} else {
				return branches;
			}
		} else {
			return null;
		}	
	}

	public MapMatcherTimerService getMapMatcherTimerService() {
		return mapMatcherTimerService;
	}

	public void setMapMatcherTimerService(MapMatcherTimerService mapMatcherTimerService) {
		this.mapMatcherTimerService = mapMatcherTimerService;
	}

	public TimedMapMatcherTaskFactory getMapMatcherTaskFactory() {
		return mapMatcherTaskFactory;
	}

	public void setMapMatcherTaskFactory(TimedMapMatcherTaskFactory mapMatcherTaskFactory) {
		this.mapMatcherTaskFactory = mapMatcherTaskFactory;
	}
	
}
