/**
 * Graphium Neo4j - Module of Graphserver for Map Matching specifying client functionality
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
package at.srfg.graphium.mapmatching.inputformat;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

import at.srfg.graphium.mapmatching.dto.MatchedBranchDTO;
import at.srfg.graphium.mapmatching.dto.MatchedWaySegmentDTO;

/**
 * @author mwimmer
 *
 */
public class MatchedBranchInputFormat {

	protected Logger log = LoggerFactory.getLogger(this.getClass());
	protected JsonFactory f = new MappingJsonFactory();
	
	public MatchedBranchDTO deserialize(InputStream stream) throws IOException, ParseException {
		return parse(stream);
    }
	
	protected MatchedBranchDTO parse(InputStream stream) throws IOException, ParseException
	{	
		JsonParser jp = f.createParser(stream);
		WKTReader reader = new WKTReader();
		
		MatchedBranchDTO branch = new MatchedBranchDTO();
		JsonToken token = jp.nextToken();
		
		while (!jp.isClosed()){
			token = jp.nextToken();
			if (token != null) {
				if (jp.getCurrentToken() == JsonToken.FIELD_NAME &&
					jp.getCurrentName().equals("segments")) {
					parseSegments(jp, reader, branch);
				} else {
					parseBranch(jp, reader, branch);
				}
			}
		}

		return branch;
	}
	
	/**
	 * @param jp
	 * @param reader
	 * @return
	 * @throws IOException 
	 * @throws JsonParseException 
	 */
	protected void parseBranch(JsonParser jp, WKTReader reader, MatchedBranchDTO branch) throws JsonParseException, IOException {
		while (jp.getCurrentToken() != JsonToken.END_OBJECT) {
			if (jp.getCurrentToken() == JsonToken.FIELD_NAME) {
				String name = jp.getCurrentName();
				printFieldNameAndMove2Value(jp);

				if (name.equals("finished")) {
					if (jp.getCurrentToken() != JsonToken.VALUE_NULL) {
						branch.setFinished(jp.getBooleanValue());
					}
				} else if (name.equals("nrOfUTurns")) {
					if (jp.getCurrentToken() != JsonToken.VALUE_NULL) {
						branch.setNrOfUTurns(jp.getIntValue());
					}
				} else if (name.equals("nrOfShortestPathSearches")) {
					if (jp.getCurrentToken() != JsonToken.VALUE_NULL) {
						branch.setNrOfShortestPathSearches(jp.getIntValue());
					}
				} else if (name.equals("length")) {
					if (jp.getCurrentToken() != JsonToken.VALUE_NULL) {
						branch.setLength(jp.getDoubleValue());
					}
				} else if (name.equals("matchedFactor")) {
					if (jp.getCurrentToken() != JsonToken.VALUE_NULL) {
						branch.setMatchedFactor(jp.getDoubleValue());
					}
				} else if (name.equals("matchedPoints")) {
					if (jp.getCurrentToken() != JsonToken.VALUE_NULL) {
						branch.setMatchedPoints(jp.getIntValue());
					}
				} else if (name.equals("certainPathEndSegmentId")) {
					if (jp.getCurrentToken() != JsonToken.VALUE_NULL) {
						branch.setCertainPathEndSegmentId(jp.getLongValue());
					}
				} else {
					if (jp.getCurrentToken() == JsonToken.START_OBJECT) {
						eatObject(jp);
					} else if (jp.getCurrentToken() == JsonToken.START_ARRAY) {
						eatArray(jp);
					}
				}
			}
			jp.nextToken();
		}
	}

	/**
	 * @param jp
	 * @param reader
	 * @param branch
	 * @throws IOException 
	 * @throws JsonParseException 
	 * @throws ParseException 
	 */
	protected void parseSegments(JsonParser jp, WKTReader reader, MatchedBranchDTO branch) throws JsonParseException, IOException, ParseException {
		List<MatchedWaySegmentDTO> segments = new ArrayList<MatchedWaySegmentDTO>();
		JsonToken token = jp.nextToken();
		MatchedWaySegmentDTO segment;
		
		while (!jp.isClosed() && (token = jp.nextToken()) != JsonToken.END_ARRAY){
			if (token != null) {
				segment = new MatchedWaySegmentDTO();
				parseSegment(jp, segment, reader);
				segments.add(segment);
			}
		}
		
		branch.setSegments(segments);
	}

	protected void parseSegment(JsonParser jp, MatchedWaySegmentDTO segment, WKTReader reader) throws IOException, ParseException {
		while (jp.nextToken() != JsonToken.END_OBJECT) {
			
			if (jp.getCurrentToken() == JsonToken.FIELD_NAME) {
				String name = jp.getCurrentName();
				printFieldNameAndMove2Value(jp);
				
				if (name.equals("accessTow")) {
					segment.setAccessTow(parseAccessTypes(jp));
				} else if (name.equals("accessBkw")) {
					segment.setAccessBkw(parseAccessTypes(jp));
				} else if (name.equals("tags")) {
					segment.setTags(parseTagMapNew(jp));
				} else {
					if (jp.getCurrentToken() != JsonToken.VALUE_NULL) {
						assignValueToSegment(segment, jp, name);
					}
				}
			}
		}
	}	

