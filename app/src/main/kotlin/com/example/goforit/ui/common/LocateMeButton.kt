package com.example.goforit.ui.common

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationOptions

@Composable
fun LocateMeButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Surface(
        onClick = { if (enabled) onClick() },
        modifier = modifier
            .size(44.dp)
            .alpha(if (enabled) 1f else 0.46f),
        shape = CircleShape,
        color = Color(0xFFFFFDF8),
        shadowElevation = 5.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                Icons.Default.MyLocation,
                contentDescription = "定位到目前位置",
                tint = Color(0xFF6E5E52),
                modifier = Modifier.size(23.dp)
            )
        }
    }
}

@SuppressLint("MissingPermission")
fun currentLocationPoint(context: Context): Point? {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return latestKnownLocation(locationManager)?.toPoint()
}

@SuppressLint("MissingPermission")
fun requestCurrentLocationPoint(
    context: Context,
    onLocation: (Point) -> Unit
) {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    latestKnownLocation(locationManager)?.let {
        onLocation(it.toPoint())
        return
    }

    val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        .filter { provider ->
            runCatching { locationManager.isProviderEnabled(provider) }.getOrDefault(false)
        }
    if (providers.isEmpty()) return

    var delivered = false
    lateinit var listener: LocationListener
    listener = LocationListener { location ->
        if (delivered) return@LocationListener
        delivered = true
        providers.forEach { provider ->
            runCatching { locationManager.removeUpdates(listener) }
        }
        onLocation(location.toPoint())
    }

    providers.forEach { provider ->
        runCatching {
            locationManager.requestSingleUpdate(
                provider,
                listener,
                Looper.getMainLooper()
            )
        }
    }
}

fun moveMapToPoint(
    mapView: MapView,
    point: Point,
    zoom: Double = 16.0,
    pitch: Double? = null,
    bearing: Double? = null
) {
    val camera = CameraOptions.Builder()
        .center(point)
        .zoom(zoom)
        .also { builder ->
            pitch?.let(builder::pitch)
            bearing?.let(builder::bearing)
        }
        .build()
    mapView.mapboxMap.setCamera(camera)
}

fun showCurrentLocationMarker(
    manager: CircleAnnotationManager,
    point: Point
) {
    manager.deleteAll()
    manager.create(
        CircleAnnotationOptions()
            .withPoint(point)
            .withCircleRadius(9.0)
            .withCircleColor("#D8473F")
            .withCircleStrokeWidth(3.0)
            .withCircleStrokeColor("#FFFFFF")
    )
}

private fun latestKnownLocation(locationManager: LocationManager): Location? =
    listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        .mapNotNull { provider ->
            runCatching {
                if (locationManager.isProviderEnabled(provider)) {
                    locationManager.getLastKnownLocation(provider)
                } else {
                    null
                }
            }.getOrNull()
        }
        .maxByOrNull { it.time }

private fun Location.toPoint(): Point = Point.fromLngLat(longitude, latitude)
