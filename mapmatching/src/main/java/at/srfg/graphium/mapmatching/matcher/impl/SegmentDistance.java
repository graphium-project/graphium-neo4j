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
package at.srfg.graphium.mapmatching.matcher.impl;

public class SegmentDistance<T> implements Comparable<SegmentDistance<T>> {

	private T segment;
	private double distance;
	
	public SegmentDistance(T segment, double distance) {
		super();
		this.segment = segment;
		this.distance = distance;
	}
	
	public T getSegment() {
		return segment;
	}
	public void setSegment(T segment) {
		this.segment = segment;
	}
	
	public double getDistance() {
		return distance;
	}
	public void setDistance(double distance) {
		this.distance = distance;
	}
	
	@Override
	public int compareTo(SegmentDistance<T> o) {
		if (distance < o.getDistance()) {
			return -1;
		} else if (distance == o.getDistance()) {
			return 0;
		} else {
			return 1;
		}
	}

	@Override
	public String toString() {
		return "SegmentDistance [segment=" + segment + ", distance=" + distance + "]";
	}
	
}
