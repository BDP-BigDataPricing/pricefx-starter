import net.pricefx.common.api.FieldFormatType
import net.pricefx.formulaengine.TableContext
import net.pricefx.server.dto.calculation.ResultMatrix

if (api.isSyntaxCheck()) return
if (api.global.pricePointField == null) return

TableContext tableContext = api.global.tableCtx

def query = """
    SELECT like_product, price_point/invoice_base_qty AS total, invoice_base_qty, sold_account_number, site_name
    FROM transactions
    ORDER BY total
"""

def baseQueryResult = tableContext.executeQuery(query)
//api.trace("size", null, baseQueryResult?.toResultMatrix()?.entries?.size())
api.global.likeProductsList = baseQueryResult?.getColumnValues(0)?.unique()

def opportunities = [
        "10th": api.local.opportunity10,
        "90th": api.local.opportunity90,
        "75th": api.local.opportunity75,
        "50th": api.local.opportunity50,
        "25th": api.local.opportunity25,
]

def columnLabels = [
        "like_product"              : Acelerator.getElement("LikeProduct")?.label,
//        "variation"                 : Acelerator.getElement("variation")?.label,
//        "opp_at_selected_percentile": Acelerator.getElement("selectedOpportunity")?.label + api.global.selectedOppLabel,//api.global.opportunityBasis,
        "pricepoint"                : api.global.pricePointLabel,
//        "min_price"                 : Acelerator.getElement("min_price")?.label,
//        "price_th10"                : Acelerator.getElement("price_th10")?.label,
//        "price_th25"                : Acelerator.getElement("price_th25")?.label,
//        "price_th50"                : Acelerator.getElement("price_th50")?.label,
//        "price_th75"                : Acelerator.getElement("price_th75")?.label,
//        "price_th90"                : Acelerator.getElement("price_th90")?.label,
//        "max_price"                 : Acelerator.getElement("max_price")?.label,
//        "count_sold_tos"            : Acelerator.getElement("count_sold_tos")?.label,
//        "count_sites"               : Acelerator.getElement("count_sites")?.label,
]

Acelerator.getElementsByType("table1Column")?.each{
    if(it.hide == 'No'){
        def mapToAdd = [:]
        if(it.element == 'OpportunityAt'){
            mapToAdd.put('selectedOpportunity', it.label)
        }else{
            mapToAdd.put(it.element, it.label)
        }
        columnLabels << mapToAdd
    }
}

Acelerator.getElementsByType("tableColDyn")?.each{
    if(it.hide == 'No'){
        def mapToAdd = [:]
        mapToAdd.put(it.element, it.label)
        columnLabels << mapToAdd
    }
}

api.global.columnLabels = columnLabels

ResultMatrix summary = api.newMatrix(columnLabels.collect({ column -> column.value }))
summary.setEnableClientFilter(true)

api.global.selectedOpportunity = opportunities[api.global.opportunityBasis]

def holder = []

for (likeProduct in api.global.likeProductsList?.sort()) {
    def row = [:]

    row[columnLabels.like_product] = likeProduct
    row[columnLabels.pricepoint] = api.formatNumber(".00", api.local.sum[likeProduct] ?: 0.0)

    Acelerator.getElementsByType("table1Column")?.each{
        if(it.hide == 'No'){
            if(it.element == 'OpportunityAt'){
                row[columnLabels.selectedOpportunity] = api.global.selectedOpportunity[likeProduct] ?: BigDecimal.ZERO
            }else{
                if(it.field == 'Numeric'){
                    row[columnLabels[it.element]] = api.local[it.element][likeProduct] ?: BigDecimal.ZERO
                    summary.setColumnFormat(columnLabels[it.element], FieldFormatType.NUMERIC)
                }else{
                    row[columnLabels[it.element]] = api.local[it.element][likeProduct]
                }
            }
        }
    }

    Acelerator.getElementsByType("tableColDyn")?.each{
        if(it.hide == 'No'){
            if(it.field == 'Numeric'){
                row[columnLabels[it.element]] = api.local[it.element][likeProduct] ?: BigDecimal.ZERO
                summary.setColumnFormat(columnLabels[it.element], FieldFormatType.NUMERIC)
            }else if(it.field == 'Percent'){
                row[columnLabels[it.element]] = api.local[it.element][likeProduct] ?: BigDecimal.ZERO
                summary.setColumnFormat(columnLabels[it.element], FieldFormatType.PERCENT)
            }else{
                row[columnLabels[it.element]] = api.local[it.element][likeProduct]
            }
        }
    }

//    row[columnLabels.variation] = api.local.variation[likeProduct]
//
//    if(api.global.selectedOpportunity)
//        row[columnLabels.selectedOpportunity] = api.global.selectedOpportunity[likeProduct]?.toDouble()?.round(0)
//
//    row[columnLabels.min_price] = api.formatNumber(".##", api.local.min_price[likeProduct] ?: 0.0)
//    row[columnLabels.max_price] = api.formatNumber(".##", api.local.max_price[likeProduct] ?: 0.0)
//    row[columnLabels.price_th90] = api.formatNumber(".##", api.local.price_th90[likeProduct] ?: 0.0)
//    row[columnLabels.price_th75] = api.formatNumber(".##", api.local.price_th75[likeProduct] ?: 0.0)
//    row[columnLabels.price_th50] = api.formatNumber(".##", api.local.price_th50[likeProduct] ?: 0.0)
//    row[columnLabels.price_th25] = api.formatNumber(".##", api.local.price_th25[likeProduct] ?: 0.0)
//    row[columnLabels.price_th10] = api.formatNumber(".##", api.local.price_th10[likeProduct] ?: 0.0)
//    row[columnLabels.count_sold_tos] = api.local.count_sold_tos[likeProduct]
//    row[columnLabels.count_sites] = api.local.count_sites[likeProduct]

    holder.add(row)
   // summary.addRow(row)
}

holder?.sort({ b, a -> a[columnLabels.selectedOpportunity] <=> b[columnLabels.selectedOpportunity] })

for (item in holder) {
    summary.addRow(item)
}

summary.setColumnFormat(columnLabels.selectedOpportunity, FieldFormatType.INTEGER)
summary.setColumnFormat(columnLabels.count_sold_tos, FieldFormatType.INTEGER)
summary.setColumnFormat(columnLabels.count_sites, FieldFormatType.INTEGER)
summary.setColumnFormat(columnLabels.pricepoint, FieldFormatType.NUMERIC)

summary.onRowSelection().triggerEvent(api.dashboardWideEvent("PriceVarianceSummaryLikeProductChanged"))
        .withColValueAsEventDataAttr(columnLabels.like_product, "like_product")

return summary