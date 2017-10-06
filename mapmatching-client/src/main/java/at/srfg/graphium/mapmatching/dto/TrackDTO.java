/**
 * Graphium Neo4j - Module of Graphserver for Map Matching specifying client functionality
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
package at.srfg.graphium.mapmatching.dto;

import java.util.List;

/**
 * @author mwimmer
 *
 */
public class TrackDTO {
	
	private long id;
	private List<TrackPointDTO> trackPoints = null;
	private TrackMetadataDTO metadata;
	
	public TrackDTO() {}
	
	public TrackDTO(long id, List<TrackPointDTO> trackPoints, TrackMetadataDTO metadata) {
		super();
		this.id = id;
		this.trackPoints = trackPoints;
		this.metadata = metadata;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public List<TrackPointDTO> getTrackPoints() {
		return trackPoints;
	}

	public void setTrackPoints(List<TrackPointDTO> trackPoints) {
		this.trackPoints = trackPoints;
	}

	public TrackMetadataDTO getMetadata() {
		return metadata;
	}

	public void setMetadata(TrackMetadataDTO metadata) {
		this.metadata = metadata;
	}

}