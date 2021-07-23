package at.srfg.graphium.mapmatching.neo4j.matcher.impl;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import at.srfg.graphium.mapmatching.model.Direction;
import at.srfg.graphium.mapmatching.model.IMatchedWaySegment;
import at.srfg.graphium.mapmatching.model.impl.MatchedWaySegmentImpl;
import at.srfg.graphium.model.hd.impl.HDWaySegment;
import at.srfg.graphium.model.impl.WaySegment;

public class PathExpanderMatcherTest {
	protected Logger log = Logger.getLogger(this.getClass().getName());

	@Test
	public void testMatchedSegmentDirection() {
		IMatchedWaySegment prevSegment = new MatchedWaySegmentImpl(new WaySegment());
		prevSegment.getSegment().setId(1);
		prevSegment.getSegment().setStartNodeId(34);
		prevSegment.getSegment().setEndNodeId(38);
		IMatchedWaySegment segment = new MatchedWaySegmentImpl(new WaySegment());
		segment.getSegment().setId(2);
		segment.getSegment().setStartNodeId(38);
		segment.getSegment().setEndNodeId(42);
		prevSegment.setDirection(Direction.START_TO_END);
		segment.setDirection(Direction.START_TO_END);
		
		Assert.assertEquals(true, PathExpanderMatcher.isMatchedSegmentDirectionTow(prevSegment, segment));
		Assert.assertEquals(false, PathExpanderMatcher.isMatchedSegmentDirectionBkw(prevSegment, segment));
		
		segment.getSegment().setStartNodeId(42);
		segment.getSegment().setEndNodeId(38);
		prevSegment.setDirection(Direction.START_TO_END);
		segment.setDirection(Direction.END_TO_START);
		
		Assert.assertEquals(false, PathExpanderMatcher.isMatchedSegmentDirectionTow(prevSegment, segment));
		Assert.assertEquals(true, PathExpanderMatcher.isMatchedSegmentDirectionBkw(prevSegment, segment));
		
		IMatchedWaySegment prevHdSegment = new MatchedWaySegmentImpl(new HDWaySegment());
		prevHdSegment.getSegment().setId(1);
		prevHdSegment.getSegment().setStartNodeId(34);
		prevHdSegment.getSegment().setEndNodeId(38);
		IMatchedWaySegment hdSegment = new MatchedWaySegmentImpl(new HDWaySegment());
		hdSegment.getSegment().setId(2);
		hdSegment.getSegment().setStartNodeId(38);
		hdSegment.getSegment().setEndNodeId(42);
		prevHdSegment.setDirection(Direction.START_TO_END);
		hdSegment.setDirection(Direction.START_TO_END);
		
		Assert.assertEquals(true, PathExpanderMatcher.isMatchedSegmentDirectionTow(prevHdSegment, hdSegment));
		Assert.assertEquals(false, PathExpanderMatcher.isMatchedSegmentDirectionBkw(prevHdSegment, hdSegment));
		
		hdSegment.getSegment().setStartNodeId(42);
		hdSegment.getSegment().setEndNodeId(38);
		prevHdSegment.setDirection(Direction.START_TO_END);
		hdSegment.setDirection(Direction.END_TO_START);
		
		Assert.assertEquals(false, PathExpanderMatcher.isMatchedSegmentDirectionTow(prevHdSegment, hdSegment));
		Assert.assertEquals(true, PathExpanderMatcher.isMatchedSegmentDirectionBkw(prevHdSegment, hdSegment));
		
		prevHdSegment.getSegment().setStartNodeId(38);
		prevHdSegment.getSegment().setEndNodeId(34);
		prevHdSegment.setDirection(Direction.END_TO_START);
		hdSegment.setDirection(Direction.END_TO_START);
		
		Assert.assertEquals(false, PathExpanderMatcher.isMatchedSegmentDirectionTow(prevHdSegment, hdSegment));
		Assert.assertEquals(true, PathExpanderMatcher.isMatchedSegmentDirectionBkw(prevHdSegment, hdSegment));
		
		hdSegment.getSegment().setStartNodeId(38);
		hdSegment.getSegment().setEndNodeId(42);
		prevHdSegment.setDirection(Direction.END_TO_START);
		hdSegment.setDirection(Direction.START_TO_END);
		
		Assert.assertEquals(true, PathExpanderMatcher.isMatchedSegmentDirectionTow(prevHdSegment, hdSegment));
		Assert.assertEquals(false, PathExpanderMatcher.isMatchedSegmentDirectionBkw(prevHdSegment, hdSegment));
		
		// parallel lanes
		
		hdSegment = new MatchedWaySegmentImpl(new HDWaySegment());
		hdSegment.getSegment().setStartNodeId(39);
		hdSegment.getSegment().setEndNodeId(42);
		prevHdSegment.setDirection(Direction.START_TO_CENTER);
		hdSegment.setDirection(Direction.CENTER_TO_CENTER);
		
		Assert.assertEquals(false, PathExpanderMatcher.isMatchedSegmentDirectionTow(prevHdSegment, hdSegment));
		Assert.assertEquals(false, PathExpanderMatcher.isMatchedSegmentDirectionBkw(prevHdSegment, hdSegment));
		
		// splitting lanes
		
		hdSegment.getSegment().setStartNodeId(34);
		hdSegment.getSegment().setEndNodeId(42);
		
		Assert.assertEquals(false, PathExpanderMatcher.isMatchedSegmentDirectionTow(prevHdSegment, hdSegment));
		Assert.assertEquals(false, PathExpanderMatcher.isMatchedSegmentDirectionBkw(prevHdSegment, hdSegment));
		
		// merging lanes
		
		hdSegment.getSegment().setStartNodeId(36);
		hdSegment.getSegment().setEndNodeId(38);
		
		Assert.assertEquals(false, PathExpanderMatcher.isMatchedSegmentDirectionTow(prevHdSegment, hdSegment));
		Assert.assertEquals(false, PathExpanderMatcher.isMatchedSegmentDirectionBkw(prevHdSegment, hdSegment));
	}
}
