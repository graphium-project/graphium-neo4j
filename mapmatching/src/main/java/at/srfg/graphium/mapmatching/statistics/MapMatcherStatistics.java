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
package at.srfg.graphium.mapmatching.statistics;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MapMatcherStatistics {
	
	public static final String START_TIMESTAMP = "startTimestamp";
	public static final String END_TIMESTAMP = "endTimestamp";
	public static final String EXECUTION_TIME = "executionTime";
	public static final String UTURN_DETECTED = "uTurnDetected";
	public static final String SHORTEST_PATH_SEARCH = "shortestPathSearch";
	public static final String NUMBER_OF_TRACK_POINTS = "numberOfTrackPoints";
	public static final String TRACK_ID = "trackId";
	public static final String FLEET_ID = "fleetId";
	public static final String MATCHED_FACTORS = "matchedFactors";
	public static final String AVG_SAMPLING_RATE = "avgSamplingRate";
	public static final String TRACK_LENGTH = "trackLength";
	
	private static final String DELIMITER = ";";
	
	private Map<String, Object> statisticsMap = new HashMap<String, Object>();
	
	public void setValue(String key, Object value) {
		statisticsMap.put(key, value);
	}
	
	public void incrementValue(String key) {
		if (statisticsMap.containsKey(key) && statisticsMap.get(key) instanceof Integer) {
			statisticsMap.put(key, (Integer)statisticsMap.get(key) + 1);
		} else {
			statisticsMap.put(key, new Integer(1));
		}
	}
	
	public Object getValue(String key) {
		if (statisticsMap.containsKey(key)) {
			return statisticsMap.get(key);
		} else {
			return null;
		}
		
	}

	public Map<String, Object> getStatisticsMap() {
		return statisticsMap;
	}
	
	public void reset() {
		statisticsMap.clear();
	}
	
	@Override
	public String toString() {
		SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss.SSS");
		StringBuilder builder = new StringBuilder("\n====================================\n");
		for (String key : statisticsMap.keySet()) {
			Object value = statisticsMap.get(key);
			if (value instanceof Date) {
				value = df.format(value);
			}
			builder.append(key + ": " + value + "\n");
		}
		builder.append("====================================\n");
		return builder.toString();
	}
	
	public String toCsv() {
		SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss.SSS");
		return statisticsMap.get(TRACK_ID) + DELIMITER +
			   statisticsMap.get(NUMBER_OF_TRACK_POINTS) + DELIMITER +
			   statisticsMap.get(TRACK_LENGTH) + DELIMITER +
			   statisticsMap.get(AVG_SAMPLING_RATE) + DELIMITER +
			   (statisticsMap.containsKey(MATCHED_FACTORS) ? statisticsMap.get(MATCHED_FACTORS) : "") + DELIMITER +
			   statisticsMap.get(EXECUTION_TIME) + DELIMITER +
			   (statisticsMap.containsKey(START_TIMESTAMP) ? df.format(statisticsMap.get(START_TIMESTAMP)) : "") + DELIMITER +
			   (statisticsMap.containsKey(END_TIMESTAMP) ? df.format(statisticsMap.get(END_TIMESTAMP)) : "") + DELIMITER +
			   statisticsMap.get(SHORTEST_PATH_SEARCH) + DELIMITER +
			   statisticsMap.get(UTURN_DETECTED);
	}
	
}
