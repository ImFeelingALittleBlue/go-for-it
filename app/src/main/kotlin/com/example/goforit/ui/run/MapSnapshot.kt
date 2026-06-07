package com.example.goforit.ui.run

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapSnapshotOptions
import com.mapbox.maps.MapboxMapSnapshotter
import com.mapbox.maps.Size
import com.mapbox.maps.Style
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.tan

// 用 Mapbox Snapshotter 生成「真實底圖 + 路線折線」的分享圖片
// Snapshotter 會完整載入地圖 tile 再回呼，不受螢幕渲染時序影響
// onResult 在主執行緒呼叫，可直接更新 Compose state
fun createRouteMapBitmap(
    context: Context,
    routePoints: List<Point>,
    onResult: (Bitmap) -> Unit
) {
    if (routePoints.size < 2) {
        onResult(createRouteShareBitmap(routePoints))
        return
    }

    val lats = routePoints.map { it.latitude() }
    val lngs = routePoints.map { it.longitude() }
    val centerLat = (lats.min() + lats.max()) / 2
    val centerLng = (lngs.min() + lngs.max()) / 2
    val center    = Point.fromLngLat(centerLng, centerLat)

    // 依路線範圍自動計算 zoom level（讓整條路線都在畫面內）
    val span = maxOf(lats.max() - lats.min(), lngs.max() - lngs.min())
    val zoom = when {
        span < 0.004 -> 15.5
        span < 0.009 -> 14.5
        span < 0.02  -> 13.5
        span < 0.05  -> 12.5
        span < 0.1   -> 11.5
        else         -> 10.5
    }

    val imgW = 900f; val imgH = 480f
    val options = MapSnapshotOptions.Builder()
        .size(Size(imgW, imgH))
        .pixelRatio(1f)
        .build()

    val snapshotter = MapboxMapSnapshotter(options)
    snapshotter.setStyleUri(Style.MAPBOX_STREETS)
    snapshotter.setCameraOptions(
        CameraOptions.Builder().center(center).zoom(zoom).build()
    )

    snapshotter.start { snapshot ->
        val base = snapshot?.bitmap()
        // 切換回主執行緒更新 Compose state
        Handler(Looper.getMainLooper()).post {
            if (base != null) {
                val bmp = base.copy(Bitmap.Config.ARGB_8888, true)
                drawRouteOnBitmap(bmp, routePoints, centerLat, centerLng, zoom)
                onResult(bmp)
            } else {
                // 若 Snapshotter 失敗（無網路等），退回純路線圖
                onResult(createRouteShareBitmap(routePoints))
            }
            snapshotter.cancel()
        }
    }
}

// 在已有底圖的 Bitmap 上疊繪路線折線與起終點
private fun drawRouteOnBitmap(
    bmp: Bitmap,
    routePoints: List<Point>,
    centerLat: Double,
    centerLng: Double,
    zoom: Double
) {
    val canvas = Canvas(bmp)
    val bw = bmp.width.toFloat()
    val bh = bmp.height.toFloat()

    // Web Mercator 投影：經緯度 → 畫素座標
    val scale = 2.0.pow(zoom) * 256.0
    fun mX(lng: Double) = (lng + 180.0) / 360.0 * scale
    fun mY(lat: Double): Double {
        val r = Math.toRadians(lat)
        return (1.0 - ln(tan(r) + 1.0 / cos(r)) / Math.PI) / 2.0 * scale
    }
    val cx = mX(centerLng); val cy = mY(centerLat)
    fun toX(lng: Double) = (mX(lng) - cx + bw / 2).toFloat()
    fun toY(lat: Double) = (mY(lat) - cy + bh / 2).toFloat()

    // 橘色路線折線
    val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#D4822A")
        strokeWidth = 10f; style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    val path = android.graphics.Path()
    routePoints.forEachIndexed { i, pt ->
        val x = toX(pt.longitude()); val y = toY(pt.latitude())
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    canvas.drawPath(path, linePaint)

    // 起終點圓圈（橘底白邊）
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#D4822A")
    }
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE; strokeWidth = 5f; style = Paint.Style.STROKE
    }
    listOf(routePoints.first(), routePoints.last()).forEach { pt ->
        val x = toX(pt.longitude()); val y = toY(pt.latitude())
        canvas.drawCircle(x, y, 18f, fillPaint)
        canvas.drawCircle(x, y, 18f, borderPaint)
    }
}
