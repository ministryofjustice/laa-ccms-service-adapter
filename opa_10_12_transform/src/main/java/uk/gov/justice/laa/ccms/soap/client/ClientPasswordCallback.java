package uk.gov.justice.laa.ccms.soap.client;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ClientPasswordCallback implements CallbackHandler {

   @Value("${client.opa12Assess.security.user.password}")
   private String password;

   @Override
   public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
      for (int i = 0; i < callbacks.length; i++) {
         WSPasswordCallback pc = (WSPasswordCallback) callbacks[i];
         pc.setPassword(password);
      }
   }
}