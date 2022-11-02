Acelerator.getElementsByType("Filter")?.each{ element ->
	if(element.hide == 'No'){
		def options = [label: element?.label,
					   field: element?.field,
					   datamart: element?.source,
					   multiselect: element?.multiselect == "Yes" ? true : false]
		return Acelerator.dropdown(
				options,
				true
		)
	}
}