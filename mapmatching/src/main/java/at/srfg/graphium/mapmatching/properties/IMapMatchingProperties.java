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
package at.srfg.graphium.mapmatching.properties;

public interface IMapMatchingProperties {

	int getIntialRadiusMeter();
	void setIntialRadiusMeter(int intialRadiusMeter);

	int getMaxMatchingRadiusMeter();
	void setMaxMatchingRadiusMeter(int maxMatchingRadiusMeter);

	int getMaxSegmentsForShortestPath();
	void setMaxSegmentsForShortestPath(int maxSegmentsForShortestPath);

	int getTempMaxSegmentsForShortestPath();
	void setTempMaxSegmentsForShortestPath(int tempMaxSegmentsForShortestPath);

	int getMinLength();
	void setMinLength(int minLength);

	int getMaxNrOfBestPaths();
	void setMaxNrOfBestPaths(int maxNrOfBestPaths);

	int getMinNrOfPoints();
	void setMinNrOfPoints(int minNrOfPoints);

	int getMinSegmentsPerSection();
	void setMinSegmentsPerSection(int minSegmentsPerSection);

	int getNrOfPointsToSkip();
	void setNrOfPointsToSkip(int nrOfPointsToSkip);

	int getSrid();
	void setSrid(int srid);

	int getThresholdForLowSamplingsInSecs();
	void setThresholdForLowSamplingsInSecs(int thresholdForLowSamplingsInSecs);

	int getMeanSamplingInterval();
	void setMeanSamplingInterval(int meanSamplingInterval);
	
	boolean isLowSamplingInterval();
	void setLowSamplingInterval(boolean lowSamplingInterval);

	boolean isOnlyBestResult();
	void setOnlyBestResult(boolean onlyBestResult);

	/**
	 * @return
	 */
	int getNrOfPointsForInitialMatch();
	/**
	 * @param nrOfPointsForInitialMatch
	 */
	void setNrOfPointsForInitialMatch(int nrOfPointsForInitialMatch);
	/**
	 * @return
	 */
	int getMaxCountLoopsWithoutPathExtension();
	/**
	 * @param maxCountLoopsWithoutPathExtension
	 */
	void setMaxCountLoopsWithoutPathExtension(
			int maxCountLoopsWithoutPathExtension);

	int getEnvelopeSideLength();
	void setEnvelopeSideLength(int envelopeSideLength);
	
	int getNrOfHops();
	void setNrOfHops(int nrOfHops);
	
	boolean isOnline();
	void setOnline(boolean online);
	
	int getRouteCacheSize();
	void setRouteCacheSize(int routeCacheSize);
	
	public int getThresholdSamplingIntervalForTryingFurtherPathSearches();
	public void setThresholdSamplingIntervalForTryingFurtherPathSearches(int thresholdSamplingIntervalForTryingFurtherPathSearches);
	
	int getPointsDiffThresholdForSkipRouting();
	void setPointsDiffThresholdForSkipRouting(int pointsDiffThresholdForSkipRouting);
	
	String getRoutingMode();
	void setRoutingMode(String routingMode);
	
	String getRoutingCriteria();
	void setRoutingCriteria(String routingCriteria);
	
}