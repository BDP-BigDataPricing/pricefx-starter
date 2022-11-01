import net.pricefx.formulaengine.DatamartContext
import net.pricefx.formulaengine.TableContext

if (api.isSyntaxCheck()) return
if (api.global.pricePointField == null) return

api.global.tableCtx = api.getTableContext()

//Setting filters into api.global
api.global.fiscalYear = out.FiscalYearFilter
api.global.baselinePeriod = out.BaselinePeriodFilter
api.global.opportunityBasis = api.global.selectedOpp
api.global.pricePoint = out.PricePointFilter
api.global.currency = out.CurrencyFilter
api.global.filters = [:]
Acelerator.getElementsByType("Filter")?.each{ element ->
    if(element.hide == 'No'){
        api.global.filters[element?.field] = api.input(element?.field)
    }
}

//Main variables (context)
DatamartContext datamartCtx = api.getDatamartContext()

def salesDM = datamartCtx.getDatamart(Acelerator.getElement("SalesDM")?.field)
DatamartContext.Query query = datamartCtx.newQuery(salesDM, true)

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
api.trace("periodsList",periodsList)
Filter periodsSliceFilter = Acelerator.createFilterForDatamartSlice(Acelerator.getElement("Period2")?.field, periodsList[0],
        Acelerator.getElement("Period1")?.field, periodsList[1])
api.trace("periodsSliceFilter",periodsSliceFilter)
//use the dataslice to add filters with OR
DatamartContext.DataSlice dmSliceFilteredByPeriods = datamartCtx.newDatamartSlice()
dmSliceFilteredByPeriods.addFilter(periodsSliceFilter)

query.identity {

    select(Acelerator.getElement("LikeProduct")?.field, "like_product")//0
    select("SUM(" + api.global.pricePointField +")", "price_point")//1
    select(Acelerator.getElement("qty")?.field, "invoice_base_qty") //2
    select(Acelerator.getElement("sold_to")?.field, "sold_account_number") //3
    select(Acelerator.getElement("shipTo")?.field, "site_name") //4
    select(Acelerator.getElement("Transaction_Id")?.field, "Transaction_Id") // TEST TTTTTTTT

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

    orderBy(Acelerator.getElement("LikeProduct")?.field)
}

if (api.global.currency) query.setOptions(["currency": api.global.currency])

def result = datamartCtx.executeQuery(query)

TableContext tableContext = api.global.tableCtx
tableContext.createTableFromQueryResult("transactions", result)