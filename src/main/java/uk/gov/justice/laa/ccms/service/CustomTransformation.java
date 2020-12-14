package uk.gov.justice.laa.ccms.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.oracle.determinations.server._12_2.rulebase.assess.types.AttributeNodeType;
import com.oracle.determinations.server._12_2.rulebase.assess.types.AttributeType;
import com.oracle.determinations.server._12_2.rulebase.assess.types.DecisionReportType;
import com.oracle.determinations.server._12_2.rulebase.assess.types.GlobalInstanceType;

public class CustomTransformation {
  
  private static final Logger logger = LoggerFactory.getLogger(CustomTransformation.class);

  private static final String GLOBAL = "global";
  public List<String> level0Entities = Arrays.asList("global");
  
  private List<String> ignoreAttributes = Arrays.asList("UNKNOWN_TEXT_VALUE",
      "UNKNOWN_NUMBER_VALUE","UNKNOWN_DATE_VALUE","UNKNOWN_CURRENCY_VALUE","UNKNOWN_BOOLEAN_VALUE");
  
  private final ObjectFactory factory = new ObjectFactory();

  public DecisionReportType getDecisionReport(GlobalInstanceType globalInstanceType, String goalAttribute) {
    if ( globalInstanceType != null ){
      for ( AttributeType attributeType : globalInstanceType.getAttribute() ){
        if (goalAttribute.equalsIgnoreCase(attributeType.getId())) {
          return attributeType.getDecisionReport();
        }
      }
    }
    return null;
  }
  
