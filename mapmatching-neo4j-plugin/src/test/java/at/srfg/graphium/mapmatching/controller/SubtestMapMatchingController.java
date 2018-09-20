/**
 * Graphium Neo4j - Neo4j Server Plugin providing graphium map matching api for graphs integrated in neo4j server
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vividsolutions.jts.io.ParseException;

import at.srfg.graphium.mapmatching.client.MapMatcherClient;
import at.srfg.graphium.mapmatching.dto.MatchedBranchDTO;
import at.srfg.graphium.mapmatching.dto.MatchedWaySegmentDTO;
import at.srfg.graphium.mapmatching.dto.TrackDTO;

/**
 * @author mwimmer
 *
 */
public class SubtestMapMatchingController {

	private static Logger log = LoggerFactory.getLogger(SubtestMapMatchingController.class);
	
	private MapMatcherClient mapMatchingClient;
	
	@Test
	public void testMapMatching() throws JsonGenerationException, JsonMappingException, IOException, ParseException {
		String graphName = "osm_at_lower_levels";
		String fileName = "C:/development/Graphium/working_data/mapmatcher/json/20652297.json";
		
		mapMatchingClient = new MapMatcherClient("http://localhost:7474/graphium/api/");
		mapMatchingClient.setConnectionRequestTimeout(60000);
		mapMatchingClient.setConnectTimeout(60000);
		mapMatchingClient.setSocketTimeout(60000);
		mapMatchingClient.setup();
		
		TrackDTO track = mapJsonToDto(fileName);
		
		MatchedBranchDTO branch = mapMatchingClient.matchTrack(track, graphName, true, true, 60000);
		
		printBranch(branch);
	}
	
	private TrackDTO mapJsonToDto(String fileName) {
		ObjectMapper mapper = new ObjectMapper();
		TrackDTO track = null;
		try {
			track = mapper.readValue(new File(fileName), TrackDTO.class);
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}
		return track;
	}
	
	private void printBranch(MatchedBranchDTO branch) {
		log.info("Branch with matched factor " + branch.getMatchedFactor());
		List<Long> segmentIds = new ArrayList<>(branch.getSegments().size());
		for (MatchedWaySegmentDTO segment : branch.getSegments()) {
			segmentIds.add(segment.getSegmentId());
		}
		log.info(StringUtils.join(segmentIds, ", "));
	}

}
