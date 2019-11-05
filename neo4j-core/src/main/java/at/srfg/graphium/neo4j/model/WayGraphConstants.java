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
package at.srfg.graphium.neo4j.model;

/**
 * @author mwimmer
 *
 */
public class WayGraphConstants {

	public static final String SEGMENT_LABEL = "waysegment";
	public static final String SUBSCRIPTION_LABEL = "subscription";
	public static final String SUBSCRIPTION_GROUP_LABEL = "subscription_group";
	public static final String VIEW_LABEL = "view";
	public static final String METADATA_LABEL = "metadata";
	public static final String WAYGRAPH_LABEL = "waygraph";
	public static final String SEGMENT_XINFO_LABEL = "segmentXInfo";
	
	public static final String SEGMENT_ID = "segment_id";
	public static final String SEGMENT_NAME = "name";
	public static final String SEGMENT_MAXSPEED_TOW = "maxspeed_tow";
	public static final String SEGMENT_MAXSPEED_BKW = "maxspeed_bkw";
	public static final String SEGMENT_SPEED_CALC_TOW = "speed_calc_tow";
	public static final String SEGMENT_SPEED_CALC_BKW = "speed_calc_bkw";
	public static final String SEGMENT_LANES_TOW = "lanes_tow";
	public static final String SEGMENT_LANES_BKW = "lanes_bkw";
	public static final String SEGMENT_FRC = "frc";
	public static final String SEGMENT_FOW = "fow"; // form of way
	public static final String SEGMENT_STREETTYPE = "streettype";
	public static final String SEGMENT_WAY_ID = "way_id"; // exists if not equal to segment_id
	public static final String SEGMENT_STARTNODE_ID = "startnode_id";
	public static final String SEGMENT_STARTNODE_INDEX = "startnode_index";
	public static final String SEGMENT_ENDNODE_ID = "endnode_id";
	public static final String SEGMENT_ENDNODE_INDEX = "endnode_index";
	public static final String SEGMENT_TUNNEL = "tunnel"; // exists if true
	public static final String SEGMENT_BRIDGE = "bridge"; // exists if true
	public static final String SEGMENT_URBAN = "urban"; // exists if true
	public static final String SEGMENT_TIMESTAMP = "timestamp"; // not necessary yet
	public static final String SEGMENT_ACCESS_TOW = "access_tow";
	public static final String SEGMENT_ACCESS_BKW = "access_bkw";
	public static final String SEGMENT_LENGTH = "length";
	public static final String SEGMENT_CURRENT_DURATION_TOW = "current_duration_tow";
	public static final String SEGMENT_CURRENT_DURATION_BKW = "current_duration_bkw";
	public static final String SEGMENT_MIN_DURATION_TOW = "min_duration_tow";
	public static final String SEGMENT_MIN_DURATION_BKW = "min_duration_bkw";
	public static final String SEGMENT_GEOM = "geom";
	public static final String SEGMENT_START_X = "start_x";
	public static final String SEGMENT_START_Y = "start_y";
	public static final String SEGMENT_END_X = "end_x";
	public static final String SEGMENT_END_Y = "end_y";
	public static final String SEGMENT_TAG_PREFIX = "segtag:";

	public static final String HDSEGMENT_LEFT_BOARDER_GEOM = "left_boarder_geom";
	public static final String HDSEGMENT_LEFT_BOARDER_STARTNODE_ID = "left_boarder_startnode_id";
	public static final String HDSEGMENT_LEFT_BOARDER_ENDNODE_ID = "left_boarder_endnode_id";
	public static final String HDSEGMENT_RIGHT_BOARDER_GEOM = "right_boarder_geom";
	public static final String HDSEGMENT_RIGHT_BOARDER_STARTNODE_ID = "right_boarder_startnode_id";
	public static final String HDSEGMENT_RIGHT_BOARDER_ENDNODE_ID = "right_boarder_endnode_id";

	public static final String SEGMENT_XINFO_DIRECTION_TOW = "segxinfo_direction_tow";
	
	public static final String CONNECTION_ACCESS = "access";
	public static final String CONNECTION_NODE_ID = "node_id";
	
	public static final String METADATA_GRAPHNAME = "meta_graphname";
	public static final String METADATA_GRAPHVERSIONNAME = "meta_graphversionname";
	public static final String METADATA_VERSION = "meta_version";
	public static final String METADATA_ORIGIN_GRAPHNAME = "meta_origin_graphname";
	public static final String METADATA_ORIGIN_VERSION = "meta_origin_version";
	public static final String METADATA_STATE = "meta_state";
	public static final String METADATA_VALID_FROM = "meta_valid_from";
	public static final String METADATA_VALID_TO = "meta_valid_to";
	public static final String METADATA_COVERED_AREA = "meta_covered_area";
	public static final String METADATA_SEGMENTS_COUNT = "meta_segments_count";
	public static final String METADATA_CONNECTIONS_COUNT = "meta_connections_count";
	public static final String METADATA_ACCESSTYPES = "meta_accesstypes";
	// TODO: Eine Map kann nicht als Property gespeichert werden. Daher sollten Tags als String oder String[] gespeichert werden. In einem String
	//       kann ein Key-Value-Pair abgebildet werden. Der Wert muss entsprechend geparst werden können.
	public static final String METADATA_TAGS = "meta_tags";
	public static final String METADATA_SOURCE = "meta_source";
	public static final String METADATA_TYPE = "meta_type";
	public static final String METADATA_DESCRIPTION = "meta_description";
	public static final String METADATA_CREATION_TIMESTAMP = "meta_creation_timestamp";
	public static final String METADATA_STORAGE_TIMESTAMP = "meta_storage_timestamp";
	public static final String METADATA_CREATOR = "meta_creator";
	public static final String METADATA_ORIGIN_URL = "meta_origin_url";

	public static final String VIEW_NAME = "view_name";
	public static final String VIEW_GRAPH_NAME = "view_graph_name";
	public static final String VIEW_FILTER = "view_filter";
	public static final String VIEW_COVERED_AREA = "view_covered_area";
	public static final String VIEW_SEGMENTS_COUNT = "view_segments_count";
	public static final String VIEW_CONNECTIONS_COUNT = "view_connections_counts";
	// TODO: Eine Map kann nicht als Property gespeichert werden. Daher sollten Tags als String oder String[] gespeichert werden. In einem String
	//       kann ein Key-Value-Pair abgebildet werden. Der Wert muss entsprechend geparst werden können.
	public static final String VIEW_TAGS = "view_tags";
	public static final String VIEW_CREATION_TIMESTAMP = "view_creation_timestamp";

	public static final String INDEX_METADATA_GRAPHNAME = "index_metadata_graphname";
	public static final String INDEX_VIEW_NAME = "index_view_name";
	
	public static final String SUBSCRIPTION_GROUP_NAME = "subscription_group_name";

	public static final String SUBSCRIPTION_SERVER_NAME = "subscription_server_name";
	public static final String SUBSCRIPTION_GRAPH_NAME = "subscription_graph_name";
	public static final String SUBSCRIPTION_VIEW_NAME = "subscription_view_name";
	public static final String SUBSCRIPTION_URL = "subscription_url";
	public static final String SUBSCRIPTION_TIMESTAMP = "subscription_timestamp";
	public static final String SUBSCRIPTION_USERNAME = "subscription_username";
	public static final String SUBSCRIPTION_PASSWORD = "subscription_password";

	public static final String WAYGRAPH_NAME = "waygraph_name";
	
}