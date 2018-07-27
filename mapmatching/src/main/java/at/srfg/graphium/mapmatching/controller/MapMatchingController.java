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
package at.srfg.graphium.mapmatching.controller;

import java.util.List;
import java.util.concurrent.CancellationException;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import at.srfg.graphium.core.exception.GraphNotExistsException;
import at.srfg.graphium.core.persistence.IWayGraphVersionMetadataDao;
import at.srfg.graphium.io.adapter.IAdapter;
import at.srfg.graphium.mapmatching.dto.MatchedBranchDTO;
import at.srfg.graphium.mapmatching.dto.TrackDTO;
import at.srfg.graphium.mapmatching.matcher.IMapMatchingService;
import at.srfg.graphium.mapmatching.model.IMatchedBranch;
import at.srfg.graphium.mapmatching.model.ITrack;
import at.srfg.graphium.model.IWayGraphVersionMetadata;
import at.srfg.graphium.model.State;

/**
 * @author mwimmer
 *
 */
@Controller
public class MapMatchingController {
	
	private static Logger log = LoggerFactory.getLogger(MapMatchingController.class);

	private IMapMatchingService mapMatchingService;
	private IAdapter<ITrack, TrackDTO> trackAdapter;
	private IAdapter<MatchedBranchDTO, IMatchedBranch> branchAdapter;
	private IAdapter<MatchedBranchDTO, IMatchedBranch> verboseBranchAdapter;
	private IWayGraphVersionMetadataDao metadataDao;
	
	@RequestMapping(value="/graphs/{graph}/matchtrack", method=RequestMethod.POST)
    public @ResponseBody MatchedBranchDTO matchTrack(
    		@PathVariable(value = "graph") String graphName,
    		@RequestParam(name = "startSegmentId", required = false) Long startSegmentId,
    		@RequestParam(name = "timeoutMs", required = false, defaultValue = "10000") int timeout, // timeout in milliseconds
    		@RequestParam(name = "outputVerbose", required = false, defaultValue = "false") boolean outputVerbose, // if true return additionally waysegments 
    		@RequestParam(name = "routingMode", required = false, defaultValue = "car") String routingMode, // optional routingMode
    		@RequestBody TrackDTO trackDto) throws GraphNotExistsException, CancellationException {

		ITrack track = trackAdapter.adapt(trackDto);
		
		String graphVersion = null;
		List<IWayGraphVersionMetadata> metadataList = metadataDao.getWayGraphVersionMetadataList(graphName, State.ACTIVE, 
														track.getMetadata().getStartDate(), track.getMetadata().getStartDate(), null);
		if (metadataList == null || metadataList.isEmpty()) {
			String msg = "No valid graph version for graph " + graphName + " found for track " + track.getId() + " and timestamp " + track.getMetadata().getStartDate();
			log.error(msg);
			throw new GraphNotExistsException(msg, graphName);
		} else {
			if (metadataList.size() > 1) {
				log.warn("More than one graph versions found for track " + track.getId() + " and timestamp " + track.getMetadata().getStartDate());
			}
			graphVersion = metadataList.get(0).getVersion();
		}
		
		return match(graphName, graphVersion, track, startSegmentId, timeout, outputVerbose, routingMode);
	}

	@RequestMapping(value="/graphs/{graph}/versions/current/matchtrack", method=RequestMethod.POST)
    public @ResponseBody MatchedBranchDTO matchTrackOnCurrentVersion(
    		@PathVariable(value = "graph") String graphName,
    		@RequestParam(name = "startSegmentId", required = false) Long startSegmentId,
    		@RequestParam(name = "timeoutMs", required = false, defaultValue = "10000") int timeout, // timeout in milliseconds
    		@RequestParam(name = "outputVerbose", required = false, defaultValue = "false") boolean outputVerbose, // if true return additionally waysegments 
    		@RequestParam(name = "routingMode", required = false, defaultValue = "car") String routingMode, // optional routingMode
    		@RequestBody TrackDTO trackDto) throws GraphNotExistsException, CancellationException {

		ITrack track = trackAdapter.adapt(trackDto);
		return match(graphName, null, track, startSegmentId, timeout, outputVerbose, routingMode);
		
	}

	private MatchedBranchDTO match(String graphName, String graphVersion, ITrack track, Long startSegmentId, int timeout, boolean outputVerbose, String routingMode) 
		throws GraphNotExistsException {
		
		List<IMatchedBranch> branches = mapMatchingService.matchTrack(graphName, graphVersion, track, startSegmentId, null, timeout, true, routingMode);
		
		if (branches != null && !branches.isEmpty()) {
			// return only first/best branch
			if (outputVerbose) {
				return verboseBranchAdapter.adapt(branches.get(0));
			} else {
				return branchAdapter.adapt(branches.get(0));
			}
		} else {
			return null;
		}
	}
	
	// TODO: Is exception handling correct?
	
	@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR, reason = "timeout exceeded")
	@ExceptionHandler(CancellationException.class)
	public void handleCancellationException(
			CancellationException ex, HttpServletRequest request) {
	}

	@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR, reason = "graph does not exist")
	@ExceptionHandler(GraphNotExistsException.class)
	public void handleGraphNotExistsException(
			GraphNotExistsException ex, HttpServletRequest request) {
	}

	public IAdapter<ITrack, TrackDTO> getTrackAdapter() {
		return trackAdapter;
	}

	public IMapMatchingService getMapMatchingService() {
		return mapMatchingService;
	}

	public void setMapMatchingService(IMapMatchingService mapMatchingService) {
		this.mapMatchingService = mapMatchingService;
	}

	public void setTrackAdapter(IAdapter<ITrack, TrackDTO> trackAdapter) {
		this.trackAdapter = trackAdapter;
	}

	public IAdapter<MatchedBranchDTO, IMatchedBranch> getBranchAdapter() {
		return branchAdapter;
	}

	public void setBranchAdapter(IAdapter<MatchedBranchDTO, IMatchedBranch> branchAdapter) {
		this.branchAdapter = branchAdapter;
	}

	public IAdapter<MatchedBranchDTO, IMatchedBranch> getVerboseBranchAdapter() {
		return verboseBranchAdapter;
	}

	public void setVerboseBranchAdapter(IAdapter<MatchedBranchDTO, IMatchedBranch> verboseBranchAdapter) {
		this.verboseBranchAdapter = verboseBranchAdapter;
	}

	public IWayGraphVersionMetadataDao getMetadataDao() {
		return metadataDao;
	}

	public void setMetadataDao(IWayGraphVersionMetadataDao metadataDao) {
		this.metadataDao = metadataDao;
	}
	
}
