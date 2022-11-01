import net.pricefx.formulaengine.DatamartContext
import net.pricefx.server.dto.calculation.ContextParameter

api.global.dev = false

api.local.creator = api.findLookupTableValues("PriceVariance").collect{
	["element" : it.name,
	 "field" : it.attribute1,
	 "type" : it.attribute2,
	 "description" : it.attribute5,
	 "source" : it.attribute6,
	 "hide" : it.attribute3,
	 "label" : it.attribute4,
	 "multiselect" : it.attribute7,
	 "rule" : it.attribute8,
	 "value" : it.attribute9,
	]
}

def getElement(element) {
	try {
		api.local.creator?.find{it.element == element}
	} catch (Exception e) {
		api.throwException("Cant find element:" + element)
	}
}

def getElementsByType(type) {
	try {
		api.local.creator?.findAll{it.type == type}
	} catch (Exception e) {
		api.throwException("Cant find elements by type:" + type)
	}
}

/**
 *
 * @param args
 *  label: string, required
 *  datamart: string, required
 *  field: string, required
 *  multiselect: default false
 *  defaultByIndex: index position of posible values order asc
 *  defaultValue: index position of posible values order asc
 * @return filter option/options object
 *
 * multiselect from values may not work todo...
 */
def dropdown(java.util.Map args) {
	api.trace("dropdown map args", null, args)

	def defaultIndex = args.defaultByIndex
	def defaultValue = args.defaultValue

	def multiselect = args.multiselect

	def optionValues = args.values

	def datamart = args.datamart
	def field = args.field
	def label = args.label

	if (!label)
		throw new Exception("common.UIElements.dropdown, Label arg missing")

	if ((!datamart && !field && !optionValues) || (datamart && field && optionValues))
		throw new Exception("common.UIElements.dropdown, Params args need a source of thrust datamart and field, or map values.")

	if ((!datamart && field) || (datamart && !field))
		throw new Exception("common.UIElements.dropdown, Params args datamart and field values work together.")

	if (defaultIndex && defaultValue)
		throw new Exception("common.UIElements.dropdown, Params args defaultIndex or defaultValue.")

//    if (defaultIndex && defaultValue)
//        throw new Exception("common.UIElements.dropdown, Params args defaultIndex or defaultValue.")


	def columnValues
	def columnOptions
	def dropdown

	if (datamart && field) {
		def ctx = api.getDatamartContext()
		def dm = ctx.getDatamart(datamart)
		DatamartContext.Query query = ctx.newQuery(dm, true)

		query.select(field)
		query.where(Filter.isNotNull(field))

		def sql = "select * FROM T1"

		def values = ctx.executeSqlQuery(sql, query)?.getColumnValues(0)

		api.trace("values", null, values)

		if (!values) api.throwException("common.UIElements.dropdown, field not find (no values")

		def isNumericList = true
		values?.find { if(!((String) it).isNumber()) isNumericList = false  }
		columnValues = isNumericList
				? values?.sort { Integer.valueOf(it) }
				: values?.sort()

		def optionsMap = values?.collectEntries { [it, it] }
		api.trace("optionsMap", null, optionsMap)
		columnOptions = isNumericList
				? optionsMap?.sort { Integer.valueOf(it.value) }
				: optionsMap?.sort()

		dropdown = multiselect
				? api.options(label, columnValues, columnOptions)
				: api.option(label, columnValues, columnOptions)
	}

	if (optionValues?.size() > 0) {

		if (optionValues instanceof java.util.Map) {
			columnValues = optionValues.keySet() as List
			columnOptions = optionValues

			dropdown = multiselect
					? api.options(label, columnValues, columnOptions)
					: api.option(label, columnValues, columnOptions)
		} else if (optionValues instanceof List) {
			api.trace("Quarters?", optionValues)
			columnValues = optionValues
			dropdown = multiselect
					? api.options(label, optionValues)
					: api.option(label, optionValues)
			api.trace("test", dropdown )
		}

	}

	if (defaultIndex) {
		ContextParameter p = api.getParameter(label)
		api.trace("parameter", null, p)
		def index = defaultIndex >= 0
				? defaultIndex
				: columnValues.size() + defaultIndex
		if (p != null && p.getValue() == null){
			p.setValue(multiselect ? [columnValues.get(index)] : columnValues.get(index))
		}
	}

	if (defaultValue) {
		ContextParameter p = api.getParameter(label)
		api.trace("parameter", null, p)
		api.trace("default", null, defaultValue)
//        p.setValue(multiselect ? [defaultValue] : defaultValue)
//        p.setValue(defaultValue)
		if (p != null && p.getValue() == null){
			p.setValue(multiselect ? [defaultValue] : defaultValue)
		}
	}

	if (optionValues && optionValues instanceof java.util.Map && dropdown && !multiselect) {

		return optionValues.find { dropdown == it.key }
	}

	return dropdown
}

