package uk.gov.justice.laa.ccms.soap.error;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "detail")
@XmlAccessorType(XmlAccessType.FIELD)
public class Detail {

  @XmlElement(name = "error-response")
  private OpaErrorResponse errorResponse;

  public Detail(){}

  public Detail(OpaErrorResponse errorResponse){
    this.errorResponse = errorResponse;
  }

  public OpaErrorResponse getErrorResponse() {
    return errorResponse;
  }

  public void setErrorResponse(OpaErrorResponse errorResponse) {
    this.errorResponse = errorResponse;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("Detail: ");
    builder.append("[OpaErrorResponse = " + errorResponse + "]");
    return builder.toString();
  }

}
