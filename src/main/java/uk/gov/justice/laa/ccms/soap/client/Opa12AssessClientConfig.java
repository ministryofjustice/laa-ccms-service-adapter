package uk.gov.justice.laa.ccms.soap.client;

import com.oracle.determinations.server._12_2.rulebase.assess.types.OdsAssessServiceGeneric122MeansAssessmentV12Type;
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

@SuppressWarnings("deprecation")
@Configuration
public class Opa12AssessClientConfig {

  private static final Logger logger = LoggerFactory.getLogger(Opa12AssessClientConfig.class);

  @Value("${client.opa12Assess.means.address}")
  private String meansAddress;

  @Value("${client.opa12Assess.billing.address}")
  private String billingAddress;

  @Value("${client.opa12Assess.security.user.name}")
  private String userName;

  @Autowired
  private ClientPasswordCallback clientPasswordCallback;

  private static final int DEFAULT_LIMIT = -1;

  @Bean(name = "opa12MeansAssessServiceProxy")
  public OdsAssessServiceGeneric122MeansAssessmentV12Type opa12MeansAssessServiceProxy() {
    return getOdsAssessServiceGeneric122MeansAssessmentV12Type(meansAddress);
  }

  @Bean(name = "opa12BillingAssessServiceProxy")
  public OdsAssessServiceGeneric122MeansAssessmentV12Type opa12BillingAssessServiceProxy() {
    return getOdsAssessServiceGeneric122MeansAssessmentV12Type(billingAddress);
  }

  private OdsAssessServiceGeneric122MeansAssessmentV12Type getOdsAssessServiceGeneric122MeansAssessmentV12Type(
      String address) {
    logger.debug("Address:" + address);

    JaxWsProxyFactoryBean jaxWsProxyFactoryBean = new JaxWsProxyFactoryBean();
    jaxWsProxyFactoryBean.setServiceClass(OdsAssessServiceGeneric122MeansAssessmentV12Type.class);
    jaxWsProxyFactoryBean.setAddress(address);
    jaxWsProxyFactoryBean.getInInterceptors().add(new LoggingInInterceptor(DEFAULT_LIMIT));
    jaxWsProxyFactoryBean.getOutInterceptors().add(new LoggingOutInterceptor(DEFAULT_LIMIT));

    OdsAssessServiceGeneric122MeansAssessmentV12Type proxy = (OdsAssessServiceGeneric122MeansAssessmentV12Type) jaxWsProxyFactoryBean
        .create();

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
