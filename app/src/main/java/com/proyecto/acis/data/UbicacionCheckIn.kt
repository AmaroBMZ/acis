package com.proyecto.acis.data

import android.content.Context
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import java.util.Locale
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Obtiene las coordenadas geográficas de la ubicación aproximada del dispositivo
 * durante el check-in de una visita técnica. Retorna null si no hay permiso o señal.
 */
suspend fun obtenerUbicacionCheckIn(context: Context): String? {
    // Sin permiso de ubicación aproximada se devuelve null y la visita puede continuar sin coordenadas.
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) return null
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    // Limita la espera a 12 segundos; el tiempo agotado también produce null.
    val ubicacion = withTimeoutOrNull(12000) {
        // Adapta el callback de Android a una función suspendida, sin bloquear el hilo.
        suspendCancellableCoroutine<Location?> { espera ->
            val listener = object : LocationListener {
                // Al recibir la ubicación, retira el listener y reanuda la corutina si aún está activa.
                override fun onLocationChanged(location: Location) {
                    runCatching { manager.removeUpdates(this) }
                    if (espera.isActive) espera.resume(location)
                }
            }
            // Si se cancela la espera, deja de escuchar actualizaciones para no mantener recursos activos.
            espera.invokeOnCancellation { runCatching { manager.removeUpdates(listener) } }
            try {
                // Usa NETWORK_PROVIDER y ubicación aproximada; no solicita GPS preciso ni seguimiento continuo.
                if (manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    manager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, listener, Looper.getMainLooper())
                } else {
                    espera.resume(null)
                }
            // Contempla que el permiso se retire mientras se está solicitando la ubicación.
            } catch (_: SecurityException) {
                if (espera.isActive) espera.resume(null)
            // Un proveedor no disponible también se trata como ausencia de ubicación.
            } catch (_: IllegalArgumentException) {
                if (espera.isActive) espera.resume(null)
            }
        }
    }
    // Si hay coordenadas, las formatea con punto decimal y cinco decimales.
    return ubicacion?.let { String.format(Locale.US, "%.5f, %.5f", it.latitude, it.longitude) }
}
