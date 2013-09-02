package redep;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;
import org.mitre.dsmiley.httpproxy.ProxyServlet;

@WebServlet(name = "redep", urlPatterns = "/*", initParams = @WebInitParam(name = ProxyServlet.P_TARGET_URI, value = "http://localhost:9000"))
public class Redeployer extends ProxyServlet {
	private static final long serialVersionUID = 1L;
	private String target = null;
	private ModelControllerClient client;

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			if (client == null) {
				// http://www.alemoi.com/dev/httpaccess/
				client = ModelControllerClient.Factory.create(InetAddress.getByName("localhost"), 9999);
			}
			if (target == null) {
				ModelNode operation = new ModelNode();
				operation.get("operation").set("read-children-resources");
				operation.get("child-type").set("deployment");
				
				String result = client.execute(operation).toString();
				System.out.println(result);
				target = "okolab";
			}
			targetUri = new URI("http://" + request.getServerName() + ":" + request.getServerPort() + "/" + target);
			File deployments = new File(System.getProperty("jboss.server.base.dir"), "deployments");
			File war = new File(deployments, target + ".war");
			File[] monitor = { new File(war, "WEB-INF/classes"), new File(war, "WEB-INF/lib"), };
			File deployed = new File(deployments, target + ".war.redep");
			long lastDeployed = deployed.lastModified();
			if (isNewer(lastDeployed, monitor) || request.getParameter("reload") != null) {
				deployed.createNewFile();
				deployed.setLastModified(new Date().getTime());
				System.out.println("redeploying...");

				ModelNode operation = new ModelNode();
				operation.get("address").add("deployment", target + ".war");
				operation.get("operation").set("redeploy");

				String result = client.execute(operation).toString();
				System.out.println("redeployment finished:" + result);
			}
			super.service(request, response);
		} catch (Exception e) {
			e.printStackTrace(response.getWriter());
		}
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
				if (newer) {
					System.out.println(msg);
					return newer;
				}
			}
		}
		return false;
	}
}
