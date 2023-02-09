package uk.gov.justice.laa.ccms.service;

import com.oracle.determinations.server._10_0.rulebase.types.AssessResponse;
import com.oracle.determinations.server._10_0.rulebase.types.AttributeDecisionNode;
import com.oracle.determinations.server._10_0.rulebase.types.AttributeTypeEnum;
import com.oracle.determinations.server._10_0.rulebase.types.DecisionReport;
import com.oracle.determinations.server._10_0.rulebase.types.ListEvents;
import com.oracle.determinations.server._10_0.rulebase.types.ObjectFactory;
import com.oracle.determinations.server._10_0.rulebase.types.RulebaseEvent;
import com.oracle.determinations.server._12_2_1.rulebase.assess.types.AttributeNodeType;
import com.oracle.determinations.server._12_2_1.rulebase.assess.types.DecisionReportType;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import org.apache.cxf.binding.soap.SoapFault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.ccms.soap.error.Detail;
import uk.gov.justice.laa.ccms.soap.error.Event;

@Service
public class OpaErrorResponseTransformation {

  private static final Logger logger = LoggerFactory.getLogger(OpaErrorResponseTransformation.class);
  public static final String NONE = "none";

  private final String OPA_EVENT_ERROR = "assess.request.event.error";

  public AssessResponse tranformSoapFault (SoapFault soapFault) {

    try {
      Detail detail = getDetail(soapFault);

      if ( detail != null ){
        return createOpa10AssessResponse( detail );
      }

    } catch (JAXBException e) {
      logger.error("********************* ERROR ******************");
      e.printStackTrace();
    }
    return null;
  }

  private AssessResponse createOpa10AssessResponse(Detail detail) {
    /**
     * <?xml version="1.0" encoding="UTF-8"?>
     * <typ:assess-response>
     *   <typ:events>
     *     <typ:event name="Fatal" entity-id="87491089">
     *       <typ:message>"The figure entered for the balance of the bank account is a negative. Please change this to £0"</typ:message>
     *       <typ:parameters>
     *         <typ:value>"The figure entered for the balance of the bank account is a negative. Please change this to £0"</typ:value>
     *       </typ:parameters>
     *       <typ:decision-report report-style="base-attributes">
     *         <typ:attribute-decision-node id="dn:0" entity-id="87491089" text="The current balance of the bank account is -£263.38." attribute-id="BANKACC_INPUT_C_7WP2_18A" entity-type="BANKACC" type="currency" inferencing-type="base-level">
     *           <typ:number-val>-263.38</typ:number-val>
     *         </typ:attribute-decision-node>
     *       </typ:decision-report>
     *     </typ:event>
     *   </typ:events>
     */
    AssessResponse assessResponse = null;
    ObjectFactory factory = new ObjectFactory();
    if ( detail.getErrorResponse() != null ) {
      assessResponse = factory.createAssessResponse();
      if ( detail.getErrorResponse().getEvents() != null
          && detail.getErrorResponse().getEvents().getEventList() != null ){
        ListEvents listEvents = factory.createListEvents();
        List<RulebaseEvent> rulebaseEvents = listEvents.getEvent();
        List<Event> events = detail.getErrorResponse().getEvents().getEventList();
        for ( Event event : events ){
          rulebaseEvents.add(createEvent(event, factory));
        }
        assessResponse.setEvents(listEvents);
      } else {
        ListEvents listEvents = factory.createListEvents();
        List<RulebaseEvent> rulebaseEvents = listEvents.getEvent();

        RulebaseEvent rulebaseEvent = getSoapFaultRulebaseEvent(detail, factory);
        addRulebaseEventParamters(detail, rulebaseEvent);
        addDecisionReport(factory, rulebaseEvent);
        rulebaseEvents.add(rulebaseEvent);
        assessResponse.setEvents(listEvents);
      }
    }

    return assessResponse;
  }

  private void addDecisionReport(ObjectFactory factory, RulebaseEvent rulebaseEvent) {
    DecisionReport decisionReport = factory.createDecisionReport();
    decisionReport.setReportStyle("base-attributes");
    List<Object> attributeDecisionNodes = decisionReport.getRelationshipDecisionNodeOrAttributeDecisionNode();
    AttributeDecisionNode attributeDecisionNode = factory.createAttributeDecisionNode();
    attributeDecisionNode.setId("dn:2");
    attributeDecisionNode.setEntityType(NONE);
    attributeDecisionNode.setEntityId(NONE);
    attributeDecisionNode.setAttributeId(NONE);
    attributeDecisionNode.setType(AttributeTypeEnum.TEXT);
    attributeDecisionNode.setText(NONE);
    attributeDecisionNode.setTextVal("");
    attributeDecisionNodes.add(attributeDecisionNode);
    rulebaseEvent.setDecisionReport(decisionReport);
  }

  private RulebaseEvent getSoapFaultRulebaseEvent(Detail detail, ObjectFactory factory) {
    RulebaseEvent rulebaseEvent = factory.createRulebaseEvent();
    rulebaseEvent.setName("Fatal");
    rulebaseEvent.setEntityId("00000");
    rulebaseEvent.setMessage(detail.getErrorResponse().getMessage());
    return rulebaseEvent;
  }

