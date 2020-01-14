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

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * @author mwimmer
 *
 */
@JsonInclude(value=Include.NON_EMPTY)
public class MatchedWaySegmentDTO {

	private long segmentId;
	private int startPointIndex;	// inclusive
	private int endPointIndex;		// exclusive
	private boolean enteringThroughStartNode;
	private boolean leavingThroughStartNode;
	private boolean enteringThroughEndNode;
	private boolean leavingThroughEndNode;
	private boolean startSegment = false;
	private boolean afterSkippedPart = false;
	private boolean fromPathSearch = false;
	private boolean uTurnSegment = false;
	private double weight;
	private double matchedFactor;
	private String geometry;
	
	// optional attributes for verbose output
	private String name;
	private float length;
	private short maxSpeedTow;
	private short maxSpeedBkw;
	private short calcSpeedTow;
	private short calcSpeedBkw;
	private short lanesTow;
	private short lanesBkw;
	private short frc;
	private String formOfWay;
	private String streetType;
	private long wayId;
	private int startNodeIndex;
	private long startNodeId;
	private int endNodeIndex;
	private long endNodeId;
	private Set<String> accessTow;
	private Set<String> accessBkw;
	private boolean tunnel;
	private boolean bridge;
	private boolean urban;
	private Map<String, String> tags;
	
	public MatchedWaySegmentDTO() {}
	
	public MatchedWaySegmentDTO(long segmentId, int startPointIndex, int endPointIndex,
			boolean enteringThroughStartNode, boolean leavingThroughStartNode,
			boolean enteringThroughEndNode, boolean leavingThroughEndNode, boolean startSegment,
			boolean fromPathSearch, boolean uTurnSegment, double weight, double matchedFactor, String geometry,
			String name, float length, short maxSpeedTow, short maxSpeedBkw, short calcSpeedTow,
			short calcSpeedBkw, short lanesTow, short lanesBkw, short frc, String formOfWay,
			String streetType, long wayId, int startNodeIndex,
			long startNodeId, int endNodeIndex, long endNodeId, Set<String> accessTow,
			Set<String> accessBkw, boolean tunnel, boolean bridge, boolean urban, Map<String, String> tags) {
		super();
		this.segmentId = segmentId;
		this.startPointIndex = startPointIndex;
		this.endPointIndex = endPointIndex;
		this.enteringThroughStartNode = enteringThroughStartNode;
		this.leavingThroughStartNode = leavingThroughStartNode;
		this.enteringThroughEndNode = enteringThroughEndNode;
		this.leavingThroughEndNode = leavingThroughEndNode;
		this.startSegment = startSegment;
		this.fromPathSearch = fromPathSearch;
		this.uTurnSegment = uTurnSegment;
		this.weight = weight;
		this.matchedFactor = matchedFactor;
		this.geometry = geometry;
		this.name = name;
		this.length = length;
		this.maxSpeedTow = maxSpeedTow;
		this.maxSpeedBkw = maxSpeedBkw;
		this.calcSpeedTow = calcSpeedTow;
		this.calcSpeedBkw = calcSpeedBkw;
		this.lanesTow = lanesTow;
		this.lanesBkw = lanesBkw;
		this.frc = frc;
		this.formOfWay = formOfWay;
		this.streetType = streetType;
		this.wayId = wayId;
		this.startNodeIndex = startNodeIndex;
		this.startNodeId = startNodeId;
		this.endNodeIndex = endNodeIndex;
		this.endNodeId = endNodeId;
		this.accessTow = accessTow;
		this.accessBkw = accessBkw;
		this.tunnel = tunnel;
		this.bridge = bridge;
		this.urban = urban;
		this.tags = tags;
	}

	public long getSegmentId() {
		return segmentId;
	}

	public void setSegmentId(long segmentId) {
		this.segmentId = segmentId;
	}

	public int getStartPointIndex() {
		return startPointIndex;
	}

	public void setStartPointIndex(int startPointIndex) {
		this.startPointIndex = startPointIndex;
	}

	public int getEndPointIndex() {
		return endPointIndex;
	}

	public void setEndPointIndex(int endPointIndex) {
		this.endPointIndex = endPointIndex;
	}

	public boolean isEnteringThroughStartNode() {
		return enteringThroughStartNode;
	}

	public void setEnteringThroughStartNode(boolean enteringThroughStartNode) {
		this.enteringThroughStartNode = enteringThroughStartNode;
	}

	public boolean isLeavingThroughStartNode() {
		return leavingThroughStartNode;
	}

	public void setLeavingThroughStartNode(boolean leavingThroughStartNode) {
		this.leavingThroughStartNode = leavingThroughStartNode;
	}

	public boolean isEnteringThroughEndNode() {
		return enteringThroughEndNode;
	}

	public void setEnteringThroughEndNode(boolean enteringThroughEndNode) {
		this.enteringThroughEndNode = enteringThroughEndNode;
	}

	public boolean isLeavingThroughEndNode() {
		return leavingThroughEndNode;
	}

	public void setLeavingThroughEndNode(boolean leavingThroughEndNode) {
		this.leavingThroughEndNode = leavingThroughEndNode;
	}

