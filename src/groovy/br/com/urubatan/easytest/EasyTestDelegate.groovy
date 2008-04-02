package br.com.urubatan.easytest

import groovy.lang.Closure

import org.disco.easyb.delegates.Plugable

class EasyTestDelegate implements Plugable{
	def void easytest(value){
		println("hahaha --- hahaha ${value}")
	}
}
