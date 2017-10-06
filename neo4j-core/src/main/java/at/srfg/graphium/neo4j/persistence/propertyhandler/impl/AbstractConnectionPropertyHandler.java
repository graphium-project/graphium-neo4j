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
package at.srfg.graphium.neo4j.persistence.propertyhandler.impl;

import at.srfg.graphium.model.IConnectionXInfo;
import at.srfg.graphium.neo4j.persistence.propertyhandler.IConnectionXInfoPropertyHandler;
import org.neo4j.graphdb.Relationship;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author mwimmer
 *
 */
public abstract class AbstractConnectionPropertyHandler<X extends IConnectionXInfo>
        implements IConnectionXInfoPropertyHandler<X> {

    private String xInfoType;
    private Class<? extends IConnectionXInfo> xInfoClass;

    private static final int TYPE_POSITION = 0;
    private static final int GROUP_KEY_POSITION = 1;
    private static final int ATTRIBUTE_POSITION = 2;

    public AbstractConnectionPropertyHandler(X object) {
        xInfoType = object.getXInfoType();
        xInfoClass = object.getClass();
    }

    @Override
    public String getResponsibleType() {
        return this.xInfoType;
    }

    @Override
    public Class<? extends IConnectionXInfo> getXInfoClass() {
        return this.xInfoClass;
    }

    public int deleteProperties(Relationship connectionRelationship, String groupKey) {
        int count = 0;
        for (String key : connectionRelationship.getAllProperties().keySet()) {
            String[] splitKey = splitKey(key);
            if (this.getResponsibleType().equals(splitKey[TYPE_POSITION])
                    && (groupKey == null || groupKey.equals(splitKey[GROUP_KEY_POSITION]))) {
                connectionRelationship.removeProperty(key);
                count++;
            }
        }
        return count;
    }

    /**
     * This method splits the composed key into groupKey and Key Value pairs. One groupKey relates to one object
     * that can be created
     *
     * @param relationShipProperties the properties of this neo4j relationship
     * @return a map of properties
     */
    protected Map<String, Map<String, Object>> getMappingForType(Map<String, Object> relationShipProperties) {
        final Map<String, Map<String, Object>> resultPropertyGroupedMap = new HashMap<>();
        relationShipProperties.forEach((key, value) -> {
            String[] splitKey = splitKey(key);
            if (this.getResponsibleType().equals(splitKey[TYPE_POSITION])) {
                Map<String, Object> resultPropertyMap = resultPropertyGroupedMap.get(splitKey[GROUP_KEY_POSITION]);
                if (resultPropertyMap == null) {
                    resultPropertyMap = new HashMap<>();
                    resultPropertyGroupedMap.put(splitKey[GROUP_KEY_POSITION], resultPropertyMap);
                }
                resultPropertyMap.put(splitKey[ATTRIBUTE_POSITION], value);
            }
        });
        return resultPropertyGroupedMap;
    }

    /**
     * Returns all attributes for this type and group according to the DB relation key
     *
     * @param relationShipProperties the relationship properties
     * @param groupKey the key to be grouped
     * @return a map of all objects
     */
    protected Map<String,Object> getMappingForGroupKey(Map<String,Object> relationShipProperties,
                                                       String groupKey) {
        final Map<String,Object> resultPropertyMap = new HashMap<>();
        relationShipProperties.forEach((key,value) -> {
            String[] splitKey = splitKey(key);
            if (this.getResponsibleType().equals(splitKey[TYPE_POSITION])
                    && groupKey.equals(splitKey[GROUP_KEY_POSITION])) {
                resultPropertyMap.put(splitKey[ATTRIBUTE_POSITION],value);
            }
        });
        return resultPropertyMap;
    }

    protected void addAttribute(Relationship connectionRelationship, String groupKey, String attibuteName, Object value) {
        connectionRelationship.setProperty(this.createKey(groupKey,attibuteName),value);
    }

    private static String[] splitKey(String key) {
        if (key != null) {
            String[] splitted =  key.split(":");
            if (splitted.length == 3) {
                return splitted;
            }
        }
        return new String[]{null,null,null};
    }

    String createKey(String groupKey, String attribute) {
        return this.getResponsibleType() + ":" + groupKey + ":" + attribute;
    }

    public static List<String> getTypesOnRelationship(final Relationship relationship) {
        List<String> resultList = new ArrayList<>();
        relationship.getAllProperties().keySet().forEach((key) -> {
            String type = splitKey(key)[0];
            if (type != null) {
                resultList.add(type);
            }
        });
        return resultList;
    }

}