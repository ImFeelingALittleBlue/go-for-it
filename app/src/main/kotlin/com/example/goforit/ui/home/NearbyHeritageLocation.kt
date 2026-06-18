package com.example.goforit.ui.home

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.goforit.data.Heritage

private const val NEARBY_RADIUS_METERS = 40f
private const val LOCATION_ACCURACY_TOLERANCE_CAP_METERS = 60f

@SuppressLint("MissingPermission")
@Composable
fun rememberIsNearHeritage(
    heritage: Heritage,
    hasLocationPermission: Boolean
): State<Boolean> {
    val context = LocalContext.current
    val isNearby = remember(heritage.id) { mutableStateOf(false) }

    DisposableEffect(heritage.id, hasLocationPermission) {
        if (!hasLocationPermission) {
            isNearby.value = false
            return@DisposableEffect onDispose {}
        }

        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val listener = LocationListener { location ->
            val distance = FloatArray(1)
            Location.distanceBetween(
                location.latitude,
                location.longitude,
                heritage.lat,
                heritage.lng,
                distance
            )
            val accuracyTolerance = if (location.hasAccuracy()) {
                location.accuracy.coerceIn(0f, LOCATION_ACCURACY_TOLERANCE_CAP_METERS)
            } else {
                0f
            }
            isNearby.value = distance[0] <= NEARBY_RADIUS_METERS + accuracyTolerance
        }

        listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
            .forEach { provider ->
                manager.getLastKnownLocation(provider)?.let(listener::onLocationChanged)
                manager.requestLocationUpdates(
                    provider,
                    1_000L,
                    1f,
                    listener,
                    Looper.getMainLooper()
                )
            }

        onDispose { manager.removeUpdates(listener) }
    }

    return isNearby
}
