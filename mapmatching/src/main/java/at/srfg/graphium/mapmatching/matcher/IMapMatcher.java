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
package at.srfg.graphium.mapmatching.matcher;

import at.srfg.graphium.core.exception.GraphNotExistsException;
import at.srfg.graphium.mapmatching.model.ITrack;

public interface IMapMatcher {

	public IMapMatcherTask getTask(ITrack origTrack) throws GraphNotExistsException;
	
	public IMapMatcherTask getTask(String graphName, ITrack origTrack) throws GraphNotExistsException;
	
	public IMapMatcherTask getTask(String graphName, String graphVersion, ITrack origTrack) throws GraphNotExistsException;
	
	public String getDefaultGraphName();
	public void setDefaultGraphName(String defaultGraphName);
	
	public int getMaxMatchingRadiusMeter();
	public void setMaxMatchingRadiusMeter(int maxMatchingRadiusMeter);

	public int getSrid();
	public void setSrid(int srid);

	public int getIntialRadiusMeter();
	public void setIntialRadiusMeter(int intialRadiusMeter);

	public int getMaxSegmentsForShortestPath();
	public void setMaxSegmentsForShortestPath(int maxSegmentsForShortestPath);

	public boolean isOnlyBestResult();
	public void setOnlyBestResult(boolean onlyBestResult);

	int getMaxNrOfBestPaths();
	void setMaxNrOfBestPaths(int maxNrOfBestPaths);

	public int getMinNrOfPoints();
	public void setMinNrOfPoints(int minNrOfPoints);

	public int getMinLength();
	public void setMinLength(int length);

	public int getMinSegmentsPerSection();
	public void setMinSegmentsPerSection(int minSegmentsPerSection);

	int getNrOfPointsForInitialMatch();
	void setNrOfPointsForInitialMatch(int nrOfPointsForInitialMatch);

	int getMaxCountLoopsWithoutPathExtension();
	void setMaxCountLoopsWithoutPathExtension(
			int maxCountLoopsWithoutPathExtension);

	int getEnvelopeSideLength();
	void setEnvelopeSideLength(int envelopeSideLength);

	int getNrOfHops();
	void setNrOfHops(int nrOfHops);

	int getRouteCacheSize();
	void setRouteCacheSize(int routeCacheSize);

	int getThresholdForLowSamplingsInSecs();
	void setThresholdForLowSamplingsInSecs(int thresholdForLowSamplingsInSecs);

}
