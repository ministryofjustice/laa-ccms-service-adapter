package uk.gov.justice.laa.ccms.soap.error;

import com.oracle.determinations.server._12_2_1.rulebase.assess.types.AttributeNodeType;
import com.oracle.determinations.server._12_2_1.rulebase.assess.types.DecisionReportType;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class Event {

  @XmlAttribute(name = "entity-id")
  private String entityId;
  @XmlAttribute(name = "instance-id")
  private String instanceId;
  @XmlAttribute(name = "name")
  private String name;
  @XmlElement(name = "message")
  private String message;
  @XmlElement(name = "parameters")
  private Parameters parameters;
  @XmlElement(name = "decision-report")
  private DecisionReportType decisionReport;

  public Event(){}

  public Event(String entityId, String instanceId, String name,
      String message, Parameters parameters, DecisionReportType decisionReport){
    this.entityId = entityId;
    this.instanceId = instanceId;
    this.name = name;
    this.message = message;
    this.parameters = parameters;
    this.decisionReport = decisionReport;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public Parameters getParameters() {
    return parameters;
  }

  public void setParameters(Parameters parameters) {
    this.parameters = parameters;
  }

  public DecisionReportType getDecisionReport() {
    return decisionReport;
  }

  public void setDecisionReport(
      DecisionReportType decisionReport) {
    this.decisionReport = decisionReport;
  }

  public String getEntityId() {
    return entityId;
  }

  public void setEntityId(String entityId) {
    this.entityId = entityId;
  }

  public String getInstanceId() {
    return instanceId;
  }

  public void setInstanceId(String instanceId) {
    this.instanceId = instanceId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("Event: ");
    builder.append("[entityId = " + entityId + "]");
    builder.append("[instanceId = " + instanceId + "]");
    builder.append("[name = " + name + "]");
    builder.append("[Parameters = " + parameters + "]");
    builder.append("DecisionReportType: ");
    builder.append("[ReportStyle = " + decisionReport.getReportStyle() + "]");
    for (Object node : decisionReport.getRelationshipNodeOrAttributeNodeOrAlreadyProvenNode()) {
      if (node instanceof AttributeNodeType) {
        AttributeNodeType node1 = (AttributeNodeType) node;
        builder.append("[id = " + node1.getId() + "]");
        builder.append("[entityId = " + node1.getEntityId() + "]");
        builder.append("[instanceId = " + node1.getInstanceId() + "]");
        builder.append("[HypotheticalInstance = " + node1.isHypotheticalInstance() + "]");
        builder.append("[AttributeId = " + node1.getAttributeId() + "]");
        builder.append("[Type = " + node1.getType() + "]");
        builder.append("[Text = " + node1.getText() + "]");
        builder.append("[Inferred = " + node1.isInferred() + "]");
        builder.append("[NumberVal = " + node1.getNumberVal() + "]");
      }
    }

    return builder.toString();
  }

}