  private void addRulebaseEventParamters(Detail detail, RulebaseEvent rulebaseEvent) {
    RulebaseEvent.Parameters parameters = new RulebaseEvent.Parameters();
    List<String> parametersValue = parameters.getValue();
    parametersValue.add("Message : " + detail.getErrorResponse().getMessage());
    parametersValue.add("Response Code : " + detail.getErrorResponse().getCode());
    rulebaseEvent.setParameters(parameters);
  }

  private RulebaseEvent createEvent(Event opa12Event, ObjectFactory factory) {
    RulebaseEvent rulebaseEvent = factory.createRulebaseEvent();

    if ( "error".equalsIgnoreCase(opa12Event.getName()) ){
      rulebaseEvent.setName("Fatal");
    }

    rulebaseEvent.setMessage(opa12Event.getMessage());

    //Parameters
    RulebaseEvent.Parameters parameters = factory.createRulebaseEventParameters();
    parameters.getValue().add(opa12Event.getParameters().getValue());
    rulebaseEvent.setParameters(parameters);

    //Decision Report
    DecisionReportType decisionReportType = opa12Event.getDecisionReport();
    if ( decisionReportType != null ){
      DecisionReport decisionReport = factory.createDecisionReport();
      decisionReport.setReportStyle(decisionReportType.getReportStyle());
      addNodes(decisionReport, decisionReportType, factory, rulebaseEvent);

      rulebaseEvent.setDecisionReport(decisionReport);
    }

    return rulebaseEvent;
  }

  private void addNodes(DecisionReport opa10DecisionReport,
      DecisionReportType opa18DecisionReportType, ObjectFactory factory,
      RulebaseEvent rulebaseEvent) {
    for ( Object object : opa18DecisionReportType.getRelationshipNodeOrAttributeNodeOrAlreadyProvenNode() ){
      if ( object instanceof AttributeNodeType ){
        AttributeNodeType opa12NodeType = ( AttributeNodeType ) object;
        AttributeDecisionNode node = factory.createAttributeDecisionNode();
        createAttributeDecisionNode(node, opa12NodeType, factory, rulebaseEvent);
        opa10DecisionReport.getRelationshipDecisionNodeOrAttributeDecisionNode().add(node);
      }
    }
  }

  private void createAttributeDecisionNode(AttributeDecisionNode node,
      AttributeNodeType opa12NodeType, ObjectFactory factory, RulebaseEvent rulebaseEvent) {
    node.setId(opa12NodeType.getId());
    node.setAttributeId(opa12NodeType.getAttributeId());
    node.setEntityId(opa12NodeType.getInstanceId());
    node.setText(opa12NodeType.getText());
    node.setEntityType(opa12NodeType.getEntityId());

    setAttributeTypeEnum( node, opa12NodeType );

    rulebaseEvent.setEntityId(opa12NodeType.getInstanceId());

    //Datatype & data
    node.setBooleanVal(opa12NodeType.isBooleanVal());
    node.setDatetimeVal(opa12NodeType.getDatetimeVal());
    node.setDateVal(opa12NodeType.getDateVal());
    node.setNumberVal(opa12NodeType.getNumberVal());
    node.setTextVal(opa12NodeType.getTextVal());
    node.setTimeVal(opa12NodeType.getTimeVal());
    if ( opa12NodeType.getUnknownVal() != null ){
      node.setUnknownVal( factory.createUnknownValue() );
    }
    if ( opa12NodeType.getUncertainVal() != null ){
      node.setUncertainVal( factory.createUncertainValue() );
    }
  }

  private void setAttributeTypeEnum(AttributeDecisionNode node, AttributeNodeType opa12NodeType) {
    if ( "boolean".equalsIgnoreCase(opa12NodeType.getType().value()) ){
      node.setType(AttributeTypeEnum.BOOLEAN);
    } else if ( "text".equalsIgnoreCase(opa12NodeType.getType().value()) ){
      node.setType(AttributeTypeEnum.TEXT);
    } else if ( "number".equalsIgnoreCase(opa12NodeType.getType().value()) ){
      node.setType(AttributeTypeEnum.NUMBER);
    } else if ( "currency".equalsIgnoreCase(opa12NodeType.getType().value()) ){
      node.setType(AttributeTypeEnum.CURRENCY);
    } else if ( "date".equalsIgnoreCase(opa12NodeType.getType().value()) ){
      node.setType(AttributeTypeEnum.DATE);
    } else if ( "datetime".equalsIgnoreCase(opa12NodeType.getType().value()) ){
      node.setType(AttributeTypeEnum.DATETIME);
    } else if ( "timeofday".equalsIgnoreCase(opa12NodeType.getType().value()) ){
      node.setType(AttributeTypeEnum.TIMEOFDAY);
    }
  }

  private Detail getDetail( SoapFault soapFault ) throws JAXBException {
    JAXBContext jaxbContext = JAXBContext.newInstance(Detail.class);
    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
    Detail detail = (Detail) unmarshaller.unmarshal(soapFault.getDetail());
    logger.info("*************detail************* : " + detail);
    return detail;
  }

}
