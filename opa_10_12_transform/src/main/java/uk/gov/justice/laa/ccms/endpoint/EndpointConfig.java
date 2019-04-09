package uk.gov.justice.laa.ccms.endpoint;

import java.util.HashMap;
import java.util.Map;

import javax.xml.ws.Endpoint;

import org.apache.cxf.Bus;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import com.oracle.determinations.server._10_0.rulebase.types.OpadsRulebaseGeneric;

@SuppressWarnings("deprecation")
@Configuration
@PropertySource(value = "classpath:application-env.properties")
@PropertySource(value = "classpath:application.properties")
public class EndpointConfig {

   @Autowired
   private Bus bus;

   @Value("${server.opa10Assess.security.user.name}")
   private String userName;

   @Value("${server.opa10Assess.path}")
   private String path;

   @Autowired
   private ServerPasswordCallback serverPasswordCallback;

   @Autowired
   private OpadsRulebaseGeneric opadsRulebaseGeneric;

   @Bean
   public Endpoint endpoint() {
      EndpointImpl endpoint = new EndpointImpl(bus, opadsRulebaseGeneric);
      endpoint.publish(path);
      
      endpoint.getInInterceptors().add(new LoggingInInterceptor());
      endpoint.getOutInterceptors().add(new LoggingOutInterceptor());

      Map<String, Object> inProps = new HashMap<String, Object>();
      inProps.put(WSHandlerConstants.ACTION, WSConstants.USERNAME_TOKEN_LN);

      inProps.put(WSHandlerConstants.PASSWORD_TYPE, WSConstants.PW_TEXT);
      inProps.put(WSHandlerConstants.USER, userName);
      inProps.put(WSHandlerConstants.PW_CALLBACK_REF, serverPasswordCallback);

      //endpoint.getInInterceptors().add(new WSS4JInInterceptor(inProps));

      return endpoint;
   }
}