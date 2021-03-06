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
import org.codehaus.groovy.grails.commons.ApplicationHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.orm.hibernate3.SessionFactoryUtils;

import org.disco.easyb.report.Report;
import groovy.lang.GroovyShell;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.orm.hibernate3.SessionHolder;



import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.disco.easyb.listener.BehaviorListener;
import org.disco.easyb.listener.DefaultListener;
import org.disco.easyb.report.XmlReportWriter;
import org.disco.easyb.report.ReportWriter;
import org.disco.easyb.report.Report;
import org.disco.easyb.report.TxtSpecificationReportWriter;
import org.disco.easyb.report.TxtStoryReportWriter;
import org.disco.easyb.util.BehaviorStepType;
import org.disco.easyb.util.ReportFormat;
import org.disco.easyb.util.ReportType;
import org.disco.easyb.*;


Ant.property(environment: "env")
grailsHome = Ant.antProject.properties."env.GRAILS_HOME"
grailsApp = null
appCtx = null

includeTargets << new File ( "${grailsHome}/scripts/Package.groovy" )
includeTargets << new File ( "${grailsHome}/scripts/Bootstrap.groovy" )


generateLog4jFile = true

target('default': "Run a Grails applications easyb tests") {
  depends( checkVersion, configureProxy, packageApp, classpath)
  classLoader = new URLClassLoader([classesDir.toURL()] as URL[], rootLoader)
  Thread.currentThread().setContextClassLoader(classLoader)
  loadApp()
  configureApp()
  testApp()
}


