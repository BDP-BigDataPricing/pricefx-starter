import net.pricefx.formulaengine.TableContext

if (api.isSyntaxCheck()) return

if (api.global.pricePointField == null) return

TableContext tableContext = api.global.summay

//def query = """
//    SELECT like_product, percentile10, percentile25, percentile50, percentile75, percentile90,
//        min, max,  std, cov, avg, variation, opportunity25, opportunity50, opportunity75, opportunity90, opportunity10,
//         sold_to, site_name, sum
//    FROM percentile
//"""

def query = "SELECT like_product, percentile10, percentile25, percentile50, percentile75, percentile90, "
query += "min, max,  std, cov, avg, variation, opportunity25, opportunity50, opportunity75, opportunity90, opportunity10, "
query += "sold_to, site_name, sum "
Acelerator.getElementsByType("tableColDyn")?.each{
    if(it.hide == 'No'){
        query += ", " + it.element
    }
}
query += " FROM percentile"

def percentileData = tableContext.executeQuery(query)

//to iterate element in summary dashboard
//api.global.likeProductsList = percentileData?.getColumnValues(0)?.unique()

//api.trace("percentileData", percentileData)
//api.trace("percentileData Size", percentileData.size())

api.local.price_th10 = percentileData?.getMap("like_product", "percentile10")
api.local.price_th25 = percentileData?.getMap("like_product", "percentile25")
api.local.price_th50 = percentileData?.getMap("like_product", "percentile50")
api.local.price_th75 = percentileData?.getMap("like_product", "percentile75")
api.local.price_th90 = percentileData?.getMap("like_product", "percentile90")

api.local.opportunity25 = percentileData?.getMap("like_product", "opportunity25")
api.local.opportunity75 = percentileData?.getMap("like_product", "opportunity75")
api.local.opportunity50 = percentileData?.getMap("like_product", "opportunity50")
api.local.opportunity90 = percentileData?.getMap("like_product", "opportunity90")
api.local.opportunity10 = percentileData?.getMap("like_product", "opportunity10")

api.local.min_price = percentileData?.getMap("like_product", "min")
api.local.max_price = percentileData?.getMap("like_product", "max")
api.local.avg = percentileData?.getMap("like_product", "avg")
api.local.std = percentileData?.getMap("like_product", "std")
api.local.cov = percentileData?.getMap("like_product", "cov")
api.local.sum = percentileData?.getMap("like_product", "sum")
api.local.variation = percentileData?.getMap("like_product", "variation")
api.local.count_sold_tos = percentileData?.getMap("like_product", "sold_to")
api.local.count_sites = percentileData?.getMap("like_product", "site_name")

Acelerator.getElementsByType("tableColDyn")?.each{
    if(it.hide == 'No'){
        api.local[it.element] = percentileData?.getMap("like_product", it.element)
    }
}