package com.example.goforit.ui.run

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.PixelCopy
import com.mapbox.geojson.Point

// 用 Android Canvas 手動畫出成就卡片，存為 PNG 回傳
fun createShareBitmap(
    coveredKm: Float,
    timeStr: String,
    heritageCount: Int,
    silverEarned: Int
): Bitmap {
    val w = 900; val h = 480
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val cvs = Canvas(bmp)

    // 深棕背景
    cvs.drawColor(android.graphics.Color.parseColor("#2C2218"))

    // 標題
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 68f
        typeface = Typeface.DEFAULT_BOLD
    }
    cvs.drawText("古蹟探索完成！", 48f, 100f, titlePaint)

    // 副標
    val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#D4A96A")
        textSize = 44f
    }
    cvs.drawText("Go For It ‧ 臺南古蹟跑步", 48f, 168f, subPaint)

    // 分隔線
    val linePaint = Paint().apply {
        color = android.graphics.Color.parseColor("#5C3D1E")
        strokeWidth = 2f
    }
    cvs.drawLine(48f, 196f, (w - 48).toFloat(), 196f, linePaint)

    // 四格統計
    val valPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 56f
        typeface = Typeface.DEFAULT_BOLD
    }
    val lblPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#9E8E7A")
        textSize = 34f
    }
    val cols = listOf(
        "${"%.2f".format(coveredKm)} km" to "距離",
        timeStr              to "時間",
        "$heritageCount 處"  to "解鎖古蹟",
        "+$silverEarned"     to "時光銀鹽"
    )
    val colW = (w - 96) / 4f
    cols.forEachIndexed { i, (value, label) ->
        val x = 48f + colW * i
        cvs.drawText(value, x, 290f, valPaint)
        cvs.drawText(label, x, 346f, lblPaint)
    }

    // 品牌浮水印
    val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#5C3D1E")
        textSize = 30f
    }
    cvs.drawText("#GoForIt  #臺南古蹟跑步", 48f, 436f, brandPaint)

    return bmp
}

// 用 PixelCopy（Android 8+）截取任意 View 的完整渲染畫面（含 OpenGL/硬體加速圖層）
// onResult 傳回 Bitmap（成功）或 null（失敗/舊版裝置），呼叫端自行決定 fallback
fun captureMapView(
    activity: Activity,
    mapView: android.view.View,
    onResult: (Bitmap?) -> Unit
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
        mapView.width > 0 && mapView.height > 0) {
        val bitmap = Bitmap.createBitmap(mapView.width, mapView.height, Bitmap.Config.ARGB_8888)
        val loc = IntArray(2); mapView.getLocationInWindow(loc)
        PixelCopy.request(
            activity.window,
            Rect(loc[0], loc[1], loc[0] + mapView.width, loc[1] + mapView.height),
            bitmap,
            { result -> onResult(if (result == PixelCopy.SUCCESS) bitmap else null) },
            Handler(Looper.getMainLooper())
        )
    } else {
        onResult(null)   // 舊版裝置或 View 尚未 layout → 由呼叫端 fallback
    }
}

// 把路線點畫成分享圖片：米色背景 + 橘色折線 + 起終點圓圈 + 浮水印
fun createRouteShareBitmap(routePoints: List<Point>): Bitmap {
    val w = 900; val h = 480
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val cvs = Canvas(bmp)
    cvs.drawColor(android.graphics.Color.parseColor("#F5F0EB"))  // 米色背景

    if (routePoints.size >= 2) {
        val lats = routePoints.map { it.latitude() }
        val lngs = routePoints.map { it.longitude() }
        val minLat = lats.min(); val maxLat = lats.max()
        val minLng = lngs.min(); val maxLng = lngs.max()
        val latSpan = maxOf(maxLat - minLat, 0.001)
        val lngSpan = maxOf(maxLng - minLng, 0.001)
        val pad = 72f
        // 經緯度轉畫布座標（緯度軸反轉）
        fun toX(lng: Double) = (pad + (lng - minLng) / lngSpan * (w - 2 * pad)).toFloat()
        fun toY(lat: Double) = (h - pad - (lat - minLat) / latSpan * (h - 2 * pad)).toFloat()

        // 橘色路線
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#D4822A")
            strokeWidth = 9f; style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
        }
        val path = android.graphics.Path()
        routePoints.forEachIndexed { i, pt ->
            val x = toX(pt.longitude()); val y = toY(pt.latitude())
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        cvs.drawPath(path, linePaint)

        // 起終點圓圈（橘底白邊）
        val fillPaint   = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.parseColor("#D4822A") }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE; strokeWidth = 4f; style = Paint.Style.STROKE
        }
        listOf(routePoints.first(), routePoints.last()).forEach { pt ->
            cvs.drawCircle(toX(pt.longitude()), toY(pt.latitude()), 14f, fillPaint)
            cvs.drawCircle(toX(pt.longitude()), toY(pt.latitude()), 14f, borderPaint)
        }
    }

    // 品牌浮水印
    val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#C8B8A0"); textSize = 26f
    }
    cvs.drawText("Go For It · 臺南古蹟跑步", 24f, (h - 18).toFloat(), brandPaint)
    return bmp
}

// 把 Bitmap 存入系統相簿（不需要 WRITE_EXTERNAL_STORAGE，Android 10+）
// 回傳 true 表示成功
fun saveBitmapToGallery(context: Context, bitmap: Bitmap): Boolean {
    return try {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME,
                "GoForIt_${System.currentTimeMillis()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = context.contentResolver
            .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
        context.contentResolver.openOutputStream(uri)
            ?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        context.contentResolver.update(uri, values, null, null)
        true
    } catch (e: Exception) { false }
}

// 把 GPS 軌跡存成 GPX 檔到 Downloads（Android 10+）
// 回傳 true 表示成功
fun saveGpxToDownloads(context: Context, trackPoints: List<Point>): Boolean {
    return try {
        val gpx = buildString {
            appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
            appendLine("<gpx version=\"1.1\" xmlns=\"http://www.topografix.com/GPX/1/1\">")
            appendLine("  <trk>")
            appendLine("    <name>GoForIt 跑步紀錄</name>")
            appendLine("    <trkseg>")
            trackPoints.forEach { pt ->
                appendLine("      <trkpt lat=\"${pt.latitude()}\" lon=\"${pt.longitude()}\"></trkpt>")
            }
            appendLine("    </trkseg>")
            appendLine("  </trk>")
            append("</gpx>")
        }
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME,
                "GoForIt_${System.currentTimeMillis()}.gpx")
            put(MediaStore.Downloads.MIME_TYPE, "application/gpx+xml")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = context.contentResolver
            .insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return false
        context.contentResolver.openOutputStream(uri)
            ?.use { it.write(gpx.toByteArray()) }
        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        context.contentResolver.update(uri, values, null, null)
        true
    } catch (e: Exception) { false }
}
