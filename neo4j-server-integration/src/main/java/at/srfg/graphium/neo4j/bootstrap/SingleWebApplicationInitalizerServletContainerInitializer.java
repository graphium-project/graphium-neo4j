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
package at.srfg.graphium.neo4j.bootstrap;

import java.util.Set;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.WebApplicationInitializer;

/**
 * 
 * @author anwagner
 *
 */
public class SingleWebApplicationInitalizerServletContainerInitializer implements ServletContainerInitializer { 

    private static final Logger log = LoggerFactory.getLogger(SingleWebApplicationInitalizerServletContainerInitializer.class);
    
	WebApplicationInitializer initializer;
	
	public SingleWebApplicationInitalizerServletContainerInitializer(WebApplicationInitializer initializer) {
		this.initializer = initializer;;
	}
	
    
	@Override
	public void onStartup(Set<Class<?>> webAppInitializerClasses, ServletContext servletContext)
			throws ServletException {
		log.info("called ServletContainerInitalizer: " + initializer.getClass());
		initializer.onStartup(servletContext);
	}
}
