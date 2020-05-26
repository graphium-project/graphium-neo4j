package at.srfg.graphium.mapmatching.properties.impl;

import java.util.Map;

/**
 * Maximum speeds per frc/urban
 */
public class MaxSpeedForRoutingMode {

	private int defaultSpeed = 100;
	private Map<Short, Integer> frcOverrides = null;
	private boolean urbanEnabled = false;
	private int urbanSpeed = 70;
	
	public int getDefaultSpeed() {
		return defaultSpeed;
	}
	public void setDefaultSpeed(int defaultSpeed) {
		this.defaultSpeed = defaultSpeed;
	}
	
	public Map<Short, Integer> getFrcOverrides() {
		return frcOverrides;
	}
	public void setFrcOverrides(Map<Short, Integer> frcOverrides) {
		this.frcOverrides = frcOverrides;
	}
	
	public boolean isUrbanEnabled() {
		return urbanEnabled;
	}
	public void setUrbanEnabled(boolean urbanEnabled) {
		this.urbanEnabled = urbanEnabled;
	}
	
	public int getUrbanSpeed() {
		return urbanSpeed;
	}
	public void setUrbanSpeed(int urbanSpeed) {
		this.urbanSpeed = urbanSpeed;
	}
}
