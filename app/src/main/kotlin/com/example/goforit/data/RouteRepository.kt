package com.example.goforit.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Xml
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mapbox.geojson.Point
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.time.Instant
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object RouteRepository {
    private val records = mutableStateListOf<RouteRecord>()
    private var started = false

    fun records(): SnapshotStateList<RouteRecord> = records

    fun start() {
        if (started) return
        started = true
        val auth = Firebase.auth
        if (auth.currentUser == null) {
            auth.signInAnonymously().addOnSuccessListener { listen() }
        } else {
            listen()
        }
    }

    private fun listen() {
        collection()?.orderBy("recordedAt", Query.Direction.DESCENDING)
            ?.addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener
                records.clear()
                for (doc in snapshot.documents) {
                    doc.toObject(RouteRecord::class.java)
                        ?.copy(docId = doc.id)
                        ?.let { records.add(it) }
                }
            }
    }

    suspend fun addGpx(context: Context, uri: Uri) = withContext(Dispatchers.IO) {
        val fileName = displayName(context, uri)
        val gpx = context.contentResolver.openInputStream(uri)
            ?.bufferedReader()
            ?.use { it.readText() }
            ?: return@withContext
        val parsed = parseGpx(gpx)
        val data = hashMapOf(
            "name" to (parsed.name.ifBlank { fileName.substringBeforeLast('.') }),
            "gpxFileName" to fileName,
            "gpxContent" to gpx,
            "distanceMeters" to parsed.distanceMeters,
            "pointCount" to parsed.pointCount,
            "startedAt" to parsed.startedAt,
            "recordedAt" to System.currentTimeMillis()
        )
        collection()?.add(data)
    }

    fun delete(docId: String) {
        collection()?.document(docId)?.delete()
    }

    // 切換愛心狀態：liked=true 時路線會出現在已儲存路線頁
    fun toggleLike(docId: String, liked: Boolean) {
        collection()?.document(docId)?.update("liked", liked)
    }

    fun addRun(
        name: String,
        points: List<Point>,
        routeGpx: String = "",
        elapsedSeconds: Int = 0,
        silverEarned: Int = 0,
        unlockedCount: Int = 0
    ) {
        val now = System.currentTimeMillis()
        val gpx = when {
            routeGpx.isNotBlank() -> routeGpx
            points.size >= 2      -> buildGpx(name, points, now)
            else                  -> ""
        }
        val routePoints = points.map { RoutePoint(it.latitude(), it.longitude()) }
        val dist = if (routePoints.size >= 2)
            routePoints.zipWithNext().sumOf { (a, b) -> distanceMeters(a, b) }
        else 0.0
        val data = hashMapOf(
            "name" to name, "gpxFileName" to "run-$now.gpx", "gpxContent" to gpx,
            "distanceMeters" to dist, "pointCount" to points.size,
            "startedAt" to now, "recordedAt" to now,
            "elapsedSeconds" to elapsedSeconds,
            "silverEarned" to silverEarned,
            "unlockedCount" to unlockedCount
        )
        collection()?.add(data)
    }

    private fun collection(): CollectionReference? {
        val uid = Firebase.auth.currentUser?.uid ?: return null
        return Firebase.firestore
            .collection("users").document(uid)
            .collection("routes")
    }

    private fun displayName(context: Context, uri: Uri): String {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) {
                return cursor.getString(index)
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/') ?: "route.gpx"
    }

    private fun parseGpx(gpx: String): ParsedRoute {
        val parser = Xml.newPullParser().apply {
            setInput(gpx.reader())
        }
        val points = mutableListOf<RoutePoint>()
        var routeName = ""
        var currentPoint: RoutePoint? = null
        var currentTag = ""
        var insideTrack = false
        var firstTime = 0L

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name.orEmpty()
                    when (currentTag) {
                        "trk" -> insideTrack = true
                        "trkpt", "rtept" -> {
                            val lat = parser.getAttributeValue(null, "lat")?.toDoubleOrNull()
                            val lon = parser.getAttributeValue(null, "lon")?.toDoubleOrNull()
                            if (lat != null && lon != null) currentPoint = RoutePoint(lat, lon)
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    val text = parser.text?.trim().orEmpty()
                    if (text.isNotBlank()) {
                        when {
                            currentTag == "name" && insideTrack && routeName.isBlank() -> routeName = text
                            currentTag == "time" && currentPoint != null -> {
                                val millis = runCatching { Instant.parse(text).toEpochMilli() }.getOrDefault(0L)
                                currentPoint = currentPoint?.copy(timeMillis = millis)
                                if (firstTime == 0L) firstTime = millis
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "trk" -> insideTrack = false
                        "trkpt", "rtept" -> {
                            currentPoint?.let(points::add)
                            currentPoint = null
                        }
                    }
                    currentTag = ""
                }
            }
            event = parser.next()
        }

        return ParsedRoute(
            name = routeName,
            distanceMeters = points.zipWithNext().sumOf { (a, b) -> distanceMeters(a, b) },
            pointCount = points.size,
            startedAt = firstTime
        )
    }

    private fun distanceMeters(a: RoutePoint, b: RoutePoint): Double {
        val earthRadius = 6_371_000.0
        val dLat = Math.toRadians(b.lat - a.lat)
        val dLon = Math.toRadians(b.lon - a.lon)
        val lat1 = Math.toRadians(a.lat)
        val lat2 = Math.toRadians(b.lat)
        val h = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
        return 2 * earthRadius * atan2(sqrt(h), sqrt(1 - h))
    }

    private fun buildGpx(name: String, points: List<Point>, timeMillis: Long): String {
        val time = Instant.ofEpochMilli(timeMillis)
        return buildString {
            append("""<?xml version="1.0" encoding="UTF-8"?>""")
            append('\n')
            append("""<gpx version="1.1" creator="GoForIt">""")
            append('\n')
            append("<trk><name>")
            append(escapeXml(name))
            append("</name><trkseg>")
            append('\n')
            points.forEach { point ->
                append("""<trkpt lat="${point.latitude()}" lon="${point.longitude()}"><time>$time</time></trkpt>""")
                append('\n')
            }
            append("</trkseg></trk>")
            append('\n')
            append("</gpx>")
        }
    }

    private fun escapeXml(value: String): String =
        value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")

    private data class RoutePoint(val lat: Double, val lon: Double, val timeMillis: Long = 0L)
    private data class ParsedRoute(
        val name: String,
        val distanceMeters: Double,
        val pointCount: Int,
        val startedAt: Long
    )
}
