package uk.gov.justice.laa.ccms.service;

import com.oracle.determinations.server._10_0.rulebase.types.AssessResponse;
import com.oracle.determinations.server._10_0.rulebase.types.Attribute;
import com.oracle.determinations.server._10_0.rulebase.types.AttributeDecisionNode;
import com.oracle.determinations.server._10_0.rulebase.types.DecisionReport;
import com.oracle.determinations.server._10_0.rulebase.types.Entity;
import com.oracle.determinations.server._10_0.rulebase.types.ListEntity;
import com.oracle.determinations.server._10_0.rulebase.types.ObjectFactory;
import com.oracle.determinations.server._10_0.rulebase.types.RulebaseEvent;
import com.oracle.determinations.server._10_0.rulebase.types.ScreenControl;
import com.oracle.determinations.server._10_0.rulebase.types.ScreenDefinition;
import com.oracle.determinations.server._12_2_1.rulebase.assess.types.AttributeNodeType;
import com.oracle.determinations.server._12_2_1.rulebase.assess.types.AttributeType;
import com.oracle.determinations.server._12_2_1.rulebase.assess.types.DecisionReportType;
import com.oracle.determinations.server._12_2_1.rulebase.assess.types.GlobalInstanceType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DecisionReportTransformation {

  private static final Logger logger = LoggerFactory.getLogger(DecisionReportTransformation.class);

  private final ObjectFactory factory = new ObjectFactory();

  private final String MEANS_CALCULATIONS = "MEANS_CALCULATIONS";
  private final String MEANS_OUTPUTS = "MEANS_OUTPUTS";
  private final String BILLING_IS_COMPLETE = "BILLING_IS_COMPLETE";

  //Hardcoded data, this should be derived from BR100
  private List<String> level0Entities = Arrays.asList("global");
  private List<String> level1Entities = Arrays.asList("ADDPROPERTY", "BUSINESSPART", "COMPANY", "EMPLOYMENT_CLIENT",
      "EMPLOYMENT_PARTNER", "SELFEMPLOY");
  private List<String> level2Entities = Arrays.asList("ADDTHIRD", "BUSPARTBANK", "BPFAMILYEMPL", "BPPROPERTY", "COPROPERTY",
      "SHARE", "CLI_NON_HM_WAGE_SLIP", "CLI_NON_HM_L17", "EMPLOY_BEN_CLIENT", "EMP_CLI_KNOWN_CHANGE",
      "EMPLOY_BEN_PARTNER", "PAR_EMPLOY_KNOWN_CHANGE", "PAR_NON_HM_L17", "PAR_NON_HM_WAGE_SLIP",
      "SELFEMPBANK", "SEFAMILYEMPL", "SEPROPERTY");
  private List<String> ignoreAttributes = Arrays.asList("UNKNOWN_TEXT_VALUE",
      "UNKNOWN_NUMBER_VALUE","UNKNOWN_DATE_VALUE","UNKNOWN_CURRENCY_VALUE","UNKNOWN_BOOLEAN_VALUE");


  /**
   * OPA 12 doesn't support screen/screen-control types, hence manual tranformation is required
   * to convert Decision Report data to OPA10 screen data
   *
   * @param assess12Response
   * @param assess10Response
   */
  public void tranformToScreenDataAscending( com.oracle.determinations.server._12_2_1.rulebase.assess.types.AssessResponse assess12Response,
      com.oracle.determinations.server._10_0.rulebase.types.AssessResponse assess10Response ){

    //Get Decision Report from OPA12 response
    DecisionReportType decisionReportType = getDecisionReport(assess12Response.getGlobalInstance());

    if ( decisionReportType != null ){
      //Extract MOD309 goal attribute node from Decision Report
      AttributeNodeType goalAttributeNodeType = getGoalAttributeNodeFromDecisionReport(decisionReportType);

      String goalAttributeInstanceId = goalAttributeNodeType.getInstanceId();
      logger.debug("Goal attribute instance ID : " + goalAttributeInstanceId);

      if ( goalAttributeNodeType != null ){
        List<?> childNodes = goalAttributeNodeType.getRelationshipNodeOrAttributeNodeOrAlreadyProvenNode();

        //Extract UNKNOWN Attribute nodes and ignore relationships
        List<AttributeNodeType> unknownAttributeNodeList = getUnknownAttributeNodeList(childNodes);

        //Check if there are Level zero Entities
        //List<String> level0List = Arrays.asList(level0Entities);
        String matchingLevel0Entity = getMatchingLevelEntity(unknownAttributeNodeList, level0Entities);
        logger.debug("Level-0 matching entity : " + matchingLevel0Entity);
        if (StringUtils.isEmpty(matchingLevel0Entity)){

          //Level-0 entities doesn't exist, hence check Level-1 entities
          String matchingLevel1Entity = getMatchingLevelEntity(unknownAttributeNodeList, level1Entities);
          logger.debug("Level-1 matching sub-entity : " + matchingLevel1Entity);
          if (StringUtils.isEmpty(matchingLevel1Entity)){

            //Process Level-1 Non-SubEntities data
            List<String> level1RemainingEntities = mergeList();
            String level1AllEntities = getNotMatchingLevelEntity(unknownAttributeNodeList, level1RemainingEntities);
            logger.debug("Level-1 matching non sub-entities : " + level1AllEntities);
            if (StringUtils.isEmpty(level1AllEntities)){
              //Process Level-1 Non-SubEntities data
              String matchingLevel2Entity = getMatchingLevelEntity(unknownAttributeNodeList, level2Entities);
              if (StringUtils.isNotEmpty(matchingLevel2Entity)){
                logger.debug("--------------------- Process Level-2 entities ---------------------");
                //Process Level-1 entities first
                createOpa10ScreenData(goalAttributeInstanceId, matchingLevel2Entity,
                    unknownAttributeNodeList, assess10Response);
              }else {
                logger.debug("--------------------- NOTHING TO PROCESS ---------------------");
              }
            } else {
              logger.debug("--------------------- Process Level-1 non sub-entities ---------------------");
              createOpa10ScreenData(goalAttributeInstanceId, level1AllEntities,
                  unknownAttributeNodeList, assess10Response);
            }
          } else {
            logger.debug("--------------------- Process Level-1 entities ---------------------");
            //Process Level-1 entities first
            createOpa10ScreenData(goalAttributeInstanceId, matchingLevel1Entity,
                unknownAttributeNodeList, assess10Response);
          }
        } else {
          logger.debug("--------------------- Process Level-0 entities ---------------------");
          //Process Level-2 entities first
          //Create OPA10 SCREEN data for AssessResponse
          createOpa10ScreenData(goalAttributeInstanceId, matchingLevel0Entity,
              unknownAttributeNodeList, assess10Response);
        }
      }
    }
  }

  private List<String> mergeList() {
    List<String> list = new ArrayList<>();

    list.addAll(level0Entities);
    list.addAll(level1Entities);
    list.addAll(level2Entities);

    return list;
  }

  /**
   * OPA 12 doesn't support screen/screen-control types, hence manual transformation is required
   * to convert Decision Report data to OPA10 screen data
   *
   * @param assess12Response
   * @param assess10Response
   */
  public void tranformToScreenData( com.oracle.determinations.server._12_2_1.rulebase.assess.types.AssessResponse assess12Response,
      com.oracle.determinations.server._10_0.rulebase.types.AssessResponse assess10Response ){

    //Get Decision Report from OPA12 response
    DecisionReportType decisionReportType = getDecisionReport(assess12Response.getGlobalInstance());

    if ( decisionReportType != null ){
      //Extract MOD309 goal attribute node from Decision Report
      AttributeNodeType goalAttributeNodeType = getGoalAttributeNodeFromDecisionReport(decisionReportType);

      String goalAttributeInstanceId = goalAttributeNodeType.getInstanceId();
      logger.debug("Goal attribute instance ID : " + goalAttributeInstanceId);

      if ( goalAttributeNodeType != null ){
        List<?> childNodes = goalAttributeNodeType.getRelationshipNodeOrAttributeNodeOrAlreadyProvenNode();

        //Extract UNKNOWN Attribute nodes and ignore relationships
        List<AttributeNodeType> unknownAttributeNodeList = getUnknownAttributeNodeList(childNodes);

        //Check if there are any 2nd Level Entities
        //List<String> level2List = Arrays.asList(level2Entities);
        String matchingLevel2Entity = getMatchingLevelEntity(unknownAttributeNodeList, level2Entities);
        logger.debug("Level-2 matching entity : " + matchingLevel2Entity);
        if (StringUtils.isEmpty(matchingLevel2Entity)){

          //Level-2 sub-entities doesn't exist, hence check Level-1 entities
          //List<String> level1List = Arrays.asList(level1Entities);
          String matchingLevel1Entity = getMatchingLevelEntity(unknownAttributeNodeList, level1Entities);
          logger.debug("Level-1 matching entity : " + matchingLevel1Entity);
          if (StringUtils.isEmpty(matchingLevel1Entity)){

            logger.debug("Process entities which are not in Level-1 & 2");
            String firstNonSubEntity = null;
            for ( AttributeNodeType attributeNodeType : unknownAttributeNodeList ) {
              firstNonSubEntity =  attributeNodeType.getEntityId();
              break;
            }

            logger.debug("firstNonSubEntity - " + firstNonSubEntity);
            if (StringUtils.isNotEmpty(firstNonSubEntity)){
              //Process Level-1 entities first
              createOpa10ScreenData(goalAttributeInstanceId, firstNonSubEntity,
                  unknownAttributeNodeList, assess10Response);
            }

          } else {
            logger.debug("Process Level-1 entities");
            //Process Level-1 entities first
            createOpa10ScreenData(goalAttributeInstanceId, matchingLevel1Entity,
                unknownAttributeNodeList, assess10Response);
          }
        } else {
          logger.debug("Process Level-2 entities");
          //Process Level-2 entities first
          //Create OPA10 SCREEN data for AssessResponse
          createOpa10ScreenData(goalAttributeInstanceId, matchingLevel2Entity,
              unknownAttributeNodeList, assess10Response);
        }
      }
    }
  }

  private void createOpa10ScreenData(String parentEntity, String matchingEntity,
                              List<AttributeNodeType> unknownNodeList, AssessResponse assess10Response) {
    logger.debug("Parent Entity : " + parentEntity + ", matchingEntity : " + matchingEntity);

    ScreenDefinition screenDefinition = createScreenDefinition(matchingEntity, assess10Response, unknownNodeList);

    //Add Screen
    if ( (screenDefinition != null) && (screenDefinition.getScreenControl() != null)
                    && (screenDefinition.getScreenControl().size()>0) ) {
      logger.debug("ScreenControl size " + screenDefinition.getScreenControl());
      addScreenDefinition(screenDefinition, assess10Response);
      logger.debug("------------------------------ ADDED SCREEN-DEFINITION TO RESPONSE -------------------------------");
    }
  }

  /**
   * Create ScreenDefinition for MEANS goal attribute
   * @return
   */
  private ScreenDefinition createScreenDefinition(String entityType, AssessResponse assess10Response, List<AttributeNodeType> unknownNodeList) {
    ScreenDefinition screenDefinition = factory.createScreenDefinition();
    screenDefinition.setId("SCREEN");
    screenDefinition.setTitle("OPA-18 Screen");
    screenDefinition.setIsAutomatic(true);
    screenDefinition.setEntityType(entityType);

    //instance-id doesn't exist in OPA18, hence retrieve from opa10 response
    screenDefinition.setEntityId(getGlobalInstanceId(assess10Response));
    logger.debug("Global Instance ID : " + screenDefinition.getEntityId() );

    createScreenControl(screenDefinition, entityType, unknownNodeList);

    return screenDefinition;
  }

  private String getGlobalInstanceId(AssessResponse assess10Response) {
    for ( ListEntity listEntity : assess10Response.getSessionData().getListEntity() ){
      if ( "global".equalsIgnoreCase(listEntity.getEntityType()) ){
        for ( Entity entity : listEntity.getEntity() ){
          return entity.getId();
        }
      }
    }
    return null;
  }

  /**
   * Create screenControl and set values from OPA12 Decision Report
   * @param screenDefinition
   */
  private void createScreenControl(ScreenDefinition screenDefinition, String entityType, List<AttributeNodeType> unknownNodeList) {
    List<ScreenControl> controls = screenDefinition.getScreenControl();
    for ( AttributeNodeType attributeNodeType : unknownNodeList ){
      if ( entityType.equalsIgnoreCase(attributeNodeType.getEntityId()) ){
        ScreenControl screenControl = factory.createScreenControl();
        screenControl.setControlType(attributeNodeType.getType().value());
        screenControl.setCaption(attributeNodeType.getText());
        screenControl.setIsVisible(true);
        screenControl.setTextStyle("");
        screenControl.setAttributeId(attributeNodeType.getAttributeId());
        logger.debug("Attribute ID added to ScreenControl : " + attributeNodeType.getAttributeId());
        screenControl.setIsReadOnly(false);
        screenControl.setIsMandatory(false);
        screenControl.setIsInferred(attributeNodeType.isInferred());
        controls.add(screenControl);
      }
    }
  }

  /**
   * Add ScreenDefinition to OPA-10 AssessResponse
   * @param screenDefinition
   * @param assess10Response
   */
  private void addScreenDefinition(ScreenDefinition screenDefinition, AssessResponse assess10Response) {
    printScreenDefinitionData(screenDefinition);
    for (ListEntity listEntity : assess10Response.getSessionData().getListEntity() ){
      if ( "global".equalsIgnoreCase(listEntity.getEntityType()) ){
        for ( Entity entity : listEntity.getEntity() ) {
          for ( Attribute attribute : entity.getAttribute() ){
            if ( MEANS_CALCULATIONS.equalsIgnoreCase(attribute.getId()) ||
                              MEANS_OUTPUTS.equalsIgnoreCase(attribute.getId()) ){
              logger.debug("Attribute ID added to ScreenControl : " +attribute.getId());
              attribute.setDecisionReport(null);
              attribute.setScreen(screenDefinition);
              break;
            }
          }
        }
      }
    }
  }

  private void printScreenDefinitionData(ScreenDefinition sd) {
    logger.debug("------------------------- Print ScreenDefinition before adding to Attribute ------------------------- ");
    logger.debug("ScreenDefinition : Entity ID : " + sd.getEntityId()
                                + ", EntityType : "+ sd.getEntityType()
                                + ", Id : "+ sd.getId()
                                + ", Name : "+ sd.getName()
                                + ", Title : "+ sd.getTitle());
    //logger.debug();
    for ( ScreenControl sc : sd.getScreenControl() ){
      logger.debug("ScreenControl : Attribute ID : " + sc.getAttributeId()
          + ", Caption : "+ sc.getCaption()
          + ", ConfigFile : "+ sc.getConfigFile()
          + ", ControlType : "+ sc.getControlType()
          + ", DocType : "+ sc.getDocumentType()
          + ", Default : "+ sc.getDefault()
          + ", EntityId : "+ sc.getEntityId()
          + ", EntityType : "+ sc.getEntityType()
          + ", IsVisible : "+ sc.isIsVisible()
          + ", Style : "+ sc.getStyle()
          + ", IsInferred : "+ sc.isIsInferred()
          + ", IsMandatory : "+ sc.isIsMandatory()
          + ", IsReadOnly : "+ sc.isIsReadOnly());
    }
  }

  /**
   * Get Entity ID which matches the levelList
   * @param unknownAttributeNodeList
   * @param levelList
   * @return
   */
  private String getMatchingLevelEntity(List<AttributeNodeType> unknownAttributeNodeList, List<String> levelList) {
    for ( AttributeNodeType attributeNodeType : unknownAttributeNodeList ) {
      if ( levelList.contains(attributeNodeType.getEntityId()) ){
        return attributeNodeType.getEntityId();
      }
    }
    return null;
  }

  /**
   * Get Entity ID which matches the levelList
   * @param unknownAttributeNodeList
   * @param levelList
   * @return
   */
  private String getNotMatchingLevelEntity(List<AttributeNodeType> unknownAttributeNodeList, List<String> levelList) {
    for ( AttributeNodeType attributeNodeType : unknownAttributeNodeList ) {
      if ( !levelList.contains(attributeNodeType.getEntityId()) ){
        return attributeNodeType.getEntityId();
      }
    }
    return null;
  }

  /**
   * Extract MOD309 goal from the node list
   *
   * @param decisionReportType
   * @return AttributeNodeType
   */
  private AttributeNodeType getGoalAttributeNodeFromDecisionReport(DecisionReportType decisionReportType) {
    List<?> nodes = (List<?>) decisionReportType.getRelationshipNodeOrAttributeNodeOrAlreadyProvenNode();
    for (Object node : nodes) {
      if (node instanceof AttributeNodeType) {
        AttributeNodeType opaAttributeNodeType = (AttributeNodeType) node;
        logger.debug("AttributeNodeType Id = " + opaAttributeNodeType.getAttributeId());

        if ( MEANS_CALCULATIONS.equalsIgnoreCase(opaAttributeNodeType.getAttributeId()) ||
                      MEANS_OUTPUTS.equalsIgnoreCase(opaAttributeNodeType.getAttributeId()) ){
          return opaAttributeNodeType;
        }
      }
    }
    return null;
  }

  /**
   * Retrieve UNKNOWN type attributeNodes
   *
   * @param nodeList
   * @return List<AttributeNodeType>
   */
  private List<AttributeNodeType> getUnknownAttributeNodeList (List nodeList) {
    List<AttributeNodeType> nodeTypeList = new ArrayList<AttributeNodeType>();

    // Loop through each node in the nodeList and extract the AttributeNode
    for (Object node : nodeList) {
      if (node instanceof AttributeNodeType) {
        AttributeNodeType attributeNodeType = (AttributeNodeType) node;

        if ( (attributeNodeType.getUnknownVal() != null) && ( !ignoreAttributes.contains(attributeNodeType.getAttributeId())) ) {
          //logger.debug("Unknown AttributeNodeType Id = " + attributeNodeType.getAttributeId());
          nodeTypeList.add(attributeNodeType);
        }
      }
    }

    return nodeTypeList;
  }

  private DecisionReportType getDecisionReport(GlobalInstanceType globalInstanceType) {
    if ( globalInstanceType != null ){
      for ( AttributeType attributeType : globalInstanceType.getAttribute() ){
        if (MEANS_CALCULATIONS.equalsIgnoreCase(attributeType.getId())) {
             //MEANS_OUTPUTS.equalsIgnoreCase(attributeType.getId()) ){
          return attributeType.getDecisionReport();
        }
      }
    }
    return null;
  }
  
  /**
   * OPA10 global entity isn't supported in OPA18 as the AssessService hierarchy is different
   * @param globalEntityId
   * @param assess10Response
   */
  public void restructureDecisionReport( String globalEntityId,
      com.oracle.determinations.server._10_0.rulebase.types.AssessResponse assess10Response ){
    
    //Reset at GOAL attribute DecisionReport
    DecisionReport decisionReport = getDecisionReport(assess10Response);
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
  
  /**
   * Set globalId at warning if entity is global & return warning DecisionReport
   * @param globalEntityId
   * @param assess10Response
   * @return
   */
  private DecisionReport getErrorOrWarningDecisionReport(String globalEntityId, AssessResponse assess10Response, String eventName) {
    logger.debug("eventName ==== " + eventName);
    if ( (assess10Response.getEvents() != null) && (assess10Response.getEvents().getEvent() != null) ) {
      logger.debug("============= 1");
      for ( RulebaseEvent event : assess10Response.getEvents().getEvent() ) {
        logger.debug("============= event.getName() = " + event.getName());
        if ( eventName.equalsIgnoreCase(event.getName())) {
          logger.debug("============= 2");
          if ( "global".equalsIgnoreCase(event.getEntityId()) ) {
            event.setEntityId(globalEntityId);
          }
          logger.debug("============= 3");
          return event.getDecisionReport();
        }
      }
    }
    logger.debug("============= 4");
    return null;
  }
  
  /**
   * Get DecisionReport at GOAL attribute level
   * @param assess10Response
   * @return
   */
  private DecisionReport getDecisionReport(
      com.oracle.determinations.server._10_0.rulebase.types.AssessResponse assess10Response ){
    for (ListEntity listEntity : assess10Response.getSessionData().getListEntity()) {
      if ( "global".equalsIgnoreCase(listEntity.getEntityType()) ) {
    	for (Entity entity : listEntity.getEntity()) {
          for ( Attribute attribute : entity.getAttribute()){
            if ( MEANS_OUTPUTS.equalsIgnoreCase(attribute.getId()) ||
                  MEANS_CALCULATIONS.equalsIgnoreCase(attribute.getId()) ||
                    BILLING_IS_COMPLETE.equalsIgnoreCase(attribute.getId())) {
              return attribute.getDecisionReport();
            }
          }
        }
      }
    }
    return null;
  }
  
  /**
   * Replace OPA10 AssessResponse global entityId with OPA10 ID
   * @param nodeList
   * @param globalEntityId
   */
  private void resetGlobalEntityId(List nodeList, String globalEntityId) {
    // Loop through each node in the nodeList and extract the AttributeNode and RelationshipNode objects but ignore the AlreadyProvenNodes
    for (Object node : nodeList) {
      if (node instanceof AttributeDecisionNode) {
        AttributeDecisionNode attributeDecisionNode = (AttributeDecisionNode) node;

        if ( "global".equalsIgnoreCase(attributeDecisionNode.getEntityId()) ){
          //logger.debug("Attribute updated - " + attributeDecisionNode.getAttributeId());
          attributeDecisionNode.setEntityId(globalEntityId);
        }

        // If this attribute has child attributes then process these.
        if ( attributeDecisionNode.getAttributeDecisionNodeOrRelationshipDecisionNode() != null ) {
          List<?> nodes = (List<?>) attributeDecisionNode.getAttributeDecisionNodeOrRelationshipDecisionNode();
          resetGlobalEntityId(nodes, globalEntityId );
        }
      }
    }
  }
  
  /**
   * To restructure SoapFault constructed OPA10Response for global entity errors
   * @param globalEntityId
   * @param assess10Response
   */
  public void restructureErrorDecisionReport(String globalEntityId, AssessResponse assess10Response) {
    logger.debug("globalEntityId ==== " + globalEntityId);
    //Reset at warning level
    DecisionReport errorDecisionReport = getErrorOrWarningDecisionReport(globalEntityId, assess10Response, "Fatal");
    logger.debug("errorDecisionReport ==== " + errorDecisionReport);
    if ( errorDecisionReport != null ) {
      //Loop through nodes in Decision Report & retrieve AttributeNodeTypes and RelationshipNodeTypes
      List<?> nodes = (List<?>) errorDecisionReport.getRelationshipDecisionNodeOrAttributeDecisionNode();
      resetGlobalEntityId(nodes, globalEntityId);
    }
    
  }

}
