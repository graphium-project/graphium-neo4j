package at.srfg.graphium.mapmatching.properties.impl;

import java.util.Map;

import at.srfg.graphium.routing.model.impl.RoutingMode;

/**
 * maximum speeds per routing mode and frc/urban
 */
public class MaxSpeedForRouting {
	
	private Map<RoutingMode, MaxSpeedForRoutingMode> speedsPerRoutingMode = null;

	public Map<RoutingMode, MaxSpeedForRoutingMode> getSpeedsPerRoutingMode() {
		return speedsPerRoutingMode;
	}
	public void setSpeedsPerRoutingMode(Map<RoutingMode, MaxSpeedForRoutingMode> speedsPerRoutingMode) {
		this.speedsPerRoutingMode = speedsPerRoutingMode;
	}
}
