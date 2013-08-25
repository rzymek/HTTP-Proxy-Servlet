package org.mitre.dsmiley.httpproxy;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Date;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.RealmCallback;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;

@WebServlet(urlPatterns = "/*", initParams = @WebInitParam(name = ProxyServlet.P_TARGET_URI, value = "http://localhost:8080/okolab"))
public class RedeployerProxy extends ProxyServlet {
	private static final long serialVersionUID = 1L;
	private String target = "okolab";
	private String user = "rzymek";
	private String pass = "q";

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		System.out.println("RedeployerProxy.service()");
		File deployments = new File(System.getProperty("jboss.server.base.dir"), "deployments");
		File war = new File(deployments, target + ".war");
		File[] monitor = { new File(war, "WEB-INF/classes"), new File(war, "WEB-INF/lib"), };
		long lastDeployed = new File(deployments, target + ".war.deployed").lastModified();
		if (isNewer(lastDeployed, monitor)) {
			new File(deployments, target + ".war.deployed").setLastModified(new Date().getTime());
			System.out.println("redeploying...");
			ModelControllerClient client = createClient(InetAddress.getByName("localhost"), 9999, user, pass, "management");
			ModelNode operation = new ModelNode();
			operation.get("operation").set("redeploy");
			operation.get("address").add("deployment", "okolab.war");
			String result = client.execute(operation).toString();
			System.out.println(result);
		}
		super.service(request, response);
	}

	private boolean isNewer(long lastDeployed, File... entries) {
		for (File entry : entries) {
			if (entry.isDirectory()) {
				File[] files = entry.listFiles();
				if (isNewer(lastDeployed, files)) {
					return true;
				}
			} else {
				boolean newer = entry.lastModified() > lastDeployed;
				String isnewer = newer ? " !!!!!!! " : "";
				String msg = new Date(entry.lastModified()) + " vs. " + new Date(lastDeployed) + " - " + entry.getName() + isnewer;
				System.out.println(msg);
				if(newer) {
					return newer;
				}
			}
		}
		return false;
	}

	static ModelControllerClient createClient(final InetAddress host, final int port, final String username, final String password,
			final String securityRealmName) {

		final CallbackHandler callbackHandler = new CallbackHandler() {

			public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
				for (Callback current : callbacks) {
					if (current instanceof NameCallback) {
						NameCallback ncb = (NameCallback) current;
						ncb.setName(username);
					} else if (current instanceof PasswordCallback) {
						PasswordCallback pcb = (PasswordCallback) current;
						pcb.setPassword(password.toCharArray());
					} else if (current instanceof RealmCallback) {
						RealmCallback rcb = (RealmCallback) current;
						rcb.setText(rcb.getDefaultText());
					} else {
						throw new UnsupportedCallbackException(current);
					}
				}
			}
		};

		return ModelControllerClient.Factory.create(host, port, callbackHandler);
	}
}
