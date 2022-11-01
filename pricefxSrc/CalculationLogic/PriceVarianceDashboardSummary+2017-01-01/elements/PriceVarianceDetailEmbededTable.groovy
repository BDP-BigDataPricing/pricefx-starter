if (api.isSyntaxCheck()) return
if (api.global.pricePointField == null) return

def percentileList = [
        "Min": api.local.min_price,
        "Max": api.local.max_price,
        "10th": api.local.price_th10,
        "25th": api.local.price_th25,
        "50th": api.local.price_th50,
        "75th": api.local.price_th75,
        "90th": api.local.price_th90,
]

def selectedPercentile = percentileList[api.global.opportunityBasis]

api.trace("selected percentile", selectedPercentile)

return api.dashboard("PriceVarianceDetail")
        .setParam("selected_percentile", selectedPercentile)
        .setParam("percentile10", api.local.price_th10)
        .setParam("percentile25", api.local.price_th25)
        .setParam("percentile50", api.local.price_th50)
        .setParam("percentile75", api.local.price_th75)
        .setParam("percentile90", api.local.price_th90)
        .setParam("pricePointField", api.global.pricePointField)
        .setParam("pricePointLabel", api.global.pricePointLabel)
        .setParam("year", api.global.fiscalYear )
        .setParam("period", api.global.baselinePeriod)
        .setParam("opportunity_basis", api.global.opportunityBasis)
		.setParam("opportunity_basis_label", api.global.selectedOppLabel)
		.setParam("price_point", api.global.pricePoint)
        .setParam("currency", api.global.currency)
		.setParam("filters", api.global.filters)
        .showEmbedded()
        .andRecalculateOn(api.dashboardWideEvent("PriceVarianceSummaryLikeProductChanged"))
        .withEventDataAttr("like_product").asParam("like_product")

