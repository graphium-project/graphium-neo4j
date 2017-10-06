/**
 * Graphium Neo4j - Server integration for Graphium modules in Neo4j Standalone server as unmanaged Extensions
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
package at.srfg.graphium.neo4j.integration;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.harness.ServerControls;
import org.neo4j.harness.TestServerBuilder;
import org.neo4j.harness.TestServerBuilders;
import org.neo4j.test.server.HTTP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.srfg.graphium.neo4j.bootstrap.GraphiumServerBootstrapper;

/**
 */
public class StartNeo4jServerWithGraphiumTest {

	private static Logger log = LoggerFactory.getLogger(StartNeo4jServerWithGraphiumTest.class);
	
//	private String dbDirectory = "D:/databases/Neo4j/3.0/gip_at_frc_0_4_16_02_160426";
//	private String workingDirectory = "D:/development/project_data/graphserver/tests/working_directory_unittests";
	
	private String dbDirectory = "C:/development/Graphserver/tmp/neo4j/unittest-databases/template";
	private String workingDirectory = "C:/development/Graphserver/tmp/neo4j/unittest-databases/unittest-work";
	
	
	@Test
	public void testStartAndDoNothing() throws Exception {
		log.info("server started... and shutting down..");

		// Given
	    try ( ServerControls server = getServerBuilder()
	            .withExtension( "/test", GraphiumServerBootstrapper.class )
//	            .withConfig("graphium.secured", "true")
	            .newServer() )
	    {
	        // When
	        HTTP.Response response = HTTP.GET(
	                HTTP.GET( server.httpURI().resolve( "test" ).toString() ).location() );

	        // Then
	        // no controllers started
	        assertEquals( 404, response.status() );
	    }
	}
	
	@Test
	public void testStartAndDoNothingWithHttpLogging() throws Exception {
		log.info("server started... and shutting down..");

		// Given
	    try ( ServerControls server = getServerBuilder()
	            .withExtension( "/test", GraphiumServerBootstrapper.class )
	            .withConfig("dbms.logs.http.enabled", "true")
	            .newServer() )
	    {
	        // When
	        HTTP.Response response = HTTP.GET(
	                HTTP.GET( server.httpURI().resolve( "test" ).toString() ).location() );

	        // Then
	        // no controllers started
	        assertEquals( 404, response.status() );
	    }
	}
	
	@Ignore
	@Test
	public void testStartAndDoNothingWithSecurity() throws Exception {
		log.info("server started... and shutting down..");

		// Given
	    try ( ServerControls server = getServerBuilder()
	            .withExtension( "/test", GraphiumServerBootstrapper.class )
	            .withConfig("graphium.secured", "true")
	            .newServer() )
	    {
	        // When
	        HTTP.Response response = HTTP.GET(
	                HTTP.GET( server.httpURI().resolve( "test" ).toString() ).location() );

	        // Then
	        // no controllers started but security filter kicks in
	        assertEquals( 401, response.status() );
	    }
	}
	
	
	private TestServerBuilder getServerBuilder( ) throws IOException
    {
		// create TestServerBuilder using workingDirectory for temporarily storing database
		TestServerBuilder serverBuilder = TestServerBuilders.newInProcessBuilder(new File(workingDirectory));
//        serverBuilder.withConfig( ServerSettings.certificates_directory.name(),
//                ServerTestUtils.getRelativePath( new File(workingDirectory), ServerSettings.certificates_directory ) )
//                ServerTestUtils.getRelativePath( ServerTestUtils.getSharedTestTemporaryFolder(), ServerSettings.certificates_directory ))
		
		// copy an existing database to workingDirectory (will be removed at shutdown)
		serverBuilder.copyFrom(new File(dbDirectory));
        return serverBuilder;
    }

}