/**
 * Graphium Neo4j - Module of Graphserver for Map Matching using Neo4j
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
package at.srfg.graphium.mapmatching.neo4j.matcher.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Polygon;

import at.srfg.graphium.geomutils.GeometryUtils;
import at.srfg.graphium.mapmatching.model.ITrack;
import at.srfg.graphium.mapmatching.model.utils.TrackUtils;
import at.srfg.graphium.mapmatching.properties.IMapMatchingProperties;
import at.srfg.graphium.model.IWayGraphVersionMetadata;

public class TrackSanitizer {
	
	private static Logger log = LoggerFactory.getLogger(TrackSanitizer.class);

	/**
	 * Calculates the mean sampling rate and adjusts the parameters accordingly.
	 * 
	 * @param origTrack
	 * @param properties
	 */
	public void analyseTrack(ITrack origTrack, IMapMatchingProperties properties) {
		int meanSamplingRateSec = TrackUtils.getMeanSamplingRate(origTrack);
		properties.setMeanSamplingInterval(meanSamplingRateSec);
		
		log.info("track has a mean sampling rate of " + meanSamplingRateSec + "sec");			
	
		if (meanSamplingRateSec > properties.getThresholdForLowSamplingsInSecs()) {
			log.info("setting parameters for low sampling rated track!");
			properties.setTempMaxSegmentsForShortestPath(2 * properties.getMaxSegmentsForShortestPath());
			properties.setLowSamplingInterval(true);
		} else {
			log.info("setting parameters for normal sampling rated track!");
			properties.setTempMaxSegmentsForShortestPath(properties.getMaxSegmentsForShortestPath());
			properties.setLowSamplingInterval(false);
		}
	}
		
	/**
	 * Checks if the track lies within the graphs bounds.
	 * 
	 * @param origTrack
	 * @param graphMetadata
	 * @param graphName
	 * @return true, if the track is covered by the graph
	 */
	public boolean validateTrack(ITrack origTrack, IWayGraphVersionMetadata graphMetadata, String graphName) {

		if (graphMetadata == null) {
			log.error("No graph metadata found!");
			return false;
		}
		
		if (graphMetadata.getGraphName().equals(graphName)) {
			Polygon bounds = graphMetadata.getCoveredArea();
			// TODO consider also doing an exact check with the convex hull of the graph 
			if (bounds == null) {
				String errorMessage = "Bounds of graph " + graphName + " in version " + graphMetadata.getVersion() + " is null!";
				log.error(errorMessage);
				throw new RuntimeException(errorMessage);
			}
			if (origTrack.getLineString() != null && !bounds.intersects(origTrack.getLineString())) {
				log.error("track " + origTrack.getId() + " not in bounds of graph " + graphName + "!");
				return false;
			} else {
				return true;
			}
		}
		
		return false;
	}
	
	public int determineNextValidPointForSegmentSearch(ITrack track, int startPointIndex, int envelopeSideLength) {
		int pointsToSkip = 0;
		// Aufspannen eines Envelopes miITrackKantenlänge envelopeSideLength in Meter und Berücksichtigung aller Punkte innerhalb dieses Envelopes;
		// Nimm jenen Punkt, der wieder außerhalb dieses Envelopes liegt.
		// Dadurch soll das Routing Punktwolken erkennen und ausschließen.
		Envelope env = GeometryUtils.createEnvelopeInMeters(track.getTrackPoints().get(startPointIndex + pointsToSkip).getPoint(), envelopeSideLength);
		int index = 0;
		while ((index = startPointIndex + ++pointsToSkip) < track.getTrackPoints().size() && 
				env.contains(track.getTrackPoints().get(index).getPoint().getCoordinate())) {
		}
		return pointsToSkip;
	}
	
}