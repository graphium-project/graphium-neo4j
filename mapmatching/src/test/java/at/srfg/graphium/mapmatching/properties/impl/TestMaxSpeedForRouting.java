package at.srfg.graphium.mapmatching.properties.impl;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import at.srfg.graphium.mapmatching.properties.IMapMatchingProperties;
import at.srfg.graphium.model.FuncRoadClass;
import at.srfg.graphium.routing.model.impl.RoutingMode;

public class TestMaxSpeedForRouting {

	@Test
	public void testMaxSpeedForRouting() {
		
		MaxSpeedForRoutingMode defaultSpeeds = new MaxSpeedForRoutingMode();
		
		String maxSpeedForRoutingJson = "{\"speedsPerRoutingMode\" : {\"CAR\": {\"defaultSpeed\": 93, \"frcOverrides\": {\"0\": 138, \"1\": 115, \"2\": 104, \"3\": 104, \"4\": 104}, \"urbanEnabled\": true, \"urbanSpeed\": 70}, \"BIKE\": {\"defaultSpeed\": 50}, \"PEDESTRIAN\": {\"defaultSpeed\": 20}}}";

		IMapMatchingProperties properties = new MapMatchingProperties();
		properties.setMaxSpeedForRoutingJson(maxSpeedForRoutingJson);
		
		MaxSpeedForRouting maxSpeedForRouting = properties.getMaxSpeedForRouting();
		
		Assert.assertNotNull(maxSpeedForRouting);
		Assert.assertEquals(3, maxSpeedForRouting.getSpeedsPerRoutingMode().size());
		
		Assert.assertTrue(maxSpeedForRouting.getSpeedsPerRoutingMode().containsKey(RoutingMode.CAR));
		Assert.assertTrue(maxSpeedForRouting.getSpeedsPerRoutingMode().containsKey(RoutingMode.BIKE));
		Assert.assertTrue(maxSpeedForRouting.getSpeedsPerRoutingMode().containsKey(RoutingMode.PEDESTRIAN));
		
		Assert.assertEquals(93, maxSpeedForRouting.getSpeedsPerRoutingMode().get(RoutingMode.CAR).getDefaultSpeed());
		Assert.assertEquals(50, maxSpeedForRouting.getSpeedsPerRoutingMode().get(RoutingMode.BIKE).getDefaultSpeed());
		Assert.assertEquals(20, maxSpeedForRouting.getSpeedsPerRoutingMode().get(RoutingMode.PEDESTRIAN).getDefaultSpeed());
		
		Assert.assertEquals(70, maxSpeedForRouting.getSpeedsPerRoutingMode().get(RoutingMode.CAR).getUrbanSpeed());
		Assert.assertEquals(defaultSpeeds.getUrbanSpeed(), maxSpeedForRouting.getSpeedsPerRoutingMode().get(RoutingMode.BIKE).getUrbanSpeed());
		Assert.assertEquals(defaultSpeeds.getUrbanSpeed(), maxSpeedForRouting.getSpeedsPerRoutingMode().get(RoutingMode.PEDESTRIAN).getUrbanSpeed());
		
		Assert.assertTrue(maxSpeedForRouting.getSpeedsPerRoutingMode().get(RoutingMode.CAR).isUrbanEnabled());
		Assert.assertFalse(maxSpeedForRouting.getSpeedsPerRoutingMode().get(RoutingMode.BIKE).isUrbanEnabled());
		Assert.assertFalse(maxSpeedForRouting.getSpeedsPerRoutingMode().get(RoutingMode.PEDESTRIAN).isUrbanEnabled());
		
		Assert.assertNotNull(maxSpeedForRouting.getSpeedsPerRoutingMode().get(RoutingMode.CAR).getFrcOverrides());
		Assert.assertNull(maxSpeedForRouting.getSpeedsPerRoutingMode().get(RoutingMode.BIKE).getFrcOverrides());
		Assert.assertNull(maxSpeedForRouting.getSpeedsPerRoutingMode().get(RoutingMode.PEDESTRIAN).getFrcOverrides());
		
		Map<Short, Integer> frcOverrides = maxSpeedForRouting.getSpeedsPerRoutingMode().get(RoutingMode.CAR).getFrcOverrides();
		
		Assert.assertTrue(frcOverrides.containsKey(FuncRoadClass.MOTORWAY_FREEWAY_OR_OTHER_MAJOR_MOTORWAY.getValue()));
		Assert.assertTrue(frcOverrides.containsKey(FuncRoadClass.MAJOR_ROAD_LESS_IMORTANT_THAN_MOTORWAY.getValue()));
		Assert.assertTrue(frcOverrides.containsKey(FuncRoadClass.OTHER_MAJOR_ROAD.getValue()));
		Assert.assertTrue(frcOverrides.containsKey(FuncRoadClass.SECONDARY_ROAD.getValue()));
		Assert.assertTrue(frcOverrides.containsKey(FuncRoadClass.LOCAL_CONNECTING_ROAD.getValue()));
		Assert.assertFalse(frcOverrides.containsKey(FuncRoadClass.LOCAL_ROAD_OF_HIGH_IMPORTANCE.getValue()));
		
		Assert.assertEquals(138, frcOverrides.get(FuncRoadClass.MOTORWAY_FREEWAY_OR_OTHER_MAJOR_MOTORWAY.getValue()).intValue());
		Assert.assertEquals(115, frcOverrides.get(FuncRoadClass.MAJOR_ROAD_LESS_IMORTANT_THAN_MOTORWAY.getValue()).intValue());
		Assert.assertEquals(104, frcOverrides.get(FuncRoadClass.OTHER_MAJOR_ROAD.getValue()).intValue());
		Assert.assertEquals(104, frcOverrides.get(FuncRoadClass.SECONDARY_ROAD.getValue()).intValue());
		Assert.assertEquals(104, frcOverrides.get(FuncRoadClass.LOCAL_CONNECTING_ROAD.getValue()).intValue());
	}
}
