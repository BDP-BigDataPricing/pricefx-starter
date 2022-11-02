import net.pricefx.common.api.pa.DataType
import net.pricefx.formulaengine.DatamartContext
import net.pricefx.formulaengine.TableContext
import net.pricefx.formulaengine.scripting.Matrix2D

import java.math.RoundingMode

if (api.isSyntaxCheck()) return
if (api.global.pricePointField == null) return

//Main variables (context)
DatamartContext datamartCtx = api.getDatamartContext()

def salesDM = datamartCtx.getDatamart(Acelerator.getElement("SalesDM").field)
DatamartContext.Query query = datamartCtx.newQuery(salesDM, false)

//Filters
List<String> years = new ArrayList<String>()
List<String> periods = new ArrayList<String>()
if ( api.global.fiscalYear instanceof Integer){
    years.add(api.global.fiscalYear)
}else{
    years = api.global.fiscalYear
}

if ( api.global.baselinePeriod instanceof String){
    periods.add(api.global.baselinePeriod)
}else{
    periods = api.global.baselinePeriod
}

def periodsList = Acelerator.getPeriodValuesFromKeyListV2(periods, years)
Filter periodsSliceFilter = Acelerator.createFilterForDatamartSlice(Acelerator.getElement("Period2")?.field, periodsList[0],
        Acelerator.getElement("Period1")?.field, periodsList[1])

//use the dataslice to add filters with OR
DatamartContext.DataSlice dmSliceFilteredByPeriods = datamartCtx.newDatamartSlice()
dmSliceFilteredByPeriods.addFilter(periodsSliceFilter)

