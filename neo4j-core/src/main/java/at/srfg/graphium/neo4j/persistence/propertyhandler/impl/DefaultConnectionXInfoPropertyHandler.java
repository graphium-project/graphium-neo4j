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

import org.neo4j.graphdb.Relationship;

import at.srfg.graphium.model.IDefaultConnectionXInfo;
import at.srfg.graphium.model.impl.DefaultConnectionXInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author mwimmer
 *
 */
public class DefaultConnectionXInfoPropertyHandler extends AbstractConnectionPropertyHandler<IDefaultConnectionXInfo> {

	public DefaultConnectionXInfoPropertyHandler() {
		super(new DefaultConnectionXInfo());
	}

	@Override
	public void setXInfoProperties(IDefaultConnectionXInfo xInfo, String groupKey, Relationship connectionRelationship) {
		xInfo.getValues().forEach((key,value) -> addAttribute(connectionRelationship,groupKey,key,value));
	}

	@Override
	public IDefaultConnectionXInfo getXInfoProperty(Relationship connectionRelationship, String groupKey) {
		Map<String,Object> groupedKeyValueMap = this.getMappingForGroupKey(connectionRelationship.getAllProperties(),groupKey);
		final IDefaultConnectionXInfo xinfo = new DefaultConnectionXInfo();
		xinfo.setGroupKey(groupKey);
		groupedKeyValueMap.forEach(xinfo::setValue);
		return xinfo;
	}

	@Override
	public List<IDefaultConnectionXInfo> getXInfoProperties(Relationship connectionRelationship) {
		Map<String,Map<String,Object>> typedGroupedMap = this.getMappingForType(connectionRelationship.getAllProperties());
		List<IDefaultConnectionXInfo> resultList = new ArrayList<>(typedGroupedMap.size());
		typedGroupedMap.forEach((groupKey,attributeMap) -> {
			final IDefaultConnectionXInfo xinfo = new DefaultConnectionXInfo();
			xinfo.setGroupKey(groupKey);
			attributeMap.forEach(xinfo::setValue);
			resultList.add(xinfo);
		});
		return resultList;
	}
}
