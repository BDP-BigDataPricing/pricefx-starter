import net.pricefx.common.api.FieldFormatType
import net.pricefx.common.api.pa.DataType
import net.pricefx.formulaengine.DatamartContext
import net.pricefx.formulaengine.TableContext
import net.pricefx.formulaengine.scripting.Matrix2D

if (api.isSyntaxCheck()) return

// Args
api.global.likeProduct = api.input("like_product")
api.global.year = api.input("year")
api.global.period = api.input("period")
api.global.opportunityBasis = api.input("opportunity_basis")
api.global.opportunity_basis_label = api.input("opportunity_basis_label")
api.global.pricePoint = api.input("price_point")
api.global.selectedPercentile = api.input("selected_percentile")
api.global.fiscalYear = api.input("year")
api.global.baselinePeriod = api.input("period")
api.global.currency = api.input("currency")

//Filters
api.global.filters = api.input("filters")

//PricePoint Selected
api.global.pricePointField = api.input("pricePointField")
api.global.pricePointLabel = api.input("pricePointLabel")

//calculated percentile
api.global.percentile90 = api.input("percentile90")
api.global.percentile75 = api.input("percentile75")
api.global.percentile50 = api.input("percentile50")
api.global.percentile25 = api.input("percentile25")
api.global.percentile10 = api.input("percentile10")

DatamartContext dmCtx = api.getDatamartContext()
def dm = dmCtx.getDatamart(Acelerator.getElement("SalesDM")?.field)
def query = dmCtx.newQuery(dm, true)

query.identity {
    select(Acelerator.getElement("LikeProduct")?.field, "like_product")
    select(Acelerator.getElement("Column4")?.field, "Column4")
    select(Acelerator.getElement("Column5")?.field, "Column5")
    select(Acelerator.getElement("Sold_To_Name")?.field, "Sold_To_Name")
    select(Acelerator.getElement("Column0")?.field, "Column0")
    select(Acelerator.getElement("ItemNumber")?.field, "ItemNumber")
    select(Acelerator.getElement("Column6")?.field, "Column6") // 6
    select(Acelerator.getElement("Transaction_Id")?.field, "Transaction_Id") // 7
    select("SUM(" + api.global.pricePointField +")", "total") //8
    select("" + api.global.pricePointField , "PricePoint") //9
    select(Acelerator.getElement("qty")?.field, "Qty") //10

    select(Acelerator.getElement("Column1")?.field, "Column1") // 11
    select(Acelerator.getElement("Column2")?.field, "Column2") // 12
    select(Acelerator.getElement("Column3")?.field, "Column3") // 13

    Acelerator.getElementsByType("tableColDyn")?.each{
        if(it.hide == 'No'){
            select(it.element.toLowerCase(), it.element.toLowerCase())
        }
    }

    //Periods and slice with Datamart
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

    DatamartContext.DataSlice dmSlice = dmCtx.newDatamartSlice()
    dmSlice.addFilter(periodsSliceFilter)

    query.where(dmSlice)

    if (api.global.fiscalYear) where(Filter.in(Acelerator.getElement("Year")?.field, api.global.fiscalYear))

    //where(Filter.equal("INTRACOMPANY", "No"))
    //where(Filter.equal("INTERCOMPANY", "No"))
    //where(Filter.isNotNull("ACCOUNT_NAME"))
    where(Filter.equal(Acelerator.getElement("LikeProduct")?.field, api.global.likeProduct))
    where(Filter.greaterThan(Acelerator.getElement("qty")?.field, 0))
    where(Filter.isNotNull(Acelerator.getElement("LikeProduct")?.field))
    where(Filter.greaterThan(api.global.pricePointField, 0))
    where(Filter.isNotNull(api.global.pricePointField))

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

    orderBy(Acelerator.getElement("LikeProduct")?.field)
}

if (api.global.currency) query.setOptions(["currency": api.global.currency])

def sql = """ SELECT * FROM T1 """
Matrix2D data = dmCtx.executeSqlQuery(sql, query)

def columnLabels = [
        "Column4"                   : Acelerator.getElement("Column4")?.label,
        "Column5"                   : Acelerator.getElement("Column5")?.label,
        "price_unit"                : api.global.pricePointLabel ,
        "price_increase"            : "Price Increase %",
        "SoldToName"                : Acelerator.getElement("Sold_To_Name")?.label,
        "Column0"                   : Acelerator.getElement("Column0")?.label,
        "ItemNumberCol"             : Acelerator.getElement("ItemNumber")?.label,
        "Column6"                   : Acelerator.getElement("Column6")?.label,
        "Column1"                   : Acelerator.getElement("Column1")?.label,
        "Column2"                   : Acelerator.getElement("Column2")?.label,
        "Column3"                   : Acelerator.getElement("Column3")?.label,
        "Transaction_Id_Col"        : Acelerator.getElement("Transaction_Id")?.label,
        "opportunity_basis"         : api.global.opportunity_basis_label,
        "qty_divide_by_pricePoint"  : api.global.pricePointLabel + Acelerator.getElement("qty_divide_by_pricePoint")?.label,
        "Qty"                       : Acelerator.getElement("qty")?.label,
        "Opportunity"               : Acelerator.getElement("Opportunity")?.label
]

