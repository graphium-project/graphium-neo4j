/**
 * Graphium Neo4j - Map Matching module of Graphium
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
package at.srfg.graphium.mapmatching.matcher;

import at.srfg.graphium.core.exception.GraphNotExistsException;
import at.srfg.graphium.mapmatching.model.ITrack;
import at.srfg.graphium.mapmatching.properties.impl.MapMatchingProperties;
import at.srfg.graphium.routing.exception.RoutingParameterException;

public interface IMapMatcher {

	public IMapMatcherTask getTask(ITrack origTrack, String routingMode) throws GraphNotExistsException, RoutingParameterException;
	
	public IMapMatcherTask getTask(String graphName, ITrack origTrack, String routingMode) throws GraphNotExistsException, RoutingParameterException;
	
	public IMapMatcherTask getTask(String graphName, String graphVersion, ITrack origTrack, String routingMode) throws GraphNotExistsException, RoutingParameterException;
	
	public String getDefaultGraphName();
	public void setDefaultGraphName(String defaultGraphName);

	public MapMatchingProperties getProperties();
	public void setProperties(MapMatchingProperties properties);

	public MapMatchingProperties getPropertiesHd();
	public void setPropertiesHd(MapMatchingProperties propertiesHd);

}
