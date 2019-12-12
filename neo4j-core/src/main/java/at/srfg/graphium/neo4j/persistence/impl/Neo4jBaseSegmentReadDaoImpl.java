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
package at.srfg.graphium.neo4j.persistence.impl;

import at.srfg.graphium.core.exception.GraphNotExistsException;
import at.srfg.graphium.core.persistence.IBaseSegmentReadDao;
import at.srfg.graphium.io.adapter.exception.XInfoNotSupportedException;
import at.srfg.graphium.io.exception.WaySegmentSerializationException;
import at.srfg.graphium.io.outputformat.ISegmentOutputFormat;
import at.srfg.graphium.model.IBaseSegment;
import at.srfg.graphium.model.IWaySegmentConnection;
import at.srfg.graphium.neo4j.persistence.nodemapper.INeo4jXInfoNodeMapper;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 *
 *
 * Created by shennebe on 19.10.2016.
 */
public class Neo4jBaseSegmentReadDaoImpl extends AbstractNeo4jDaoImpl implements IBaseSegmentReadDao {

    private INeo4jXInfoNodeMapper<IBaseSegment> baseSegmentMapper;
    private INeo4jXInfoNodeMapper<List<IWaySegmentConnection>> connectionsXInfoMapper;

    @Override
    public void streamBaseConnectionXInfos(String graph, String version, ISegmentOutputFormat<IBaseSegment> outputFormat, String... types) throws GraphNotExistsException, WaySegmentSerializationException, XInfoNotSupportedException {
        try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx()) {
            super.iterateSegmentNodes(this.getNodeIterator(graph,version),
                    outputFormat, baseSegmentMapper,connectionsXInfoMapper, false, graph, version, types);
            tx.success();
        }
    }

    @Override
    public void streamBaseSegmentXInfos(String graph, String version, ISegmentOutputFormat<IBaseSegment> outputFormat, String... types) throws GraphNotExistsException, WaySegmentSerializationException, XInfoNotSupportedException {
        try (Transaction tx = graphDatabaseProvider.getGraphDatabase().beginTx()) {
            super.iterateSegmentNodes(this.getNodeIterator(graph,version),
                    outputFormat, baseSegmentMapper, connectionsXInfoMapper, true, graph, version, types);
            tx.success();
        }
    }

    public INeo4jXInfoNodeMapper<IBaseSegment> getBaseSegmentMapper() {
        return baseSegmentMapper;
    }

    public void setBaseSegmentMapper(INeo4jXInfoNodeMapper<IBaseSegment> baseSegmentMapper) {
        this.baseSegmentMapper = baseSegmentMapper;
    }

    public INeo4jXInfoNodeMapper<List<IWaySegmentConnection>> getConnectionsXInfoMapper() {
        return connectionsXInfoMapper;
    }

    public void setConnectionsXInfoMapper(INeo4jXInfoNodeMapper<List<IWaySegmentConnection>> connectionsXInfoMapper) {
        this.connectionsXInfoMapper = connectionsXInfoMapper;
    }
}
