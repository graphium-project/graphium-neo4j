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

import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.srfg.graphium.mapmatching.matcher.IMapMatcherTask;

/**
 * @author mwimmer
 *
 */
public class TimeoutGuard extends TimerTask {

	private static Logger log = LoggerFactory.getLogger(TimeoutGuard.class);
	
	private MapMatcherTimerService service;
	
	public TimeoutGuard(MapMatcherTimerService service) {
		this.service = service;
	}
	
	@Override
	public void run() {
		long now = System.currentTimeMillis();
		for (IMapMatcherTask task : service.getMapMatcherTasks().keySet()) {
			if (service.getMapMatcherTasks().get(task).getTime() < now) {
				if (log.isDebugEnabled()) {
					log.debug("Cancelling map matching task for trackId " + task.getTrack().getId() 
							+ " (expired at " + service.getMapMatcherTasks().get(task) + ")");
					log.info((service.getMapMatcherTasks().keySet().size() - 1) + " map matching tasks active");
				}
				try {
					task.cancel();
				} catch (InterruptedException e) {
					
				}
			}
		}
	}
	
	public void cancelAllTasks() {
		log.info("Cancelling all map matching tasks...");
		for (IMapMatcherTask task : service.getMapMatcherTasks().keySet()) {
			try {
				task.cancel();
			} catch (InterruptedException e) {
				
			}
		}
	}

}