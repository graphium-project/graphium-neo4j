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
package at.srfg.graphium.mapmatching.model.utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

import at.srfg.graphium.geomutils.GeometryUtils;
import at.srfg.graphium.mapmatching.model.ITrack;
import at.srfg.graphium.mapmatching.model.ITrackPoint;

/**
 * @author mwimmer
 *
 */
public class TrackUtils {

	/**
	 * Calculate relevant inter point - metadata
	 * @param track
	 * @return
	 */
	public static void calculateTrackValues(ITrack track) {
		ITrackPoint lastPoint = null;
		
		List<ITrackPoint> trackPoints = track.getTrackPoints();
		// sort track points
		trackPoints.sort(new Comparator<ITrackPoint>() {

			@Override
			public int compare(ITrackPoint o1, ITrackPoint o2) {
				return o1.getTimestamp().compareTo(o2.getTimestamp());
			}
		});
		
		int i = 1;
        for (ITrackPoint trackPoint : trackPoints) {
            lastPoint = calculateTrackPointValues(trackPoint, lastPoint);
            trackPoint.setNumber(i);
            // set id of track to trackpoints, to restore state correctly
            trackPoint.setTrackId(track.getId());
            i++;
        }
	}

	/**
	 * 
	 * @param point the point for which the values are to calculate
	 * @param lastPoint
	 * @return
	 */
	public static ITrackPoint calculateTrackPointValues(ITrackPoint point, ITrackPoint lastPoint) {

		// init calculated values
		int timeElapsedMs = 0;			
		// velocity (requires spherical distance calculation
		float distance = Float.NaN;
		// can be NaN if timeelapsed = 0;
		float calcedVelocity = Float.NaN;
		// acceleration (Beschleunigung)
		float calcedAcceleration = Float.NaN;	
		
		// calculated values
		if (lastPoint != null && point.getTimestamp() != null && lastPoint.getTimestamp() != null) {
			
			// calculate time elapsed since last point
			timeElapsedMs = (int)(point.getTimestamp().getTime() - lastPoint.getTimestamp().getTime());

			distance = (float)GeometryUtils.distanceAndoyer((Point)lastPoint.getPoint(), (Point) point.getPoint());
			
			if (timeElapsedMs != 0) {
				
				double timeElapsedSeconds = timeElapsedMs / 1000.0;
				// can be NaN if timeelapsed = 0;
				calcedVelocity = (float)(distance / timeElapsedSeconds);
				
				// acceleration (Beschleunigung)
				calcedAcceleration = (float)((calcedVelocity - lastPoint.getVCalc()) / timeElapsedSeconds);

			}			
		} 
		point.setDistCalc(distance);
		point.setVCalc(calcedVelocity);
		point.setACalc(calcedAcceleration);
		return point;
	}
	
	/**
	 * Calculates a LineString from all trackpoints of a track
	 * @param track the track which should be considered for calculation
	 * @return the LineString
	 */
	public static LineString calculateLineString(ITrack track) {
		List<Coordinate> coords = new ArrayList<Coordinate>(track.getTrackPoints().size());
	    Coordinate lastCoordinate = null;
		for (ITrackPoint point : track.getTrackPoints()) {
	        if (lastCoordinate != null) {
	            //All other beside the first iteration
	            Coordinate coordinate = point.getPoint().getCoordinate();
	            //Only add the coordinate if it does'nt equal the coordinate before. This is to prevent many points
	            //within the linestring with an equal coordinate which invalidates the geometry
	            if (!coordinate.equals(lastCoordinate)) {
	                coords.add(coordinate);
	                lastCoordinate = coordinate;
	            }
	        } else {
	            //First iteration, set the coordinate to compare to
	            lastCoordinate = point.getPoint().getCoordinate();
	            coords.add(lastCoordinate);
	        }
		}
	
		if (coords.size() > 1) {
			int SRID = track.getTrackPoints().get(0).getPoint().getSRID();
			Coordinate[] coordinates = new Coordinate[coords.size()];
	        coordinates = coords.toArray(coordinates);
			return GeometryUtils.createLineString(coordinates, SRID);
		} else {
			return null;
		}
	}

	/**
	 * Calculates metadata of a track
	 * @param track
	 */
	public static void calculateTrackMetadata(ITrack track) {
		track.getMetadata().setNumberOfPoints(track.getTrackPoints().size());
		track.getMetadata().setLength(calculateLength(track.getTrackPoints(), 0, track.getTrackPoints().size()));	
		
		if (track.getTrackPoints() != null && !track.getTrackPoints().isEmpty()) {
			track.getMetadata().setStartDate(track.getTrackPoints().get(0).getTimestamp());
			track.getMetadata().setEndDate(track.getTrackPoints().get(track.getTrackPoints().size() - 1).getTimestamp());
		}
		track.getMetadata().setDuration(new Date(calculateDuration(track)));
		

	}
	
	/**
	 * calculates length adding dist_calc values of the points from startIndex to endIndex - 1.
	 * This means that distance from startIndex - 1 to startIndex is also included.
	 * TODO refactor - distance should be length from startIndex to endIndex as user expects.
	 * @param trackPoints
	 * @param startIndex
	 * @param endIndex
	 * @return
	 */
	public static double calculateLength(List<? extends ITrackPoint> trackPoints, int startIndex, int endIndex ) {
		double length = Double.NaN;
		if (trackPoints.size() > 1) {
			length = 0;
			for (int i = startIndex; i < endIndex; i++) {
				double dist =  ((ITrackPoint)trackPoints.get(i)).getDistCalc();
				if (!Double.isNaN(dist)) {
					length = length + dist;	
				}
			}
		} 
		return length;
	}
	
	/**
	 * Calculates the duration of a track in milliseconds
	 * @param track
	 * @return
	 */
	public static long calculateDuration(ITrack track) {
		
		long duration = -1;
		ITrackPoint first = null;
		for (int i = 0; i < track.getTrackPoints().size(); i++) {
			ITrackPoint tp = track.getTrackPoints().get(i);
			if (tp.getTimestamp() != null) {
				
				first = tp;
				break;
			}
		}
		
		ITrackPoint last = null;
		if (first != null) {
			for (int i = track.getTrackPoints().size() - 1; i > -1 ; i--) {
				ITrackPoint tp = track.getTrackPoints().get(i);
				if (tp.getTimestamp() != null) {
					last = tp;
					break;
				}
			}
			
			if (last != null) {
				duration = last.getTimestamp().getTime() - first.getTimestamp().getTime();
			}
		}
		
		return duration;
	}
	
	public static int getMeanSamplingRate(ITrack track) {
		long timeDiff = track.getMetadata().getEndDate().getTime() - track.getMetadata().getStartDate().getTime();
		int meanSamplingRateMs = (int) (timeDiff / track.getTrackPoints().size());
		int meanSamplingRateSec = meanSamplingRateMs / 1000;
		return meanSamplingRateSec;
	}
	
}
