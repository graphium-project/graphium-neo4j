/**
 * Graphium Neo4j - Neo4j Server Plugin providing Graphium routing api for graphs integrated in Neo4j server
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
package at.srfg.graphium.api.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

/**
 * @author mwimmer
 *
 */
public class TestRoutingController {

	@Test
	public void testRoute() throws URISyntaxException {
		String url = "http://localhost:7474/graphium/api/graphs/osm_at_lower_levels/routing/getRoute.do";
		
		double startX = 13.043516;
		double startY = 47.812558;
		double endX = 13.044208;
		double endY = 47.812287;
		
    	SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
    	URI uri = new URIBuilder()
    			.setPath(url)
    	        .setParameter("timestamp", df.format(new Date()))
    	        .setParameter("startX", "" + startX)
    	        .setParameter("startY", "" + startY)
    	        .setParameter("endX", "" + endX)
    	        .setParameter("endY", "" + endY)
    	        .setParameter("routingMode", "CAR")
    	        .setParameter("routingCriteria", "length")
    	        .build();
    	
        execute(uri);

	}

	@Test
	public void testRouteOnGraphVersion() throws URISyntaxException {
		String url = "http://localhost:7474/graphium/api/graphs/osm_at_lower_levels/version/20170203/routing/getRoute.do";
		
		double startX = 13.043516;
		double startY = 47.812558;
		double endX = 13.044208;
		double endY = 47.812287;
		
    	SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
    	URI uri = new URIBuilder()
    			.setPath(url)
    	        .setParameter("startX", "" + startX)
    	        .setParameter("startY", "" + startY)
    	        .setParameter("endX", "" + endX)
    	        .setParameter("endY", "" + endY)
    	        .setParameter("routingMode", "CAR")
    	        .setParameter("routingCriteria", "length")
    	        .build();
    	
        execute(uri);

	}

	@Test
	public void testRouteSegments() throws URISyntaxException {
		String url = "http://localhost:7474/graphium/api/graphs/osm_at_lower_levels/routing/getRouteSegments.do";
		
		double startX = 13.043516;
		double startY = 47.812558;
		double endX = 13.044208;
		double endY = 47.812287;
		
    	SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
    	URI uri = new URIBuilder()
    			.setPath(url)
    	        .setParameter("timestamp", df.format(new Date()))
    	        .setParameter("startX", "" + startX)
    	        .setParameter("startY", "" + startY)
    	        .setParameter("endX", "" + endX)
    	        .setParameter("endY", "" + endY)
    	        .setParameter("routingMode", "CAR")
    	        .setParameter("routingCriteria", "length")
    	        .build();
    	
        execute(uri);

	}

	@Test
	public void testRouteSegmentsOnGraphVersion() throws URISyntaxException {
		String url = "http://localhost:7474/graphium/api/graphs/osm_at_lower_levels/version/20170203/routing/getRouteSegments.do";
		
		double startX = 13.043516;
		double startY = 47.812558;
		double endX = 13.044208;
		double endY = 47.812287;
		
    	SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
    	URI uri = new URIBuilder()
    			.setPath(url)
    	        .setParameter("startX", "" + startX)
    	        .setParameter("startY", "" + startY)
    	        .setParameter("endX", "" + endX)
    	        .setParameter("endY", "" + endY)
    	        .setParameter("routingMode", "CAR")
    	        .setParameter("routingCriteria", "length")
    	        .build();
    	
        execute(uri);

	}

	private void execute(URI uri) {
        try {
			CloseableHttpClient httpclient = HttpClients.createDefault();
			try {
		        HttpGet httpget = new HttpGet(uri);
		        
		        System.out.println("executing request " + httpget.getRequestLine());
		        CloseableHttpResponse response = httpclient.execute(httpget);
		        try {
		            System.out.println("----------------------------------------");
		            System.out.println(response.getStatusLine());
		            HttpEntity resEntity = response.getEntity();
		            if (resEntity != null) {
		                System.out.println("Response content length: " + resEntity.getContentLength());
		                System.out.println("Response content: " + getFileContent(resEntity.getContent(), "UTF8"));
		            }
		            EntityUtils.consume(resEntity);
		        } finally {
		            response.close();
		        }
		    } finally {
		        httpclient.close();
		    }
		} catch (Exception e) {
	        System.out.println("Error: " + e.getMessage());
	    }

	}

	private String getFileContent(InputStream fis, String encoding ) throws IOException
	 {
	   try( BufferedReader br =
	           new BufferedReader( new InputStreamReader(fis, encoding )))
	   {
	      StringBuilder sb = new StringBuilder();
	      String line;
	      while(( line = br.readLine()) != null ) {
	         sb.append( line );
	         sb.append( '\n' );
	      }
	      return sb.toString();
	   }
	}
}
