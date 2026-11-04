
package com.example.lab12_maps

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import kotlinx.coroutines.launch

@Composable
fun MapScreen() {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Ubicaciones principales
    val arequipaLocation = LatLng(-16.4040102, -71.559611)
    val yuraLocation = LatLng(-16.2520984, -71.6836503)

    // Posición inicial de la cámara
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            arequipaLocation,
            12f
        )
    }

    // Inicializar Google Maps
    remember(context) {
        MapsInitializer.initialize(context)
        true
    }

    // Crear icono personalizado
    val iconoMontana = remember(context) {

        val drawable = ContextCompat.getDrawable(
            context,
            R.drawable.ic_montana
        )!!

        val size = (
                48 * context.resources.displayMetrics.density
                ).toInt()

        val bitmap = Bitmap.createBitmap(
            size,
            size,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(bitmap)

        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)

        BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    // Puntos de interés
    val locations = listOf(
        "José Luis Bustamante y Rivero" to
                LatLng(-16.433415, -71.5442652),

        "Paucarpata" to
                LatLng(-16.4205151, -71.4945209),

        "Zamacola" to
                LatLng(-16.3524187, -71.5675994)
    )

    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        // Botones para mover la cámara
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {

            Button(
                onClick = {
                    scope.launch {
                        cameraPositionState.animate(
                            update = CameraUpdateFactory.newLatLngZoom(
                                yuraLocation,
                                12f
                            ),
                            durationMs = 3000
                        )
                    }
                }
            ) {
                Text("Ir a Yura")
            }

            Button(
                onClick = {
                    scope.launch {
                        cameraPositionState.animate(
                            update = CameraUpdateFactory.newLatLngZoom(
                                arequipaLocation,
                                12f
                            ),
                            durationMs = 3000
                        )
                    }
                }
            ) {
                Text("Arequipa")
            }
        }

        // Mapa de Google
        GoogleMap(
            modifier = Modifier.weight(1f),
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

            // Marcador personalizado de Plaza de Armas
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
}
