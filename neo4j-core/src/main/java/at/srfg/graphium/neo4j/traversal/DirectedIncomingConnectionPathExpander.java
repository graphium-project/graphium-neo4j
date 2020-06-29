/**
 * Graphium Neo4j - Module of Graphserver for Neo4j extension
 * Copyright © 2019 Salzburg Research Forschungsgesellschaft (graphium@salzburgresearch.at)
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

public class DirectedIncomingConnectionPathExpander<STATE> implements IDirectedConnectionPathExpander<STATE> {

    protected Logger log = Logger.getLogger(this.getClass().getName());

    protected WaySegmentRelationshipType initialRelation;
    protected boolean init = true;
	protected List<Predicate<? super Node>> nodeFilters = new ArrayList<>();
	protected List<Predicate<? super Relationship>> relationshipFilters = new ArrayList<>();

    public DirectedIncomingConnectionPathExpander(WaySegmentRelationshipType initialRelation) {
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
        
        Node startNode = path.startNode();
		for (Predicate<? super Node> nodeFilter : nodeFilters) {
			if (!nodeFilter.test(startNode)) {
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
        Long connectedNodeId = null;
    	if (lastRel != null) {
    		connectedNodeId = (Long) lastRel.getProperty(WayGraphConstants.CONNECTION_NODE_ID);
    	}
    	addRelationships(result, path, connectedNodeId);
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
    
    private void addRelationships(List<Relationship> result, Path path, Long connectedNodeId) {
		Iterable<Relationship> res = path.startNode().getRelationships(Direction.INCOMING,
				WaySegmentRelationshipType.SEGMENT_CONNECTION_ON_STARTNODE,
				WaySegmentRelationshipType.SEGMENT_CONNECTION_ON_ENDNODE);
		if (res != null) {
			for (Relationship r : res) {
				long relNodeId = (Long) r.getProperty(WayGraphConstants.CONNECTION_NODE_ID);
				if (connectedNodeId == null || connectedNodeId != relNodeId) {
					result.add(r);
					
					if (log.isDebugEnabled()) {
						if (r.getStartNode() != null && r.getEndNode() != null) {
							log.debug(r.getStartNode().getProperty(WayGraphConstants.SEGMENT_ID)
									+ "[" + r.getStartNode().getId() + "]"
									+ " -> " 
									+ r.getEndNode().getProperty(WayGraphConstants.SEGMENT_ID)
									+ "[" + r.getEndNode().getId() + "]");
						}
					}
				}
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
