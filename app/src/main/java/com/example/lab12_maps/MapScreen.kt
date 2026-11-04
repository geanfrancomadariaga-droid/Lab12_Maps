
package com.example.lab12_maps

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState

@Composable
fun MapScreen() {

    val context = LocalContext.current

    val arequipaLocation = LatLng(-16.4040102, -71.559611)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            arequipaLocation,
            12f
        )
    }

    com.google.android.gms.maps.MapsInitializer.initialize(context)

    val iconoMontana = remember {
        val drawable = ContextCompat.getDrawable(
            context,
            R.drawable.ic_montana
        )!!

        val size = (48 * context.resources.displayMetrics.density).toInt()
        val bitmap = Bitmap.createBitmap(
            size,
            size,
            Bitmap.Config.ARGB_8888 )

        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)

        BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    val locations = listOf(
        "José Luis Bustamante y Rivero" to
                LatLng(-16.433415, -71.5442652),

        "Paucarpata" to
                LatLng(-16.4205151, -71.4945209),

        "Zamacola" to
                LatLng(-16.3524187, -71.5675994)
    )

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState
    ) {

        // Marcador azul de Arequipa
        Marker(
            state = rememberMarkerState(
                position = arequipaLocation
            ),
            title = "Arequipa, Perú",
            icon = BitmapDescriptorFactory.defaultMarker(
                BitmapDescriptorFactory.HUE_BLUE
            )
        )

        // Marcador con icono personalizado
        Marker(
            state = rememberMarkerState(
                position = LatLng(-16.398866, -71.536961)
            ),
            title = "Plaza de Armas",
            snippet = "Marcador personalizado",
            icon = iconoMontana
        )

        // Marcadores adicionales
        locations.forEach { (nombre, ubicacion) ->
            key(nombre) {
                Marker(
                    state = rememberMarkerState(
                        position = ubicacion
                    ),
                    title = nombre,
                    snippet = "Punto de interés"
                )
            }
        }
    }
}