Acelerator.getElementsByType("tableColDyn")?.each{
    if(it.hide == 'No'){
        def mapToAdd = [:]
        mapToAdd.put(it.element, it.label)
        columnLabels << mapToAdd
    }
}

def detail = api.newMatrix(columnLabels.collect({ column -> column.value }))
detail.setEnableClientFilter(true)

if (data) {
    for (myrow = 0; myrow < data?.getRowCount(); ++myrow) {
        def row = [:]
        def pricePoint = data.getValue(myrow, 8)?.toBigDecimal()
        api.local.pricePoint = pricePoint

        def percentile = api.global.selectedPercentile[api.global.likeProduct]?.toBigDecimal()

        def priceIncreace = pricePoint > 0 ? (percentile > pricePoint
            ? ((percentile - pricePoint) / pricePoint)
            : 0) : 0

         row[columnLabels.Column4] = data.getValue(myrow, 1)
         row[columnLabels.Column5] = data.getValue(myrow, 2)
         row[columnLabels.price_unit] = api.formatNumber("0.00", pricePoint)
         row[columnLabels.opportunity_basis] = api.formatNumber("0.00", percentile)
         row[columnLabels.price_increase] = priceIncreace

         row[columnLabels.Column0] = data.getValue(myrow, 4)
         row[columnLabels.SoldToName] = data.getValue(myrow, 3)
         row[columnLabels.ItemNumberCol] = data.getValue(myrow, 5)

        row[columnLabels.Column6] = data.getValue(myrow, 6)
        row[columnLabels.Transaction_Id_Col] = data.getValue(myrow, 7)

        row[columnLabels.Qty] = data.getValue(myrow, 10)?.toBigDecimal()
        def qty_divide_by_pricePoint = data.getValue(myrow, 9) && data.getValue(myrow, 10) ? data.getValue(myrow, 9)?.toBigDecimal() / data.getValue(myrow, 10)?.toBigDecimal() : 0
        row[columnLabels.qty_divide_by_pricePoint] = qty_divide_by_pricePoint

        def oportunity = qty_divide_by_pricePoint < percentile ? (percentile - qty_divide_by_pricePoint)* data.getValue(myrow, 10)?.toBigDecimal() : 0

        row[columnLabels.Column1] = data.getValue(myrow, 11)
        row[columnLabels.Column2] = data.getValue(myrow, 12)
        row[columnLabels.Column3] = data.getValue(myrow, 13)

        row[columnLabels.Opportunity] = api.formatNumber("0.00", oportunity)

        Acelerator.getElementsByType("tableColDyn")?.each{
            if(it.hide == 'No'){
                if(it.field == 'Numeric'){
                    if(it.rule == '/'){
                        row[columnLabels[it.element]] = (data.selectRow(myrow).collect{it}[0][it.element.toLowerCase()] ?: BigDecimal.ZERO) / api.local[it.value]
                    }else{
                        row[columnLabels[it.element]] = data.selectRow(myrow).collect{it}[0][it.element.toLowerCase()] ?: BigDecimal.ZERO
                    }
                    detail.setColumnFormat(columnLabels[it.element], FieldFormatType.NUMERIC)
                }else if(it.field == 'Percent'){
                    if(it.rule == '/'){
                        row[columnLabels[it.element]] = (data.selectRow(myrow).collect{it}[0][it.element.toLowerCase()] ?: BigDecimal.ZERO) / api.local[it.value]
                    }else{
                        row[columnLabels[it.element]] = data.selectRow(myrow).collect{it}[0][it.element.toLowerCase()] ?: BigDecimal.ZERO
                    }
                    detail.setColumnFormat(columnLabels[it.element], FieldFormatType.PERCENT)
                }else{
                    row[columnLabels[it.element]] = data.selectRow(myrow).collect{it}[0][it.element.toLowerCase()]
                }
            }
        }

        detail.addRow(row)
    }
}

detail.setColumnFormat(columnLabels.price_unit, FieldFormatType.NUMERIC)
detail.setColumnFormat(columnLabels.price_increase, FieldFormatType.PERCENT)
detail.setColumnFormat(columnLabels.Qty, FieldFormatType.NUMERIC)
detail.setColumnFormat(columnLabels.qty_divide_by_pricePoint, FieldFormatType.NUMERIC)
detail.setColumnFormat(columnLabels.Opportunity, FieldFormatType.NUMERIC)
detail.setColumnFormat(columnLabels.opportunity_basis, FieldFormatType.NUMERIC)

return detail
