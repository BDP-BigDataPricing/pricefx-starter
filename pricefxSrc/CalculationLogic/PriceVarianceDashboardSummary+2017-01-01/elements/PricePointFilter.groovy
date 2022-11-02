
def element = Acelerator.getElement("PricePoint")
def table = element?.source?.split(":")?.getAt(1)?.trim()
api.trace("table", table)

def values =  [:]

api.findLookupTableValues("PricePoint").collect {
	values.put(it.name,it.attribute1)
}

def index = api.findLookupTableValues(table).collect{it.attribute2}.indexOf("Yes") ?: 0

java.util.Map.Entry selectedPricePoint = Acelerator.dropdown(
		label: element?.label,
		values: values,
		defaultByIndex: index
)


//save the selected value in the global variable.
api.global.pricePointField = selectedPricePoint?.key
api.global.pricePointLabel = selectedPricePoint?.value

