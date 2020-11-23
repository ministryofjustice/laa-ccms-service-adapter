package uk.gov.justice.laa.ccms.service;

import com.oracle.determinations.server._10_0.rulebase.types.AssessResponse;
import com.oracle.determinations.server._10_0.rulebase.types.AttributeDecisionNode;
import com.oracle.determinations.server._10_0.rulebase.types.AttributeTypeEnum;
import com.oracle.determinations.server._10_0.rulebase.types.DecisionReport;
import com.oracle.determinations.server._10_0.rulebase.types.ListEvents;
import com.oracle.determinations.server._10_0.rulebase.types.ObjectFactory;
import com.oracle.determinations.server._10_0.rulebase.types.RulebaseEvent;
import com.oracle.determinations.server._12_2.rulebase.assess.types.AttributeNodeType;
import com.oracle.determinations.server._12_2.rulebase.assess.types.DecisionReportType;
import java.util.Iterator;
import java.util.NoSuchElementException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import org.apache.cxf.binding.soap.SoapFault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import uk.gov.justice.laa.ccms.soap.error.Detail;
import uk.gov.justice.laa.ccms.soap.error.Event;

@Service
public class OpaErrorResponseTransformation {

  private static final Logger logger = LoggerFactory.getLogger(OpaErrorResponseTransformation.class);

  private final String OPA_EVENT_ERROR = "assess.request.event.error";
  /*
  public AssessResponse tranformSoapFault (SoapFault soapFault) {

    Element element = soapFault.getDetail();

    if (  "detail".equalsIgnoreCase(element.getTagName()) ){

      Node errorResponseNode = element.getFirstChild();

      if ( errorResponseNode.hasChildNodes() ){

        NodeList nodeList = errorResponseNode.getChildNodes();
        for (Node node : iterable(nodeList)) {
          if ( "code".equalsIgnoreCase(node.getLocalName()) ){
            logger.debug("Code value : "+node.getTextContent());
          } else if( "message".equalsIgnoreCase(node.getLocalName()) ){
            logger.debug("Message value : "+node.getTextContent());
          } else if( "events".equalsIgnoreCase(node.getLocalName()) ){
            //Retrieve events data
            Node eventNode = node.getFirstChild();
            if ( "event".equalsIgnoreCase(eventNode.getLocalName()) ){
              //<typ:event entity-id="BANKACC" instance-id="87491089" name="error">
              NamedNodeMap map = eventNode.getAttributes();
              String entityId = eventNode.getAttributes().getNamedItem("entity-id").getTextContent();
              String instanceId = eventNode.getAttributes().getNamedItem("instance-id").getTextContent();
              String name = eventNode.getAttributes().getNamedItem("name").getTextContent();
              logger.debug("entityId : instanceId : name : " + entityId + " : " + instanceId + " : " + name);

              NodeList eventChildNodeList = eventNode.getChildNodes();
              for ( Node eventChildNode : iterable(eventChildNodeList) ){
                if ( "message".equalsIgnoreCase(eventChildNode.getLocalName()) ){
                  logger.debug("Error Message : "+eventChildNode.getTextContent());
                } else if( "parameters".equalsIgnoreCase(eventChildNode.getLocalName()) ){
                  logger.debug("Parameter Value " + eventChildNode.getFirstChild().getTextContent());
                } else if ( "decision-report".equalsIgnoreCase(eventChildNode.getLocalName()) ){
                  logger.debug("Decision Report " + eventChildNode.getLocalName());
                }
              }
            }
          }
        }
      }
    }
    try {
      getDetail(soapFault);
    } catch (JAXBException e) {
      logger.error("********************* ERROR ******************");
      e.printStackTrace();
    }
    return null;
  }
  */
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
      //if ( OPA_EVENT_ERROR.equalsIgnoreCase(detail.getErrorResponse().getCode())){
      ObjectFactory factory = new ObjectFactory();
      AssessResponse assessResponse = factory.createAssessResponse();
      ListEvents listEvents = factory.createListEvents();
      listEvents.getEvent().add(createEvent(detail, factory));
      assessResponse.setEvents(listEvents);
      return assessResponse;
    //}
  }

  private RulebaseEvent createEvent(Detail detail, ObjectFactory factory) {
    RulebaseEvent event = null;
    for ( Event opa12Event : detail.getErrorResponse().getEvents().getEventList()){
      event = factory.createRulebaseEvent();
      if ( "error".equalsIgnoreCase(opa12Event.getName()) ){
        event.setName("Fatal");
      }
      event.setEntityId(opa12Event.getInstanceId());
      event.setMessage(opa12Event.getMessage());

      //Parameters
      RulebaseEvent.Parameters parameters = factory.createRulebaseEventParameters();
      parameters.getValue().add(opa12Event.getParameters().getValue());
      event.setParameters(parameters);

      //Decision Report
      DecisionReportType decisionReportType = opa12Event.getDecisionReport();
      if ( decisionReportType != null ){
        DecisionReport decisionReport = factory.createDecisionReport();
        decisionReport.setReportStyle(decisionReportType.getReportStyle());
        addNodes(decisionReport, decisionReportType, factory);

        event.setDecisionReport(decisionReport);
      }

    }
    return event;
  }

  private void addNodes(DecisionReport opa10DecisionReport,
              DecisionReportType opa18DecisionReportType, ObjectFactory factory) {
    for ( Object object : opa18DecisionReportType.getRelationshipNodeOrAttributeNodeOrAlreadyProvenNode() ){
      if ( object instanceof AttributeNodeType ){
        AttributeNodeType opa12NodeType = ( AttributeNodeType ) object;
        AttributeDecisionNode node = factory.createAttributeDecisionNode();
        createAttributeDecisionNode(node, opa12NodeType, factory);
        opa10DecisionReport.getRelationshipDecisionNodeOrAttributeDecisionNode().add(node);
      }
    }
  }

  private void createAttributeDecisionNode(AttributeDecisionNode node,
                                  AttributeNodeType opa12NodeType, ObjectFactory factory) {
    node.setId(opa12NodeType.getId());
    node.setAttributeId(opa12NodeType.getAttributeId());
    node.setEntityId(opa12NodeType.getInstanceId());
    node.setText(opa12NodeType.getText());
    node.setEntityType(opa12NodeType.getEntityId());

    setAttributeTypeEnum( node, opa12NodeType );

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
    logger.debug("*************detail************* : " + detail);
    return detail;
  }

  /*
  public static void main(String args[])  {
    try {
      File file = new File("/Users/muralidharputtanna/Documents/SoapFault.xml");
      JAXBContext jaxbContext = JAXBContext.newInstance(Detail.class);
      Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
      Detail detail = (Detail) unmarshaller.unmarshal(file);
      System.out.println(detail);
    } catch (JAXBException e) {
      e.printStackTrace();
    }
  }*/

  private Iterable<Node> iterable(final NodeList nodeList) {
    return () -> new Iterator<Node>() {
      private int index = 0;

      @Override
      public boolean hasNext() {
        return index < nodeList.getLength();
      }

      @Override
      public Node next() {
        if (!hasNext())
          throw new NoSuchElementException();
        return nodeList.item(index++);
      }
    };
  }

}
