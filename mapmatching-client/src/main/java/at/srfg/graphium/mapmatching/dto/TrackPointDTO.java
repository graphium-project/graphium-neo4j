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

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @author mwimmer
 *
 */
public class TrackPointDTO {

	private long id;
	private long timestamp;
	@JsonIgnore
	private long trackId;
	private double x;
	private double y;
	private double z;
	@JsonIgnore
	private Float distCalc;
	@JsonIgnore
	private Float vCalc;
	@JsonIgnore
	private Float aCalc;
	@JsonIgnore
	private Float hcr;
	
	public TrackPointDTO() {}
	
	public TrackPointDTO(long id, long timestamp, long trackId, double x, double y, double z, Float distCalc, Float vCalc,
			Float aCalc, Float hcr) {
		super();
		this.id = id;
		this.timestamp = timestamp;
		this.trackId = trackId;
		this.x = x;
		this.y = y;
		this.z = z;
		this.distCalc = distCalc;
		this.vCalc = vCalc;
		this.aCalc = aCalc;
		this.hcr = hcr;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public long getTrackId() {
		return trackId;
	}

	public void setTrackId(long trackId) {
		this.trackId = trackId;
	}

	public double getX() {
		return x;
	}

	public void setX(double x) {
		this.x = x;
	}

	public double getY() {
		return y;
	}

	public void setY(double y) {
		this.y = y;
	}

	public double getZ() {
		return z;
	}

	public void setZ(double z) {
		this.z = z;
	}

	public Float getDistCalc() {
		return distCalc;
	}

	public void setDistCalc(Float distCalc) {
		this.distCalc = distCalc;
	}

	public Float getVCalc() {
		return vCalc;
	}

	public void setVCalc(Float vCalc) {
		this.vCalc = vCalc;
	}

	public Float getACalc() {
		return aCalc;
	}

	public void setACalc(Float aCalc) {
		this.aCalc = aCalc;
	}

	public Float getHcr() {
		return hcr;
	}

	public void setHcr(Float hcr) {
		this.hcr = hcr;
	}

}
