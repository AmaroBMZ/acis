package com.proyecto.acis.data

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import java.util.Locale
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

suspend fun obtenerUbicacionCheckIn(context: Context): String? {
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val ubicacion = withTimeoutOrNull(12000) {
        suspendCancellableCoroutine<Location?> { espera ->
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    runCatching { manager.removeUpdates(this) }
                    if (espera.isActive) espera.resume(location)
                }
            }
            espera.invokeOnCancellation { runCatching { manager.removeUpdates(listener) } }
            try {
                if (manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    manager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, listener, Looper.getMainLooper())
                } else {
                    espera.resume(null)
                }
            } catch (_: Exception) {
                if (espera.isActive) espera.resume(null)
            }
        }
    }
    return ubicacion?.let { String.format(Locale.US, "%.5f, %.5f", it.latitude, it.longitude) }
}
