/**
* Gant script that runs the Grails easyb tests
*
* @author Rodrigo Urubatan Ferreira Jardim
*
*/

import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU;
import grails.util.GrailsUtil as GU;
import grails.util.GrailsWebUtil as GWU
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.support.*
import java.lang.reflect.Modifier;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.codehaus.groovy.grails.commons.spring.GrailsRuntimeConfigurator as GRC;
import org.apache.tools.ant.taskdefs.optional.junit.*
import org.springframework.mock.web.*
import org.springframework.core.io.*
import org.springframework.web.context.request.RequestContextHolder;
import org.codehaus.groovy.grails.plugins.*
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.transaction.support.TransactionCallback
import org.springframework.transaction.TransactionStatus
import org.apache.commons.logging.LogFactory
//import br.com.urubatan.easybtest.BehaviorRunner;
import org.disco.easyb.report.Report;


Ant.property(environment: "env")
grailsHome = Ant.antProject.properties."env.GRAILS_HOME"
grailsApp = null
appCtx = null

includeTargets << new File("${grailsHome}/scripts/Bootstrap.groovy")

generateLog4jFile = true

target('default': "Run a Grails applications easyb tests") {

  testApp()
}


target(testApp: "The test app implementation target") {
  depends(packageApp, classpath, checkVersion, configureProxy, bootstrap)

  antTestSource = Ant.path {
    fileset ( dir : "${basedir}/test/behavior" , includes : '**/*' ){
      include(name:"** /*Story.groovy")
      include(name:"** /*.story")
      include(name:"** /*Specification.groovy")
      include(name:"** /*.specification")
    }
  }
  testSource = []
  antTestSource.list().each{
    testSource.add(new File(it))
  }

  testDir = "${basedir}/test/reports"

  if(config.grails.testing.reports.destDir) {
    testDir = config.grails.testing.reports.destDir
  }

  Ant.mkdir(dir: testDir)
  Ant.mkdir(dir: "${testDir}/xml")
  Ant.mkdir(dir: "${testDir}/plain")

  reports = [new Report(location:"${testDir}/xml/easyb.xml",format:"xml",type:"easyb"),new Report(location:"${testDir}/plain/stories.xml",format:"txt",type:"story"),new Report(location:"${testDir}/plain/specifications.txt",format:"txt",type:"specification")];

  br.com.urubatan.easybtest.BehaviorRunner br = new br.com.urubatan.easybtest.BehaviorRunner(reports);
  br.runBehavior(testSource)

}
