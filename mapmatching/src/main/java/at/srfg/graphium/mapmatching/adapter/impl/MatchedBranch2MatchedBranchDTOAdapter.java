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
import at.srfg.graphium.mapmatching.dto.MatchedBranchDTO;
import at.srfg.graphium.mapmatching.dto.MatchedWaySegmentDTO;
import at.srfg.graphium.mapmatching.model.IMatchedBranch;
import at.srfg.graphium.mapmatching.model.IMatchedWaySegment;

/**
 * @author mwimmer
 *
 */
public class MatchedBranch2MatchedBranchDTOAdapter implements IAdapter<MatchedBranchDTO, IMatchedBranch> {

	private IAdapter<MatchedWaySegmentDTO, IMatchedWaySegment> segmentAdapter;
	
	@Override
	public MatchedBranchDTO adapt(IMatchedBranch branchToAdapt) {
		if (branchToAdapt == null) {
			return null;
		}
		
		MatchedBranchDTO branch = new MatchedBranchDTO();
		branch.setFinished(branchToAdapt.isFinished());
		branch.setLength(branchToAdapt.getLength());
		branch.setMatchedFactor(branchToAdapt.getMatchedFactor());
		branch.setMatchedPoints(branchToAdapt.getMatchedPoints());
		branch.setNrOfShortestPathSearches(branchToAdapt.getNrOfShortestPathSearches());
		branch.setNrOfUTurns(branchToAdapt.getNrOfUTurns());
		if (branchToAdapt.getCertainPathEndSegment() != null) {
			branch.setCertainPathEndSegmentId(branchToAdapt.getCertainPathEndSegment().getId());
		}
		
		if (branchToAdapt.getMatchedWaySegments() != null && !branchToAdapt.getMatchedWaySegments().isEmpty()) {
			List<MatchedWaySegmentDTO> segments = new ArrayList<>();
			for (IMatchedWaySegment seg : branchToAdapt.getMatchedWaySegments()) {
				segments.add(segmentAdapter.adapt(seg));
			}
			branch.setSegments(segments);
		}
		
		return branch;
	}

	public IAdapter<MatchedWaySegmentDTO, IMatchedWaySegment> getSegmentAdapter() {
		return segmentAdapter;
	}

	public void setSegmentAdapter(IAdapter<MatchedWaySegmentDTO, IMatchedWaySegment> segmentAdapter) {
		this.segmentAdapter = segmentAdapter;
	}

}