def getLastClosePeriod() {
	int year = Calendar.getInstance().get(Calendar.YEAR);
	int month = Calendar.getInstance().get(Calendar.MONTH) + 1;

	def lastCloseQuarter
	def lastCloseYear = year

	if (month >= 1 && month <= 3) {
		lastCloseQuarter = "Q4"
		lastCloseYear = year - 1
	} else if (month >= 4 && month <= 6) {
		lastCloseQuarter = "Q1"
		lastCloseYear = year
	} else if (month >= 7 && month <= 9) {
		lastCloseQuarter = "Q2"
		lastCloseYear = year
	} else if (month >= 10 && month <= 12) {
		lastCloseQuarter = "Q3"
		lastCloseYear = year
	}

	return [
			quarter: lastCloseQuarter,
			year   : lastCloseYear
	]

}



def createFilterForDatamartSlice(String propertyA, List<String> aValues, String propertyB, List<String> bValues) {

	if (aValues.size() == 0 && bValues.size() == 0) return null

	def aValuesFilter = Filter.or()
	def bValuesFilter = Filter.or()

	if (aValues.size() == 1) {
		aValuesFilter = new Filter(propertyA, aValues[0])
	} else {
		for (value in aValues) {
			aValuesFilter.add(new Filter(propertyA, value))
		}
	}

	if (bValues.size() == 1) {
		bValuesFilter = new Filter(propertyB, bValues[0])
	} else {
		for (value in bValues) {
			bValuesFilter.add(new Filter(propertyB, value))
		}
	}

	if (aValues.size() == 0) return bValuesFilter
	if (bValues.size() == 0) return aValuesFilter

	return Filter.or(aValuesFilter, bValuesFilter)
}

def getPeriodValuesFromKeyList(List<String> periods) {
	if (periods == null) return [ [], [] ]  //return empty arrays

	def values = ["q1", "q2", "q3", "q4", "m1", "m2", "m3", "m4", "m5", "m6", "m7", "m8", "m9", "m10", "m11", "m12"]
	periods = periods.collect { it.toLowerCase() }

	if (!values.containsAll(periods)) {  //periods === values
		api.throwException("Invalid values pass to combinedMultipleQuarterMonth, valid options: " + values.join(","))
	}
	def quarters = periods.findAll { period -> period[0] == "q" }.collect { it.substring(1, it.length()) }
	def months = periods.findAll { period -> period[0] == "m" }.collect { it.substring(1, it.length()) }

	return [ quarters,months ]

}

def getPeriodValuesFromKeyListV2(List<String> periods, List<String> years) {
	if (periods == null) return [ [], [] ]  //return empty arrays

	def quarters = periods.findAll { period -> period[0] == "Q" }.collect { it }
	def months = periods.findAll { period -> period[0] == "M" }.collect { it }
	def quartersWithYear = findCart(quarters, years)
	def monthsWithYear = findCart(months, years)

	return [ quartersWithYear,monthsWithYear ]

}

def findCart(List<String> periods, List<String> years){
	def product = []
	for (int i = 0; i < periods.size(); i++)
		for (int j = 0; j < years.size(); j++)
			product.add(years.get(j)+"-"+periods.get(i))

	return product
}

def getFilter(java.util.Map args){

	if(args.rule == "equal"){
		return Filter.equal(args.field, args.field.value)
	}

	if(args.rule == "isNotNull"){
		return Filter.isNotNull(args.field)
	}

	if(args.rule == "greaterThan"){
		return Filter.greaterThan(args.field, 0)
	}

}
