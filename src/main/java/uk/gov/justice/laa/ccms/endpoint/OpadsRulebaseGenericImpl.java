package uk.gov.justice.laa.ccms.endpoint;

import com.oracle.determinations.server._10_0.rulebase.types.AssessRequest;
import com.oracle.determinations.server._10_0.rulebase.types.AssessResponse;
import com.oracle.determinations.server._10_0.rulebase.types.AttributeOutcome;
import com.oracle.determinations.server._10_0.rulebase.types.Entity;
import com.oracle.determinations.server._10_0.rulebase.types.ErrorResponse;
import com.oracle.determinations.server._10_0.rulebase.types.ListEntity;
import com.oracle.determinations.server._10_0.rulebase.types.OpadsRulebaseGeneric;
import com.oracle.determinations.server._12_2.rulebase.assess.types.AttributeType;
import com.oracle.determinations.server._12_2.rulebase.assess.types.OdsAssessServiceGeneric122MeansAssessmentV12Type;
import com.oracle.determinations.server._12_2.rulebase.assess.types.OutcomeStyleEnum;
import java.util.List;
import org.apache.cxf.binding.soap.SoapFault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.ccms.mapper.AssessRequestMapper;
import uk.gov.justice.laa.ccms.mapper.AssessResponseMapper;
import uk.gov.justice.laa.ccms.mapper.EntityLevelRelocationService;
import uk.gov.justice.laa.ccms.service.DecisionReportTransformation;
import uk.gov.justice.laa.ccms.service.OpaErrorResponseTransformation;

;

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

  @Autowired
  private DecisionReportTransformation decisionReportTransformation;

  @Autowired
  private OpaErrorResponseTransformation opaErrorResponseTransformation;

  private final String MEANS_CALCULATIONS = "MEANS_CALCULATIONS";
  private final String MEANS_OUTPUTS = "MEANS_OUTPUTS";
  private boolean isMeans = false;


  @Override
  public AssessResponse assess(AssessRequest assessRequest) throws ErrorResponse {

    logger.debug("------------------------------------->> NEW REQUEST <<-------------------------------------");

    AssessResponse response = null;

    com.oracle.determinations.server._12_2.rulebase.assess.types.AssessRequest request = assessRequestMapper
        .map(assessRequest);

    entityLevelRelocationService.moveGlobalAttributesAndRelationsToBaseLevel(request);

    entityLevelRelocationService.moveSubEntitiesToLowerLevel(request);

    com.oracle.determinations.server._12_2.rulebase.assess.types.AssessResponse assess12Response;

    isMeans = isMeansAssessment(assessRequest);

    try{

      if (isMeans) {

        resetAssessOutcomesStyle(request);

        assess12Response = opa12MeansAssessServiceProxy.assess(request);

      } else {
        assess12Response = opa12BillingAssessServiceProxy.assess(request);
      }

      entityLevelRelocationService.moveGlobalEntityToLowerLevel(assess12Response,
          entityLevelRelocationService.getGlobalEntityId(assessRequest));

      entityLevelRelocationService.moveSubEntitiesToUpperLevel(assess12Response);

      response = assessResponseMapper.map(assess12Response);

      entityLevelRelocationService.mapOpa10Relationships(response);

      if ( isMeans ){

        decisionReportTransformation.tranformToScreenDataAscending(assess12Response, response);

        decisionReportTransformation.restructureDecisionReport(getGlobalEntityId(assessRequest), response);

      }

    } catch (Exception e){
      if ( e.getCause() instanceof SoapFault ){
        SoapFault soapFault = (SoapFault) e.getCause();
        if ( soapFault.hasDetails() ){
          response = opaErrorResponseTransformation.tranformSoapFault(soapFault);
          
          decisionReportTransformation.restructureErrorDecisionReport(getGlobalEntityId(assessRequest), response);
        }
      } else {
        throw e;
      }
    }

    return response;
  }

  boolean isMeansAssessment(AssessRequest assessRequest) {
    List<ListEntity> listEntityList = assessRequest.getSessionData().getListEntity();
    for (ListEntity listEntity : listEntityList) {
      List<Entity> entityList = listEntity.getEntity();
      for (Entity entity : entityList) {
        List<AttributeOutcome> attributes = entity.getAttributeOutcome();
        for (AttributeOutcome attributeOutcome : attributes) {
          if ( MEANS_CALCULATIONS.equalsIgnoreCase(attributeOutcome.getId()) ||
              MEANS_OUTPUTS.equalsIgnoreCase(attributeOutcome.getId()) ) {
            return true;
          }
        }
      }
    }
    return false;
  }

  /**
   * To amend outcome style to base-attributes so that Decision Report will
   * generate required UNKNOWN attributes
   *
   * @param assessRequest
   */
  private void resetAssessOutcomesStyle(com.oracle.determinations.server._12_2.rulebase.assess.types.AssessRequest assessRequest) {
    for ( AttributeType attributeType : assessRequest.getGlobalInstance().getAttribute() ){
      if (MEANS_CALCULATIONS.equalsIgnoreCase(attributeType.getId())){
        attributeType.setUnknownOutcomeStyle(OutcomeStyleEnum.BASE_ATTRIBUTES);
        break;
      } else if (MEANS_OUTPUTS.equalsIgnoreCase(attributeType.getId())){
        assessRequest.getConfig().setResolveIndecisionRelationships(false);
        break;
      }
    }
  }


  private String getGlobalEntityId( AssessRequest assessRequest){
    List<ListEntity> listEntities = assessRequest.getSessionData().getListEntity();
    for ( ListEntity listEntity : listEntities ){
      if ( "global".equalsIgnoreCase(listEntity.getEntityType()) ) {
        for ( Entity entity : listEntity.getEntity()) {
          return entity.getId();
        }
      }
    }
    return null;
  }

}
