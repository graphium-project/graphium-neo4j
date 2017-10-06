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
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

/**
 * Default Security config. Example how web security can be enabled using java configuration. 
 * To boot e.g. graphiums api security context or another default xml config for security
 * at.srfg.graphium.neo4j.bootstrap.security.GraphiumSpringSecurityBootstrapper 
 * class is used.  Bootstrapping process recognises if default named  application context 
 * (/security/application-context-graphium-api-security.xml) is present in classpath 
 * (e.g. from  graphium-api jar) and imports it
 *
 * @author anwagner
 *
 */
@EnableWebSecurity
public class GraphiumSpringSecurityDefaultConfig {

	@Autowired
	public void configureGlobal(AuthenticationManagerBuilder auth)
			throws Exception {
		auth.inMemoryAuthentication().withUser("user").password("password")
				.roles("USER");
	}
}
