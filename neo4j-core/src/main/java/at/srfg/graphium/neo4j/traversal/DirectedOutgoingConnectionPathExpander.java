/**
 * Graphium Neo4j - Module of Graphserver for Neo4j extension
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
package at.srfg.graphium.neo4j.traversal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

import org.apache.log4j.Logger;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.BranchState;

import at.srfg.graphium.neo4j.model.WayGraphConstants;
import at.srfg.graphium.neo4j.model.WaySegmentRelationshipType;

/**
 * The DirectedOutgoingConnectionPathExpander is an own implementation of the PathExpander that allows to expand OUTGOING relationships
 * under consideration of the direction on the segment. Outgoing rels. are only returned if they are on the "other side"
 * of the segment than the path has entered the segment. Finding out which connection type is the correct one (start or end node connection)
 * is done using nodeId compares.
 * 
 * The inital state is passed in during object creation, it requires the segment and the direction to find out its start direction (rel. type)
 *
 * @author shennebe, anwagner
 */
public class DirectedOutgoingConnectionPathExpander<STATE> implements PathExpander<STATE> {

    protected Logger log = Logger.getLogger(this.getClass().getName());

    protected WaySegmentRelationshipType initialRelation;
    protected boolean init = true;
	protected List<Predicate<? super Node>> nodeFilters = new ArrayList<>();
	protected List<Predicate<? super Relationship>> relationshipFilters = new ArrayList<>();

    public DirectedOutgoingConnectionPathExpander(WaySegmentRelationshipType initialRelation) {
    	this.initialRelation = initialRelation;
    }
    
	public void addNodeFilter(Predicate<? super Node> nodeFilter) {
		nodeFilters.add(nodeFilter);
	}
	
	public void addRelationshipFilter(Predicate<? super Relationship> relationshipFilter) {
		relationshipFilters.add(relationshipFilter);
	}

	@Override
    public Iterable<Relationship> expand(Path path, BranchState<STATE> state) {
        WaySegmentRelationshipType initialRelationType = null;
        if (init) {
            initialRelationType = this.initialRelation;
            init = false;
        }
        
        Node endNode = path.endNode();
		for (Predicate<? super Node> nodeFilter : nodeFilters) {
			if (!nodeFilter.test(endNode)) {
				return new ArrayList<>();
			}
		}

		Relationship lastRel = getLastRelationship(path);
		if (lastRel != null) {
			for (Predicate<? super Relationship> relationshipFilter : relationshipFilters) {
				if (!relationshipFilter.test(lastRel)) {
					return new ArrayList<>();
				}
			}
		}
        
        return doExpand(path, initialRelationType);      
    }

    private Iterable<Relationship> doExpand(Path path, WaySegmentRelationshipType initialRelationType) {
        //Iterable<Relationship> result;
    	List<Relationship> result = new ArrayList<>();
    	Relationship lastRel = getLastRelationship(path);
        if (lastRel == null) {
        	if (initialRelationType != null) {
        		addRelationships(result, path, Direction.OUTGOING, initialRelationType);
        	} else {
        		addRelationships(result, path, Direction.OUTGOING, WaySegmentRelationshipType.SEGMENT_CONNECTION_ON_STARTNODE);
        		addRelationships(result, path, Direction.OUTGOING, WaySegmentRelationshipType.SEGMENT_CONNECTION_ON_ENDNODE);
        		addRelationships(result, path, Direction.OUTGOING, WaySegmentRelationshipType.SEGMENT_CONNECTION_WITHOUT_NODE);
        	}
        } else if (lastRel.getProperty(WayGraphConstants.CONNECTION_NODE_ID)
        		.equals(path.endNode().getProperty(WayGraphConstants.SEGMENT_STARTNODE_ID))) {
        	addRelationships(result, path, Direction.OUTGOING, WaySegmentRelationshipType.SEGMENT_CONNECTION_ON_ENDNODE);
    		addRelationships(result, path, Direction.OUTGOING, WaySegmentRelationshipType.SEGMENT_CONNECTION_WITHOUT_NODE);
        } else if (lastRel.getProperty(WayGraphConstants.CONNECTION_NODE_ID)
        		.equals(path.endNode().getProperty(WayGraphConstants.SEGMENT_ENDNODE_ID))) {
        	addRelationships(result, path, Direction.OUTGOING, WaySegmentRelationshipType.SEGMENT_CONNECTION_ON_STARTNODE);
    		addRelationships(result, path, Direction.OUTGOING, WaySegmentRelationshipType.SEGMENT_CONNECTION_WITHOUT_NODE);
        } else {
        	// last relationship was a SEGMENT_CONNECTION_WITHOUT_NODE
        	// make sure the direction does not change
        	Relationship lastRelationshipViaNodeId = getLastRelationshipViaNodeId(path);
        	if (lastRelationshipViaNodeId == null) {
            	if (initialRelationType != null) {
            		addRelationships(result, path, Direction.OUTGOING, initialRelationType);
            	} else {
            		addRelationships(result, path, Direction.OUTGOING, WaySegmentRelationshipType.SEGMENT_CONNECTION_ON_STARTNODE);
            		addRelationships(result, path, Direction.OUTGOING, WaySegmentRelationshipType.SEGMENT_CONNECTION_ON_ENDNODE);
            		addRelationships(result, path, Direction.OUTGOING, WaySegmentRelationshipType.SEGMENT_CONNECTION_WITHOUT_NODE);
            	}
        	} else {
        		if (lastRelationshipViaNodeId.getProperty(WayGraphConstants.CONNECTION_NODE_ID) != null) {
	        		if (lastRelationshipViaNodeId.getProperty(WayGraphConstants.CONNECTION_NODE_ID)
	        				.equals(lastRelationshipViaNodeId.getEndNode().getProperty(WayGraphConstants.SEGMENT_STARTNODE_ID))) {
		        		addRelationships(result, path, Direction.OUTGOING, WaySegmentRelationshipType.SEGMENT_CONNECTION_ON_ENDNODE);
		        		addRelationships(result, path, Direction.OUTGOING, WaySegmentRelationshipType.SEGMENT_CONNECTION_WITHOUT_NODE);
		            } else if (lastRelationshipViaNodeId.getProperty(WayGraphConstants.CONNECTION_NODE_ID)
		            		.equals(lastRelationshipViaNodeId.getEndNode().getProperty(WayGraphConstants.SEGMENT_ENDNODE_ID))) {
		        		addRelationships(result, path, Direction.OUTGOING, WaySegmentRelationshipType.SEGMENT_CONNECTION_ON_STARTNODE);
		        		addRelationships(result, path, Direction.OUTGOING, WaySegmentRelationshipType.SEGMENT_CONNECTION_WITHOUT_NODE);
		            }
        		}
            } 
        }
        return result;
    }

