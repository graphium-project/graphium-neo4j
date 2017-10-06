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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import at.srfg.graphium.io.exception.WaySegmentSerializationException;
import at.srfg.graphium.io.outputformat.ISegmentOutputFormat;
import at.srfg.graphium.model.IBaseSegment;
import at.srfg.graphium.model.IWaySegmentConnection;
import at.srfg.graphium.neo4j.persistence.nodemapper.INeo4jXInfoNodeMapper;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.IndexManager;

import at.srfg.graphium.core.helper.GraphVersionHelper;
import at.srfg.graphium.model.State;
import at.srfg.graphium.neo4j.model.WayGraphConstants;
import at.srfg.graphium.neo4j.persistence.configuration.IGraphDatabaseProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author mwimmer
 *
 */
public abstract class AbstractNeo4jDaoImpl {

	protected IGraphDatabaseProvider graphDatabaseProvider;

	private static Logger log = LoggerFactory.getLogger(AbstractNeo4jDaoImpl.class);

	protected Index<Node> getSegmentIdIndex(String graphVersionName) {
		String indexName = getSegmentIdIndexName(graphVersionName);
		return getIndex(indexName);
	}
	
	protected Index<Node> getIndex(String indexName) {
		IndexManager indexManager = graphDatabaseProvider.getGraphDatabase().index();
		return indexManager.forNodes(indexName);
	}
	
	protected Node getSegmentNode(String graphVersionName, long segmentId) {
		IndexHits<Node> hits = getSegmentIdIndex(graphVersionName).get(WayGraphConstants.SEGMENT_ID, segmentId);
		Node node = null;
		if (hits.hasNext()) {
			node = hits.next();
		}
		hits.close();
		return node;
	}

	protected Node getCurrentWayGraphVersionMetadataNode(String graphName, Set<State> states) {
		Set<String> stateNames = new HashSet<String>();
		for (State state : states) {
			stateNames.add(state.name());
		}
		
		ResourceIterator<Node> nodeIterator = getGraphDatabase().findNodes(Label.label(WayGraphConstants.METADATA_LABEL),
				WayGraphConstants.METADATA_GRAPHNAME, graphName);
		Node metadataNode = null;
		boolean found = false;
		while (nodeIterator.hasNext() && !found) {
			metadataNode = nodeIterator.next();
			if (stateNames.contains(metadataNode.getProperty(WayGraphConstants.METADATA_STATE)) &&
				!metadataNode.hasProperty(WayGraphConstants.METADATA_VALID_TO)) {
				found = true;
			}
		}
		nodeIterator.close();
		if (found) {
			return metadataNode;
		} else {
			return null;
		}
	}
	
	protected String getSegmentIdIndexName(String graphVersionName) {
		return "index_" + graphVersionName + "_segment_id";
	}

	protected String getMetadataIndexName() {
		return "index_metadata";
	}
	
	protected String getViewIndexName() {
		return "index_view";
	}

	protected ResourceIterator<Node> getNodeIterator(String graphName, String version) {
		// Derzeit wird das View-System nur mit Default-Views unterstützt. Zusätzliche Views mit Filterdefinitionen werden nicht berücksichtigt.
		// Der Name der Default-View entspricht dem Graphnamen. Daher kann mit dem View-Namen direkt auf den entsprechenden Graphversions-Layer
		// abgefragt werden.

		String graphVersionName = createGraphVersionName(graphName, version);
		return getGraphDatabase().findNodes(Label.label(Neo4jWaySegmentHelperImpl.createSegmentNodeLabel(graphVersionName)));
	}


	protected String createGraphVersionName(String graphName, String version) {
		if (version == null) {
			Set<State> states = new HashSet<>();
			states.add(State.ACTIVE);
			Node metadataNode = getCurrentWayGraphVersionMetadataNode(graphName, states);
			version = (String) metadataNode.getProperty(WayGraphConstants.METADATA_VERSION);
		}
		return GraphVersionHelper.createGraphVersionName(graphName, version);
	}

