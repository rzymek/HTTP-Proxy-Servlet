package redep

import java.io.File
import java.io.IOException
import java.net.InetAddress
import java.net.URI
import java.util.Date
import javax.servlet.ServletException
import javax.servlet.annotation.WebInitParam
import javax.servlet.annotation.WebServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import org.jboss.^as.controller.client.ModelControllerClient
import org.jboss.dmr.ModelNode
import org.mitre.dsmiley.httpproxy.ProxyServlet

@WebServlet(name='redep', urlPatterns='/*', initParams=@WebInitParam(name=ProxyServlet.P_TARGET_URI, value='http://localhost:9000'))
class Redeployer extends ProxyServlet {
	var String target = null
	var ModelControllerClient client

	protected override service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			if (client == null) {
				client = ModelControllerClient.Factory.create(InetAddress.getByName('localhost'), 9999)
			}
			if (target == null) {
				var result = client.execute(
					new ModelNode() => [
						get('operation').set('read-children-resources')
						get('child-type').set('deployment')
					])
				val wars = result.get('result').asList.map[it.keys.head].map[it.replaceFirst('.war$', '')].filter[
					it != 'redep']
				if (wars.size != 1) {
					response.contentType = 'text/plain'
					response.writer.println('Place deploy one and only one .war application (besides redep.war)')
					return;
				} else {
					target = wars.head
				}
			}
			targetUri = new URI('''http://«request.serverName»:«request.serverPort»/«target»''')
			var deployments = new File(System.getProperty('jboss.server.base.dir'), 'deployments')
			var war = new File(deployments, target + '.war')
			var monitor = #[new File(war, 'WEB-INF/classes'), new File(war, 'WEB-INF/lib')]
			var deployed = new File(deployments, target + '.war.redep')
			var lastDeployed = deployed.lastModified
			if (isNewer(lastDeployed, monitor) || request.getParameter('reload') !== null) {
				deployed.createNewFile
				deployed.lastModified = new Date().time
				println('redeploying...')
				var result = client.execute(
					new ModelNode() => [
						get('address').add('deployment', target + '.war')
						get('operation').set('redeploy')
					])

				//				if(result.get('outcome'))
				if(!result.get('outcome').asString.equals('success')) {
					response.contentType = 'text/plain'
					response.writer.println('''
					Redeployment failed: «result.get('failure-description')?.asString»
					
					Full response: «result»
					''')	
					return
				}				
				println('redeployment finished:' + result)
			}
			super.service(request, response)
		} catch (Exception e) {
			e.printStackTrace(response.writer)
		}
	}

	private def boolean isNewer(long lastDeployed, File... entries) {
		for (entry : entries) {
			if (entry.directory) {
				var files = entry.listFiles
				if (isNewer(lastDeployed, files)) {
					return true;
				}
			} else {
				var newer = entry.lastModified > lastDeployed
				var msg = new Date(entry.lastModified) + ' vs. ' + new Date(lastDeployed) + ' - ' + entry.name
				if (newer) {
					println(msg)
					return newer;
				}
			}
		}
		false
	}
}
