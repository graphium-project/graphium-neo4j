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
package at.srfg.graphium.mapmatching.neo4j;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.Resource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTWriter;

import at.srfg.graphium.core.exception.GraphNotExistsException;
import at.srfg.graphium.core.persistence.IWayGraphReadDao;
import at.srfg.graphium.model.IWaySegment;

/**
 *  (C) 2017 Salzburg Research Forschungsgesellschaft m.b.H.
 *  
 *  All rights reserved.
 *
 *  @author mwimmer
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:/application-context-graphium-neo4j_test.xml",
		"classpath:/application-context-graphium-core.xml"})
public class TestGenerateMapMatchedGeometriesCSV {

	private static Logger log = Logger.getLogger("TestGenerateMapMatchedGeometriesCSV");	
	
	@Resource(name="neo4jWayGraphReadDao")
	private IWayGraphReadDao<IWaySegment> neo4jGraphReadDao;

	private WKTWriter wktWriter = new WKTWriter();
	
	private String graphName = "osm_at";
	private String versionName = "161223";
	
	private String expectedRoutesDirectory = "/data/expectedroutes/";	

	long[] trackIds = new long[] {
		14079459,15254282,15596028,19162072,19185806,19215506,19233778,19264223,19275232,19283623,19293805,19428248,
		19457078,19612003,19961224,19990926,19991780,20337699,20338050,20338051,20348005,20376051,20702591,21378862,21378863,21380678,
		21958131,21977328,21992793,22061090,22883329,23053374,
		14079459005L,15254282005L,15596028005L,19162072005L,19185806005L,19215506005L,19233778005L,19264223005L,19275232005L,19283623005L,19293805005L,19428248005L,
		19457078005L,19612003005L,19961224005L,19990926005L,19991780005L,20337699005L,20338050005L,20338051005L,20348005005L,20376051005L,20702591005L,21378862005L,21378863005L,21380678005L,
		21958131005L,21977328005L,21992793005L,22061090005L,22883329005L,23053374005L,
		140794590010L,152542820010L,155960280010L,191620720010L,191858060010L,192155060010L,192337780010L,192642230010L,192752320010L,192836230010L,192938050010L,194282480010L,
		194570780010L,196120030010L,199612240010L,199909260010L,199917800010L,203376990010L,203380500010L,203380510010L,203480050010L,203760510010L,207025910010L,213788620010L,213788630010L,213806780010L,
		219581310010L,219773280010L,219927930010L,220610900010L,228833290010L,230533740010L,
		140794590020L,152542820020L,155960280020L,191620720020L,191858060020L,192155060020L,192337780020L,192642230020L,192752320020L,192836230020L,192938050020L,194282480020L,
		194570780020L,196120030020L,199612240020L,199909260020L,199917800020L,203376990020L,203380500020L,203380510020L,203480050020L,203760510020L,207025910020L,213788620020L,213788630020L,213806780020L,
		219581310020L,219773280020L,219927930020L,220610900020L,228833290020L,230533740020L
		};

	@Test
	public void generateExpectedRouteGeometryFromSegmentIds() {
//		long[] trackIds = new long[] {14079459};
		
		String fileName = "expectedRoutesWkt.csv";		
		File wktFile = new File(expectedRoutesDirectory + fileName);
		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(new FileWriter(wktFile));

			for (Long trackId : trackIds) {
				List<Long> segmentIds = new ArrayList<>();
				String[] metadata = readExpectedRouteSegments(trackId, segmentIds);
				String wkt = generateGeometry(segmentIds);
				writeWktToFile(bw, trackId, wkt, metadata);
			}
			bw.flush();
		} catch (IOException e) {
			log.warning(e.getMessage());
		} finally {
			if (bw != null) {
				try {
					bw.close();
				} catch (IOException e) {
					log.warning(e.getMessage());
				}			
			}
		}
	}

	private String generateGeometry(List<Long> segmentIds) {
		String wkt = null;
		Geometry geom = null;
		
		try {
			List<IWaySegment> segments = neo4jGraphReadDao.getSegmentsById(graphName, versionName, segmentIds, true);
			
			for (IWaySegment segment : segments) {
				if (geom == null) {
					geom = segment.getGeometry();
				} else {
					geom = geom.union(segment.getGeometry());
				}
			}
			
			wkt = wktWriter.write(geom);

		} catch (GraphNotExistsException e) {
			log.warning(e.getMessage());
		}
		
		 return wkt;
	}

	private String[] readExpectedRouteSegments(Long trackId, List<Long> ids) {
		String[] metadataTokens = new String[9];
		
		String graph_id = "1";
		String fileName = "route_" + graph_id + "_" + trackId + ".txt";
		File file = new File(expectedRoutesDirectory + fileName);

		BufferedReader br = null;
		try {
			
			if (!file.exists()) {
				log.warning("route " + fileName + " does not exist!");
				return null;
			}
			
			br = new BufferedReader(new FileReader(file));
			String line = null;
			
			int i=0;

			while ((line = br.readLine()) != null) {
				if (i < 9) {
					metadataTokens[i++] = line;
				} else {
					if (!line.equals("routepart")) {
						ids.add(parseMatchedSegmentId(line));
					}
				}
			}

		} catch (IOException e) {
			log.warning(e.toString());
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					log.warning(e.toString());
				}
			}
		}
		return metadataTokens;
	}

	private long parseMatchedSegmentId(String line) {
		
		try {
			String[] tokens = line.split(";");
			long segmentId = Long.parseLong(tokens[0]);
//			int startIndex = Integer.parseInt(tokens[1]);
//			int endIndex = Integer.parseInt(tokens[2]);
//			int direction = Integer.parseInt(tokens[3]);
//			double matchedFactor = Double.parseDouble(tokens[4]);			
//			double meanDistance = Double.parseDouble(tokens[5]);
//			boolean fromPathSearch = Boolean.parseBoolean(tokens[6]);
			
			return segmentId;
			
		} catch (Exception e) {
			log.warning(e.toString());
		}

		return 0;
	}

	private void writeWktToFile(BufferedWriter bw, long trackId, String wkt, String[] metadata) throws IOException {
		// CSV: trackId;wkt;nrTotalPoints;nrMatchPoints
		String[] totalPointsTokens = metadata[4].split("=");
		String[] matchedPointsTokens = metadata[6].split("=");
		String line = trackId + ";" + totalPointsTokens[1] + ";" + matchedPointsTokens[1] + ";" + wkt;
		bw.write(line);
		bw.newLine();
	}
}