	/**
	 * @param jp
	 * @throws IOException 
	 * @throws JsonParseException 
	 */
	protected void eatArray(JsonParser jp) throws IOException {
		JsonToken jt;
		while ((jt = jp.nextToken()) != JsonToken.END_ARRAY) {;
			if (jt == JsonToken.FIELD_NAME) {
				eatField(jp);
			} else if (jt == JsonToken.START_OBJECT) {
				eatObject(jp);
			} else if (jt == JsonToken.START_ARRAY) {
				eatArray(jp);
			}
		}
	}

	/**
	 * Does move tokens until object end for current object start is here
	 * 
	 * @param jp
	 * @throws IOException 
	 * @throws JsonParseException 
	 */
	protected void eatObject(JsonParser jp) throws IOException {
		JsonToken jt;
		while ((jt = jp.nextToken()) != JsonToken.END_OBJECT) {;
			if (jt == JsonToken.FIELD_NAME) {
				eatField(jp);
			} else if (jt == JsonToken.START_OBJECT) {
				eatObject(jp);
			} else if (jt == JsonToken.START_ARRAY) {
				eatArray(jp);
			}
		}
		return; // state is END_OBJECT
	}

	/**
	 * @param jp
	 * @throws IOException 
	 * @throws JsonParseException 
	 */
	protected void eatField(JsonParser jp) throws IOException {
		JsonToken jt = jp.nextToken();
		if (jt == JsonToken.START_OBJECT) {
			eatObject(jp);
		} else if (jt == JsonToken.START_ARRAY) {
			eatArray(jp);
		} 
	}

	protected void assignValueToSegment(MatchedWaySegmentDTO segment, JsonParser jp, String name) throws IOException {
		if (name.equals("segmentId")) {
			segment.setSegmentId(jp.getLongValue());
		} else if (name.equals("startPointIndex")) {
			segment.setStartPointIndex(jp.getIntValue());
		} else if (name.equals("endPointIndex")) {
			segment.setEndPointIndex(jp.getIntValue());
		} else if (name.equals("enteringThroughStartNode")) {
			segment.setEnteringThroughStartNode(jp.getBooleanValue());
		} else if (name.equals("leavingThroughStartNode")) {
			segment.setLeavingThroughStartNode(jp.getBooleanValue());
		} else if (name.equals("startSegment")) {
			segment.setStartSegment(jp.getBooleanValue());
		} else if (name.equals("fromPathSearch")) {
			segment.setFromPathSearch(jp.getBooleanValue());
		} else if (name.equals("uTurnSegment")) {
			segment.setuTurnSegment(jp.getBooleanValue());
		} else if (name.equals("weight")) {
			segment.setWeight(jp.getDoubleValue());
		} else if (name.equals("matchedFactor")) {
			segment.setMatchedFactor(jp.getDoubleValue());
		} else if (name.equals("geometry")) {
			segment.setGeometry(jp.getText());
		} else if (name.equals("endNodeId")) {
			segment.setEndNodeId(jp.getLongValue());
		} else if (name.equals("endNodeIndex")) {
			segment.setEndNodeIndex(jp.getIntValue());
		} else if (name.equals("startNodeId")) {
			segment.setStartNodeId(jp.getLongValue());
		} else if (name.equals("startNodeIndex")) {
			segment.setStartNodeIndex(jp.getIntValue());
		} else if (name.equals("wayId")) {
			segment.setWayId(jp.getLongValue());
		} else if (name.equals("formOfWay")) {
			segment.setFormOfWay(jp.getText());
		} else if (name.equals("frc")) {
			segment.setFrc(jp.getShortValue());
		} else if (name.equals("lanesTow")) {
			segment.setLanesTow(jp.getShortValue());
		} else if (name.equals("lanesBkw")) {
			segment.setLanesBkw(jp.getShortValue());
		} else if (name.equals("maxSpeedBkw")) {
			segment.setMaxSpeedBkw(jp.getShortValue());
		} else if (name.equals("maxSpeedTow")) {
			segment.setMaxSpeedTow(jp.getShortValue());
		} else if (name.equals("calcSpeedBkw")) {
			segment.setCalcSpeedBkw(jp.getShortValue());
		} else if (name.equals("calcSpeedTow")) {
			segment.setCalcSpeedTow(jp.getShortValue());
		} else if (name.equals("streetType")) {
			segment.setStreetType(jp.getText());
		} else if (name.equals("name")) {
			segment.setName(jp.getText());
		} else if (name.equals("length")) {
			segment.setLength(jp.getFloatValue());
		} else if (name.equals("tunnel")) {
			segment.setTunnel(jp.getBooleanValue());
		} else if (name.equals("bridge")) {
			segment.setBridge(jp.getBooleanValue());
		} else if (name.equals("urban")) {
			segment.setUrban(jp.getBooleanValue());
		}	
	}

	protected Set<String> parseAccessTypes(JsonParser jp) throws JsonParseException, IOException {
		// expecting an array
		if (jp.getCurrentToken() == JsonToken.VALUE_NULL) {
			return null;
		} else {
			Set<String> accessTypes = new HashSet();
			while (jp.nextToken() != JsonToken.END_ARRAY) {
				accessTypes.add(jp.getText());
			}
			return accessTypes;
		}
	}
	
	protected Map<String,String> parseTagMapNew(JsonParser jp) throws IOException {
		jp.nextToken();
		return jp.readValueAs(Map.class);
	}
	
	protected void printFieldNameAndMove2Value(JsonParser jp) throws IOException {
		log.debug("fieldname: " + jp.getCurrentName());
		jp.nextToken();
		log.debug( " value " + jp.getText());
	}

}
