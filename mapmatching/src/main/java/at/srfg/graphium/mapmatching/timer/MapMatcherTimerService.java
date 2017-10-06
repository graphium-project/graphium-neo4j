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

import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.srfg.graphium.mapmatching.matcher.IMapMatcherTask;

/**
 * @author mwimmer
 *
 */
public class MapMatcherTimerService {
	
	private static Logger log = LoggerFactory.getLogger(MapMatcherTimerService.class);
	
	private Map<IMapMatcherTask, Date> mapMatcherTasks;
	private Timer timer;
	private TimeoutGuard timeoutGuard;
	private int delay = 1000;
	private int expireTime = 3000;
	
	@PostConstruct
	public void setup() {
		timeoutGuard = new TimeoutGuard(this);
		mapMatcherTasks = new ConcurrentHashMap<IMapMatcherTask, Date>();
		timer = new Timer();
		timer.schedule(timeoutGuard, 0, delay);
	}
	
	@PreDestroy
	public void shutdown() {
		timeoutGuard.cancelAllTasks();
	}

	public Map<IMapMatcherTask, Date> getMapMatcherTasks() {
		return mapMatcherTasks;
	}
	
	public void addMapMatcherTask(IMapMatcherTask task, int timeoutInMs) {
		if (timeoutInMs <= 0) {
			timeoutInMs = expireTime;
		}
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MILLISECOND, timeoutInMs);
		Date expireTimestamp = cal.getTime();
		mapMatcherTasks.put(task, expireTimestamp);
		
		if (log.isDebugEnabled()) {
			log.debug("added map matching task for trackId " + task.getTrack().getId() + " with expireTimestamp " + expireTimestamp);
		}
	}

	public int getDelay() {
		return delay;
	}

	/**
	 * delay for timeout check of map matching tasks in milliseconds
	 */
	public void setDelay(int delay) {
		this.delay = delay;
	}

	public int getExpireTime() {
		return expireTime;
	}

	/**
	 * time period in milliseconds after that a map matching task expires
	 */
	public void setExpireTime(int expireTime) {
		this.expireTime = expireTime;
	}
	
}