package uk.gov.justice.laa.ccms.mapper;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableMap;
import com.oracle.determinations.server._12_2.rulebase.assess.types.AssessRequest;
import com.oracle.determinations.server._12_2.rulebase.assess.types.AssessResponse;
import com.oracle.determinations.server._12_2.rulebase.assess.types.AttributeType;
import com.oracle.determinations.server._12_2.rulebase.assess.types.EntityInstanceType;
import com.oracle.determinations.server._12_2.rulebase.assess.types.EntityType;

@Component
public class EntityLevelRelocationServiceImpl implements EntityLevelRelocationService {

   private static final Map<String, String> SUBENTITY_MAP = ImmutableMap.<String, String>builder()
            .put("ADDTHIRD", "ADDPROPERTY")
            .put("BPFAMILYEMPL", "BUSINESSPART")
            .put("BPPROPERTY", "BUSINESSPART")
            .put("BUSPARTBANK", "BUSINESSPART")
            .put("CLI_NON_HM_L17", "EMPLOYMENT_CLIENT")
            .put("CLI_NON_HM_WAGE_SLIP", "EMPLOYMENT_CLIENT")
            .put("COPROPERTY", "COMPANY")
            .put("EMPLOY_BEN_CLIENT", "EMPLOYMENT_CLIENT")
            .put("EMPLOY_BEN_PARTNER", "EMPLOYMENT_PARTNER")
            .put("EMP_CLI_KNOWN_CHANGE", "EMPLOYMENT_CLIENT")
            .put("PAR_EMPLOY_KNOWN_CHANGE", "EMPLOYMENT_PARTNER")
            .put("PAR_NON_HM_L17", "EMPLOYMENT_PARTNER")
            .put("PAR_NON_HM_WAGE_SLIP", "EMPLOYMENT_PARTNER")
            .put("SEFAMILYEMPL", "SELFEMPLOY")
            .put("SELFEMPBANK", "SELFEMPLOY")
            .put("SEPROPERTY", "SELFEMPLOY")
            .put("SHARE", "COMPANY")
            .build();

   @Override
   public String getGlobalEntityId(com.oracle.determinations.server._10_0.rulebase.types.AssessRequest request) {
      return request.getSessionData().getListEntity()
               .stream()
               .filter(entity -> entity.getEntityType().equalsIgnoreCase("global"))
               .findFirst().map(e -> e.getEntity().get(0).getId())
               .get();

   }

   @Override
   public void moveGlobalAttributesAndRelationsToBaseLevel(AssessRequest request) {
      List<AttributeType> attributeTypes = request.getGlobalInstance().getEntity()
               .stream()
               .filter(entity -> entity.getId().equalsIgnoreCase("global"))
               .map(entity -> entity.getInstance())
               .flatMap(el -> el.stream())
               .map(e -> e.getAttribute())
               .flatMap(al -> al.stream())
               .collect(Collectors.toList());

      request.getGlobalInstance().getAttribute().addAll(attributeTypes);

      request.getGlobalInstance().getEntity()
               .removeIf(list -> list.getId().equalsIgnoreCase("global"));

   }

   @Override
   public void moveSubEntitiesToLowerLevel(AssessRequest request) {

      List<EntityType> subEntities = request.getGlobalInstance().getEntity()
               .stream()
               .filter(entity -> SUBENTITY_MAP.keySet().contains(entity.getId()))
               .collect(Collectors.toList());
      if (subEntities == null) {
         return;
      }

      for (EntityType subEntity : subEntities) {
         List<EntityType> parentEntities = request.getGlobalInstance().getEntity()
                  .stream()
                  .filter(parent -> SUBENTITY_MAP.get(subEntity.getId()).equals(parent.getId()))
                  .collect(Collectors.toList());
         if (parentEntities.size() != 1) {
            throw new IllegalStateException();
         }

         parentEntities.get(0).getInstance().get(0).getEntity().add(subEntity);
      }

      request.getGlobalInstance().getEntity()
               .removeIf(entity -> subEntities.contains(entity));
   }

   @Override
   public void moveGlobalEntityToLowerLevel(AssessResponse response, String globalEntityId) {

      EntityType entityType = new EntityType();
      entityType.setId("global");
      EntityInstanceType entityInstanceType = new EntityInstanceType();
      entityInstanceType.setId(globalEntityId);
      entityInstanceType.getAttribute().addAll(response.getGlobalInstance().getAttribute());
      entityInstanceType.getRelationship().addAll(response.getGlobalInstance().getRelationship());

      entityType.getInstance().add(entityInstanceType);

      response.getGlobalInstance().getEntity().add(entityType);
   }

   @Override
   public void moveSubEntitiesToUpperLevel(AssessResponse response) {

      List<EntityType> nestedEntities = response.getGlobalInstance().getEntity()
               .stream()
               .map(entityType -> entityType.getInstance())
               .flatMap(el -> el.stream())
               .map(entityInstanceType -> entityInstanceType.getEntity())
               .flatMap(entityTypeList -> entityTypeList.stream())
               .collect(Collectors.toList());

      response.getGlobalInstance().getEntity().addAll(nestedEntities);

      // we do not need to remove these from nested level as the mapstruct mapping takes care for this

   }
}
