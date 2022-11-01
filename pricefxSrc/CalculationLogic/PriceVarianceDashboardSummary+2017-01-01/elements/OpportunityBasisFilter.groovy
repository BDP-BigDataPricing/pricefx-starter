def values = [:]
values = ["10th": "10th", "25th": "25th", "50th": "50th", "75th": "75th", "90th": "90th"]


java.util.Map.Entry selectedOpp = Acelerator.dropdown(
		label: Acelerator.getElement("OpportunityBasis").label,
		values: values,
		defaultByIndex: 1 // default element by index position (start by zero)
)

//if (api.isSyntaxCheck()) return

//save the selected value in the global variable.
api.global.selectedOpp = selectedOpp?.key
api.global.selectedOppLabel = selectedOpp?.value

