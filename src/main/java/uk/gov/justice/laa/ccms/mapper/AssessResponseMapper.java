package uk.gov.justice.laa.ccms.mapper;

import com.oracle.determinations.server._10_0.rulebase.types.AssessResponse;
import com.oracle.determinations.server._10_0.rulebase.types.Attribute;
import com.oracle.determinations.server._10_0.rulebase.types.AttributeDecisionNode;
import com.oracle.determinations.server._10_0.rulebase.types.DecisionReport;
import com.oracle.determinations.server._10_0.rulebase.types.Entity;
import com.oracle.determinations.server._10_0.rulebase.types.ListEntity;
import com.oracle.determinations.server._10_0.rulebase.types.Relationship;
import com.oracle.determinations.server._10_0.rulebase.types.RelationshipDecisionNode;
import com.oracle.determinations.server._10_0.rulebase.types.RelationshipTarget;
import com.oracle.determinations.server._10_0.rulebase.types.Session;
import com.oracle.determinations.server._12_2_1.rulebase.assess.types.AlreadyProvenNodeType;
import com.oracle.determinations.server._12_2_1.rulebase.assess.types.AssessmentConfiguration;
import com.oracle.determinations.server._12_2_1.rulebase.assess.types.AttributeNodeType;
import com.oracle.determinations.server._12_2_1.rulebase.assess.types.AttributeType;
import com.oracle.determinations.server._12_2_1.rulebase.assess.types.DecisionReportType;
import com.oracle.determinations.server._12_2_1.rulebase.assess.types.EntityInstanceType;
import com.oracle.determinations.server._12_2_1.rulebase.assess.types.EntityType;
import com.oracle.determinations.server._12_2_1.rulebase.assess.types.GlobalInstanceType;
import com.oracle.determinations.server._12_2_1.rulebase.assess.types.RelationshipNodeType;
import com.oracle.determinations.server._12_2_1.rulebase.assess.types.RelationshipTargetType;
import com.oracle.determinations.server._12_2_1.rulebase.assess.types.RelationshipType;
import com.oracle.determinations.server._12_2_1.rulebase.assess.types.UncertainValue;
import com.oracle.determinations.server._12_2_1.rulebase.assess.types.UnknownValue;
import java.util.ArrayList;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AssessResponseMapper {

  @Mapping(target = "alreadyProven", ignore = true)
  @Mapping(target = "entityType", source = "entityId")
  @Mapping(target = "entityId", source = "instanceId")
  @Mapping(target = "inferencingType", expression = "java(com.oracle.determinations.server._10_0.rulebase.types.InferencingTypeEnum.GOAL)")
  @Mapping(target = "attributeDecisionNodeOrRelationshipDecisionNode", source = "relationshipNodeOrAttributeNodeOrAlreadyProvenNode")
  AttributeDecisionNode map(AttributeNodeType attributeDecisionNode);

  @Mapping(target = "alreadyProven", ignore = true)
  @Mapping(target = "source", source = "sourceInstanceId")
  @Mapping(target = "relationshipName", source = "relationshipId")
  @Mapping(target = "attributeDecisionNodeOrRelationshipDecisionNode", source = "relationshipNodeOrAttributeNodeOrAlreadyProvenNode")
  @Mapping(target = "target", ignore = true)
  RelationshipDecisionNode map(RelationshipNodeType relationshipNodeType);

  @Mapping(target = "outcomeId", ignore = true)
  @Mapping(target = "relationshipDecisionNodeOrAttributeDecisionNode", source = "relationshipNodeOrAttributeNodeOrAlreadyProvenNode")
  DecisionReport map(DecisionReportType decisionReportType);

  @Mapping(target = "name", source = "id")
  Relationship map(RelationshipType relationship);

  @Mapping(target = "entityId", source = "instanceId")
  RelationshipTarget map(RelationshipTargetType relationshipTargetType);

  @Mapping(target = "userData", ignore = true)
  @Mapping(target = "screen", ignore = true)
  @Mapping(target = "inferencingType", expression = "java(com.oracle.determinations.server._10_0.rulebase.types.InferencingTypeEnum.GOAL)")
  Attribute map(AttributeType attribute);

  @Mapping(target = "properties", ignore = true)
  @Mapping(target = "attributeOutcome", ignore = true)
  @Mapping(target = "relationships.relationship", source = "relationship")
  Entity map(EntityInstanceType entity);

  @Mapping(target = "collected", ignore = true)
  @Mapping(target = "entity", source = "instance")
  @Mapping(target = "entityType", source = "id")
  ListEntity map(EntityType listEntity);

  com.oracle.determinations.server._10_0.rulebase.types.UncertainValue map(UncertainValue value);

  com.oracle.determinations.server._10_0.rulebase.types.UnknownValue map(UnknownValue value);

  @Mapping(target = "listEntity", source = "entity")
  Session map(GlobalInstanceType session);

  com.oracle.determinations.server._10_0.rulebase.types.AssessmentConfiguration map(
      AssessmentConfiguration assessmentConfiguration);

  @Mapping(target = "sessionData", source = "globalInstance")
  AssessResponse map(
      com.oracle.determinations.server._12_2_1.rulebase.assess.types.AssessResponse assessResponse);

  default List<Object> mapObjects(List<Object> objects) {
    List<Object> returnObjects = new ArrayList<Object>();

    for (Object object : objects) {
      if (object instanceof AttributeNodeType type1) {
        returnObjects.add(map(type1));
      } else if (object instanceof RelationshipNodeType type) {
        returnObjects.add(map(type));
      } else if (object instanceof AlreadyProvenNodeType) {
        // ignore as there is no corresponding object in the opa10 response
      } else {
        returnObjects.add(object);
      }
    }
    return returnObjects;
  }
}
