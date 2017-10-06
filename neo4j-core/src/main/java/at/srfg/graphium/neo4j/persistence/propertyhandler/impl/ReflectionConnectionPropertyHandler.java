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
import org.neo4j.graphdb.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This is a generic property setter, that writes all fields of the connectionXInfo to the node connection xinfo. There
 * are no generics in the constructor so that it can be instantiated as spring bean and no additional inherited class
 * is needed.
 *
 * Created by shennebe on 07.10.2016.
 */
public class ReflectionConnectionPropertyHandler extends AbstractConnectionPropertyHandler<IConnectionXInfo> {

    private static Logger log = LoggerFactory.getLogger(ReflectionConnectionPropertyHandler.class);

    public ReflectionConnectionPropertyHandler(IConnectionXInfo object) {
        super(object);
    }

    @Override
    public void setXInfoProperties(IConnectionXInfo xInfo, String groupKey, Relationship connectionRelationship) {
        if (xInfo != null && connectionRelationship != null) {
            Field[] fields = xInfo.getClass().getDeclaredFields();
            for (Field field : fields) {
                try {
                	// TODO: ist das gut hier ein Accessible zu setzen? Das würde doch eine private methode zu public machen und 
                	// nie wieder zurück tauschen?
                    field.setAccessible(true);
                    this.addAttribute(connectionRelationship,groupKey,field.getName(),field.get(xInfo));
                } catch (IllegalAccessException e) {
                    log.error("Illegal access of xInfo Property: " + field.getName() + " from xinfo " + xInfo.getXInfoType());
                }
            }
        }
    }

    @Override
    public IConnectionXInfo getXInfoProperty(Relationship connectionRelationship, String groupKey) {
        Map<String,Object> attributeMap = this.getMappingForGroupKey(connectionRelationship.getAllProperties(),groupKey);
        try {
            IConnectionXInfo xInfo = this.getXInfoClass().newInstance();
            xInfo.setGroupKey(groupKey);
            return this.setAttributesToField(attributeMap,xInfo);            
        } catch (InstantiationException | IllegalAccessException e) {
            log.error("XInfo " + this.getXInfoClass() + " cannot be instantiated");
        }
        return null;
    }


    @Override
    public List<IConnectionXInfo> getXInfoProperties(Relationship connectionRelationship) {
        Map<String,Map<String,Object>> attributeList = this.getMappingForType(connectionRelationship.getAllProperties());
        final List<IConnectionXInfo> resultList = new ArrayList<>(attributeList.size());
        attributeList.forEach((groupKey,attributeMap) -> {
            try {
                IConnectionXInfo xInfo = this.getXInfoClass().newInstance();
                xInfo.setGroupKey(groupKey);
                resultList.add(this.setAttributesToField(attributeMap,xInfo));
            } catch (InstantiationException | IllegalAccessException e) {
                log.error("XInfo " + this.getXInfoClass() + " cannot be instantiated");
            }
        });
        return resultList;
    }

    private IConnectionXInfo setAttributesToField(Map<String,Object> attributeMap, final IConnectionXInfo xInfo) {
        attributeMap.forEach((key,value) -> {
            try {
                Field field = this.getXInfoClass().getDeclaredField(key);
                field.setAccessible(true);
                field.set(xInfo,value);
            } catch (NoSuchFieldException e) {
                log.error("Field " + key + " of class " + this.getResponsibleType() + " not found!");
            } catch (IllegalAccessException e) {
                log.error("Illegal access of field  " + key);
            }
        });
        return xInfo;
    }
}
