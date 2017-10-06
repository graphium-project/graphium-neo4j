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
package at.srfg.graphium.mapmatching.model.impl;

import java.util.ArrayList;
import java.util.List;

import com.vividsolutions.jts.geom.LineString;

import at.srfg.graphium.mapmatching.model.ITrack;
import at.srfg.graphium.mapmatching.model.ITrackMetadata;
import at.srfg.graphium.mapmatching.model.ITrackPoint;
import at.srfg.graphium.mapmatching.model.utils.TrackUtils;

/**
 * @author mwimmer
 *
 */
public class TrackImpl implements ITrack {

	private static final long serialVersionUID = 8284969449713682229L;
	
	private long id;
	private List<ITrackPoint> trackPoints = null;
	private LineString lineString;
	private ITrackMetadata metadata;

	@Override
	public long getId() {
		return id;
	}

	@Override
	public void setId(long id) {
		this.id = id;
	}

	@Override
	public void addTrackPoint(ITrackPoint point) {
		if (trackPoints == null) {
			trackPoints = new ArrayList<>();
		}
		trackPoints.add(point);
	}

	@Override
	public void removeTrackPoint(ITrackPoint point) {
		if (trackPoints != null) {
			trackPoints.remove(point);
		}
	}

	@Override
	public List<ITrackPoint> getTrackPoints() {
		return trackPoints;
	}

	@Override
	public void setTrackPoints(List<ITrackPoint> trackPoints) {
		this.trackPoints = trackPoints;
	}

	@Override
	public LineString getLineString() {
		return lineString;
	}

	@Override
	public void setLineString(LineString lineString) {
		this.lineString = lineString;
	}

	@Override
	public ITrackMetadata getMetadata() {
		return metadata;
	}

	@Override
	public void setMetadata(ITrackMetadata metadata) {
		this.metadata = metadata;
	}

    @Override
    public void calculateTrackPointValues() {
        TrackUtils.calculateTrackValues(this);
    }

    @Override
    public void calculateMetaData(boolean updateGeometry) {
    	if (updateGeometry) {
    		this.setLineString(TrackUtils.calculateLineString(this));
    	}
    	TrackUtils.calculateTrackMetadata(this);
    }
    
    @Override
    public void calculateLineString() {
    	TrackUtils.calculateTrackValues(this);
    	this.setLineString(TrackUtils.calculateLineString(this));
    }
    
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (id ^ (id >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TrackImpl other = (TrackImpl) obj;
		if (id != other.id)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "TrackImpl [id=" + id + ", trackPoints=" + trackPoints + ", lineString=" + lineString + ", metadata="
				+ metadata + "]";
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		TrackImpl returnObject = null;
        returnObject = (TrackImpl) super.clone();
		ArrayList<ITrackPoint> clonedTrackPoints = new ArrayList<ITrackPoint>();
		for (ITrackPoint currentTrackPoint : returnObject.getTrackPoints()){
			clonedTrackPoints.add((ITrackPoint) currentTrackPoint.clone());
		}
		returnObject.setTrackPoints(clonedTrackPoints);
		returnObject.setMetadata((ITrackMetadata) this.metadata.clone());
		
        return returnObject;
	}

}
