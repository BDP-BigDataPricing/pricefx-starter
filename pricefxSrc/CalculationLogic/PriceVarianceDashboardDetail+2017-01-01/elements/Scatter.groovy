import net.pricefx.formulaengine.DatamartContext
import net.pricefx.formulaengine.scripting.Matrix2D
import net.pricefx.common.api.chart.AuxLineColor

if (api.isSyntaxCheck()) return


def perInchPriceExpression 	= null
def perInchPriceDesc = null
if (api.global.pricePointField) {
	perInchPriceDesc = api.global.pricePointLabel + "/ Unit"
	perInchPriceExpression = "SUM(" +api.global.pricePointField + ")/SUM(${Acelerator.getElement("volume")?.field})"
}

def listSeries = []
def markers = []

if (api.global.likeProduct  != null) {

//QUERY
	def dmCtx = api.getDatamartContext()
	def NAanalyticsDM = dmCtx.getTable(Acelerator.getElement("SalesDM")?.field)
	def query = dmCtx.newQuery(NAanalyticsDM, true)

	query.identity {
		select(Acelerator.getElement("AggregationBy")?.field, "aggregationBy")
		select(Acelerator.getElement("BandBy")?.field, "bandBy")
		select(perInchPriceExpression, "y_axis")
		select(Acelerator.getElement("volume")?.field, "x_axis")

		orderBy('x_axis')

		//Periods and slice with Datamart
		//Filters
		List<String> years = new ArrayList<String>()
		List<String> periods = new ArrayList<String>()
		if (api.global.fiscalYear instanceof Integer) {
			years.add(api.global.fiscalYear)
		} else {
			years = api.global.fiscalYear
		}

		if (api.global.baselinePeriod instanceof String) {
			periods.add(api.global.baselinePeriod)
		} else {
			periods = api.global.baselinePeriod
		}

		def periodsList = Acelerator.getPeriodValuesFromKeyListV2(periods, years)
		Filter periodsSliceFilter = Acelerator.createFilterForDatamartSlice(Acelerator.getElement("Period2")?.field, periodsList[0],
				Acelerator.getElement("Period1")?.field, periodsList[1])

		DatamartContext.DataSlice dmSlice = dmCtx.newDatamartSlice()
		dmSlice.addFilter(periodsSliceFilter)

		query.where(dmSlice)

		if (api.global.fiscalYear) where(Filter.in(Acelerator.getElement("Year")?.field, api.global.fiscalYear))

		where(Filter.equal(Acelerator.getElement("LikeProduct")?.field, api.global.likeProduct))
		where(Filter.greaterThan(Acelerator.getElement("qty")?.field, 0))
		where(Filter.isNotNull(Acelerator.getElement("LikeProduct")?.field))
		where(Filter.greaterThan(api.global.pricePointField, 0))
		where(Filter.isNotNull(api.global.pricePointField))

		if (api.global.currency) query.setOptions(["currency": api.global.currency])

		Acelerator.getElementsByType("DefaultFilter")?.each{
			if(it.hide == 'No'){
				where(Acelerator.getFilter( it ))
			}
		}

		Acelerator.getElementsByType("Filter")?.each{ element ->
			if(element.hide == 'No'){
				def filterValue = api.global.filters[element.field]
				if( filterValue != null && (filterValue as List && filterValue.size() > 0) ){
					if(element.multiselect == "Yes"){
						where(Filter.in(element.field, filterValue))
					} else {
						where(Filter.equal(element.field, filterValue))
					}
				}
			}
		}
	}

	def sql = """ SELECT * FROM T1 """
	Matrix2D seriesResultset = dmCtx.executeSqlQuery(sql, query)

	def data = seriesResultset.collect {
		[
				transactionId: it.get("aggregationby"),
				x            : it.get("x_axis"),
				y            : it.get("y_axis"),
				siteName     : it.get('bandby')
		]
	}
	//logger("[MAQ] data", api.jsonEncode(data))
	def mapSeries = [:]
	def listForSiteName
	data.each {
		listForSiteName = (mapSeries.containsKey(it.siteName)) ? mapSeries[it.siteName] : []
		listForSiteName << it
		mapSeries[it.siteName] = listForSiteName
	}

	listSeries = mapSeries.collect {
		[
				name  : it.key,
				type  : 'scatter',
				marker: [radius: 3, symbol: 'circle', states: [hover: [enabled: true, lineColor: 'rgb(100,100,100)']]],
				data  : it.value
		]
	}

//logger("[MAQ] listSeries", api.jsonEncode(listSeries))
	def rowCount = seriesResultset?.getRowCount()
	def xMinVal = seriesResultset?.getRowValues(0)?.'x_axis'
	def xMaxVal = seriesResultset?.getRowValues(rowCount - 1)?.'x_axis'
	xMaxVal = xMaxVal == xMinVal ? xMaxVal+1 : xMaxVal
//	xMinVal = xMinVal == 1 ? xMinVal : xMinVal-1
//	xMaxVal = xMaxVal+1

	def auxLineColorMap = [
			(Acelerator.getElement("price_th90")?.label): "GREEN",
			(Acelerator.getElement("price_th75")?.label): "BLUE",
			(Acelerator.getElement("price_th50")?.label): "GRAY",
			(Acelerator.getElement("price_th25")?.label): "YELLOW",
			(Acelerator.getElement("price_th10")?.label): "RED"]

	def percentilePricing = [
			(Acelerator.getElement("price_th90")?.label): String.valueOf(api.global.percentile90.get(api.global.likeProduct)),
			(Acelerator.getElement("price_th75")?.label): String.valueOf(api.global.percentile75.get(api.global.likeProduct)),
			(Acelerator.getElement("price_th50")?.label): String.valueOf(api.global.percentile50.get(api.global.likeProduct)),
			(Acelerator.getElement("price_th25")?.label): String.valueOf(api.global.percentile25.get(api.global.likeProduct)),
			(Acelerator.getElement("price_th10")?.label): String.valueOf(api.global.percentile10.get(api.global.likeProduct))
	]

	def auxLineAttributesList = []
	percentilePricing.each { key, value ->
		def auxLineMap = [:]
		auxLineMap.label = key
		auxLineMap.color = AuxLineColor.valueOf(auxLineColorMap?.getAt(key))
		auxLineMap.yIntercept = value
		auxLineAttributesList.add(auxLineMap)
	}
	markers = auxLineAttributesList.collect {
		[
				name     : it.label,
				color    : it.color,
				type     : 'line',
				lineWidth: 2,
				data     : [
						[xMinVal, it.yIntercept.toDouble()],
						[x         : xMaxVal,
						 y         : it.yIntercept.toDouble(),
						 dataLabels: [enabled: false]
						]
				],
				tooltip  : [
						useHTML      : true,
						headerFormat : '<table>',
						pointFormat  : '<tr><th>' + it.label + ':' + api.formatNumber("#,##0.00", it.yIntercept.toDouble()) +'</th></tr>',
						footerFormat : '</table>',
						followPointer: false
				]
		]
	}

}//end if likeProduct != null

