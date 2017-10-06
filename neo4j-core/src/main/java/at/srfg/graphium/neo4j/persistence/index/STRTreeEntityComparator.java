/**
 * Graphium Neo4j - Module of Graphserver for Neo4j extension
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
/**
 * (C) 2016 Salzburg Research Forschungsgesellschaft m.b.H.
 *
 * All rights reserved.
 *
 */
package at.srfg.graphium.neo4j.persistence.index;

import java.util.Comparator;

import com.vividsolutions.jts.geom.Point;

import at.srfg.graphium.neo4j.model.index.STRTreeEntity;

/**
 * @author mwimmer
 */
public class STRTreeEntityComparator implements Comparator<STRTreeEntity> {

	private Point referencPoint = null;
	
	public STRTreeEntityComparator(Point referencPoint) {
		this.referencPoint = referencPoint;
	}
	
	@Override
	public int compare(STRTreeEntity e1, STRTreeEntity e2) {
		double dist1 = e1.distance(referencPoint);
		double dist2 = e2.distance(referencPoint);
		if (dist1 < dist2) {
			return -1;
		} else if (dist1 == dist2) {
			return 0;
		} else {
			return 1;
		}
	}

}
