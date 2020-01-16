/**
 * Graphium Neo4j - Map Matching module of Graphium
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
package at.srfg.graphium.mapmatching.properties.impl;

import at.srfg.graphium.mapmatching.properties.IMapMatchingProperties;


public class MapMatchingProperties implements Cloneable, IMapMatchingProperties {

	
	// radius in meter for initial search of segments in the near of starting trackpoints; this value has to be greater than 
	// minMatchingRadiusMeter because normally there more than one potentially possible start segments.
	private int initialRadiusMeter = 150;

	// minimal radius in meter a match between track or point and segment is valid 
	private int maxMatchingRadiusMeter = 30;
	
	private boolean lowSamplingInterval = false;
	
	// maximum number of segments valid for a shortest path search (e.g. in case of loss of gps track points)
	private int maxSegmentsForShortestPath = 15;

	// maximum number of best resulting paths with which will be processed preferable
	private int maxNrOfBestPaths = 5;
		
	// number of points to skip during path initialization or alternative path search, when no path can be found for a point
	private int nrOfPointsToSkip = 3;

	// flag which defines if only the best result or all results will be returned
	private boolean onlyBestResult = true;
	
	private int srid = 4326;
	
	// timespan (in seconds) between trackpoints which defines if a given sampling interval is low
	private int thresholdForLowSamplingsInSecs = 10;
	
//	// temporary variables which can be manipulated if the sampling interval is low
//	private boolean tempActivateSmoother = true;
//	
	private int tempMaxSegmentsForShortestPath;
	
	private int nrOfPointsForInitialMatch = 3;
	
	// envelope side length used to determine next valid track point for searching start segment (used in initial phase + alternative routing)
	private int envelopeSideLength = 300;

	// number of hops within a single matching iteration; means how much connected segments should be matched in an matching iteration
	private int nrOfHops = 3;
	
	private int routeCacheSize = 100;
	
	// track's mean sampling interval in seconds 
	private int meanSamplingInterval = 0;
	
	// for shortest path searches a track point will be identified creating a routed path to; in some cases this track point has a big GPS error so routing
	// will not be successful; then the algorithm will try the next n points as routing targets;
	// statistically this methodology results in worse paths for higher sampling intervals
	private int thresholdSamplingIntervalForTryingFurtherPathSearches = 90;
	
	private String csvLoggerName = null;
	
	/**
	 * In case of routing we won't route for parts of track which possible left the underlying graph. Usually such parts of track consist of a number of valid
	 * GPS points. In that case we want to skip routing and start a new path, which means we create a gap within the routing paths. On the other hand routing 
	 * makes sense in case of GPS errors. For such parts of a track we have to consider only a few GPS points, possible partly invalid because GPS error.
	 * To differ those cases we need a threshold: the maximum number of points we want consider for routing (difference between last matched point and first
	 * point which determines a target segment for routing). If the number of points considered for routing exceed this threshold a new path will be created.
	 * In case of low sampled tracks this value will be automatically divided in half.
	 */
	private int pointsDiffThresholdForSkipRouting = 10;
	
	/**
	 * Minimum number of matching segments per section. If a section has less than
	 * {@code minSegmentsPerSection} matching segments, this section is remove from
	 * the path.
	 */
	private int minSegmentsPerSection = 5;
	
	/**
	 * Minimum number of points. The track is not matched if it
	 * has less than {@code minNrOfPoints} track points.
	 */
	private int minNrOfPoints = 20;
	
	/**
	 * Minimum track length (in meter). The track is not matched if
	 * the length is less than {@code minLength} meters.
	 */
	private int minLength = 400;
	
	private int maxCountLoopsWithoutPathExtension = 15;
	
	/**
	 * Switches between online and offline map matching. Online means iterative map matching.
	 */
	private boolean online = false;

	/**
	 * routing options
	 */
	private String routingMode;
	private String routingCriteria;
	private String routingAlgorithm;
	
	
	boolean activateExtendedPathMatching;
	/**
	 * Maximum distance of path between two track points without routing (in meter)
	 */
	int maxDistanceForExtendedPathMatching = 700;
	
	@Override
	public String toString() {
		return "MapMatchingProperties [initialRadiusMeter="
				+ initialRadiusMeter + ", maxMatchingRadiusMeter="
				+ maxMatchingRadiusMeter + ", lowSamplingInterval="
				+ lowSamplingInterval + ", meanSamplingInterval="
				+ meanSamplingInterval + ", maxSegmentsForShortestPath="
				+ maxSegmentsForShortestPath + ", maxNrOfBestPaths="
				+ maxNrOfBestPaths + ", nrOfPointsToSkip=" + nrOfPointsToSkip
				+ ", onlyBestResult=" + onlyBestResult + ", srid=" + srid
				+ ", thresholdForLowSamplingsInSecs="
				+ thresholdForLowSamplingsInSecs + ", tempMaxSegmentsForShortestPath="
				+ tempMaxSegmentsForShortestPath + ", minSegmentsPerSection="
				+ minSegmentsPerSection + ", minNrOfPoints=" + minNrOfPoints
				+ ", minLength=" + minLength + ", envelopeSideLength=" + envelopeSideLength 
				+ ", thresholdSamplingIntervalForTryingFurtherPathSearches=" + thresholdSamplingIntervalForTryingFurtherPathSearches
				+ ", pointsDiffThresholdForSkipRouting=" + pointsDiffThresholdForSkipRouting
				+ ", online=" + online 
				+ ", routingMode=" + routingMode
				+ ", routingCriteria=" + routingCriteria
				+ ", routingAlgorithm=" + routingAlgorithm
				+ ", activateExtendedPathMatching=" + activateExtendedPathMatching
				+ ", maxDistanceForExtendedPathMatching=" + maxDistanceForExtendedPathMatching
				+ "]";
	}
	
	@Override
	public MapMatchingProperties clone() {

		MapMatchingProperties properties = null;
        try {
        	properties = (MapMatchingProperties) super.clone();
        } catch (CloneNotSupportedException e) {}
        
        return properties;
	}

	@Override
	public int getIntialRadiusMeter() {
		return initialRadiusMeter;
	}
	
	@Override
	public int getMaxSegmentsForShortestPath() {
		return maxSegmentsForShortestPath;
	}
	
	@Override
	public int getMinLength() {
		return minLength;
	}

	@Override
	public int getMaxMatchingRadiusMeter() {
		return maxMatchingRadiusMeter;
	}

	@Override
	public int getMaxNrOfBestPaths() {
		return maxNrOfBestPaths;
	}

	@Override
	public int getMinNrOfPoints() {
		return minNrOfPoints;
	}

	@Override
	public int getMinSegmentsPerSection() {
		return minSegmentsPerSection;
	}

	@Override
	public int getNrOfPointsToSkip() {
		return nrOfPointsToSkip;
	}

	@Override
	public int getSrid() {
		return srid;
	}

	@Override
	public int getTempMaxSegmentsForShortestPath() {
		return tempMaxSegmentsForShortestPath;
	}

	@Override
	public int getThresholdForLowSamplingsInSecs() {
		return thresholdForLowSamplingsInSecs;
	}

	@Override
	public boolean isLowSamplingInterval() {
		return lowSamplingInterval;
	}
	
	@Override
	public boolean isOnlyBestResult() {
		return onlyBestResult;
	}

	@Override
	public void setIntialRadiusMeter(int intialRadiusMeter) {
		this.initialRadiusMeter = intialRadiusMeter;
	}

	
	@Override
	public void setLowSamplingInterval(boolean lowSamplingInterval) {
		this.lowSamplingInterval = lowSamplingInterval;
	}

	
	@Override
	public void setMaxSegmentsForShortestPath(int maxSegmentsForShortestPath) {
		this.maxSegmentsForShortestPath = maxSegmentsForShortestPath;
	}

	
	@Override
	public void setMinLength(int minLength) {
		this.minLength = minLength;
	}

	
	@Override
	public void setMaxMatchingRadiusMeter(int maxMatchingRadiusMeter) {
		this.maxMatchingRadiusMeter = maxMatchingRadiusMeter;
	}

	
	@Override
	public void setMaxNrOfBestPaths(int maxNrOfBestPaths) {
		this.maxNrOfBestPaths = maxNrOfBestPaths;
	}

	
	@Override
	public void setMinNrOfPoints(int minNrOfPoints) {
		this.minNrOfPoints = minNrOfPoints;
	}

	
	@Override
	public void setMinSegmentsPerSection(int minSegmentsPerSection) {
		this.minSegmentsPerSection = minSegmentsPerSection;
	}

	@Override
	public void setNrOfPointsToSkip(int nrOfPointsToSkip) {
		this.nrOfPointsToSkip = nrOfPointsToSkip;
	}
	
	@Override
	public void setOnlyBestResult(boolean onlyBestResult) {
		this.onlyBestResult = onlyBestResult;
	}
	
	@Override
	public void setSrid(int srid) {
		this.srid = srid;
	}

	@Override
	public void setTempMaxSegmentsForShortestPath(int tempMaxSegmentsForShortestPath) {
		this.tempMaxSegmentsForShortestPath = tempMaxSegmentsForShortestPath;
	}

	@Override
	public void setThresholdForLowSamplingsInSecs(int thresholdForLowSamplingsInSecs) {
		this.thresholdForLowSamplingsInSecs = thresholdForLowSamplingsInSecs;
	}

	@Override
	public int getNrOfPointsForInitialMatch() {
		return nrOfPointsForInitialMatch;
	}

	@Override
	public void setNrOfPointsForInitialMatch(int nrOfPointsForInitialMatch) {
		this.nrOfPointsForInitialMatch = nrOfPointsForInitialMatch;
	}

	@Override
	public int getMaxCountLoopsWithoutPathExtension() {
		return maxCountLoopsWithoutPathExtension;
	}

	@Override
	public void setMaxCountLoopsWithoutPathExtension(
			int maxCountLoopsWithoutPathExtension) {
		this.maxCountLoopsWithoutPathExtension = maxCountLoopsWithoutPathExtension;
	}

	@Override
	public int getEnvelopeSideLength() {
		return envelopeSideLength;
	}

	@Override
	public void setEnvelopeSideLength(int envelopeSideLength) {
		this.envelopeSideLength = envelopeSideLength;
	}

	@Override
	public int getNrOfHops() {
		return nrOfHops;
	}

	@Override
	public void setNrOfHops(int nrOfHops) {
		this.nrOfHops = nrOfHops;
	}

	@Override
	public boolean isOnline() {
		return online;
	}

	@Override
	public void setOnline(boolean online) {
		this.online = online;
	}

	@Override
	public int getRouteCacheSize() {
		return routeCacheSize;
	}

	@Override
	public void setRouteCacheSize(int routeCacheSize) {
		this.routeCacheSize = routeCacheSize;
	}

	@Override
	public int getMeanSamplingInterval() {
		return meanSamplingInterval;
	}

	@Override
	public void setMeanSamplingInterval(int meanSamplingInterval) {
		this.meanSamplingInterval = meanSamplingInterval;
	}

	@Override
	public int getThresholdSamplingIntervalForTryingFurtherPathSearches() {
		return thresholdSamplingIntervalForTryingFurtherPathSearches;
	}

	@Override
	public void setThresholdSamplingIntervalForTryingFurtherPathSearches(
			int thresholdSamplingIntervalForTryingFurtherPathSearches) {
		this.thresholdSamplingIntervalForTryingFurtherPathSearches = thresholdSamplingIntervalForTryingFurtherPathSearches;
	}

	@Override
	public int getPointsDiffThresholdForSkipRouting() {
		return pointsDiffThresholdForSkipRouting;
	}

	@Override
	public void setPointsDiffThresholdForSkipRouting(int pointsDiffThresholdForSkipRouting) {
		this.pointsDiffThresholdForSkipRouting = pointsDiffThresholdForSkipRouting;
	}

	@Override
	public String getRoutingMode() {
		return routingMode;
	}

	@Override
	public void setRoutingMode(String routingMode) {
		this.routingMode = routingMode;
	}

	@Override
	public String getRoutingCriteria() {
		return routingCriteria;
	}

	@Override
	public void setRoutingCriteria(String routingCriteria) {
		this.routingCriteria = routingCriteria;
	}

	@Override
	public String getRoutingAlgorithm() {
		return routingAlgorithm;
	}

	@Override
	public void setRoutingAlgorithm(String routingAlgorithm) {
		this.routingAlgorithm = routingAlgorithm;
	}

	@Override
	public boolean isActivateExtendedPathMatching() {
		return activateExtendedPathMatching;
	}

	@Override
	public void setActivateExtendedPathMatching(boolean activateExtendedPathMatching) {
		this.activateExtendedPathMatching = activateExtendedPathMatching;
	}

	@Override
	public int getMaxDistanceForExtendedPathMatching() {
		return maxDistanceForExtendedPathMatching;
	}

	@Override
	public void setMaxDistanceForExtendedPathMatching(int maxDistanceForExtendedPathMatching) {
		this.maxDistanceForExtendedPathMatching = maxDistanceForExtendedPathMatching;
	}

	@Override
	public String getCsvLoggerName() {
		return csvLoggerName;
	}

	@Override
	public void setCsvLoggerName(String csvLoggerName) {
		this.csvLoggerName = csvLoggerName;
	}
	
}
