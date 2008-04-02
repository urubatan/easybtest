/**
* Gant script that runs the Grails easyb tests
*
* @author Rodrigo Urubatan Ferreira Jardim
*
*/

Ant.property(environment: "env")
grailsHome = Ant.antProject.properties."env.GRAILS_HOME"
grailsApp = null
appCtx = null
def antProperty = Ant.project.properties

includeTargets << new File("${grailsHome}/scripts/Bootstrap.groovy")

generateLog4jFile = true

target('default': "Run a Grails applications easyb tests") {

  testApp()
}


target(testApp: "The test app implementation target") {
  depends(packageApp, classpath)

  antTestSource = Ant.path {
    fileset ( dir : "${basedir}/test/behavior" , includes : '**/*' ){
      include(name:"** /*Story.groovy")
      include(name:"** /*.story")
      include(name:"** /*Specification.groovy")
      include(name:"** /*.specification")
    }
  }

  testDir = "${basedir}/test/reports"

  if(config.grails.testing.reports.destDir) {
    testDir = config.grails.testing.reports.destDir
  }

  Ant.mkdir(dir: testDir)
  Ant.mkdir(dir: "${testDir}/xml")
  Ant.mkdir(dir: "${testDir}/plain")


  Ant.java(classpathref:"grails.classpath",classname:"org.disco.easyb.BehaviorRunner",fork:true){
    arg(value:"-xmleasyb")
    arg(value:"${testDir}/xml/easyb.xml")
    arg(value:"-txtspecification")
    arg(value:"${testDir}/plain/specifications.txt")
    arg(value:"-txtstory")
    arg(value:"${testDir}/plain/stories.txt")
    antTestSource.list().each{
      arg(value:it)
    }
    jvmarg(value:"-Dbasedir=${basedir}")
  }

}
