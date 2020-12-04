package uk.gov.justice.laa.ccms.soap.error;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class Events {

  @XmlElement(name = "event")
  private List<Event> eventList;

  public Events(){}

  public Events(List<Event> eventList){
    this.eventList = eventList;
  }

  public List<Event> getEventList() {
    if ( eventList ==  null ){
      eventList = new ArrayList<Event>();
    }
    return eventList;
  }

  public void setEventList(List<Event> eventList) {
    this.eventList = eventList;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("Events: ");
    for ( Event event : eventList ) {
      builder.append("\n");
      builder.append(event);
    }
    return builder.toString();
  }

}
