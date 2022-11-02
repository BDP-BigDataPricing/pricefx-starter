import net.pricefx.formulaengine.DatamartContext
import net.pricefx.formulaengine.scripting.Matrix2D
if (api.isSyntaxCheck()) return

int binsCount = 8

def perInchPriceExpression 	= null
def perInchPriceDescription = null
if (api.global.pricePointField) {
    perInchPriceDescription = api.global.pricePointLabel + "/ Unit"
    perInchPriceExpression = "SUM(" +api.global.pricePointField + ")/SUM(${Acelerator.getElement("qty")?.field})"
}

def serieCompleta = []
def xAxis = []
def yAxis = []
def zAxis = []

//QUERY
if (api.global.likeProduct != null) {
    def dmCtx = api.getDatamartContext()

    def NAanalyticsDM = dmCtx.getTable(Acelerator.getElement("SalesDM")?.field)
    def query  = dmCtx.newQuery(NAanalyticsDM,true)

    query.identity {

        Acelerator.getElementsByType("Histogram")?.each{
            select(Acelerator.getElement(it.field)?.field, it.element) //count         y ??
        }
//       select(Acelerator.getElement("sold_to")?.field, "y_axis") //count         y ??

       select(api.global.pricePointField, 'pricePoint')
       select(perInchPriceExpression, "pricePointPerInch")  //x
       select("SUM(" + api.global.pricePointField + ")", "SumPricePoint")  //extra line   z???
       orderBy('pricePointPerInch')

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

    def data2 = seriesResultset.collect{
        [
                "pricepoint" : it.pricepointperinch?.setScale(5, BigDecimal.ROUND_DOWN),
                "y_axis" : it.y_axis,
                "sumpricepoint" : it.sumpricepoint
        ]
    }.sort{x,y ->
        x.pricepoint <=> y.pricepoint
    }

    if(data2.size() > 0) {

        BigDecimal  firstValue = data2?.first().pricepoint
        BigDecimal  lastValue = data2?.last().pricepoint

        double diff = ((lastValue - firstValue)/binsCount)//?.setScale(5, BigDecimal.ROUND_DOWN)

        BigDecimal fromValue = firstValue.setScale(2, BigDecimal.ROUND_DOWN)
        BigDecimal toValue
        def i
        def siteCount = 0
        BigDecimal amountSum = 0
        for (i = 0; i< binsCount; i++) {
            toValue = (new BigDecimal(fromValue + diff)).setScale(2, BigDecimal.ROUND_UP)
            siteCount = 0
            amountSum = 0
            data2?.each {
                if (((it.pricepoint >= fromValue) || (i == 0 && it.pricepoint >= firstValue))
                        && ((it.pricepoint <= toValue) || (i == binsCount-1 && it.pricepoint <= lastValue))) {
                    siteCount++
                    amountSum += it.sumpricepoint
                }
            }
            serieCompleta << [
                    "from" : fromValue.setScale(2, BigDecimal.ROUND_UP),
                    "to" : toValue.setScale(2, BigDecimal.ROUND_UP),
                    "siteCount" : siteCount,
                    "amountSum" : (amountSum > 0) ? amountSum?.setScale(2, BigDecimal.ROUND_HALF_DOWN) : null
            ]
            fromValue = toValue.setScale(2, BigDecimal.ROUND_DOWN)
        }
        xAxis = serieCompleta?.collect{
            it.from + " → " + it.to
        }
        yAxis = serieCompleta?.collect{
            it.siteCount

        }
        zAxis = serieCompleta?.collect{
            it.amountSum
        }

    }
    //logger("[MAQ] serieCompleta", serieCompleta)
}

def definition = [
        chart: [
                type: 'column',
                height: 900,
                marginTop: 25
        ],
        title: [text: ''],
        xAxis: [
                [
                        categories: xAxis,
                        crosshair: true,
                        title: [ text: perInchPriceDescription ],
                        alignTicks: true,
                        opposite: false
                ]
        ],
        yAxis: [
                [
                        title: [ text: '∑'+api.global.pricePointLabel  + " " + (api.global.currency ?:"")],
                        opposite: true
                ],
                [
                        title: [ text: "Price Variance - Histogram" ]

                ]
        ],
        tooltip: [
            headerFormat: '<span style="font-size:12px">'+perInchPriceDescription+': {point.key}</span><table>',
            pointFormat: '<tr><td>{series.name}: {point.y} </td>' +
                    '</tr>',
            footerFormat: '</table>',
            shared: true,
            useHTML: true,
            followPointer: true
        ],
        plotOptions: [
                series: [
                        connectNulls: true
                ],
                column: [
                        pointPadding: 0.001,
                        /* borderWidth: 1, */
                        groupPadding: 0,
                        shadow: false
                ]
        ],
        series: [
                    [
                            name:
                                Acelerator.getElementsByType("Histogram")?.inject("") { elements, it ->
                                    if(it.element == 'y_axis'){
                                        elements += it.label
                                    }
                                    return elements
                                }
                            ,
                            type: "column",
                            data: yAxis,
                            yAxis: 1,
                    ],
                    [
                            type: "line",
                            name: "∑"+api.global.pricePointLabel + " " + (api.global.currency ?: ""),
                            data: zAxis,
                            color: "black",
                    ]
        ]
]


def histogram = api.buildHighchart(definition)

return histogram


