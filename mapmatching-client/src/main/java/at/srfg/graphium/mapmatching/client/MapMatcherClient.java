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
package at.srfg.graphium.mapmatching.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.annotation.PostConstruct;

import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.config.RequestConfig.Builder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vividsolutions.jts.io.ParseException;

import at.srfg.graphium.mapmatching.dto.MatchedBranchDTO;
import at.srfg.graphium.mapmatching.dto.TrackDTO;
import at.srfg.graphium.mapmatching.inputformat.MatchedBranchInputFormat;

/**
 * @author mwimmer
 *
 */
public class MapMatcherClient {
	
	private final static Logger log = LoggerFactory.getLogger(MapMatcherClient.class); 
	private final String mapMatchingApiPath = "graphs/{graphName}/matchtrack";
	private final String mapMatchingCurrentVersionApiPath = "graphs/{graphName}/versions/current/matchtrack";
	
	private String serverRoorUrl = null;
	private ObjectMapper mapper = null;
	private MatchedBranchInputFormat inputFormat = null;
	private CloseableHttpClient httpClient;
	private int connectionRequestTimeout = 5000;
	private int connectTimeout = 5000;
	private int socketTimeout = 5000;
	private int maxConnections = 25;
	
	public MapMatcherClient(String serverRoorUrl) {
		if (serverRoorUrl == null) {
			throw new RuntimeException("URL to map matching service must not be null");
		}
		this.serverRoorUrl = serverRoorUrl + (serverRoorUrl.endsWith("/") ? "" : "/");
		mapper = new ObjectMapper();
		inputFormat = new MatchedBranchInputFormat();
	}
	
	@PostConstruct
	public void setup() {
		this.httpClient = createDefaultHttpClient();		
	}
	
	protected CloseableHttpClient createDefaultHttpClient() {
		
		PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
		cm.setMaxTotal(maxConnections);
		cm.setDefaultMaxPerRoute(maxConnections);
		
		Builder config = RequestConfig.custom()
				.setConnectionRequestTimeout(connectionRequestTimeout)
				.setConnectTimeout(connectTimeout)
				.setSocketTimeout(socketTimeout);

       // TODO: Set Credentials
		CloseableHttpClient httpClient = HttpClients.custom()
		        .setConnectionManager(cm).setDefaultRequestConfig(config.build())
		        .build();
		return httpClient;		
	}
	
	/**
	 * 
	 * @param track Track to map match
	 * @param graphName Name of graph for map matching
	 * @param matchOnNewestGraphVersion true means matching on newest (current) graph's version, 
	 * 									false means matching on graph's version valid at track's start timestamp
	 * @param verboseOutput If true all available attributes of DTOs will be set
	 * @param timeoutInMs Optional setting of timeout in milliseconds
	 * @return MatchedBranchDTO
	 * @throws JsonGenerationException
	 * @throws JsonMappingException
	 * @throws IOException
	 * @throws ParseException
	 */
	public MatchedBranchDTO matchTrack(TrackDTO track, String graphName, boolean matchOnNewestGraphVersion, 
			boolean verboseOutput, int timeoutInMs) 
			throws JsonGenerationException, JsonMappingException, IOException, ParseException {
		String uri = serverRoorUrl;
		if (matchOnNewestGraphVersion) {
			uri += mapMatchingCurrentVersionApiPath;
		} else {
			uri += mapMatchingApiPath;
		}
		uri = uri.replace("{graphName}", graphName);
    	boolean paramsSet = false;
		if (verboseOutput) {
    		uri += "?outputVerbose=true";
    		paramsSet = true;
    	}
    	if (timeoutInMs > 0) {
    		if (paramsSet) {
    			uri += "&";
    		} else {
    			uri += "?";
    		}
    		uri += "timeoutMs=" + timeoutInMs;
    		paramsSet = true;
    	}

    	return callMapMatcher(uri, track);

	}

	/**
	 * 
	 * @param track Track to map match
	 * @param graphName Name of graph for map matching
	 * @param matchOnNewestGraphVersion true means matching on newest (current) graph's version, 
	 * 									false means matching on graph's version valid at track's start timestamp
	 * @param startSegmentId ID of segment where map matching should start (for incremental map matching)
	 * @param verboseOutput If true all available attributes of DTOs will be set
	 * @param timeoutInMs Optional setting of timeout in milliseconds
	 * @return MatchedBranchDTO
	 * @throws JsonGenerationException
	 * @throws JsonMappingException
	 * @throws IOException
	 * @throws ParseException
	 */
	public MatchedBranchDTO matchTrack(TrackDTO track, String graphName, boolean matchOnNewestGraphVersion, 
			long startSegmentId, boolean verboseOutput, int timeoutInMs) 
			throws JsonGenerationException, JsonMappingException, IOException, ParseException {
		String uri = serverRoorUrl;
		if (matchOnNewestGraphVersion) {
			uri += mapMatchingCurrentVersionApiPath;
		} else {
			uri += mapMatchingApiPath;
		}
		uri = uri.replace("{graphName}", graphName) + "?startSegmentId=" + startSegmentId;
    	if (verboseOutput) {
    		uri += "&outputVerbose=true";
    	}
    	if (timeoutInMs > 0) {
    		uri += "&timeoutMs=" + timeoutInMs;
    	}
  	
    	return callMapMatcher(uri, track);

	}

	private MatchedBranchDTO callMapMatcher(String uri, TrackDTO track) 
			throws JsonGenerationException, JsonMappingException, IOException, ParseException {
		long time = System.currentTimeMillis();
		
		MatchedBranchDTO branchDTO = null;
       	
        HttpPost httppost = new HttpPost(uri);

		OutputStream os = new ByteArrayOutputStream();
		mapper.writeValue(os, track);

		StringEntity input = new StringEntity(new String(os.toString()));
		input.setContentType("application/json");
        httppost.setEntity(input);
        
        CloseableHttpResponse response = this.httpClient.execute(httppost);
        try {
            HttpEntity resEntity = response.getEntity();
            if (response.getStatusLine().getStatusCode() == 200 && resEntity != null) {
            	branchDTO = inputFormat.deserialize(resEntity.getContent());
            }
            else {
            	log.error("error accessing remote mapmatcher, response code was " + response.getStatusLine().getStatusCode() +
            			" - " + response.getStatusLine().getReasonPhrase());
            }
        } catch (UnsupportedOperationException e) {
			throw new UnsupportedOperationException(e.getMessage(), e);
		} catch (ParseException e) {
			throw new ParseException(e);
		} finally {
            response.close();
        }

        if (log.isDebugEnabled()) {
        	log.debug("calling remote mapmatcher took: " + (System.currentTimeMillis() - time) + " ms from thread: " + Thread.currentThread().getName());
        }
		
		return branchDTO;	
	}
	
	public int getConnectionRequestTimeout() {
		return connectionRequestTimeout;
	}

	public void setConnectionRequestTimeout(int connectionRequestTimeout) {
		this.connectionRequestTimeout = connectionRequestTimeout;
	}

	public int getConnectTimeout() {
		return connectTimeout;
	}

	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	public int getSocketTimeout() {
		return socketTimeout;
	}

	public void setSocketTimeout(int socketTimeout) {
		this.socketTimeout = socketTimeout;
	}

	public int getMaxConnections() {
		return maxConnections;
	}

	public void setMaxConnections(int maxConnections) {
		this.maxConnections = maxConnections;
	}

}