  /**
   * Extract GOAL from the node list
   *
   * @param decisionReportType
   * @return AttributeNodeType
   */
  public AttributeNodeType getGoalAttributeNodeFromDecisionReport(DecisionReportType decisionReportType, 
            String goalAttributeId) {
    List<?> nodes = (List<?>) decisionReportType.getRelationshipNodeOrAttributeNodeOrAlreadyProvenNode();
    for (Object node : nodes) {
      if (node instanceof AttributeNodeType) {
        AttributeNodeType opaAttributeNodeType = (AttributeNodeType) node;
        logger.debug("AttributeNodeType Id = " + opaAttributeNodeType.getAttributeId());
        if ( goalAttributeId.equalsIgnoreCase(opaAttributeNodeType.getAttributeId()) ){
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
  public List<AttributeNodeType> getUnknownAttributeNodeList (List<?> nodeList) {
    List<AttributeNodeType> nodeTypeList = new ArrayList<AttributeNodeType>();

    // Loop through each node in the nodeList and extract the AttributeNode
    for (Object node : nodeList) {
      if (node instanceof AttributeNodeType) {
        AttributeNodeType attributeNodeType = (AttributeNodeType) node;

        if ( (attributeNodeType.getUnknownVal() != null) && ( !ignoreAttributes.contains(attributeNodeType.getAttributeId())) ) {
          nodeTypeList.add(attributeNodeType);
        }
      }
    }

    return nodeTypeList;
  }

  /**
   * Get Entity ID which matches the levelList
   * @param unknownAttributeNodeList
   * @param levelList
   * @return
   */
  public String getNotMatchingLevelEntity(List<AttributeNodeType> unknownAttributeNodeList, List<String> levelList) {
    for ( AttributeNodeType attributeNodeType : unknownAttributeNodeList ) {
      if ( !levelList.contains(attributeNodeType.getEntityId()) ){
        return attributeNodeType.getEntityId();
      }
    }
    return null;
  }
  
  public void createOpa10ScreenData(String parentEntity, String matchingEntity,
      List<AttributeNodeType> unknownNodeList, AssessResponse assess10Response, String goalAttributeId) {
    logger.debug("Parent Entity : " + parentEntity + ", matchingEntity : " + matchingEntity);

    ScreenDefinition screenDefinition = createScreenDefinition(matchingEntity, assess10Response, unknownNodeList);

    //Add Screen
    if ( (screenDefinition != null) && (screenDefinition.getScreenControl() != null)
        && (screenDefinition.getScreenControl().size()>0) ) {
      logger.debug("ScreenControl size " + screenDefinition.getScreenControl());
      addScreenDefinition(screenDefinition, assess10Response, goalAttributeId);
      logger.debug("------------------------------ ADDED SCREEN-DEFINITION TO RESPONSE -------------------------------");
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
  
  /**
   * To get Global InstanceID from OPA10 AssessResponse
   * @param assess10Response
   * @return
   */
  private String getGlobalInstanceId(AssessResponse assess10Response) {
    for ( ListEntity listEntity : assess10Response.getSessionData().getListEntity() ){
      if ( GLOBAL.equalsIgnoreCase(listEntity.getEntityType()) ){
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
  private void addScreenDefinition(ScreenDefinition screenDefinition, AssessResponse assess10Response, String goalAttributeId) {
    printScreenDefinitionData(screenDefinition);
    for (ListEntity listEntity : assess10Response.getSessionData().getListEntity() ){
      if ( GLOBAL.equalsIgnoreCase(listEntity.getEntityType()) ){
        for ( Entity entity : listEntity.getEntity() ) {
          for ( Attribute attribute : entity.getAttribute() ){
            if ( goalAttributeId.equalsIgnoreCase(attribute.getId()) ){
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

  /**
   * Get Entity ID which matches the levelList
   * @param unknownAttributeNodeList
   * @param levelList
   * @return
   */
  public String getMatchingLevelEntity(List<AttributeNodeType> unknownAttributeNodeList, List<String> levelList) {
    for ( AttributeNodeType attributeNodeType : unknownAttributeNodeList ) {
      if ( levelList.contains(attributeNodeType.getEntityId()) ){
        return attributeNodeType.getEntityId();
      }
    }
    return null;
  }

  /**
   * Set globalId at warning if entity is global & return warning DecisionReport
   * @param globalEntityId
   * @param assess10Response
   * @return
   */
  public DecisionReport getErrorOrWarningDecisionReport(String globalEntityId, AssessResponse assess10Response, String eventName) {
    if ( (assess10Response.getEvents() != null) && (assess10Response.getEvents().getEvent() != null) ) {
      for ( RulebaseEvent event : assess10Response.getEvents().getEvent() ) {
        logger.debug("============= event.getName() = " + event.getName());
        if ( eventName.equalsIgnoreCase(event.getName())) {
          if ( GLOBAL.equalsIgnoreCase(event.getEntityId()) ) {
            event.setEntityId(globalEntityId);
          }
          return event.getDecisionReport();
        }
      }
    }
    return null;
  }
  
  /**
   * Get DecisionReport at GOAL attribute level
   * @param assess10Response
   * @return
   */
  public DecisionReport getDecisionReport(
      com.oracle.determinations.server._10_0.rulebase.types.AssessResponse assess10Response, String goalAttributeId ){
    for (ListEntity listEntity : assess10Response.getSessionData().getListEntity()) {
      if ( GLOBAL.equalsIgnoreCase(listEntity.getEntityType()) ) {
        for (Entity entity : listEntity.getEntity()) {
          for ( Attribute attribute : entity.getAttribute()){
            if ( goalAttributeId.equalsIgnoreCase(attribute.getId())) {
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
  public void resetGlobalEntityId(List<?> nodeList, String globalEntityId) {
    // Loop through each node in the nodeList and extract the AttributeNode and RelationshipNode objects but ignore the AlreadyProvenNodes
    for (Object node : nodeList) {
      if (node instanceof AttributeDecisionNode) {
        AttributeDecisionNode attributeDecisionNode = (AttributeDecisionNode) node;

        if ( GLOBAL.equalsIgnoreCase(attributeDecisionNode.getEntityId()) ){
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




}