query.identity {
    select(Acelerator.getElement("LikeProduct")?.field, "like_product")//COLUMN 0
    select(api.global.pricePointField + "/ ${Acelerator.getElement("qty")?.field}", "price")// COLUMN 01
    select(Acelerator.getElement("qty")?.field, "invoice_base_qty") // COLUMN 2
    select(Acelerator.getElement("sold_to")?.field, "sold_account_number") // COLUMN 3
    select(Acelerator.getElement("shipTo")?.field, "site_name") // COLUMN 4
    select("" + api.global.pricePointField, "price_point_selected")// COLUMN 5

    Acelerator.getElementsByType("tableColDyn")?.each{
        if(it.hide == 'No'){
            select(it.element, it.element)
        }
    }

    where(dmSliceFilteredByPeriods)

    where(Filter.greaterThan(api.global.pricePointField, 0))

    if (api.global.fiscalYear) where(Filter.in(Acelerator.getElement("Year")?.field, api.global.fiscalYear))

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

if (api.global.currency) query.setOptions(["currency": api.global.currency])

Matrix2D data = datamartCtx.executeQuery(query)?.getData()

//get the list of products.

def uniqueProductNameList = data?.getColumnValues(0)?.unique()

def percetileCalculatedList = []

def result = datamartCtx.executeQuery(query)



//api.global.tableCtx = api.getTableContext()
TableContext tableContext2 = api.global.tableCtx//api.global.tableCtx
//tableContext2.createTableFromQueryResult("transactionsPercentile", result)

//look for the percentile of each product.
uniqueProductNameList.forEach { productName ->
    if (productName) {
        def query2 = "SELECT like_product, price_point/invoice_base_qty AS price, invoice_base_qty, sold_account_number, site_name, price_point AS price_point_selected"
        Acelerator.getElementsByType("tableColDyn")?.each{
            if(it.hide == 'No'){
                query2 += ", " + it.element
            }
        }
        query2 += " FROM transactions  WHERE like_product = '" + productName + "'"
        query2 += " ORDER BY price"

//        def query2 = """
//            SELECT like_product, price_point/invoice_base_qty AS price, invoice_base_qty, sold_account_number, site_name, price_point AS price_point_selected
//            FROM transactions  WHERE like_product = '$productName'
//            ORDER BY price
//        """

        def productMatrix
        try {
            productMatrix = tableContext2.executeQuery(query2)
        } catch (Exception e) {
        }

        def percentileMap = [:]
        def min = productMatrix?.min()?.getValue(0, 1)?.setScale(5, RoundingMode.HALF_DOWN)
        def max = productMatrix?.max()?.getValue(0, 1)?.setScale(5, RoundingMode.HALF_DOWN)
//        def porcentile10 = productMatrix?.getPercentileValue(1, 10)?.setScale(5, RoundingMode.HALF_DOWN)
        def porcentile10 = productMatrix?.getExcelPercentileValues(1)[10]?.setScale(5, RoundingMode.HALF_DOWN)
//        def percentile25 = productMatrix?.getPercentileValue(1, 25)?.setScale(5, RoundingMode.HALF_DOWN)
        def percentile25 = productMatrix?.getExcelPercentileValues(1)[25]?.setScale(5, RoundingMode.HALF_DOWN)
//        def percentile50 = productMatrix?.getPercentileValue(1, 50)?.setScale(5, RoundingMode.HALF_DOWN)
        def percentile50 = productMatrix?.getExcelPercentileValues(1)[50]?.setScale(5, RoundingMode.HALF_DOWN)
//        def percentile75 = productMatrix?.getPercentileValue(1, 75)?.setScale(5, RoundingMode.HALF_DOWN)
        def percentile75 = productMatrix?.getExcelPercentileValues(1)[75]?.setScale(5, RoundingMode.HALF_DOWN)
//        def percentile90 = productMatrix?.getPercentileValue(1, 90)?.setScale(5, RoundingMode.HALF_DOWN)
        def percentile90 = productMatrix?.getExcelPercentileValues(1)[90]?.setScale(5, RoundingMode.HALF_DOWN)

        def soldToList = productMatrix?.getColumnValues(3)?.unique()?.size()
        def siteNameList = productMatrix?.getColumnValues(4)?.unique()?.size()

        def avg = productMatrix?.mean()?.getValue(0, 1)
        def sum = productMatrix?.getColumnValues(5).collect{it}.sum()
        def std = productMatrix?.std()?.getValue(0, 1)
        def cov = avg == 0 ? 0 : (std / avg) * 100

        def variation

        if (cov < 10)
            variation = "LOW"
        else if (cov >= 10 && cov < 60)
            variation = "MEDIUM"
        else
            variation = "HIGH"

        def totalOpportunity10 = 0
        def totalOpportunity25 = 0
        def totalOpportunity50 = 0
        def totalOpportunity75 = 0
        def totalOpportunity90 = 0
        def totalPricePointSelected = 0
        def totalQuantity = 0

        api.local['pricePoint'] = 0
        Acelerator.getElementsByType("tableColDyn")?.each{
            if(it.hide == 'No'){
                api.local['total_'+it.element] = 0
            }
        }

        productMatrix.toResultMatrix().getEntries().forEach() { transaction ->

            def quantity = transaction.get("invoice_base_qty")
            def pricePoint = transaction.get("price")
            def pricePointSelected = transaction.get("price_point_selected")

            def opp10 = pricePoint < porcentile10
                    ? (porcentile10 - pricePoint) * quantity
                    : 0

            def opp25 = pricePoint < percentile25
                    ? (percentile25 - pricePoint) * quantity
                    : 0

            def opp50 = pricePoint < percentile50
                    ? (percentile50 - pricePoint) * quantity
                    : 0

            def opp75 = pricePoint < percentile75
                    ? (percentile75 - pricePoint) * quantity
                    : 0

            def opp90 = pricePoint < percentile90
                    ? (percentile90 - pricePoint) * quantity
                    : 0

            Acelerator.getElementsByType("tableColDyn")?.each{
                if(it.hide == 'No'){
                    api.local['total_'+it.element] = api.local['total_'+it.element] + (transaction.get(it.element) ?: 0)
                }
            }
            totalOpportunity10 += opp10?.toBigDecimal()
            totalOpportunity25 += opp25?.toBigDecimal()
            totalOpportunity50 += opp50?.toBigDecimal()
            totalOpportunity75 += opp75?.toBigDecimal()
            totalOpportunity90 += opp90?.toBigDecimal()

            totalPricePointSelected += pricePointSelected?.toBigDecimal()
            totalQuantity += quantity?.toBigDecimal()

        }

        api.local['pricePoint'] = totalPricePointSelected
        def pricePointAt = totalPricePointSelected / totalQuantity

        percentileMap.put("like_product", productName)
        percentileMap.put("min", min)
        percentileMap.put("max", max)
        percentileMap.put("percentile10", porcentile10)
        percentileMap.put("percentile25", percentile25)
        percentileMap.put("percentile50", percentile50)
        percentileMap.put("percentile75", percentile75)
        percentileMap.put("percentile90", percentile90)
        percentileMap.put("avg", pricePointAt.setScale(5, RoundingMode.HALF_DOWN))
        percentileMap.put("sum", sum)
        percentileMap.put("std", std)
        percentileMap.put("cov", cov)
        percentileMap.put("variation", variation)
        percentileMap.put("opportunity10", totalOpportunity10?.toBigDecimal().setScale(0, RoundingMode.HALF_DOWN))
        percentileMap.put("opportunity25", totalOpportunity25?.toBigDecimal().setScale(0, RoundingMode.HALF_DOWN))
        percentileMap.put("opportunity50", totalOpportunity50?.toBigDecimal().setScale(0, RoundingMode.HALF_DOWN))
        percentileMap.put("opportunity75", totalOpportunity75?.toBigDecimal().setScale(0, RoundingMode.HALF_DOWN))
        percentileMap.put("opportunity90", totalOpportunity90?.toBigDecimal().setScale(0, RoundingMode.HALF_DOWN))
        percentileMap.put("sold_to", soldToList)
        percentileMap.put("site_name", siteNameList)

        Acelerator.getElementsByType("tableColDyn")?.each{
            if(it.hide == 'No'){
                if(it.rule == '/'){
                    percentileMap.put(it.element,
//                            api.local['total_'+it.element] / api.local[it.value]
                            (api.local['total_'+it.element] / api.local[it.value]).toBigDecimal().setScale(5, RoundingMode.HALF_DOWN)
                    )
                }else{
                    percentileMap.put(it.element, api.local['total_'+it.element]?.toBigDecimal().setScale(5, RoundingMode.HALF_DOWN))
                }
            }
        }

        percetileCalculatedList.add(percentileMap)
    }
}

//tableCtx is used to store results in in-memory(h2) database for performance access.
TableContext tableContext = api.getTableContext()

def columns = [
        "like_product" : DataType.STRING,
        "percentile10" : DataType.STRING,
        "percentile25" : DataType.STRING,
        "percentile50" : DataType.STRING,
        "percentile75" : DataType.STRING,
        "percentile90" : DataType.STRING,
        "opportunity10": DataType.NUMBER,
        "opportunity25": DataType.NUMBER,
        "opportunity50": DataType.NUMBER,
        "opportunity75": DataType.NUMBER,
        "opportunity90": DataType.NUMBER,
        "sold_to"      : DataType.NUMBER,
        "site_name"    : DataType.NUMBER,
        "min"          : DataType.STRING,
        "std"          : DataType.NUMBER,
        "max"          : DataType.STRING,
        "cov"          : DataType.NUMBER,
        "avg"          : DataType.STRING,
        "sum"          : DataType.STRING,
        "variation"    : DataType.STRING,
]
Acelerator.getElementsByType("tableColDyn")?.each{
    if(it.hide == 'No'){
        def mapToAdd = [:]
        if(it.field == 'Numeric' || it.field == 'Percent'){
            mapToAdd.put(it.element, DataType.NUMBER)
        }else{
            mapToAdd.put(it.element, DataType.STRING)
        }
        columns << mapToAdd
    }
}
tableContext.createTable("percentile", columns)

tableContext.loadRows("percentile", percetileCalculatedList)
api.global.summay = tableContext
