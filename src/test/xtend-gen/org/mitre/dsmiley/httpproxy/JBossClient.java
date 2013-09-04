package org.mitre.dsmiley.httpproxy;

import java.net.InetAddress;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.InputOutput;
import org.eclipse.xtext.xbase.lib.ObjectExtensions;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.ModelControllerClient.Factory;
import org.jboss.dmr.ModelNode;
import org.junit.Ignore;
import org.junit.Test;

@SuppressWarnings("all")
public class JBossClient {
  public void main(final String[] args) {
    JBossClient _jBossClient = new JBossClient();
    _jBossClient.run();
  }
  
  @Test
  @Ignore
  public void run() {
    try {
      InetAddress _byName = InetAddress.getByName("localhost");
      final ModelControllerClient client = Factory.create(_byName, 9999);
      ModelNode _modelNode = new ModelNode();
      final Procedure1<ModelNode> _function = new Procedure1<ModelNode>() {
          public void apply(final ModelNode it) {
            ModelNode _get = it.get("address");
            _get.add("deployment", "okolab.war");
            ModelNode _get_1 = it.get("operation");
            _get_1.set("redeploy");
          }
        };
      ModelNode _doubleArrow = ObjectExtensions.<ModelNode>operator_doubleArrow(_modelNode, _function);
      ModelNode result = client.execute(_doubleArrow);
      ModelNode _get = result.get("outcome");
      String _asString = _get.asString();
      boolean _equals = _asString.equals("success");
      InputOutput.<Boolean>println(Boolean.valueOf(_equals));
      ModelNode _get_1 = result.get("failure-description");
      String _asString_1 = _get_1.asString();
      InputOutput.<String>println(_asString_1);
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
}
