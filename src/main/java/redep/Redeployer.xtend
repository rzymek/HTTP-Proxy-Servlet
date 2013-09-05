package redep

import java.io.File
import java.io.IOException
import java.net.InetAddress
import java.net.URI
import java.util.Date
import java.util.logging.Logger
import javax.servlet.ServletException
import javax.servlet.annotation.WebInitParam
import javax.servlet.annotation.WebServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import org.jboss.^as.controller.client.ModelControllerClient
import org.jboss.dmr.ModelNode
import org.mitre.dsmiley.httpproxy.ProxyServlet

@WebServlet(urlPatterns='/*', initParams=@WebInitParam(name=ProxyServlet.P_TARGET_URI, value='http://localhost:9000'))
class Redeployer extends ProxyServlet {
	static val log = Logger.getLogger(typeof(Redeployer).name)

	var String target = null
	var ModelControllerClient client = null

	override service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			if (request.getParameter('redep-reset') != null) {
				client = null;
				target = null;				
			}
			if (client == null) {
				client = ModelControllerClient.Factory.create(InetAddress.getByName('localhost'), 9999)
			}
			val targetParam = request.getParameter('redep-target')
			if(targetParam != null) {
				target = targetParam
			}
			if (target == null) {
				var result = client.execute(
					new ModelNode() => [
						get('operation').set('read-children-resources')
						get('child-type').set('deployment')
					])
				
				val wars = result.get('result').asList			// 'my.war' => { name: 'my.war', ...}
					.map[node | node.keys.map[node.get(it)]]  	
					.flatten							     	//  { name: 'my.war', ... }
					.filter[get('content').asList.size==1] 		//is not an EAR, just a single entity 
					.filter[!get('content').get(0).get('archive').asBoolean]//is exploaded 
					.filter[get('subsystem').keys.contains('web')] 			//is a web-app
					.map[get('name').asString.replaceFirst('[.]war$','')]
					.filter[!servletContext.contextPath.equals('/'+it)]		//is not this web-app
				if (wars.empty) {
					response.contentType = 'text/html'
					response.writer.println('''
					<html><head><title>Redeployer</title></head><body>
					<h3>No other exploaded web applications deployed.</h3><br/><br/>
					Here are all the deployments:<ul>
					«FOR dep : result.get('result').asList»
					<li><pre>«dep»</pre></li>
					«ENDFOR»
					</ul></body></html>''')
					return;
				}else if (wars.size != 1) {
					response.contentType = 'text/html'
					response.writer.println('''
					<html><head><title>Redeployer</title></head><body>
					Select web application for on-refresh-redeployment:<br/><ul>
					«FOR war : wars»
					<li><a href="?redep-target=«war»">«war»</a><br/></li>
					«ENDFOR»
					</ul></body></html>''')
					return;
				} else {
					target = wars.head
				}
			}
			targetUri = new URI('''http://«request.serverName»:«request.serverPort»/«target»''')
			var deployments = new File(System.getProperty('jboss.server.base.dir'), 'deployments')
			var war = new File(deployments, target + '.war')
			var monitor = #[new File(war, 'WEB-INF/classes'), new File(war, 'WEB-INF/lib')]
			val lastDeployed = servletContext.getAttribute(target+'.lastDeployed') as Long ?: Long.valueOf(0);
			if (isNewer(lastDeployed, monitor) || request.getParameter('redep') !== null) {
				servletContext.setAttribute(target+'.lastDeployed', new Date().time)
				var result = client.execute(
					new ModelNode() => [
						get('address').add('deployment', target + '.war')
						get('operation').set('redeploy')
					])

				if(!result.get('outcome').asString.equals('success')) {
					response.contentType = 'text/plain'
					response.writer.println('''
					Redeployment failed: «result.get('failure-description')?.asString»
					
					Full response: «result»
					''')	
					return
				}				
				log.info('redeployment finished:' + result)
			}
			super.service(request, response)
		} catch (Exception e) {
			e.printStackTrace(response.writer)
		}
	}
	
	private def boolean isNewer(long lastDeployed, File... entries) {
		if(entries.filter[file].exists[lastModified > lastDeployed]) {
			true			
		}else{
			entries.filter[directory].map[listFiles].exists[isNewer(lastDeployed, it)]
		}
	}

}
