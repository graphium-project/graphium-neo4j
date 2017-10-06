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

import at.srfg.graphium.neo4j.model.WayGraphConstants;
import at.srfg.graphium.neo4j.model.WaySegmentRelationshipType;
import org.apache.log4j.Logger;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.BranchState;

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

    private WaySegmentRelationshipType initialRelation;
    private boolean init = true;

    public DirectedOutgoingConnectionPathExpander(WaySegmentRelationshipType initialRelation) {
    	this.initialRelation = initialRelation;
    }
    
    @Override
    public Iterable<Relationship> expand(Path path, BranchState<STATE> state) {
        WaySegmentRelationshipType initialRelationType = null;
        if (init) {
            initialRelationType = this.initialRelation;
            init = false;
        }
        return doExpand(path, initialRelationType);      
    }


    private Iterable<Relationship> doExpand(Path path, WaySegmentRelationshipType initialRelationType) {
        Iterable<Relationship> result;
        if (path.lastRelationship() == null) {
            result = path.endNode().getRelationships(Direction.OUTGOING, initialRelationType);
        } else if (path.lastRelationship().getProperty(WayGraphConstants.CONNECTION_NODE_ID).equals
                (path.endNode().getProperty(WayGraphConstants.SEGMENT_STARTNODE_ID))) {
            result = path.endNode().getRelationships(Direction.OUTGOING, WaySegmentRelationshipType.SEGMENT_CONNECTION_ON_ENDNODE);
        } else {
            result = path.endNode().getRelationships(Direction.OUTGOING, WaySegmentRelationshipType.SEGMENT_CONNECTION_ON_STARTNODE);
        }
        return result;
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
