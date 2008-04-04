
class EasybtestGrailsPlugin {
  def version = 0.2;
  def author = "Rodrigo Uruatan Ferreira Jardim";
  def authorEmail = "blog@urubatan.com.br";
  def title = "This plugin enables the testing of the application using the BDD easyb library";
  def dependsOn = [:];
  def description = '''\
    This plugin enables the use of Behavior Driven Development to test your Grails Application,
    The library behind this is easyb, easyb is a BDD library written in groovy.
    For example, you can test your app like this:

    given "an invalid zip code", {
      invalidzipcode = "221o1"
    }
    and "given the zipcodevalidator is initialized", {
      zipvalidate = new ZipCodeValidator()
    }
    when "validate is invoked with the invalid zip code", {
      value = zipvalidate.validate(invalidzipcode)
    }
    then "the validator instance should return false", {
      value.shouldBe false
    }
    '''
  def documentation = "http://www.urubatan.info/tag/easyb-test/"

  def doWithSpring = {
  // TODO Implement runtime spring config (optional)
  }

  def doWithApplicationContext = { applicationContext ->
  // TODO Implement post initialization spring config (optional)
  }

  def doWithWebDescriptor = { xml ->
  // TODO Implement additions to web.xml (optional)
  }

  def doWithDynamicMethods = { ctx ->
    // TODO Implement registering dynamic methods to classes (optional)
  }

  def onChange = { event ->
  // TODO Implement code that is executed when this class plugin class is changed
  // the event contains: event.application and event.applicationContext objects
  }

  def onApplicationChange = { event ->
  // TODO Implement code that is executed when any class in a GrailsApplication changes
  // the event contain: event.source, event.application and event.applicationContext objects
  }
}