	protected <T extends IBaseSegment> void iterateSegmentNodes(ResourceIterator<Node> segmentNodes,
																final Collection<T> segments,
																final INeo4jXInfoNodeMapper<T> segmentMapper,
																final INeo4jXInfoNodeMapper<List<IWaySegmentConnection>> connectionMapper) {
		this.iterateSegmentNodes(segmentNodes,segments,segmentMapper,connectionMapper,false);
	}

	protected <T extends IBaseSegment> void iterateSegmentNodes(ResourceIterator<Node> segmentNodes,
																final ISegmentOutputFormat<T> outputFormat,
																final INeo4jXInfoNodeMapper<T> segmentMapper,
																final INeo4jXInfoNodeMapper<List<IWaySegmentConnection>> connectionMapper) {
		this.iterateSegmentNodes(segmentNodes,outputFormat,segmentMapper,connectionMapper,false);
	}

	protected <T extends IBaseSegment> void iterateSegmentNodes(ResourceIterator<Node> segmentNodes,
																final Collection<T> segments,
																final INeo4jXInfoNodeMapper<T> segmentMapper,
																final INeo4jXInfoNodeMapper<List<IWaySegmentConnection>> connectionMapper,
																final boolean isSegmentXInfo,
																final String... xInfoTypes) {
		segmentNodes.stream().forEach(segmentNode -> {

			T segment = this.getSegmentXInfos(segmentNode,segmentMapper,connectionMapper,isSegmentXInfo,xInfoTypes);
			segments.add(segment);
			if (segments.size()%10000 == 0) {
				log.info(segments.size() + " segments loaded");
			}
		});
		log.info(segments.size() + " segments loaded");
	}

	private <T extends IBaseSegment> T getSegmentXInfos(Node segmentNode,
														INeo4jXInfoNodeMapper<T> segmentMapper,
														INeo4jXInfoNodeMapper<List<IWaySegmentConnection>> connectionMapper,
														boolean isSegmentXInfo,
														String... xInfoTypes) {
		T segment = null;
		if (isSegmentXInfo) {
			segment = segmentMapper.mapWithXInfoTypes(segmentNode, xInfoTypes);
			if (xInfoTypes == null || xInfoTypes.length == 0) {
				//In case of no xinfos serialize all, otherwise skip the cons
				segment.setCons(connectionMapper.map(segmentNode));
			}
		} else {
			segment = segmentMapper.map(segmentNode);
			segment.setCons(connectionMapper.mapWithXInfoTypes(segmentNode, xInfoTypes));
		}
		return segment;
	}


	protected <T extends IBaseSegment> void iterateSegmentNodes(ResourceIterator<Node> segmentNodes,
																final ISegmentOutputFormat<T> outputFormat,
																final INeo4jXInfoNodeMapper<T> segmentMapper,
																final INeo4jXInfoNodeMapper<List<IWaySegmentConnection>> connectionMapper,
																final boolean isSegmentXInfo,
																final String... xInfoTypes) {
		Consumer<Node> nodeConsumer = new Consumer<Node>() {

			private int idx = 0;

			@Override
			public void accept(Node segmentNode) {
				T segment = getSegmentXInfos(segmentNode,segmentMapper,connectionMapper,isSegmentXInfo,xInfoTypes);
				try {
					outputFormat.serialize(segment);
				} catch (WaySegmentSerializationException e) {
					log.error("Serialization Failed for segment " + segment.getId());
				}
				if (this.idx % 10000 == 0) {
					log.info(this.idx + " segments loaded");
				}
				this.idx++;
			}

			@Override
			public String toString() {
				return String.valueOf(idx);
			}
		};

		segmentNodes.forEachRemaining(nodeConsumer);

		log.info(nodeConsumer.toString() + " segments loaded");
	}

	protected GraphDatabaseService getGraphDatabase() {
		return graphDatabaseProvider.getGraphDatabase();
	}

	public IGraphDatabaseProvider getGraphDatabaseProvider() {
		return graphDatabaseProvider;
	}

	public void setGraphDatabaseProvider(IGraphDatabaseProvider graphDatabaseProvider) {
		this.graphDatabaseProvider = graphDatabaseProvider;
	}
	
}
