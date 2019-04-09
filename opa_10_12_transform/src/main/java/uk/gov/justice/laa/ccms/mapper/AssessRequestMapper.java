package uk.gov.justice.laa.ccms.mapper;

import java.util.ArrayList;
import java.util.List;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.oracle.determinations.server._10_0.rulebase.types.Attribute;
import com.oracle.determinations.server._10_0.rulebase.types.AttributeDecisionNode;
import com.oracle.determinations.server._10_0.rulebase.types.AttributeOutcome;
import com.oracle.determinations.server._10_0.rulebase.types.DecisionReport;
import com.oracle.determinations.server._10_0.rulebase.types.Entity;
import com.oracle.determinations.server._10_0.rulebase.types.ListEntity;
import com.oracle.determinations.server._10_0.rulebase.types.Relationship;
import com.oracle.determinations.server._10_0.rulebase.types.RelationshipDecisionNode;
import com.oracle.determinations.server._10_0.rulebase.types.RelationshipTarget;
import com.oracle.determinations.server._10_0.rulebase.types.Session;
import com.oracle.determinations.server._12_2.rulebase.assess.types.AlreadyProvenNodeType;
import com.oracle.determinations.server._12_2.rulebase.assess.types.AssessRequest;
import com.oracle.determinations.server._12_2.rulebase.assess.types.AssessmentConfiguration;
import com.oracle.determinations.server._12_2.rulebase.assess.types.AttributeNodeType;
import com.oracle.determinations.server._12_2.rulebase.assess.types.AttributeType;
import com.oracle.determinations.server._12_2.rulebase.assess.types.DecisionReportType;
import com.oracle.determinations.server._12_2.rulebase.assess.types.EntityInstanceType;
import com.oracle.determinations.server._12_2.rulebase.assess.types.EntityType;
import com.oracle.determinations.server._12_2.rulebase.assess.types.GlobalInstanceType;
import com.oracle.determinations.server._12_2.rulebase.assess.types.RelationshipNodeType;
import com.oracle.determinations.server._12_2.rulebase.assess.types.RelationshipTargetType;
import com.oracle.determinations.server._12_2.rulebase.assess.types.RelationshipType;
import com.oracle.determinations.server._12_2.rulebase.assess.types.UncertainValue;
import com.oracle.determinations.server._12_2.rulebase.assess.types.UnknownValue;

@Mapper(componentModel = "spring")
public interface AssessRequestMapper {

   @Mapping(target = "hypotheticalInstance", ignore = true)
   @Mapping(target = "instanceId", ignore = true)
   @Mapping(target = "inferred", ignore = true)
   @Mapping(target = "relationshipNodeOrAttributeNodeOrAlreadyProvenNode", source = "attributeDecisionNodeOrRelationshipDecisionNode")
   AttributeNodeType map(AttributeDecisionNode attributeDecisionNode);

   @Mapping(target = "booleanVal", ignore = true)
   @Mapping(target = "dateVal", ignore = true)
   @Mapping(target = "datetimeVal", ignore = true)
   @Mapping(target = "decisionReport", ignore = true)
   @Mapping(target = "inferred", ignore = true)
   @Mapping(target = "numberVal", ignore = true)
   @Mapping(target = "properties", ignore = true)
   @Mapping(target = "textVal", ignore = true)
   @Mapping(target = "type", ignore = true)
   @Mapping(target = "uncertainVal", ignore = true)
   @Mapping(target = "unknownVal", ignore = true)
   @Mapping(target = "changePoint", ignore = true)
   @Mapping(target = "timeVal", ignore = true)
   AttributeType map(AttributeOutcome attributeOutcome);

   @Mapping(target = "hypotheticalInstance", ignore = true)
   @Mapping(target = "inferred", ignore = true)
   @Mapping(target = "startRelevance", ignore = true)
   @Mapping(target = "endRelevance", ignore = true)
   @Mapping(target = "sourceEntityId", ignore = true)
   @Mapping(target = "targetEntityId", ignore = true)
   @Mapping(target = "relationshipId", source = "relationshipName")
   @Mapping(target = "relationshipNodeOrAttributeNodeOrAlreadyProvenNode", source = "attributeDecisionNodeOrRelationshipDecisionNode")
   @Mapping(target = "sourceInstanceId", source = "source")
   RelationshipNodeType map(RelationshipDecisionNode relationshipDecisionNode);

