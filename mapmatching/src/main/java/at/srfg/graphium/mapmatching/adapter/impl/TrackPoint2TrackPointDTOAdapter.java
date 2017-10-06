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

import java.util.ArrayList;
import java.util.List;

import at.srfg.graphium.io.adapter.IAdapter;
import at.srfg.graphium.mapmatching.dto.TrackPointDTO;
import at.srfg.graphium.mapmatching.model.ITrackPoint;

/**
 * @author mwimmer
 *
 */
public class TrackPoint2TrackPointDTOAdapter implements IAdapter<List<TrackPointDTO>, List<ITrackPoint>> {

	@Override
	public List<TrackPointDTO> adapt(List<ITrackPoint> trackPointsToAdapt) {
		if (trackPointsToAdapt == null) {
			return null;
		}
		
		List<TrackPointDTO> trackPoints = new ArrayList<>(trackPointsToAdapt.size());
		for (ITrackPoint tp : trackPointsToAdapt) {
			TrackPointDTO trackPointDto = new TrackPointDTO();
			trackPointDto.setId(tp.getId());
			trackPointDto.setX(tp.getPoint().getX());
			trackPointDto.setY(tp.getPoint().getY());
			trackPointDto.setTimestamp(tp.getTimestamp().getTime());
			trackPoints.add(trackPointDto);
		}
		
		return trackPoints;
	}

}
