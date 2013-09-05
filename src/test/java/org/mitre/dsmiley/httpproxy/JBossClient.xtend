package org.mitre.dsmiley.httpproxy

import java.net.InetAddress
import org.jboss.^as.controller.client.ModelControllerClient
import org.jboss.dmr.ModelNode
import org.junit.Ignore
import org.junit.Test

class JBossClient {

	def void main(String[] args) {
		new JBossClient().run()
	}

	@Test
	@Ignore
	def void run() {
		val client = ModelControllerClient.Factory.create(InetAddress.getByName('localhost'), 9999)
		var result = client.execute(
			new ModelNode() => [
				get('address').add('deployment', 'okolab.war')
				get('operation').set('redeploy')
			])
		println(result.get('outcome').asString.equals('success'))
		println(result.get('failure-description').asString)
	}
}
