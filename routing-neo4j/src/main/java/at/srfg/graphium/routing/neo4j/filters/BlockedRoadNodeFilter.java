package at.srfg.graphium.routing.neo4j.filters;

import java.time.LocalDateTime;
import java.util.function.Predicate;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import at.srfg.graphium.neo4j.model.WayGraphConstants;
import at.srfg.graphium.routing.service.IRestrictionsService;

/**
 * @author mwimmer
 *
 */
public class BlockedRoadNodeFilter implements Predicate<Relationship> {
	
	private String graphName;
	private Direction direction;
	private LocalDateTime routingTimestamp;
	private IRestrictionsService restrictionsService;
	
	public BlockedRoadNodeFilter(String graphName, Direction direction, LocalDateTime routingTimestamp, IRestrictionsService restrictionsService) {
		super();
		this.graphName = graphName;
		this.direction = direction;
		this.routingTimestamp = routingTimestamp;
		this.restrictionsService = restrictionsService;
	}

	@Override
	public boolean test(Relationship rel) {
		Node startNode = rel.getStartNode();
		long startSegmentId = (Long) startNode.getProperty(WayGraphConstants.SEGMENT_ID);
		Node endNode = rel.getEndNode();
		long endSegmentId = (Long) endNode.getProperty(WayGraphConstants.SEGMENT_ID);

		boolean directionStartToEnd;
		long segmentId;
		if (direction.equals(Direction.OUTGOING)) {
			segmentId = endSegmentId;
			if ((Long)startNode.getProperty(WayGraphConstants.SEGMENT_STARTNODE_ID) == (Long)endNode.getProperty(WayGraphConstants.SEGMENT_STARTNODE_ID) ||
				(Long)startNode.getProperty(WayGraphConstants.SEGMENT_ENDNODE_ID) == (Long)endNode.getProperty(WayGraphConstants.SEGMENT_STARTNODE_ID)) {
				directionStartToEnd = true;
			} else {
				directionStartToEnd = false;
			}
		} else {
			segmentId = startSegmentId;
			if ((Long)startNode.getProperty(WayGraphConstants.SEGMENT_ENDNODE_ID) == (Long)endNode.getProperty(WayGraphConstants.SEGMENT_STARTNODE_ID) ||
				(Long)startNode.getProperty(WayGraphConstants.SEGMENT_ENDNODE_ID) == (Long)endNode.getProperty(WayGraphConstants.SEGMENT_ENDNODE_ID)) {
				directionStartToEnd = true;
			} else {
				directionStartToEnd = false;
			}
		}	
		
		if (restrictionsService.isRestrictedSegment(graphName, segmentId, directionStartToEnd, routingTimestamp)) {
			// road is blocked => rel / segment not valid for routing
			return false;
		}
		return true;
	}

}
