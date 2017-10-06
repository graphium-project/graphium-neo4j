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

import java.util.HashSet;
import java.util.Set;

import at.srfg.graphium.mapmatching.dto.MatchedWaySegmentDTO;
import at.srfg.graphium.mapmatching.model.IMatchedWaySegment;
import at.srfg.graphium.model.Access;

/**
 * @author mwimmer
 *
 */
public class MatchedWaySegment2VerboseMatchedWaySegmentDTOAdapter extends MatchedWaySegment2MatchedWaySegmentDTOAdapter {

	@Override
	public MatchedWaySegmentDTO adapt(IMatchedWaySegment segmentToAdapt) {
		MatchedWaySegmentDTO segment = super.adapt(segmentToAdapt);
		
		segment.setName(segmentToAdapt.getName());
		segment.setLength(segmentToAdapt.getLength());
		segment.setMaxSpeedTow(segmentToAdapt.getMaxSpeedTow());
		segment.setMaxSpeedBkw(segmentToAdapt.getMaxSpeedBkw());
		segment.setCalcSpeedTow(segmentToAdapt.getSpeedCalcTow());
		segment.setCalcSpeedBkw(segmentToAdapt.getSpeedCalcBkw());
		segment.setLanesTow(segmentToAdapt.getLanesTow());
		segment.setLanesBkw(segmentToAdapt.getLanesBkw());
		segment.setFrc(segmentToAdapt.getFrc().getValue());
		segment.setFormOfWay(segmentToAdapt.getFormOfWay().name());
		segment.setStreetType(segmentToAdapt.getStreetType());
		segment.setWayId(segmentToAdapt.getWayId());
		segment.setStartNodeIndex(segmentToAdapt.getStartNodeIndex());
		segment.setStartNodeId(segmentToAdapt.getStartNodeId());
		segment.setEndNodeIndex(segmentToAdapt.getEndNodeIndex());
		segment.setEndNodeId(segmentToAdapt.getEndNodeId());
		segment.setAccessTow(adaptAccess(segmentToAdapt.getAccessTow()));
		segment.setAccessBkw(adaptAccess(segmentToAdapt.getAccessBkw()));
		segment.setTunnel(segmentToAdapt.isTunnel());
		segment.setBridge(segmentToAdapt.isBridge());
		segment.setUrban(segmentToAdapt.isUrban());
		segment.setTags(segmentToAdapt.getTags());

		return segment;
	}

	private Set<String> adaptAccess(Set<Access> accessSet) {
		if (accessSet == null) {
			return null;
		} else {
			Set<String> accesses = new HashSet<>();
			for (Access access : accessSet) {
				accesses.add(access.name());
			}
			return accesses;
		}
	}

}