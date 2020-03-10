/**
 * Graphium Neo4j - Module of Graphserver for Neo4j extension
 * Copyright © 2019 Salzburg Research Forschungsgesellschaft (graphium@salzburgresearch.at)
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
package at.srfg.graphium.neo4j.model.cache;

import com.vividsolutions.jts.geom.LineString;

/**
 * @author mwimmer
 *
 */
public class SegmentCacheEntry {

	private long id;
	private LineString geometry;
	private float length;
	private short maxSpeedTow;
	private short maxSpeedBkw;
	private short frc;
	
	public SegmentCacheEntry(long id, LineString geometry, float length, short maxSpeedTow, short maxSpeedBkw, short frc) {
		super();
		this.id = id;
		this.geometry = geometry;
		this.length = length;
		this.maxSpeedTow = maxSpeedTow;
		this.maxSpeedBkw = maxSpeedBkw;
		this.frc = frc;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public LineString getGeometry() {
		return geometry;
	}

	public void setGeometry(LineString geometry) {
		this.geometry = geometry;
	}

	public float getLength() {
		return length;
	}

	public void setLength(float length) {
		this.length = length;
	}

	public int getDuration(boolean directionTow) {
		if (directionTow) {
			return calcDuration(maxSpeedTow);
		} else {
			return calcDuration(maxSpeedBkw);
		}
	}
	
	private int calcDuration(short speed) {
		return (int) Math.round(length / (speed / 3.6));
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

	public short getFrc() {
		return frc;
	}

	public void setFrc(short frc) {
		this.frc = frc;
	}
	
}
