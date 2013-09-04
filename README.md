JBoss 7 Redeploy changes on refresh
===============================

Instalation
----------

1. Deploy this web app
    git clone https://github.com/rzymek/.git
    cd redeploy-proxy
    mvn package
    cp target/*.war $JBOSS7/standalone/deployments/redep.war
		touch $JBOSS7/standalone/deployments/redep.war.dodeploy
2. Deploy your web app as an exploaded archive

   mvn war:inplace   
   cd $JBOSS7/standalone/deployments/
   ln -s $MYAPP/src/main/webapp myapp.war
   touch myapp.war.dodeploy
3. Go to http://localhost:8080/redep/. You should see your application - that is the same output as http://localhost:8080/myapp/. On any request a check is made. If any time a file in `WEB-INF/classes` or `WEB-INF/lib` is changed your application will be redeployed.


