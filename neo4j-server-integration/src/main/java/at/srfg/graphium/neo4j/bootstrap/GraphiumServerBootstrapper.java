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
/**
 * (C) 2016 Salzburg Research Forschungsgesellschaft m.b.H.
 *
 * All rights reserved.
 *
 */
package at.srfg.graphium.neo4j.bootstrap;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletException;
import javax.ws.rs.Path;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.ArrayUtil;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.component.LifeCycle;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.configuration.Config;
import org.neo4j.server.AbstractNeoServer;
import org.neo4j.server.NeoServer;
import org.neo4j.server.configuration.ServerSettings;
import org.neo4j.server.configuration.ThirdPartyJaxRsPackage;
import org.neo4j.server.plugins.Injectable;
import org.neo4j.server.plugins.SPIPluginLifecycle;
import org.neo4j.server.web.Jetty9WebServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.WebApplicationInitializer;

import at.srfg.graphium.neo4j.bootstrap.security.GraphiumSpringSecurityApiConfig;
import at.srfg.graphium.neo4j.bootstrap.security.GraphiumSpringSecurityBootstrapper;
import at.srfg.graphium.neo4j.bootstrap.security.GraphiumSpringSecurityDefaultConfig;

/**
 * @author anwagner
 */
@Path("/")
public class GraphiumServerBootstrapper implements SPIPluginLifecycle {

	private static final Logger log = LoggerFactory.getLogger(GraphiumServerBootstrapper.class);

	private static final String GRAPHIUM_PACKAGE = "at.srfg.graphium.neo4j.bootstrap";
	private final static String DEFAULT_SECURITY_XML = "/security/application-context-graphium-api-security.xml";
	private static final String GRAPHIUM_SECURED_PROPERTY = "graphium.secured";

	private boolean bootSpringSecurity = true;

	@Override
	public Collection<Injectable<?>> start(GraphDatabaseService dbService, Configuration config) {
		log.info("graphium unmanaged extension is starting");
		return Collections.emptyList();
	}

	@Override
	public Collection<Injectable<?>> start(NeoServer neoServer) {
		log.info("graphium unmanaged extension is starting");
		registerInitOnJettyCreated(neoServer);
		return Collections.emptyList();
	}

	@Override
	public void stop() {
		log.info("graphium unmanaged extension stopped");
	}

	private void registerInitOnJettyCreated(NeoServer neoServer) {
		log.info("extension registered, waiting for jetty instance...");
		Jetty9WebServer neoWebserver = (Jetty9WebServer) ((AbstractNeoServer) neoServer).getWebServer();
		final Config config = neoServer.getConfig();
		bootSpringSecurity = checkSecurityEnabled(config);

		neoWebserver.setJettyCreatedCallback(new Consumer<Server>() {

			@Override
			public void accept(Server jetty) {
				log.info("got jetty instance, waiting for starting lifecycle event ...");

				jetty.addLifeCycleListener(new AbstractLifeCycle.AbstractLifeCycleListener() {

					@Override
					public void lifeCycleStarting(LifeCycle event) {

						log.info("starting spring mvc setup ... ");
						WebApplicationInitializer webappInit = new GraphiumSpringWebAppInitializer(
								"GraphiumSpringServlet", neoServer);
						ServletContextHandler graphiumHandler = createGraphiumServletHandler(getContextPath(config),
								jetty);

						// has to be done nested, we have to react on the "starting" callback of this
						// specific handler.
						// only then we can extend the behaviour with custom servletContainerInitalizers
						// because only then
						// the corresponding servletContext is in starting state (required in servlet
						// spec, only then
						// we can add listeners in onStartup)
						// with this approach the contextLoaderListeners of springs classes are executed
						// as expected
						graphiumHandler.addLifeCycleListener(new AbstractLifeCycle.AbstractLifeCycleListener() {
							@Override
							public void lifeCycleStarting(LifeCycle event) {
								log.info("graphium handler starting, registering spring WebapplicationInitalizers...");
								ServletContainerInitializer initalizer = new SingleWebApplicationInitalizerServletContainerInitializer(
										webappInit);
								try {
									initalizer.onStartup(Collections.emptySet(), graphiumHandler.getServletContext());

									log.info(
											"booting spring security is turned " + (bootSpringSecurity ? "on" : "off"));
									if (bootSpringSecurity) {
										Class<?> securityConfigClass = determineSecurityConfigClass();

										WebApplicationInitializer securityWebAppInit = new GraphiumSpringSecurityBootstrapper(
												securityConfigClass);
										ServletContainerInitializer securityInitalizer = new SingleWebApplicationInitalizerServletContainerInitializer(
												securityWebAppInit);
										securityInitalizer.onStartup(Collections.emptySet(),
												graphiumHandler.getServletContext());
									}

								} catch (ServletException e) {
									log.error("error during registration of servlet container initializer", e);
								}
							}

						});
						graphiumHandler.getServletContext().setExtendedListenerTypes(true);
						HandlerCollection handlerCollection = getHandlerCollection(jetty);
						handlerCollection.setHandlers(ArrayUtil.prependToArray(graphiumHandler,
								handlerCollection.getHandlers(), Handler.class));
					}
				});
			}
		});
	}

