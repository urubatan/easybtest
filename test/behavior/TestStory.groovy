
scenario "Any Test could be here", {
	given "Any thing",{}
	when "Some thing happens", {}
	then "A test must run", {
		def sf = ctx.getBean("sessionFactory")
	}
}
