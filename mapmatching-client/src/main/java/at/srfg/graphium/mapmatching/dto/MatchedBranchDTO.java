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

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * @author mwimmer
 *
 */
@JsonInclude(value=Include.NON_EMPTY)
public class MatchedBranchDTO {

	private List<MatchedWaySegmentDTO> segments = new ArrayList<MatchedWaySegmentDTO>();
	private boolean finished;
	private int nrOfUTurns;
	private int nrOfShortestPathSearches;
	private double length;
	private double matchedFactor;
	private int matchedPoints;
	private long certainPathEndSegmentId = 0;
	
	public MatchedBranchDTO() {}
	
	public MatchedBranchDTO(List<MatchedWaySegmentDTO> segments, boolean finished, int nrOfUTurns,
			int nrOfShortestPathSearches, double length, double matchedFactor, int matchedPoints,
			long certainPathEndSegmentId) {
		super();
		this.segments = segments;
		this.finished = finished;
		this.nrOfUTurns = nrOfUTurns;
		this.nrOfShortestPathSearches = nrOfShortestPathSearches;
		this.length = length;
		this.matchedFactor = matchedFactor;
		this.matchedPoints = matchedPoints;
		this.certainPathEndSegmentId = certainPathEndSegmentId;
	}

	public List<MatchedWaySegmentDTO> getSegments() {
		return segments;
	}

	public void setSegments(List<MatchedWaySegmentDTO> segments) {
		this.segments = segments;
	}

	public boolean isFinished() {
		return finished;
	}

	public void setFinished(boolean finished) {
		this.finished = finished;
	}

	public int getNrOfUTurns() {
		return nrOfUTurns;
	}

	public void setNrOfUTurns(int nrOfUTurns) {
		this.nrOfUTurns = nrOfUTurns;
	}

	public int getNrOfShortestPathSearches() {
		return nrOfShortestPathSearches;
	}

	public void setNrOfShortestPathSearches(int nrOfShortestPathSearches) {
		this.nrOfShortestPathSearches = nrOfShortestPathSearches;
	}

	public double getLength() {
		return length;
	}

	public void setLength(double length) {
		this.length = length;
	}

	public double getMatchedFactor() {
		return matchedFactor;
	}

	public void setMatchedFactor(double matchedFactor) {
		this.matchedFactor = matchedFactor;
	}

	public int getMatchedPoints() {
		return matchedPoints;
	}

	public void setMatchedPoints(int matchedPoints) {
		this.matchedPoints = matchedPoints;
	}

	public long getCertainPathEndSegmentId() {
		return certainPathEndSegmentId;
	}

	public void setCertainPathEndSegmentId(long certainPathEndSegmentId) {
		this.certainPathEndSegmentId = certainPathEndSegmentId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (finished ? 1231 : 1237);
		long temp;
		temp = Double.doubleToLongBits(length);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(matchedFactor);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + matchedPoints;
		result = prime * result + nrOfShortestPathSearches;
		result = prime * result + nrOfUTurns;
		result = prime * result + ((segments == null) ? 0 : segments.hashCode());
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
		MatchedBranchDTO other = (MatchedBranchDTO) obj;
		if (finished != other.finished)
			return false;
		if (Double.doubleToLongBits(length) != Double.doubleToLongBits(other.length))
			return false;
		if (Double.doubleToLongBits(matchedFactor) != Double.doubleToLongBits(other.matchedFactor))
			return false;
		if (matchedPoints != other.matchedPoints)
			return false;
		if (nrOfShortestPathSearches != other.nrOfShortestPathSearches)
			return false;
		if (nrOfUTurns != other.nrOfUTurns)
			return false;
		if (segments == null) {
			if (other.segments != null)
				return false;
		} else if (!segments.equals(other.segments))
			return false;
		return true;
	}

}
