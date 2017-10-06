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

import java.util.Iterator;

import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import at.srfg.graphium.model.IDefaultSegmentXInfo;
import at.srfg.graphium.model.impl.DefaultSegmentXInfo;
import at.srfg.graphium.neo4j.model.WayGraphConstants;
import at.srfg.graphium.neo4j.model.WayGraphRelationshipType;

/**
 * @author mwimmer
 *
 */
public class DefaultSegmentXInfoPropertyHandler extends AbstractSegmentPropertyHandler<IDefaultSegmentXInfo> {

	public DefaultSegmentXInfoPropertyHandler() {
		super(new DefaultSegmentXInfo());
	}

	@Override
	protected void updateNodeProperties(IDefaultSegmentXInfo xInfo, Node xInfoNode) {
		for (String key : xInfo.getValues().keySet()) {
			Object value = xInfo.getValues().get(key);

			// key:   {key}
			// value: {className}:{value}
			xInfoNode.setProperty(key, value.getClass().getName() + ":" + value);
		}
	}

	@Override
	public Node getXInfoNode(IDefaultSegmentXInfo xInfo, Node segmentNode) {
		Node xInfoNode = null;
		Iterator<Relationship> relIt = segmentNode.getRelationships(WayGraphRelationshipType.SEGMENT_XINFO).iterator();
		Label label = Label.label(getResponsibleType());
		while (relIt.hasNext() && xInfoNode == null) {
			Node endNode = relIt.next().getEndNode();
			if (endNode.hasLabel(label) &&
				endNode.getProperty(WayGraphConstants.SEGMENT_XINFO_DIRECTION_TOW) == xInfo.isDirectionTow()) {
				xInfoNode = endNode;
			}
		}
		return xInfoNode;
	}

}
