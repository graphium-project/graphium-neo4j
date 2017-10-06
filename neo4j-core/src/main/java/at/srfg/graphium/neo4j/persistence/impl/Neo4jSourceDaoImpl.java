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
package at.srfg.graphium.neo4j.persistence.impl;

import java.util.List;

import at.srfg.graphium.core.persistence.ISourceDao;
import at.srfg.graphium.model.ISource;
import at.srfg.graphium.model.management.impl.Source;

/**
 * @author mwimmer
 */
public class Neo4jSourceDaoImpl implements ISourceDao {

	@Override
	public List<ISource> getSources() {
		// wird derzeit nicht verwendet
		return null;
	}

	@Override
	public ISource getSource(int id) {
		// wird derzeit nicht verwendet
		return null;
	}

	@Override
	public ISource getSource(String name) {
		return new Source(0, name);
	}

	@Override
	public void save(ISource source) {
		// sources werden derzeit nicht als eigene Entitäten gespeichert
	}

}