def definition = [
		chart      : [
				type    : 'scatter',
				zoomType: 'xy',
				height: 900,
				marginTop: 50
//				marginBottom: 0
		],
		title      : [text: ''],
		yAxis      : [
				title      : [text: perInchPriceDesc],
				//min: 0,
				startOnTick: true,
				endOnTick  : true,
				labels     : [
						format: '{value}'
				],
		],
		xAxis      : [title: [text: "${Acelerator.getElement("volume")?.label}"], gridLineWidth: 0,],
		legend     : [
				enabled      : true,
				verticalAlign: 'top'
		],
		plotOptions: [
				scatter: [
						marker : [radius: 3, states: [hover: [enabled: true, lineColor: 'rgb(100,100,100)']]],
						states : [hover: [marker: [enabled: false]]],
						cursor : "pointer",
						showInLegend: false
				],
				series : [
						turboThreshold: 0,
						animation     : false,
						showInLegend  : true
				]
		],
		series: [

*markers,
*listSeries
		],
		tooltip: [
				useHTML      : true,
				headerFormat : '<table>',
				pointFormat  : "∑${Acelerator.getElement("volume")?.label}: {point.x:,.2f} <br>" +
								"${perInchPriceDesc}: {point.y:,.2f} <br>" +
								Acelerator.getElementsByType("Scatter")?.inject("") { elements, it ->
									if(it.hide == "No") {
										elements += it.label + ": {point." + it.value + "} </br>"
									}
									return elements
								}
				,

				footerFormat : '</table>',
				followPointer: true
		],
]
def x = api.buildHighchart(definition)
return x