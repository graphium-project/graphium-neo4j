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

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;

import at.srfg.graphium.model.ISegmentXInfo;
import at.srfg.graphium.neo4j.model.WayGraphConstants;
import at.srfg.graphium.neo4j.model.WayGraphRelationshipType;
import at.srfg.graphium.neo4j.persistence.propertyhandler.ISegmentXInfoPropertyHandler;

/**
 * @author mwimmer
 *
 */
public abstract class AbstractSegmentPropertyHandler<X extends ISegmentXInfo> implements ISegmentXInfoPropertyHandler<X> {

    private String xInfoType;

    public AbstractSegmentPropertyHandler(X object) {
        xInfoType = object.getXInfoType();
    }

    @Override
    public String getResponsibleType() {
        return this.xInfoType;
    }

	@Override
	public Node setXInfoProperties(GraphDatabaseService graphDb, X xInfo, Node segmentNode) {
		Node xInfoNode = getXInfoNode(xInfo, segmentNode);
		
		if (xInfoNode == null) {
			xInfoNode = graphDb.createNode(Label.label(WayGraphConstants.SEGMENT_XINFO_LABEL), 
			  							   Label.label(getResponsibleType()));
			segmentNode.createRelationshipTo(xInfoNode, WayGraphRelationshipType.SEGMENT_XINFO);
			xInfoNode.setProperty(WayGraphConstants.SEGMENT_ID, xInfo.getSegmentId());
			if (xInfo.isDirectionTow() != null) {
				xInfoNode.setProperty(WayGraphConstants.SEGMENT_XINFO_DIRECTION_TOW, xInfo.isDirectionTow());
			}
		}
		updateNodeProperties(xInfo, xInfoNode);
		return xInfoNode;
	}

	@Override
	public X getXInfoProperties(GraphDatabaseService graphDb, Node segmentNode) {

		//TODO implement me!!
		return null;
	}

	protected abstract void updateNodeProperties(X xInfo, Node xInfoNode);
	
}