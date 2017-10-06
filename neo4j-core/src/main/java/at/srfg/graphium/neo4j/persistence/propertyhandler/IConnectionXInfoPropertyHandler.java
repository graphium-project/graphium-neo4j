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
package at.srfg.graphium.neo4j.persistence.propertyhandler;

import org.neo4j.graphdb.Relationship;

import at.srfg.graphium.model.IConnectionXInfo;
import at.srfg.graphium.model.IXInfoModelTypeAware;

import java.util.List;

/**
 * @author mwimmer
 *
 */
public interface IConnectionXInfoPropertyHandler<X extends IConnectionXInfo> extends IXInfoModelTypeAware<X> {

	int deleteProperties(Relationship connectionRelationship, String groupKey);

	void setXInfoProperties(X xInfo,String groupKey, Relationship connectionRelationship);

	List<X> getXInfoProperties(Relationship connectionRelationship);

	X getXInfoProperty(Relationship connectionRelationship, String groupKey);

	Class getXInfoClass();
}
