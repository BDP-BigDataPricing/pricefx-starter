def lastPeriod = Acelerator.getLastClosePeriod()

api.trace(lastPeriod)

def element = Acelerator.getElement("Year")
if(element?.hide == "Yes"){
	return null
}else {
	Acelerator.dropdown(
			label: element?.label,
			field: element?.field,
			datamart: element?.source,
			defaultValue: lastPeriod?.year,
			multiselect: element?.multiselect == "Yes" ? true : false
	)
}
