package uk.gov.justice.laa.ccms.soap.client;

import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import com.oracle.determinations.server._12_2.rulebase.assess.types.OdsAssessServiceGeneric122MeansAssessmentV12Type;

@SuppressWarnings("deprecation")
@Configuration
@PropertySource(value = "classpath:application-env.properties")
@PropertySource(value = "classpath:application.properties")
public class Opa12AssessClientConfig {
   private static final Logger logger = LoggerFactory.getLogger(Opa12AssessClientConfig.class);

   @Value("${client.opa12Assess.address}")
   private String address;

   @Value("${client.opa12Assess.security.user.name}")
   private String userName;

   @Autowired
   private ClientPasswordCallback clientPasswordCallback;

   @Bean(name = "opa12AssessServiceProxy")
   public OdsAssessServiceGeneric122MeansAssessmentV12Type opa12AssessServiceProxy() {

      logger.debug("Address:" + address);

      JaxWsProxyFactoryBean jaxWsProxyFactoryBean = new JaxWsProxyFactoryBean();
      jaxWsProxyFactoryBean.setServiceClass(OdsAssessServiceGeneric122MeansAssessmentV12Type.class);
      jaxWsProxyFactoryBean.setAddress(address);
      jaxWsProxyFactoryBean.getInInterceptors().add(new LoggingInInterceptor());
      jaxWsProxyFactoryBean.getOutInterceptors().add(new LoggingOutInterceptor());

      OdsAssessServiceGeneric122MeansAssessmentV12Type proxy = (OdsAssessServiceGeneric122MeansAssessmentV12Type) jaxWsProxyFactoryBean.create();

      Map<String, Object> outProps = new HashMap<String, Object>();
      outProps.put(WSHandlerConstants.ACTION, WSConstants.USERNAME_TOKEN_LN);
      outProps.put(WSHandlerConstants.PASSWORD_TYPE, WSConstants.PW_TEXT);
      outProps.put(WSHandlerConstants.USER, userName);
      outProps.put(WSHandlerConstants.PW_CALLBACK_REF, clientPasswordCallback);

      org.apache.cxf.endpoint.Client client = ClientProxy.getClient(proxy);
      client.getOutInterceptors().add(new WSS4JOutInterceptor(outProps));
      return proxy;
   }

}