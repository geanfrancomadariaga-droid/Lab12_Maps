
package com.example.lab12_maps

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

@SuppressLint("MissingPermission")
@Composable
fun MapScreen() {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 1. Ubicaciones principales
    val arequipa = LatLng(-16.4040102, -71.559611)
    val yura = LatLng(-16.2520984, -71.6836503)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(arequipa, 12f)
    }

    // 2. Inicialización de Google Maps
    remember(context) {
        MapsInitializer.initialize(context)
        true
    }

    // 3. Marcador personalizado
    val iconoMontana = remember(context) {
        val drawable = ContextCompat.getDrawable(
            context,
            R.drawable.ic_montana
        )!!

        val size = (
                48 * context.resources.displayMetrics.density
                ).toInt().coerceAtLeast(1)

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

    // 4. Marcadores adicionales
    val locations = listOf(
        "José Luis Bustamante y Rivero" to
                LatLng(-16.433415, -71.5442652),
        "Paucarpata" to
                LatLng(-16.4205151, -71.4945209),
        "Zamacola" to
                LatLng(-16.3524187, -71.5675994)
    )

    // 5. Polígonos del laboratorio
    val mallAventuraPolygon = listOf(
        LatLng(-16.432292, -71.509145),
        LatLng(-16.432757, -71.509626),
        LatLng(-16.433013, -71.509310),
        LatLng(-16.432566, -71.508853)
    )

    val parqueLambramaniPolygon = listOf(
        LatLng(-16.422704, -71.530830),
        LatLng(-16.422920, -71.531340),
        LatLng(-16.423264, -71.531110),
        LatLng(-16.423050, -71.530600)
    )

    val plazaDeArmasPolygon = listOf(
        LatLng(-16.398866, -71.536961),
        LatLng(-16.398744, -71.536529),
        LatLng(-16.399178, -71.536289),
        LatLng(-16.399299, -71.536721)
    )

    // EJERCICIO 1: Tipos de mapa
    var tipoMapa by remember {
        mutableStateOf(MapType.NORMAL)
    }

    // EJERCICIO 2: Permisos y ubicación
    val finePermission = Manifest.permission.ACCESS_FINE_LOCATION
    val coarsePermission = Manifest.permission.ACCESS_COARSE_LOCATION

    fun tienePermiso(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            finePermission
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    coarsePermission
                ) == PackageManager.PERMISSION_GRANTED
    }

    var permisoUbicacion by remember {
        mutableStateOf(tienePermiso())
    }

    var mensaje by remember {
        mutableStateOf("Mapa de Arequipa")
    }

    val fusedLocationClient = remember(context) {
        LocationServices.getFusedLocationProviderClient(context)
    }

    fun moverCamara(destino: LatLng, zoom: Float) {
        scope.launch {
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(
                    destino, zoom
                ),
                durationMs = 3000
            )
        }
    }

    fun obtenerUbicacionActual() {
        if (!tienePermiso()) {
            mensaje = "Permiso de ubicación necesario"
            return
        }

        permisoUbicacion = true
        mensaje = "Buscando ubicación..."

        try {
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
            ).addOnSuccessListener { location ->

                if (location != null) {
                    val posicion = LatLng(
                        location.latitude,
                        location.longitude
                    )

                    moverCamara(posicion, 16f)
                    mensaje = "Ubicación actual encontrada"
                } else {
                    mensaje = "Activa el GPS e inténtalo otra vez"
                }

            }.addOnFailureListener {
                mensaje = "Error al obtener la ubicación"
            }
        } catch (e: SecurityException) {
            permisoUbicacion = false
            mensaje = "Permiso de ubicación no disponible"
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permisos ->
        permisoUbicacion = permisos.values.any { it }

        if (permisoUbicacion) {
            obtenerUbicacionActual()
        } else {
            mensaje = "Permiso de ubicación denegado"
        }
    }

    // 6. Interfaz de la aplicación
    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        // Botones del ejercicio 1
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            Button(onClick = {
                tipoMapa = MapType.NORMAL
            }) {
                Text("Normal")
            }

            Button(onClick = {
                tipoMapa = MapType.HYBRID
            }) {
                Text("Híbrido")
            }

            Button(onClick = {
                tipoMapa = MapType.TERRAIN
            }) {
                Text("Terreno")
            }

            Button(onClick = {
                tipoMapa = MapType.SATELLITE
            }) {
                Text("Satélite")
            }
        }

        // Movimiento de cámara y ubicación actual
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            Button(onClick = {
                moverCamara(yura, 12f)
                mensaje = "Moviendo cámara a Yura"
            }) {
                Text("Ir a Yura")
            }

            Button(onClick = {
                moverCamara(arequipa, 12f)
                mensaje = "Mapa de Arequipa"
            }) {
                Text("Arequipa")
            }

            Button(onClick = {
                if (tienePermiso()) {
                    obtenerUbicacionActual()
                } else {
                    permissionLauncher.launch(
                        arrayOf(
                            finePermission,
                            coarsePermission
                        )
                    )
                }
            }) {
                Text("Mi ubicación")
            }
        }

        Text(
            text = mensaje,
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = 4.dp
            )
        )

        // 7. Mapa principal
        GoogleMap(
            modifier = Modifier.weight(1f),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                mapType = tipoMapa,
                isMyLocationEnabled = permisoUbicacion
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = true,
                myLocationButtonEnabled = false
            )
        ) {

            // Marcador azul de Arequipa
            Marker(
                state = rememberMarkerState(
                    position = arequipa
                ),
                title = "Arequipa, Perú",
                icon = BitmapDescriptorFactory.defaultMarker(
                    BitmapDescriptorFactory.HUE_BLUE
                )
            )

            // Marcador con imagen personalizada
            Marker(
                state = rememberMarkerState(
                    position = LatLng(-16.398866, -71.536961)
                ),
                title = "Plaza de Armas",
                snippet = "Marcador personalizado",
                icon = iconoMontana
            )

            // Otros marcadores
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

            // Polígono: Mall Aventura
            Polygon(
                points = mallAventuraPolygon,
                strokeColor = Color.Red,
                fillColor = Color.Blue.copy(alpha = 0.25f),
                strokeWidth = 5f
            )

            // Polígono: Parque Lambramani
            Polygon(
                points = parqueLambramaniPolygon,
                strokeColor = Color.Red,
                fillColor = Color.Blue.copy(alpha = 0.25f),
                strokeWidth = 5f
            )

            // Polígono: Plaza de Armas
            Polygon(
                points = plazaDeArmasPolygon,
                strokeColor = Color.Red,
                fillColor = Color.Blue.copy(alpha = 0.25f),
                strokeWidth = 5f
            )

            // Polilínea entre puntos de interés
            Polyline(
                points = listOf(
                    LatLng(-16.398866, -71.536961),
                    LatLng(-16.422704, -71.530830),
                    LatLng(-16.432292, -71.509145)
                ),
                color = Color.Magenta,
                width = 8f
            )
        }
    }
}
