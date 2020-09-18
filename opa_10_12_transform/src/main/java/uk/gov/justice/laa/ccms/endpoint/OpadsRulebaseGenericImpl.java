package uk.gov.justice.laa.ccms.endpoint;

import com.oracle.determinations.server._10_0.rulebase.types.AttributeOutcome;
import com.oracle.determinations.server._10_0.rulebase.types.Entity;
import com.oracle.determinations.server._10_0.rulebase.types.ListEntity;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.oracle.determinations.server._10_0.rulebase.types.AssessRequest;
import com.oracle.determinations.server._10_0.rulebase.types.AssessResponse;
import com.oracle.determinations.server._10_0.rulebase.types.ErrorResponse;
import com.oracle.determinations.server._10_0.rulebase.types.OpadsRulebaseGeneric;
import com.oracle.determinations.server._12_2.rulebase.assess.types.OdsAssessServiceGeneric122MeansAssessmentV12Type;

import uk.gov.justice.laa.ccms.mapper.AssessRequestMapper;
import uk.gov.justice.laa.ccms.mapper.AssessResponseMapper;
import uk.gov.justice.laa.ccms.mapper.EntityLevelRelocationService;

@Component
public class OpadsRulebaseGenericImpl implements OpadsRulebaseGeneric {

   private static final Logger logger = LoggerFactory.getLogger(OpadsRulebaseGenericImpl.class);

   @Autowired
   private AssessRequestMapper assessRequestMapper;

   @Autowired
   private AssessResponseMapper assessResponseMapper;

   @Autowired
   private OdsAssessServiceGeneric122MeansAssessmentV12Type opa12MeansAssessServiceProxy;

   @Autowired
   private OdsAssessServiceGeneric122MeansAssessmentV12Type opa12BillingAssessServiceProxy;

   @Autowired
   private EntityLevelRelocationService entityLevelRelocationService;

   
   @Override
   public AssessResponse assess(AssessRequest assessRequest) throws ErrorResponse {

      logger.debug("Assess Service" + assessRequest.toString());

      com.oracle.determinations.server._12_2.rulebase.assess.types.AssessRequest request = assessRequestMapper.map(assessRequest);

      entityLevelRelocationService.moveGlobalAttributesAndRelationsToBaseLevel(request);

      entityLevelRelocationService.moveSubEntitiesToLowerLevel(request);

      com.oracle.determinations.server._12_2.rulebase.assess.types.AssessResponse assess12Response;
      if(isMeansAssessment(assessRequest)) {
         assess12Response = opa12MeansAssessServiceProxy.assess(request);
      } else {
         assess12Response = opa12BillingAssessServiceProxy.assess(request);
      }

      entityLevelRelocationService.moveGlobalEntityToLowerLevel(assess12Response, entityLevelRelocationService.getGlobalEntityId(assessRequest));

      entityLevelRelocationService.moveSubEntitiesToUpperLevel(assess12Response);

      AssessResponse response = assessResponseMapper.map(assess12Response);
      
      entityLevelRelocationService.mapOpa10Relationships(response);

      return response;
   }

   boolean isMeansAssessment(AssessRequest assessRequest) {
      List<ListEntity> listEntityList = assessRequest.getSessionData().getListEntity();
      for (ListEntity listEntity : listEntityList) {
         List<Entity> entityList = listEntity.getEntity();
         for(Entity entity : entityList) {
            List<AttributeOutcome> attributes = entity.getAttributeOutcome();
            for (AttributeOutcome attributeOutcome : attributes) {
               if(attributeOutcome.getId().equals("MEANS_CALCULATIONS")) {
                  return true;
               }
            }
         }
      }
      return false;
   }

}
