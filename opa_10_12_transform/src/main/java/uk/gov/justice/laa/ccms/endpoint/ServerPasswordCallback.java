package uk.gov.justice.laa.ccms.endpoint;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ServerPasswordCallback implements CallbackHandler {

   @Value("${server.opa10Assess.security.user.password}")
   private String password;
   
   private static final Logger logger = LoggerFactory.getLogger(ServerPasswordCallback.class);

   @Override
   public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
      logger.info("password:" + password);
      for (int i = 0; i < callbacks.length; i++) {
         WSPasswordCallback pc = (WSPasswordCallback) callbacks[i];
         pc.setPassword(password);
      }
   }
}