    // some Path's implementation throw an exception within lastRelationship()
    private Relationship getLastRelationship(Path path) {
    	Relationship lastRel = null;
    	try {
			lastRel = path.lastRelationship();
		} catch (Exception e) {
			// do nothing
		}
    	return lastRel;
    }
    
    /**
     * @param path
     * @return the last relationship where type is SEGMENT_CONNECTION_ON_STARTNODE or SEGMENT_CONNECTION_ON_ENDNODE
     */
    private Relationship getLastRelationshipViaNodeId(Path path) {
    	Relationship lastRel = null;
    	try {
    		Iterator<Relationship> iter = path.reverseRelationships().iterator();
    		while (iter.hasNext()) {
    			Relationship rel = iter.next();
    			if (!rel.isType(WaySegmentRelationshipType.SEGMENT_CONNECTION_WITHOUT_NODE)) {
    				lastRel = rel;
    			}
    		}
		} catch (Exception e) {
			// do nothing
		}
    	return lastRel;
    }
    
    private void addRelationships(List<Relationship> result, Path path, Direction outgoing,
			WaySegmentRelationshipType initialRelationType) {
		Iterable<Relationship> res = path.endNode().getRelationships(Direction.OUTGOING, initialRelationType);
		if (res != null) {
			if (initialRelationType.equals(WaySegmentRelationshipType.SEGMENT_CONNECTION_WITHOUT_NODE)) {
				Iterator<Relationship> iterator = res.iterator();
				while (iterator.hasNext()) {
					Relationship rel = iterator.next();
					if (rel.isType(WaySegmentRelationshipType.SEGMENT_CONNECTION_WITHOUT_NODE)) {
						if (rel.hasProperty(WayGraphConstants.CONNECTION_TAG_PREFIX.concat(WayGraphConstants.CONNECTION_TYPE))) {
							if (rel.getProperty(WayGraphConstants.CONNECTION_TAG_PREFIX.concat(WayGraphConstants.CONNECTION_TYPE))
									.equals(WayGraphConstants.CONNECTION_TYPE_CONNECTS_FORBIDDEN)) {
								// Do not add relationship that represents forbidden connection
								continue;
							}
							if (rel.hasProperty(WayGraphConstants.CONNECTION_TAG_PREFIX.concat(WayGraphConstants.CONNECTION_DIRECTION)) 
									&& rel.getProperty(WayGraphConstants.CONNECTION_TAG_PREFIX.concat(WayGraphConstants.CONNECTION_DIRECTION))
									.equals(WayGraphConstants.CONNECTION_DIRECTION_REVERSE)) {
								// Do not add relationship that represents connection to lane with reverse driving direction
								continue;
							}
							result.add(rel);
						}
					} else {
						result.add(rel);
					}
				}
			} else {
				res.forEach(result::add);
			}
		}
	}

	@Override
    public PathExpander<STATE> reverse() {
    	// TODO: 
        return this;
    }


    public WaySegmentRelationshipType getInitialRelation() {
    	return this.initialRelation;
    }

    public void setInitialRelation(WaySegmentRelationshipType initialRelation) {
    	this.initialRelation = initialRelation;
    }

}
