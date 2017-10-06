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
package at.srfg.graphium.mapmatching.statistics;

/**
 * @author mwimmer
 */
public class MapMatcherGlobalStatistics {
	
	private static final String DELIMITER =";";
	
	private int successfullyMatchedTracks = 0;
	private int notSuccessfullyMatchedTracks = 0;
	private double aggregatedLengthOfMatchedTracks = 0;
	private int successfullyMatchedTrackPoints = 0;
	private int notSuccessfullyMatchedTrackPoints = 0;

	public void init() {
		successfullyMatchedTracks = 0;
		notSuccessfullyMatchedTracks = 0;
		aggregatedLengthOfMatchedTracks = 0;
		successfullyMatchedTrackPoints = 0;
		notSuccessfullyMatchedTrackPoints = 0;
	}
	
	public void incrementSuccess(int trackPoints, double length) {
		successfullyMatchedTrackPoints++;
		aggregatedLengthOfMatchedTracks += length;
		successfullyMatchedTrackPoints += trackPoints;
	}
	
	public void incrementNonSuccess(int trackPoints) {
		notSuccessfullyMatchedTrackPoints++;
		notSuccessfullyMatchedTrackPoints += trackPoints;
	}

	@Override
	public String toString() {
		return "MapMatcherGlobalStatistics [successfullyMatchedTracks=" + successfullyMatchedTracks
				+ ", notSuccessfullyMatchedTracks=" + notSuccessfullyMatchedTracks
				+ ", aggregatedLengthOfMatchedTracks=" + aggregatedLengthOfMatchedTracks
				+ ", successfullyMatchedTrackPoints=" + successfullyMatchedTrackPoints
				+ ", notSuccessfullyMatchedTrackPoints=" + notSuccessfullyMatchedTrackPoints + "]";
	}
	
	public String toCsv() {
		return successfullyMatchedTracks + DELIMITER +
			   successfullyMatchedTrackPoints + DELIMITER +
			   aggregatedLengthOfMatchedTracks + DELIMITER +
			   notSuccessfullyMatchedTracks + DELIMITER +
			   notSuccessfullyMatchedTrackPoints;
	}
	
}