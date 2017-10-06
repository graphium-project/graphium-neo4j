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

import java.util.Date;

import com.vividsolutions.jts.geom.Point;

import at.srfg.graphium.mapmatching.model.ITrackPoint;

/**
 * @author mwimmer
 *
 */
public class TrackPointImpl implements ITrackPoint {

	private static final long serialVersionUID = -1212575981264261346L;

	private long id;
	private Date timestamp;
	private long trackId;
	private Point point;
	private int number;
	private Float distCalc;
	private Float vCalc;
	private Float aCalc;
	private Float hcr;
	
	@Override
	public long getId() {
		return id;
	}

	@Override
	public void setId(long id) {
		this.id = id;
	}

	@Override
	public Date getTimestamp() {
		return timestamp;
	}

	@Override
	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	@Override
	public long getTrackId() {
		return trackId;
	}

	@Override
	public void setTrackId(long trackId) {
		this.trackId = trackId;
	}

	@Override
	public Point getPoint() {
		return point;
	}

	@Override
	public void setPoint(Point point) {
		this.point = point;
	}

	@Override
	public Float getDistCalc() {
		return distCalc;
	}

	@Override
	public void setDistCalc(Float distCalc) {
		this.distCalc = distCalc;
	}

	@Override
	public Float getVCalc() {
		return vCalc;
	}

	@Override
	public void setVCalc(Float vCalc) {
		this.vCalc = vCalc;
	}

	@Override
	public Float getHcr() {
		return hcr;
	}

	@Override
	public void setHcr(Float hcr) {
		this.hcr = hcr;
	}

	@Override
	public Float getACalc() {
		return aCalc;
	}

	@Override
	public void setACalc(Float aCalc) {
		this.aCalc = aCalc;
	}

	@Override
	public Integer getNumber() {
		return number;
	}

	@Override
	public void setNumber(Integer number) {
		this.number = number;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((point == null) ? 0 : point.hashCode());
		result = prime * result + ((timestamp == null) ? 0 : timestamp.hashCode());
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
		TrackPointImpl other = (TrackPointImpl) obj;
		if (point == null) {
			if (other.point != null)
				return false;
		} else if (!point.equals(other.point))
			return false;
		if (timestamp == null) {
			if (other.timestamp != null)
				return false;
		} else if (!timestamp.equals(other.timestamp))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "TrackPointImpl [id=" + id + ", timestamp=" + timestamp + ", trackId=" + trackId + ", point=" + point
				+ ", number=" + number + ", distCalc=" + distCalc + ", vCalc=" + vCalc + ", aCalc=" + aCalc + ", hcr="
				+ hcr + "]";
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

}