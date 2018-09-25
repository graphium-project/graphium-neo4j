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

import at.srfg.graphium.io.adapter.IAdapter;
import at.srfg.graphium.mapmatching.dto.MatchedWaySegmentDTO;
import at.srfg.graphium.mapmatching.model.IMatchedWaySegment;

/**
 * @author mwimmer
 *
 */
public class MatchedWaySegment2MatchedWaySegmentDTOAdapter implements IAdapter<MatchedWaySegmentDTO, IMatchedWaySegment> {

	@Override
	public MatchedWaySegmentDTO adapt(IMatchedWaySegment segmentToAdapt) {
		if (segmentToAdapt == null) {
			return null;
		}
		
		MatchedWaySegmentDTO segment = new MatchedWaySegmentDTO();
		segment.setEndPointIndex(segmentToAdapt.getEndPointIndex());
		segment.setEnteringThroughStartNode(segmentToAdapt.getDirection().isEnteringThroughStartNode());
		segment.setFromPathSearch(segmentToAdapt.isFromPathSearch());
		segment.setLeavingThroughStartNode(segmentToAdapt.getDirection().isLeavingThroughStartNode());
		segment.setSegmentId(segmentToAdapt.getSegment().getId());
		segment.setStartPointIndex(segmentToAdapt.getStartPointIndex());
		segment.setStartSegment(segmentToAdapt.isStartSegment());
		segment.setuTurnSegment(segmentToAdapt.isUTurnSegment());
		segment.setWeight(segmentToAdapt.getWeight());
		segment.setMatchedFactor(segmentToAdapt.getMatchedFactor());
		if (segmentToAdapt.getGeometry() != null) {
			segment.setGeometry(segmentToAdapt.getGeometry().toText());
		}
		
		return segment;
	}

}