target(testApp: "The test app implementation target") {

  antTestSource = Ant.path {
    fileset ( dir : "${basedir}/test/behavior"  ){
      include(name:"**/*Story.groovy")
      include(name:"**/*.story")
      include(name:"**/*Specification.groovy")
      include(name:"**/*.specification")
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

  reports = [new Report(location:"${testDir}/xml/easyb.xml",format:"xml",type:"easyb"),new Report(location:"${testDir}/plain/stories.txt",format:"txt",type:"story"),new Report(location:"${testDir}/plain/specifications.txt",format:"txt",type:"specification")];

  BehaviorRunner br = new BehaviorRunner(reports,appCtx,ApplicationHolder.application);
  br.runBehavior(testSource)

}

class BehaviorRunner {

  def reports = [];
    Object appCtx;
    Object grailsApp;

    public BehaviorRunner() {
      this(null);
    }

    public BehaviorRunner(List<Report> reports, Object appCtx, Object grailsApp) {
      this.appCtx = appCtx;
      this.grailsApp = grailsApp;
      this.reports = addDefaultReports(reports);
    }

    /**
    * @param specs collection of files that contain the specifications
    * @throws Exception if unable to write report file
    */
    public void runBehavior(Collection<File> specs) throws Exception {

      BehaviorListener listener = new DefaultListener();

      executeSpecifications(specs, listener);

      System.out.println("\n" +
      //prints "1 behavior run" or "x behaviors run"
      (listener.getBehaviorCount() > 1 ? listener.getBehaviorCount()  + " total behaviors run" : "1 behavior run")
      //outer ternary prints either 1..X failure(s) or no failures
      //inner ternary determines if more than one failure and makes it plural if so
      + (listener.getFailedBehaviorCount() > 0 ? " with "
      + (listener.getFailedBehaviorCount() == 1 ? "1 failure" : listener.getFailedBehaviorCount() + " failures") : " with no failures"));

      produceReports(listener);

      if (listener.getFailedBehaviorCount() > 0) {
        System.exit(-6);
      }
    }
    /**
    *
    * @param listener
    */
    private void produceReports(BehaviorListener listener) {

      for (Report report : reports) {
        if (report.getFormat().concat(report.getType()).equals(Report.XML_EASYB)) {
          new XmlReportWriter(report, listener).writeReport();
        } else if (report.getFormat().concat(report.getType()).equals(Report.TXT_STORY)) {
          new TxtStoryReportWriter(report, listener).writeReport();
        } else if (report.getFormat().concat(report.getType()).equals(Report.TXT_SPECIFICATION)) {
          new TxtSpecificationReportWriter(report, listener).writeReport();
        }
      }

    }

    /**
    *
    * @param behaviorFiles
    * @param listener
    * @throws IOException
    */
    private void executeSpecifications(final Collection<File> behaviorFiles, final BehaviorListener listener) throws IOException {
      for (File behaviorFile : behaviorFiles) {
        def sessionFactory = appCtx.getBean("sessionFactory")
        Session session = SessionFactoryUtils.getSession(sessionFactory, true);
        TransactionSynchronizationManager.bindResource(sessionFactory, new SessionHolder(session));
        session.setFlushMode(FlushMode.AUTO)
        def behavior = null;
          try {
            behavior = BehaviorFactory.createBehavior(behaviorFile);
          } catch(IllegalArgumentException iae) {
            System.out.println(iae.getMessage());
            System.exit(-1);
          }

          long startTime = System.currentTimeMillis();
          System.out.println(
          "Running ${behavior.getPhrase()} ${((behavior instanceof Story) ? ' story' : ' specification')} (${behaviorFile.getName()})"
          );


          BehaviorStep currentStep;
          GroovyShell g = null;
          if (behavior instanceof Story) {
            currentStep = listener.startStep(BehaviorStepType.STORY, behavior.getPhrase());
            g = new GroovyShell(grailsApp.classLoader,StoryBinding.getBinding(listener));
          } else {
            currentStep = listener.startStep(BehaviorStepType.SPECIFICATION, behavior.getPhrase());
            g = new GroovyShell(grailsApp.classLoader,SpecificationBinding.getBinding(listener));
          }
          g.getContext().setVariable("grailsApp",grailsApp);
          g.getContext().setVariable("appCtx",appCtx);
          g.evaluate(behaviorFile);
          listener.stopStep();

          long endTime = System.currentTimeMillis();

          printMetrics(behavior, startTime, currentStep, endTime);
          SessionHolder sessionHolder =	(SessionHolder) TransactionSynchronizationManager.unbindResource(sessionFactory);
          SessionFactoryUtils.closeSession(sessionHolder.getSession());


        }
      }

      private void printMetrics(Behavior behavior, long startTime, BehaviorStep currentStep, long endTime) {
        if(behavior instanceof Story) {
          System.out.println((currentStep.getFailedScenarioCountRecursively() == 0 ? "" : "FAILURE ") +
          "Scenarios run: " + currentStep.getScenarioCountRecursively() +
          ", Failures: " + currentStep.getFailedScenarioCountRecursively() +
          ", Pending: " + currentStep.getPendingScenarioCountRecursively() +
          ", Time Elapsed: " + (endTime - startTime) / 1000f + " sec");
        } else {
          System.out.println((currentStep.getFailedSpecificationCountRecursively() == 0 ? "" : "FAILURE ") +
          "Specifications run: " + currentStep.getSpecificationCountRecursively() +
          ", Failures: " + currentStep.getFailedSpecificationCountRecursively() +
          ", Pending: " + currentStep.getPendingSpecificationCountRecursively() +
          ", Time Elapsed: " + (endTime - startTime) / 1000f + " sec");
        }
      }

      /**
      * @param args the command line arguments
      */
      public static void main(String[] args) {
        Options options = getOptionsForMain();

        try {
          CommandLine commandLine = getCommandLineForMain(args, options);
          validateArguments(commandLine);

          BehaviorRunner runner = new BehaviorRunner(getConfiguredReports(commandLine));

          runner.runBehavior(getFileCollection(commandLine.getArgs()));
        } catch (IllegalArgumentException iae) {
          System.out.println(iae.getMessage());
          handleHelpForMain(options);
        } catch (ParseException pe) {
          System.out.println(pe.getMessage());
          handleHelpForMain(options);
        } catch (Exception e) {
          System.err.println("There was an error running the script");
          e.printStackTrace(System.err);
          System.exit(-6);
        }
      }

      private static void validateArguments(CommandLine commandLine) throws IllegalArgumentException {
        if (commandLine.getArgs().length == 0) {
          throw new IllegalArgumentException("Required arguments missing.");
        }
      }

      private static List<Report> getConfiguredReports(CommandLine line) {

        def configuredReports = new ArrayList<Report>();
          if (line.hasOption(Report.TXT_STORY)) {
            Report report = new Report();
            report.setFormat(ReportFormat.TXT.format());
            if (line.getOptionValue(Report.TXT_STORY) == null) {
              report.setLocation("easyb-story-report.txt");
            } else {
              report.setLocation(line.getOptionValue(Report.TXT_STORY));
            }
            report.setType(ReportType.STORY.type());

            configuredReports.add(report);
          }

          if (line.hasOption(Report.TXT_SPECIFICATION)) {
            Report report = new Report();
            report.setFormat(ReportFormat.TXT.format());
            if (line.getOptionValue(Report.TXT_SPECIFICATION) == null) {
              report.setLocation("easyb-specification-report.txt");
            } else {
              report.setLocation(line.getOptionValue(Report.TXT_SPECIFICATION));
            }
            report.setType(ReportType.SPECIFICATION.type());

            configuredReports.add(report);
          }

          if (line.hasOption(Report.XML_EASYB)) {
            Report report = new Report();
            report.setFormat(ReportFormat.XML.format());
            if (line.getOptionValue(Report.XML_EASYB) == null) {
              report.setLocation("easyb-report.xml");
            } else {
              report.setLocation(line.getOptionValue(Report.XML_EASYB));
            }
            report.setType(ReportType.EASYB.type());

            configuredReports.add(report);
          }

          return configuredReports;
        }

        /**
        * @param paths locations of the specifications to be loaded
        * @return collection of files where the only element is the file of the spec to be run
        */
        private static Collection<File> getFileCollection(String[] paths) {
          def coll = new ArrayList<File>();
            for (String path : paths) {
              coll.add(new File(path));
            }
            return coll;
          }

          /**
          * @param options options that are available to this specification runner
          */
          private static void handleHelpForMain(Options options) {
            new HelpFormatter().printHelp("BehaviorRunner my/path/to/MyFile.groovy", options);
          }

          /**
          * @param args    command line arguments passed into main
          * @param options options that are available to this specification runner
          * @return representation of command line arguments passed in that match the available options
          * @throws ParseException if there are any problems encountered while parsing the command line tokens
          */
          private static CommandLine getCommandLineForMain(String[] args, Options options) throws ParseException {
            CommandLineParser commandLineParser = new GnuParser();
            return commandLineParser.parse(options, args);
          }

          /**
          * @return representation of a collection of Option objects, which describe the possible options for a command-line.
          */
          private static Options getOptionsForMain() {
            Options options = new Options();

            //noinspection AccessStaticViaInstance
            Option xmleasybreport = OptionBuilder.withArgName("file").hasOptionalArg()
            .withDescription("create an easyb report in xml format").create(Report.XML_EASYB);
            options.addOption(xmleasybreport);

            //noinspection AccessStaticViaInstance
            Option storyreport = OptionBuilder.withArgName("file").hasOptionalArg()
            .withDescription("create a story report").create(Report.TXT_STORY);
            options.addOption(storyreport);

            //noinspection AccessStaticViaInstance
            Option behaviorreport = OptionBuilder.withArgName("file").hasOptionalArg()
            .withDescription("create a behavior report").create(Report.TXT_SPECIFICATION);
            options.addOption(behaviorreport);

            return options;
          }

          private List<Report> addDefaultReports(List<Report> userConfiguredReports) {
            def configuredReports = new ArrayList<Report>();

              if (userConfiguredReports != null) {
                configuredReports.addAll(userConfiguredReports);
              }

              return configuredReports;
            }
          }
