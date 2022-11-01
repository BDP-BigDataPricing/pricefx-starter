def values = ["M01", "M02", "M03", "M04", "M05", "M06", "M07", "M08", "M09", "M10", "M11", "M12", "Q1", "Q2", "Q3", "Q4"]

def lastPeriod = Acelerator.getLastClosePeriod()
def element = Acelerator.getElement("Period1")

if(element?.hide == "Yes"){
	return null
}else {
	return Acelerator.dropdown(
			label: element?.label,
			multiselect: element?.multiselect == "Yes" ? true : false,
			values: values,
			defaultValue: lastPeriod.quarter
	)
}

