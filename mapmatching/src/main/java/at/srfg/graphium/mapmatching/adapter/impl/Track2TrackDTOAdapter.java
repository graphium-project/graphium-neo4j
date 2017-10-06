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
package at.srfg.graphium.mapmatching.adapter.impl;

import java.util.List;

import at.srfg.graphium.io.adapter.IAdapter;
import at.srfg.graphium.mapmatching.dto.TrackDTO;
import at.srfg.graphium.mapmatching.dto.TrackMetadataDTO;
import at.srfg.graphium.mapmatching.dto.TrackPointDTO;
import at.srfg.graphium.mapmatching.model.ITrack;
import at.srfg.graphium.mapmatching.model.ITrackMetadata;
import at.srfg.graphium.mapmatching.model.ITrackPoint;

/**
 * @author mwimmer
 *
 */
public class Track2TrackDTOAdapter implements IAdapter<TrackDTO, ITrack> {
	
	private IAdapter<TrackMetadataDTO, ITrackMetadata> metadataAdapter;
	private IAdapter<List<TrackPointDTO>, List<ITrackPoint>> trackpointsAdapter;

	@Override
	public TrackDTO adapt(ITrack trackToAdapt) {
		if (trackToAdapt == null) {
			return null;
		}
		
		List<TrackPointDTO> trackPoints = trackpointsAdapter.adapt(trackToAdapt.getTrackPoints());
		TrackDTO track = new TrackDTO();
		track.setId(trackToAdapt.getId());
		track.setMetadata(metadataAdapter.adapt(trackToAdapt.getMetadata()));
		track.setTrackPoints(trackPoints);
		return track;
	}

	public IAdapter<TrackMetadataDTO, ITrackMetadata> getMetadataAdapter() {
		return metadataAdapter;
	}

	public void setMetadataAdapter(IAdapter<TrackMetadataDTO, ITrackMetadata> metadataAdapter) {
		this.metadataAdapter = metadataAdapter;
	}

	public IAdapter<List<TrackPointDTO>, List<ITrackPoint>> getTrackpointsAdapter() {
		return trackpointsAdapter;
	}

	public void setTrackpointsAdapter(IAdapter<List<TrackPointDTO>, List<ITrackPoint>> trackpointsAdapter) {
		this.trackpointsAdapter = trackpointsAdapter;
	}

}
