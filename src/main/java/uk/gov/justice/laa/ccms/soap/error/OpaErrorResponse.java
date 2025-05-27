package uk.gov.justice.laa.ccms.soap.error;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class OpaErrorResponse {
  @XmlElement(name = "code")
  private String code;
  @XmlElement(name = "message")
  private String message;
  @XmlElement(name = "events")
  private Events events;

  public OpaErrorResponse(){}

  public OpaErrorResponse(String code, String message, Events events){
    this.code = code;
    this.message = message;
    this.events = events;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public Events getEvents() {
    return events;
  }

  public void setEvents(Events events) {
    this.events = events;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("OpaErrorResponse: ");
    builder.append("[code = " + code + "]");
    builder.append("[message = " + message + "]");
    builder.append("[Events = " + events + "]");
    return builder.toString();
  }
}
