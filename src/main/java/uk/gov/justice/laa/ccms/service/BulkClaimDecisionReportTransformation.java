package uk.gov.justice.laa.ccms.service;

import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.oracle.determinations.server._10_0.rulebase.types.DecisionReport;
import com.oracle.determinations.server._12_2_1.rulebase.assess.types.AttributeNodeType;
import com.oracle.determinations.server._12_2_1.rulebase.assess.types.DecisionReportType;

@Service
public class BulkClaimDecisionReportTransformation extends CustomTransformation {

  private static final Logger logger = LoggerFactory.getLogger(BulkClaimDecisionReportTransformation.class);

  private final String BILLING_IS_COMPLETE = "BILLING_IS_COMPLETE";

  /**
   * OPA 12 doesn't support screen/screen-control types, hence manual transformation is required
   * to convert Decision Report data to OPA10 screen data
   *
   * @param assess12Response
   * @param assess10Response
   */
  public void tranformToScreenDataAscending( com.oracle.determinations.server._12_2_1.rulebase.assess.types.AssessResponse assess12Response,
      com.oracle.determinations.server._10_0.rulebase.types.AssessResponse assess10Response ){

    //Get Decision Report from OPA12 response
    DecisionReportType decisionReportType = getDecisionReport(assess12Response.getGlobalInstance(), BILLING_IS_COMPLETE);

    if ( decisionReportType != null ){
      //Extract Bulk Claim goal attribute node from Decision Report
      AttributeNodeType goalAttributeNodeType = getGoalAttributeNodeFromDecisionReport(decisionReportType, BILLING_IS_COMPLETE);

      String goalAttributeInstanceId = goalAttributeNodeType.getInstanceId();
      logger.debug("Goal attribute instance ID : " + goalAttributeInstanceId);

      if ( goalAttributeNodeType != null ){
        List<?> childNodes = goalAttributeNodeType.getRelationshipNodeOrAttributeNodeOrAlreadyProvenNode();

        //Extract UNKNOWN Attribute nodes and ignore relationships
        List<AttributeNodeType> unknownAttributeNodeList = getUnknownBaseAttributeNodeList(childNodes);

        //Check if there are Level zero Entities
        //List<String> level0List = Arrays.asList(level0Entities);
        String matchingLevel0Entity = getMatchingLevelEntity(unknownAttributeNodeList, level0Entities);
        logger.debug("Level-0 matching entity : " + matchingLevel0Entity);
        if (StringUtils.isEmpty(matchingLevel0Entity)){

          //Process Level-1 Non-SubEntities data
          String level1AllEntities = getNotMatchingLevelEntity(unknownAttributeNodeList, level0Entities);
          logger.debug("Level-1 matching non sub-entities : " + level1AllEntities);
          if (StringUtils.isEmpty(level1AllEntities)){
            logger.debug("--------------------- NOTHING TO PROCESS ---------------------");
          } else {
            logger.debug("--------------------- Process Level-1 entities ---------------------");
            createOpa10ScreenData(goalAttributeInstanceId, level1AllEntities,
                unknownAttributeNodeList, assess10Response, BILLING_IS_COMPLETE);
          }
        } else {
          logger.debug("--------------------- Process Level-0 entities ---------------------");
          createOpa10ScreenData(goalAttributeInstanceId, matchingLevel0Entity,
              unknownAttributeNodeList, assess10Response, BILLING_IS_COMPLETE);
        }
      }
    }
  }

  /**
   * OPA10 global entity isn't supported in OPA18 as the AssessService hierarchy is different
   * @param globalEntityId
   * @param assess10Response
   */
  public void restructureDecisionReport( String globalEntityId,
      com.oracle.determinations.server._10_0.rulebase.types.AssessResponse assess10Response ){
    
    //Reset at GOAL attribute DecisionReport
    DecisionReport decisionReport = getDecisionReport(assess10Response, BILLING_IS_COMPLETE);
    if ( decisionReport != null ) {
      //Loop through nodes in Decision Report & retrieve AttributeNodeTypes and RelationshipNodeTypes
      List<?> nodes = (List<?>) decisionReport.getRelationshipDecisionNodeOrAttributeDecisionNode();
      resetGlobalEntityId(nodes, globalEntityId);
    }
    
    //Reset at warning level
    DecisionReport warningDecisionReport = getErrorOrWarningDecisionReport(globalEntityId, assess10Response, "warning");
    if ( warningDecisionReport != null ) {
      //Loop through nodes in Decision Report & retrieve AttributeNodeTypes and RelationshipNodeTypes
      List<?> nodes = (List<?>) warningDecisionReport.getRelationshipDecisionNodeOrAttributeDecisionNode();
      resetGlobalEntityId(nodes, globalEntityId);
    }
  }
}
