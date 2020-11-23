package uk.gov.justice.laa.ccms.service;

import com.oracle.determinations.server._10_0.rulebase.types.AssessResponse;
import com.oracle.determinations.server._10_0.rulebase.types.Attribute;
import com.oracle.determinations.server._10_0.rulebase.types.AttributeDecisionNode;
import com.oracle.determinations.server._10_0.rulebase.types.DecisionReport;
import com.oracle.determinations.server._10_0.rulebase.types.Entity;
import com.oracle.determinations.server._10_0.rulebase.types.ListEntity;
import com.oracle.determinations.server._10_0.rulebase.types.ObjectFactory;
import com.oracle.determinations.server._10_0.rulebase.types.ScreenControl;
import com.oracle.determinations.server._10_0.rulebase.types.ScreenDefinition;
import com.oracle.determinations.server._12_2.rulebase.assess.types.AttributeNodeType;
import com.oracle.determinations.server._12_2.rulebase.assess.types.AttributeType;
import com.oracle.determinations.server._12_2.rulebase.assess.types.DecisionReportType;
import com.oracle.determinations.server._12_2.rulebase.assess.types.GlobalInstanceType;
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
  private String[] level2Entities = {"ADDTHIRD", "BUSPARTBANK", "BPFAMILYEMPL", "BPPROPERTY", "COPROPERTY",
      "SHARE", "CLI_NON_HM_WAGE_SLIP", "CLI_NON_HM_L17", "EMPLOY_BEN_CLIENT", "EMP_CLI_KNOWN_CHANGE",
      "EMPLOY_BEN_PARTNER", "PAR_EMPLOY_KNOWN_CHANGE", "PAR_NON_HM_L17", "PAR_NON_HM_WAGE_SLIP",
      "SELFEMPBANK", "SEFAMILYEMPL", "SEPROPERTY"};
  private String[] level1Entities = {"ADDPROPERTY", "BUSINESSPART", "COMPANY", "EMPLOYMENT_CLIENT",
      "EMPLOYMENT_PARTNER", "SELFEMPLOY"};


  /**
   * OPA 12 doesn't support screen/screen-control types, hence manual tranformation is required
   * to convert Decision Report data to OPA10 screen data
   *
   * @param assess12Response
   * @param assess10Response
   */
  public void tranformToScreenData( com.oracle.determinations.server._12_2.rulebase.assess.types.AssessResponse assess12Response,
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
        List<String> level2List = Arrays.asList(level2Entities);
        String matchingLevel2Entity = getMatchingLevelEntity(unknownAttributeNodeList, level2List);
        logger.debug("Level-2 matching entity : " + matchingLevel2Entity);
        if (StringUtils.isEmpty(matchingLevel2Entity)){

          //Level-2 sub-entities doesn't exist, hence check Level-1 entities
          List<String> level1List = Arrays.asList(level1Entities);
          String matchingLevel1Entity = getMatchingLevelEntity(unknownAttributeNodeList, level1List);
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

        if (attributeNodeType.getUnknownVal() != null) {
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

  public void restructureDecisionReport( String globalEntityId,
      com.oracle.determinations.server._10_0.rulebase.types.AssessResponse assess10Response ){

    DecisionReport decisionReport = getDecisionReport(assess10Response);

    if ( decisionReport != null ) {
      //Loop through nodes in Decision Report & retrieve AttributeNodeTypes and RelationshipNodeTypes
      List<?> nodes = (List<?>) decisionReport.getRelationshipDecisionNodeOrAttributeDecisionNode();
      resetGlobalEntityId(nodes, globalEntityId);
    }
  }

  private DecisionReport getDecisionReport(
      com.oracle.determinations.server._10_0.rulebase.types.AssessResponse assess10Response ){
    for (ListEntity listEntity : assess10Response.getSessionData().getListEntity()) {
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
    return null;
  }

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

}
