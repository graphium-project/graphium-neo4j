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
package at.srfg.graphium.neo4j.persistence.nodemapper;

import org.neo4j.graphdb.Node;

/**
 * Salzburg Research ForschungsgesmbH (c) 2019
 *
 * Project: graphium
 * Created by mwimmer
 */
public interface INeo4jXInfoConnectionMapper<T> extends INeo4jXInfoNodeMapper<T> {

	T mapWithXInfoTypes(Node node, boolean includeIncomings, boolean includeOutgoings, String... types);
	
}