	private boolean checkSecurityEnabled(Config config) {
		Map<String, String> raw = config.getRaw();
		return Boolean.parseBoolean(raw.get(GRAPHIUM_SECURED_PROPERTY));
	}

	private Class<?> determineSecurityConfigClass() {
		Resource securityXml = new ClassPathResource(DEFAULT_SECURITY_XML);
		if (securityXml.exists()) {
			return GraphiumSpringSecurityApiConfig.class;
		} else {
			return GraphiumSpringSecurityDefaultConfig.class;
		}
	}

	protected final ServletContextHandler createGraphiumServletHandler(String contextPath, Server jetty) {

		// create new handler
		ServletContextHandler handler = new ServletContextHandler(ServletContextHandler.SESSIONS);
		// with mountpoint of unmanged ext. as path
		handler.setContextPath(contextPath);
		// and register server
		handler.setServer(jetty);

		// extract existing session handler from jaxRS Mountpoints
		HandlerCollection handlerCollection = getHandlerCollection(jetty);
		for (Handler presentHandler : handlerCollection.getHandlers()) {
			if (presentHandler instanceof ServletContextHandler) {
				ServletContextHandler contextHandler = (ServletContextHandler) presentHandler;
				if (contextHandler.getSessionHandler() != null
						&& contextHandler.getSessionHandler().getSessionManager() != null) {
					handler.setSessionHandler(
							new SessionHandler(contextHandler.getSessionHandler().getSessionManager()));
					break;
				}

			}
		}
		return handler;
	}

	private HandlerCollection getHandlerCollection(Server jetty) {
		Handler handler = jetty.getHandler();
		HandlerCollection handlerCollection = null;
		if (handler instanceof RequestLogHandler) {
			Handler nestedHandler = ((RequestLogHandler) handler).getHandler();
			if (nestedHandler instanceof HandlerCollection) {
				handlerCollection = (HandlerCollection) nestedHandler;
			}
		} else {
			handlerCollection = (HandlerCollection) jetty.getHandler();
		}

		if (handlerCollection == null) {
			String msg = "error handler is nether a HandlerCollection "
					+ "nor a RequestLogHandler. Can not register required Handler!";
			log.error(msg);
			// it would be cleaner to define a checked exception at this stage...
			throw new RuntimeException(msg);
		}
		return handlerCollection;
	}

	private String getContextPath(Config config) {
		for (ThirdPartyJaxRsPackage rsPackage : config.get(ServerSettings.third_party_packages)) {
			if (rsPackage.getPackageName().equals(getPackage())) {
				String path = rsPackage.getMountPoint();
				if (StringUtils.isNotBlank(path)) {
					log.info("Mounting Graphium Framework at {}", path);
					return path;
				} else {
					throw new IllegalArgumentException("Illegal Graphium mount point: " + path);
				}
			}
		}

		throw new IllegalStateException("No mount point for Graphium");
	}

	protected String getPackage() {
		return GRAPHIUM_PACKAGE;
	}

}
