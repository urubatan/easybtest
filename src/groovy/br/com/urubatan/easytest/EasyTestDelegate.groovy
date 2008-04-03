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
import org.springframework.core.io.support.PathMatchingResourcePatternResolver


class EasyTestDelegate implements Plugable{
	
	def void easytest(value){
		initApp()
		println("hahaha --- hahaha ${value}")
	}

	private void initApp(){
		def resolver = new PathMatchingResourcePatternResolver()
		def resolveResources = {String pattern ->
			try {
				return resolver.getResources(pattern)
			}
			catch (Throwable e) {
				return []
			}
		}

		def builder = new WebBeanBuilder()
		def basedir = System.getProperty("basedir")
		def beanDefinitions = builder.beans {
			resourceHolder(org.codehaus.groovy.grails.commons.spring.GrailsResourceHolder) {
				resources = "file:${basedir}/**/grails-app/**/*.groovy"
			}
			grailsResourceLoader(org.codehaus.groovy.grails.commons.GrailsResourceLoaderFactoryBean) {
				grailsResourceHolder = resourceHolder
			}
			grailsApplication(org.codehaus.groovy.grails.commons.DefaultGrailsApplication.class, ref("grailsResourceLoader"))
			pluginMetaManager(DefaultPluginMetaManager, resolveResources("file:${basedir}/plugins/*/plugin.xml"))
		}
		def appCtx = beanDefinitions.createApplicationContext()
		def ctx = appCtx
		// The mock servlet context needs to resolve resources relative to the 'web-app'
		// directory. We also need to use a FileSystemResourceLoader, otherwise paths are
		// evaluated against the classpath - not what we want!
		def servletContext = new MockServletContext('web-app', new FileSystemResourceLoader())
		ctx.servletContext = servletContext
		def grailsApp = ctx.grailsApplication
		ApplicationHolder.application = grailsApp
		def classLoader = grailsApp.classLoader
		//pluginManager.application = grailsApp
		//pluginManager.doArtefactConfiguration()
		grailsApp.initialise()
		appCtx.resourceLoader = new  CommandLineResourceLoader()
		def config = new org.codehaus.groovy.grails.commons.spring.GrailsRuntimeConfigurator(grailsApp,appCtx)
		servletContext.setAttribute(ApplicationAttributes.APPLICATION_CONTEXT,appCtx );
		servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, appCtx);
		appCtx = config.configure(servletContext)
	}
}
