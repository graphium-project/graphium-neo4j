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
package at.srfg.graphium.mapmatching.model;

import java.io.Serializable;
import java.util.Date;

import com.vividsolutions.jts.geom.Point;

/**
 * @author mwimmer
 *
 */
public interface ITrackPoint extends Serializable, Cloneable {

	public long getId();
	public void setId(long id);
	public Date getTimestamp();
	public void setTimestamp(Date timestamp);
	public long getTrackId();
	public void setTrackId(long trackId);
	public Point getPoint();
	public void setPoint(Point point);
	public Float getDistCalc();
	public void setDistCalc(Float distCalc);
	public Float getVCalc();
	public void setVCalc(Float vCalc);
	public Float getHcr();
    public void setHcr(Float hcr);
    public Float getACalc();
	public void setACalc(Float aCalc);
	public Integer getNumber();
	public void setNumber(Integer number);

	public Object clone() throws CloneNotSupportedException;
}
