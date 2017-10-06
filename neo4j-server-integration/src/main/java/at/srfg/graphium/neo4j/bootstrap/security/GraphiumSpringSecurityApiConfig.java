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
package at.srfg.graphium.neo4j.bootstrap.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ImportResource;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

/**
 * java config to enable spring security. requires security config file security/application-context-graphium-api-security.xml
 * in classpath (e.g. present in graphium-api.jar). loads the configuration definined in the xml file
 * 
 * @author anwagner
 *
 */
@EnableWebSecurity
@ImportResource({
    "classpath:/security/application-context-graphium-api-security.xml"
})
public class GraphiumSpringSecurityApiConfig {

	@Autowired
	public void configureGlobal(AuthenticationManagerBuilder auth)
			throws Exception {
	}
}
