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
package at.srfg.graphium.neo4j.persistence.propertyhandler.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;

import at.srfg.graphium.model.IConnectionXInfo;
import at.srfg.graphium.neo4j.persistence.propertyhandler.IConnectionXInfoPropertyHandler;

/**
 * @author mwimmer
 *
 */
public class ConnectionXInfoPropertyHandlerRegistry {

	@Autowired(required=false)
	protected List<IConnectionXInfoPropertyHandler<? extends IConnectionXInfo>> propertySetterList = null;
	
	protected Map<String, IConnectionXInfoPropertyHandler<? extends IConnectionXInfo>> propertySetter = new HashMap<>();

	@PostConstruct
	public void setup() {
		if (propertySetterList != null) {
			for (IConnectionXInfoPropertyHandler<? extends IConnectionXInfo> ps : propertySetterList) {
				propertySetter.put(ps.getResponsibleType(), ps);
			}
		}
	}
	
	public IConnectionXInfoPropertyHandler<? extends IConnectionXInfo> get(String type) {
		return propertySetter.get(type);
	}

}