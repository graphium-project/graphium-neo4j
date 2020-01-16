/**
 * Graphium Neo4j - Module of Graphserver for Map Matching using Neo4j
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
package at.srfg.graphium.mapmatching.neo4j.matcher.impl;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.srfg.graphium.core.exception.GraphNotExistsException;
import at.srfg.graphium.core.service.IGraphVersionMetadataService;
import at.srfg.graphium.mapmatching.matcher.IMapMatcher;
import at.srfg.graphium.mapmatching.matcher.IMapMatcherTask;
import at.srfg.graphium.mapmatching.model.ITrack;
import at.srfg.graphium.mapmatching.properties.impl.MapMatchingProperties;
import at.srfg.graphium.mapmatching.statistics.MapMatcherGlobalStatistics;
import at.srfg.graphium.model.IWayGraphVersionMetadata;
import at.srfg.graphium.model.IWaySegment;
import at.srfg.graphium.neo4j.persistence.INeo4jWayGraphReadDao;
import at.srfg.graphium.routing.service.IRoutingService;

public class Neo4jMapMatcher implements IMapMatcher {
	
	private static Logger log = LoggerFactory.getLogger(Neo4jMapMatcher.class);
	
	private MapMatchingProperties properties;
	private MapMatchingProperties propertiesHd;
	
	private INeo4jWayGraphReadDao graphDao;

	private IGraphVersionMetadataService metadataService;
	
	private IRoutingService<IWaySegment> routingService;

	private MapMatcherGlobalStatistics globalStatistics;
	
	private Neo4jUtil neo4jUtil;
	
	private String defaultGraphName = null;
	
	// Neo4j graph metadata
//	private IWayGraphVersionMetadata graphMetadata = null;

	@Override
	public IMapMatcherTask getTask(ITrack origTrack, String routingMode) throws GraphNotExistsException {
		IWayGraphVersionMetadata graphMetadata = metadataService.getCurrentWayGraphVersionMetadata(defaultGraphName);
		if (graphMetadata == null) {
			throw new GraphNotExistsException("Default Graph not found", defaultGraphName);
		}

		return createTask(graphMetadata, origTrack, routingMode);
	}
	
	@Override
	public IMapMatcherTask getTask(String graphName, ITrack origTrack, String routingMode) throws GraphNotExistsException {
		IWayGraphVersionMetadata graphMetadata = metadataService.getCurrentWayGraphVersionMetadata(graphName);
		if (graphMetadata == null) {
			throw new GraphNotExistsException("Graph " + graphName + " not found", graphName);
		}

		return createTask(graphMetadata, origTrack, routingMode);
	}
	
	@Override
	public IMapMatcherTask getTask(String graphName, String graphVersion, ITrack origTrack, String routingMode) throws GraphNotExistsException {
		IWayGraphVersionMetadata graphMetadata = metadataService.getWayGraphVersionMetadata(graphName, graphVersion);
		if (graphMetadata == null) {
			throw new GraphNotExistsException("Graph not found", graphName);
		}
		
		return createTask(graphMetadata, origTrack, routingMode);
	}
	
	protected IMapMatcherTask createTask(IWayGraphVersionMetadata graphMetadata, ITrack origTrack, String routingMode) {
		MapMatchingProperties taskProperties;
		if (graphMetadata.getType().equals("hdwaysegment")) { //TODO replace string
			taskProperties = propertiesHd.clone();
		} else {
			taskProperties = properties.clone();
		}
		if (routingMode != null) {
			taskProperties.setRoutingMode(routingMode);
		}
		
		MapMatchingTask matchingTask = new MapMatchingTask(this, taskProperties, graphMetadata, neo4jUtil, origTrack, 
				properties.getCsvLoggerName(), globalStatistics);
		return matchingTask;
	}

	// === GETTERS AND SETTERS ===
	
	@Override
	public MapMatchingProperties getProperties() {
		return properties;
	}
	
	@Override
	public MapMatchingProperties getPropertiesHd() {
		return propertiesHd;
	}
	
	public INeo4jWayGraphReadDao getGraphDao() {
		return graphDao;
	}

	public IRoutingService<IWaySegment> getRoutingService() {
		return routingService;
	}
	
	public Neo4jUtil getNeo4jUtil() {
		return neo4jUtil;
	}

	public void setNeo4jUtil(Neo4jUtil neo4jUtil) {
		this.neo4jUtil = neo4jUtil;
	}

	public IGraphVersionMetadataService getMetadataService() {
		return metadataService;
	}

	public void setMetadataService(IGraphVersionMetadataService metadataService) {
		this.metadataService = metadataService;
	}

	@Override
	public void setProperties(MapMatchingProperties properties) {
		this.properties = properties;
	}

	@Override
	public void setPropertiesHd(MapMatchingProperties propertiesHd) {
		this.propertiesHd = propertiesHd;
	}

	public void setGraphDao(INeo4jWayGraphReadDao graphDao) {
		this.graphDao = graphDao;
	}

	public void setRoutingService(IRoutingService<IWaySegment> routingService) {
		this.routingService = routingService;
	}

	@Override
	public String getDefaultGraphName() {		
		return defaultGraphName;
	}

	@Override
	public void setDefaultGraphName(String defaultGraphName) {
		this.defaultGraphName = defaultGraphName;
	}

	public MapMatcherGlobalStatistics getGlobalStatistics() {
		return globalStatistics;
	}

	public void setGlobalStatistics(MapMatcherGlobalStatistics globalStatistics) {
		this.globalStatistics = globalStatistics;
	}
	
}