	public boolean isStartSegment() {
		return startSegment;
	}

	public void setStartSegment(boolean startSegment) {
		this.startSegment = startSegment;
	}

	public boolean isAfterSkippedPart() {
		return afterSkippedPart;
	}

	public void setAfterSkippedPart(boolean afterSkippedPart) {
		this.afterSkippedPart = afterSkippedPart;
	}

	public boolean isFromPathSearch() {
		return fromPathSearch;
	}

	public void setFromPathSearch(boolean fromPathSearch) {
		this.fromPathSearch = fromPathSearch;
	}

	public boolean isuTurnSegment() {
		return uTurnSegment;
	}

	public void setuTurnSegment(boolean uTurnSegment) {
		this.uTurnSegment = uTurnSegment;
	}

	public double getWeight() {
		return weight;
	}

	public void setWeight(double weight) {
		this.weight = weight;
	}

	public double getMatchedFactor() {
		return matchedFactor;
	}

	public void setMatchedFactor(double matchedFactor) {
		this.matchedFactor = matchedFactor;
	}

	public String getGeometry() {
		return geometry;
	}

	public void setGeometry(String geometry) {
		this.geometry = geometry;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public short getMaxSpeedTow() {
		return maxSpeedTow;
	}

	public void setMaxSpeedTow(short maxSpeedTow) {
		this.maxSpeedTow = maxSpeedTow;
	}

	public short getMaxSpeedBkw() {
		return maxSpeedBkw;
	}

	public void setMaxSpeedBkw(short maxSpeedBkw) {
		this.maxSpeedBkw = maxSpeedBkw;
	}

	public short getCalcSpeedTow() {
		return calcSpeedTow;
	}

	public void setCalcSpeedTow(short calcSpeedTow) {
		this.calcSpeedTow = calcSpeedTow;
	}

	public short getCalcSpeedBkw() {
		return calcSpeedBkw;
	}

	public void setCalcSpeedBkw(short calcSpeedBkw) {
		this.calcSpeedBkw = calcSpeedBkw;
	}

	public short getLanesTow() {
		return lanesTow;
	}

	public void setLanesTow(short lanesTow) {
		this.lanesTow = lanesTow;
	}

	public short getLanesBkw() {
		return lanesBkw;
	}

	public void setLanesBkw(short lanesBkw) {
		this.lanesBkw = lanesBkw;
	}

	public short getFrc() {
		return frc;
	}

	public void setFrc(short frc) {
		this.frc = frc;
	}

	public String getFormOfWay() {
		return formOfWay;
	}

	public void setFormOfWay(String formOfWay) {
		this.formOfWay = formOfWay;
	}

	public String getStreetType() {
		return streetType;
	}

	public void setStreetType(String streetType) {
		this.streetType = streetType;
	}

	public long getWayId() {
		return wayId;
	}

	public void setWayId(long wayId) {
		this.wayId = wayId;
	}

	public int getStartNodeIndex() {
		return startNodeIndex;
	}

	public void setStartNodeIndex(int startNodeIndex) {
		this.startNodeIndex = startNodeIndex;
	}

	public long getStartNodeId() {
		return startNodeId;
	}

	public void setStartNodeId(long startNodeId) {
		this.startNodeId = startNodeId;
	}

	public int getEndNodeIndex() {
		return endNodeIndex;
	}

	public void setEndNodeIndex(int endNodeIndex) {
		this.endNodeIndex = endNodeIndex;
	}

	public long getEndNodeId() {
		return endNodeId;
	}

	public void setEndNodeId(long endNodeId) {
		this.endNodeId = endNodeId;
	}

	public Set<String> getAccessTow() {
		return accessTow;
	}

	public void setAccessTow(Set<String> accessTow) {
		this.accessTow = accessTow;
	}

	public Set<String> getAccessBkw() {
		return accessBkw;
	}

	public void setAccessBkw(Set<String> accessBkw) {
		this.accessBkw = accessBkw;
	}

	public boolean isTunnel() {
		return tunnel;
	}

	public void setTunnel(boolean tunnel) {
		this.tunnel = tunnel;
	}

	public boolean isBridge() {
		return bridge;
	}

	public void setBridge(boolean bridge) {
		this.bridge = bridge;
	}

	public boolean isUrban() {
		return urban;
	}

	public void setUrban(boolean urban) {
		this.urban = urban;
	}

	public Map<String, String> getTags() {
		return tags;
	}

	public void setTags(Map<String, String> tags) {
		this.tags = tags;
	}

	public float getLength() {
		return length;
	}

	public void setLength(float length) {
		this.length = length;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (segmentId ^ (segmentId >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		int startIndexMax = Math.max(this.getStartPointIndex(), ((MatchedWaySegmentDTO)obj).getStartPointIndex());
		int endIndexMin = Math.min(this.getEndPointIndex(), ((MatchedWaySegmentDTO)obj).getEndPointIndex());
		
		if (((MatchedWaySegmentDTO)obj).getSegmentId() == this.getSegmentId() && 
			startIndexMax <= endIndexMin) {	// fuzzy index compare => one segment could be match twice (UTurn); in this case indices are different
			return true;
		}
		return false;
	}

}
