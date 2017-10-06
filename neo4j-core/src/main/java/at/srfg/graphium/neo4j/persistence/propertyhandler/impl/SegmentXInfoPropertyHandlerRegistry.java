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

import at.srfg.graphium.model.ISegmentXInfo;
import at.srfg.graphium.neo4j.persistence.propertyhandler.ISegmentXInfoPropertyHandler;

/**
 * @author mwimmer
 *
 */
public class SegmentXInfoPropertyHandlerRegistry<X extends ISegmentXInfo> {

	@Autowired(required=false)
	protected List<ISegmentXInfoPropertyHandler<X>> propertySetterList = null;
	
	protected Map<String, ISegmentXInfoPropertyHandler<X>> propertySetter = new HashMap<>();

	@PostConstruct
	public void setup() {
		if (propertySetterList != null) {
			for (ISegmentXInfoPropertyHandler<X> ps : propertySetterList) {
				propertySetter.put(ps.getResponsibleType(), ps);
			}
		}
	}
	
	public ISegmentXInfoPropertyHandler<? extends ISegmentXInfo> get(String type) {
		return propertySetter.get(type);
	}

}