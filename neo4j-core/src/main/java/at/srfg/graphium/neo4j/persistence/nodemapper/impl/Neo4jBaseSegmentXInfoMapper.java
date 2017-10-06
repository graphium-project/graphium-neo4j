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
package at.srfg.graphium.neo4j.persistence.nodemapper.impl;

import at.srfg.graphium.model.IBaseSegment;
import at.srfg.graphium.model.impl.BaseSegment;
import at.srfg.graphium.neo4j.model.WayGraphConstants;
import at.srfg.graphium.neo4j.persistence.nodemapper.INeo4jXInfoNodeMapper;
import at.srfg.graphium.neo4j.persistence.propertyhandler.ISegmentXInfoPropertyHandler;
import at.srfg.graphium.neo4j.persistence.propertyhandler.impl.SegmentXInfoPropertyHandlerRegistry;
import org.neo4j.graphdb.Node;

import java.util.Map;

/**
 * Created by shennebe on 19.10.2016.
 */
public class Neo4jBaseSegmentXInfoMapper implements INeo4jXInfoNodeMapper<IBaseSegment> {

    private SegmentXInfoPropertyHandlerRegistry propertyHandlerRegistry;

    @Override
    public IBaseSegment map(Node node) {
        return this.mapWithXInfoTypes(node);
    }

    @Override
    public IBaseSegment mapWithXInfoTypes(Node node, String... types) {
        IBaseSegment segment = new BaseSegment();
        Map<String, Object> properties = node.getAllProperties();
        segment.setId((long) properties.get(WayGraphConstants.SEGMENT_ID));
        this.setSegmentXInfos(segment,node,types);
        return segment;
    }

    protected void setSegmentXInfos(IBaseSegment segment, Node node, String... types) {
        if (types != null && types.length > 0) {
            for (String type : types) {
                ISegmentXInfoPropertyHandler propertyHandler = this.propertyHandlerRegistry.get(type);
                //TODO implement reading of properties
            }
        }
    }

    public SegmentXInfoPropertyHandlerRegistry getPropertyHandlerRegistry() {
        return propertyHandlerRegistry;
    }

    public void setPropertyHandlerRegistry(SegmentXInfoPropertyHandlerRegistry propertyHandlerRegistry) {
        this.propertyHandlerRegistry = propertyHandlerRegistry;
    }
}
