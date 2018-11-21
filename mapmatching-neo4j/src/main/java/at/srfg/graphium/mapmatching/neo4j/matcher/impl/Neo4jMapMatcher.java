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
	
	private MapMatchingProperties properties = new MapMatchingProperties();
		
	private INeo4jWayGraphReadDao graphDao;

	private IGraphVersionMetadataService metadataService;
	
	private IRoutingService<IWaySegment> routingService;

	private MapMatcherGlobalStatistics globalStatistics;
	
	private Neo4jUtil neo4jUtil;
	
	private String defaultGraphName = null;
	
	// Neo4j graph metadata
//	private IWayGraphVersionMetadata graphMetadata = null;
	
	private String csvLoggerName = null;

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
		MapMatchingProperties taskProperties = properties.clone();
		if (routingMode != null) {
			taskProperties.setRoutingMode(routingMode);
		}
		
		MapMatchingTask matchingTask = new MapMatchingTask(this, taskProperties, graphMetadata, neo4jUtil, origTrack, 
				csvLoggerName, globalStatistics);
		return matchingTask;
	}

	// === GETTERS AND SETTERS ===
	
	public MapMatchingProperties getProperties() {
		return properties;
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

	public void setProperties(MapMatchingProperties properties) {
		this.properties = properties;
	}

	public void setGraphDao(INeo4jWayGraphReadDao graphDao) {
		this.graphDao = graphDao;
	}

	public void setRoutingService(IRoutingService<IWaySegment> routingService) {
		this.routingService = routingService;
	}

	@Override
	public int getMaxMatchingRadiusMeter() {
		return properties.getMaxMatchingRadiusMeter();
	}

	@Override
	public void setMaxMatchingRadiusMeter(int maxMatchingRadiusMeter) {
		properties.setMaxMatchingRadiusMeter(maxMatchingRadiusMeter);
	}

	@Override
	public int getSrid() {
		return properties.getSrid();
	}

	@Override
	public void setSrid(int srid) {
		properties.setSrid(srid);
	}

	@Override
	public int getIntialRadiusMeter() {
		return properties.getIntialRadiusMeter();
	}

	@Override
	public void setIntialRadiusMeter(int intialRadiusMeter) {
		properties.setIntialRadiusMeter(intialRadiusMeter);
	}

	@Override
	public int getNrOfPointsForInitialMatch() {
		return properties.getNrOfPointsForInitialMatch();
	}

	@Override
	public void setNrOfPointsForInitialMatch(int nrOfPointsForInitialMatch) {
		properties.setNrOfPointsForInitialMatch(nrOfPointsForInitialMatch);
	}

	@Override
	public int getMaxSegmentsForShortestPath() {
		return properties.getMaxSegmentsForShortestPath();
	}

	@Override
	public void setMaxSegmentsForShortestPath(int maxSegmentsForShortestPath) {
		properties.setMaxSegmentsForShortestPath(maxSegmentsForShortestPath);
	}

	@Override
	public boolean isOnlyBestResult() {
		return properties.isOnlyBestResult();
	}

	@Override
	public void setOnlyBestResult(boolean onlyBestResult) {
		properties.setOnlyBestResult(onlyBestResult);
	}

	@Override
	public int getMaxNrOfBestPaths() {
		return properties.getMaxNrOfBestPaths();
	}

	@Override
	public void setMaxNrOfBestPaths(int minNrOfBestPaths) {
		properties.setMaxNrOfBestPaths(minNrOfBestPaths);
	}

	@Override
	public String getDefaultGraphName() {		
		return defaultGraphName;
	}

	@Override
	public void setDefaultGraphName(String defaultGraphName) {
		this.defaultGraphName = defaultGraphName;
	}

	@Override
	public int getMinNrOfPoints() {
		return properties.getMinNrOfPoints();
	}

	@Override
	public void setMinNrOfPoints(int minNrOfPoints) {
		this.properties.setMinNrOfPoints(minNrOfPoints);
	}
	
	@Override
	public int getMinLength() {
		return properties.getMinLength();
	}
	
	@Override
	public void setMinLength(int length) {
		properties.setMinLength(length);
	}
	
	@Override
	public int getMinSegmentsPerSection() {
		return properties.getMinSegmentsPerSection();
	}
	
	@Override
	public void setMinSegmentsPerSection(int minSegmentsPerSection) {
		properties.setMinSegmentsPerSection(minSegmentsPerSection);
	}
	
	@Override
	public int getMaxCountLoopsWithoutPathExtension() {
		return properties.getMaxCountLoopsWithoutPathExtension();
	}

	@Override
	public void setMaxCountLoopsWithoutPathExtension(
			int maxCountLoopsWithoutPathExtension) {
		properties.setMaxCountLoopsWithoutPathExtension(maxCountLoopsWithoutPathExtension);
	}
	
	@Override
	public int getEnvelopeSideLength() {
		return properties.getEnvelopeSideLength();
	}

	@Override
	public void setEnvelopeSideLength(int envelopeSideLength) {
		properties.setEnvelopeSideLength(envelopeSideLength);
	}

	@Override
	public int getNrOfHops() {
		return properties.getNrOfHops();
	}
	@Override
	public void setNrOfHops(int nrOfHops) {
		properties.setNrOfHops(nrOfHops);
	}

	@Override
	public int getThresholdForLowSamplingsInSecs() {
		return properties.getThresholdForLowSamplingsInSecs();
	}
	@Override
	public void setThresholdForLowSamplingsInSecs(int thresholdForLowSamplingsInSecs) {
		properties.setThresholdForLowSamplingsInSecs(thresholdForLowSamplingsInSecs);;
	}

	public String getCsvLoggerName() {
		return csvLoggerName;
	}

	public void setCsvLoggerName(String csvLoggerName) {
		this.csvLoggerName = csvLoggerName;
	}

	public MapMatcherGlobalStatistics getGlobalStatistics() {
		return globalStatistics;
	}

	public void setGlobalStatistics(MapMatcherGlobalStatistics globalStatistics) {
		this.globalStatistics = globalStatistics;
	}

	@Override
	public int getRouteCacheSize() {
		return properties.getRouteCacheSize();
	}

	@Override
	public void setRouteCacheSize(int routeCacheSize) {
		properties.setRouteCacheSize(routeCacheSize);
	}

	@Override
	public int getThresholdSamplingIntervalForTryingFurtherPathSearches() {
		return properties.getThresholdSamplingIntervalForTryingFurtherPathSearches();
	}

	@Override
	public void setThresholdSamplingIntervalForTryingFurtherPathSearches(
			int thresholdSamplingIntervalForTryingFurtherPathSearches) {
		properties.setThresholdSamplingIntervalForTryingFurtherPathSearches(thresholdSamplingIntervalForTryingFurtherPathSearches);
	}

	@Override
	public int getPointsDiffThresholdForSkipRouting() {
		return properties.getPointsDiffThresholdForSkipRouting();
	}

	@Override
	public void setPointsDiffThresholdForSkipRouting(int pointsDiffThresholdForSkipRouting) {
		properties.setPointsDiffThresholdForSkipRouting(pointsDiffThresholdForSkipRouting);
	}

	public String getRoutingMode() {
		return properties.getRoutingMode();
	}

	public void setRoutingMode(String routingMode) {
		properties.setRoutingMode(routingMode);
	}

	public String getRoutingCriteria() {
		return properties.getRoutingCriteria();
	}

	public void setRoutingCriteria(String routingCriteria) {
		properties.setRoutingCriteria(routingCriteria);;
	}

}