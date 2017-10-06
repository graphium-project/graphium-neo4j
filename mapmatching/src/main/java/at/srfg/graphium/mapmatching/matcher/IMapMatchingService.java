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
/**
 * (C) 2016 Salzburg Research Forschungsgesellschaft m.b.H.
 *
 * All rights reserved.
 *
 */
package at.srfg.graphium.mapmatching.matcher;

import java.util.List;

import at.srfg.graphium.core.exception.GraphNotExistsException;
import at.srfg.graphium.mapmatching.model.IMatchedBranch;
import at.srfg.graphium.mapmatching.model.ITrack;

/**
 * @author mwimmer
 */
public interface IMapMatchingService {

	/**
	 * Map Matching of a track
	 * @param graphName Graph's name; if not set map matching will be executed on the default graph (see Neo4jMapMatcher)
	 * @param graphVersion Graph's version; has to be version with state ACTIVE; if not set map matching will be executed on the current version 
	 * 					   (last version with state ACTIVE)
	 * @param track Track the map matching has to be executed for
	 * @param startSegmentId ID of the starting segment in the graph; optional; is used in case of iterative map matching
	 * @param previousBranches Branches of previous map matching iteration; optional; is used in case of iterative map matching
	 * @param timeoutInMs Timeout of map matching task in milliseconds; if map matching is not finished after timeout the map matcher's method cancel() 
	 * 					  will be called (the map matching will not return immediately in case of I/O operations (e.g. routing))
	 * @param onlyBestResult If set to true only the best matching branch will be returned; otherwise the best n matching branches 
	 * 						 (see IMapMatchingProperties#maxNrOfBestPaths)
	 * @return List<IMatchedBranch> representing the best matching branch(es)
	 * @throws GraphNotExistsException
	 */
	List<IMatchedBranch> matchTrack(String graphName, String graphVersion, ITrack track, Long startSegmentId, List<IMatchedBranch> previousBranches, 
			int timeoutInSecs, boolean onlyBestResult) throws GraphNotExistsException;
	
}
