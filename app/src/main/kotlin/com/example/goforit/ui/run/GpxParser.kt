package com.example.goforit.ui.run

import android.util.Xml
import com.mapbox.geojson.Point
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import kotlin.math.*

// GPX 解析結果：軌跡點清單 + 總距離
data class GpxResult(
    val points: List<Point>,   // 用於地圖折線的座標序列
    val distanceKm: Float      // 總距離（公里）
)

// 用 Android 內建的 XmlPullParser 解析 GPX 檔案
// （JPX 函式庫依賴 javax.xml.stream，Android 上不存在，所以直接手動解析）
fun parseGpx(inputStream: InputStream): GpxResult {
    val parser = Xml.newPullParser()
    parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
    parser.setInput(inputStream, null)

    val points = mutableListOf<Point>()
    var eventType = parser.eventType

    // 逐行掃描 XML，只抓 <trkpt> 元素的 lat/lon 屬性
    while (eventType != XmlPullParser.END_DOCUMENT) {
        if (eventType == XmlPullParser.START_TAG && parser.name == "trkpt") {
            val lat = parser.getAttributeValue(null, "lat")?.toDoubleOrNull()
            val lon = parser.getAttributeValue(null, "lon")?.toDoubleOrNull()
            if (lat != null && lon != null) {
                points.add(Point.fromLngLat(lon, lat))
            }
        }
        eventType = parser.next()
    }

    // 把相鄰兩點距離加總得到總公里數
    val distanceKm = points
        .zipWithNext { a, b -> haversineKm(a, b) }
        .sum()
        .toFloat()

    return GpxResult(points = points, distanceKm = distanceKm)
}

// Haversine 公式：計算地球表面兩點距離（公里）
private fun haversineKm(a: Point, b: Point): Double {
    val r = 6371.0
    val dLat = Math.toRadians(b.latitude() - a.latitude())
    val dLng = Math.toRadians(b.longitude() - a.longitude())
    val h = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(a.latitude())) *
            cos(Math.toRadians(b.latitude())) *
            sin(dLng / 2).pow(2)
    return 2 * r * asin(sqrt(h))
}