   @Mapping(target = "relationshipNodeOrAttributeNodeOrAlreadyProvenNode", source = "relationshipDecisionNodeOrAttributeDecisionNode")
   DecisionReportType map(DecisionReport decisionReport);

   @Mapping(target = "knownOutcomeStyle", ignore = true)
   @Mapping(target = "outcomeStyle", ignore = true)
   @Mapping(target = "unknownOutcomeStyle", ignore = true)
   @Mapping(target = "id", source = "name")
   RelationshipType map(Relationship relationship);

   @Mapping(target = "instanceId", source = "entityId")
   RelationshipTargetType map(RelationshipTarget relationshipTarget);

   @Mapping(target = "knownOutcomeStyle", ignore = true)
   @Mapping(target = "outcomeStyle", ignore = true)
   @Mapping(target = "unknownOutcomeStyle", ignore = true)
   @Mapping(target = "inferred", ignore = true)
   AttributeType map(Attribute attribute);

   List<AttributeType> map(List<Attribute> attributes);

   @Mapping(target = "entity", ignore = true)
   @Mapping(target = "relationship", ignore = true)
   EntityInstanceType map(Entity entity);

   @Mapping(target = "instance", source = "entity")
   @Mapping(target = "id", source = "entityType")
   @Mapping(target = "properties", ignore = true)
   @Mapping(target = "inferred", ignore = true)
   EntityType map(ListEntity listEntity);

   @Mapping(target = "properties", ignore = true)
   @Mapping(target = "attribute", ignore = true)
   @Mapping(target = "relationship", ignore = true)
   @Mapping(target = "entity", source = "listEntity")
   GlobalInstanceType map(Session session);

   UncertainValue map(com.oracle.determinations.server._10_0.rulebase.types.UncertainValue value);

   UnknownValue map(com.oracle.determinations.server._10_0.rulebase.types.UnknownValue value);

   @Mapping(target = "outcome", ignore = true)
   @Mapping(target = "showVersion", ignore = true)
   AssessmentConfiguration map(com.oracle.determinations.server._10_0.rulebase.types.AssessmentConfiguration assessmentConfiguration);

   @Mapping(target = "globalInstance", source = "sessionData")
   AssessRequest map(com.oracle.determinations.server._10_0.rulebase.types.AssessRequest assessRequest);
   
   @AfterMapping
   default void addOutcomeAttribute(@MappingTarget EntityInstanceType entityInstanceType, Entity entity) {
      for (AttributeOutcome attributeOutcome : entity.getAttributeOutcome()) {
         entityInstanceType.getAttribute().add(map(attributeOutcome));
      }
   }

   default List<Object> mapObjects(List<Object> objects) {
      List<Object> returnObjects = new ArrayList<Object>();

      for (Object object : objects) {
         if (object instanceof AttributeDecisionNode) {
            returnObjects.add(map((AttributeDecisionNode) object));
         } else if (object instanceof RelationshipDecisionNode) {
            returnObjects.add(map((RelationshipDecisionNode) object));
         } else if (object instanceof AlreadyProvenNodeType) {
            // ignore as there is no corresponding object in the opa10 response
         } else {
            returnObjects.add(object);
         }
      }
      return returnObjects;
   }

   default List<RelationshipTargetType> map(String target) {

      if (target == null) {
         return null;
      }

      List<RelationshipTargetType> relationshipTargetTypes = new ArrayList<RelationshipTargetType>();

      RelationshipTargetType relationshipTargetType = new RelationshipTargetType();
      relationshipTargetType.setInstanceId(target);
      relationshipTargetTypes.add(relationshipTargetType);
      return relationshipTargetTypes;
   }
}
