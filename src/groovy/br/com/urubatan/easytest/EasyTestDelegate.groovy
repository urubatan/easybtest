package br.com.urubatan.easytest

import groovy.lang.Closure

import org.disco.easyb.delegates.Plugable
import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU
import org.codehaus.groovy.grails.commons.ApplicationAttributes;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.ApplicationHolder;
import org.codehaus.groovy.grails.commons.spring.GrailsRuntimeConfigurator;
import org.springframework.context.ApplicationContext;
import org.codehaus.groovy.grails.plugins.*
import org.springframework.core.io.*
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.mock.web.MockServletContext
import org.codehaus.groovy.grails.cli.support.CommandLineResourceLoader;
import grails.spring.*
import org.springframework.web.context.WebApplicationContext


class EasyTestDelegate implements Plugable{
	def void easytest(value){
		initApp()
		println("hahaha --- hahaha ${value}")
	}

	private void initApp(){
		def builder = new WebBeanBuilder()
		def basedir = System.getProperty("basedir")
		beanDefinitions = builder.beans {
			resourceHolder(org.codehaus.groovy.grails.commons.spring.GrailsResourceHolder) {
				this.resources = "file:${basedir}/**/grails-app/**/*.groovy"
			}
			grailsResourceLoader(org.codehaus.groovy.grails.commons.GrailsResourceLoaderFactoryBean) {
				grailsResourceHolder = resourceHolder
			}
			grailsApplication(org.codehaus.groovy.grails.commons.DefaultGrailsApplication.class, ref("grailsResourceLoader"))
			pluginMetaManager(DefaultPluginMetaManager, resolveResources("file:${basedir}/plugins/*/plugin.xml"))
		}
		appCtx = beanDefinitions.createApplicationContext()
		def ctx = appCtx
		// The mock servlet context needs to resolve resources relative to the 'web-app'
		// directory. We also need to use a FileSystemResourceLoader, otherwise paths are
		// evaluated against the classpath - not what we want!
		servletContext = new MockServletContext('web-app', new FileSystemResourceLoader())
		ctx.servletContext = servletContext
		grailsApp = ctx.grailsApplication
		ApplicationHolder.application = grailsApp
		classLoader = grailsApp.classLoader
		pluginManager.application = grailsApp
		pluginManager.doArtefactConfiguration()
		grailsApp.initialise()
		appCtx.resourceLoader = new  CommandLineResourceLoader()
		def config = new org.codehaus.groovy.grails.commons.spring.GrailsRuntimeConfigurator(grailsApp,appCtx)
		appCtx = config.configure(servletContext)
		servletContext.setAttribute(ApplicationAttributes.APPLICATION_CONTEXT,appCtx );
		servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, appCtx);
	}
}
