def values = ["USD", "CAD"]
def element = Acelerator.getElement("Currency")
if(element?.hide == "Yes"){
	return null
}else {
return Acelerator.dropdown(
		label: "Currency",
		defaultValue: "USD",
		values: values,

)
}