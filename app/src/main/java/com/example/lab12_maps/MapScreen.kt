
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
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

import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polygon
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState

import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest

import kotlinx.coroutines.launch

@SuppressLint("MissingPermission")
@Composable
fun MapScreen() {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // --------------------------------------------------
    // 1. UBICACIONES PRINCIPALES
    // --------------------------------------------------

    val arequipa = LatLng(-16.4040102, -71.559611)
    val yura = LatLng(-16.2520984, -71.6836503)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            arequipa,
            12f
        )
    }

    // --------------------------------------------------
    // 2. INICIALIZAR GOOGLE MAPS
    // --------------------------------------------------

    remember(context) {
        MapsInitializer.initialize(context.applicationContext)
        true
    }

    // --------------------------------------------------
    // 3. INICIALIZAR GOOGLE PLACES
    // --------------------------------------------------

    val placesApiKey = remember(context) {
        val appInfo = context.packageManager
            .getApplicationInfo(
                context.packageName,
                PackageManager.GET_META_DATA
            )

        appInfo.metaData?.getString(
            "com.google.android.geo.API_KEY"
        ).orEmpty()
    }

    val placesClient = remember(context, placesApiKey) {

        if (!Places.isInitialized()) {
            Places.initializeWithNewPlacesApiEnabled(
                context.applicationContext,
                placesApiKey
            )
        }

        Places.createClient(context.applicationContext)
    }

    // --------------------------------------------------
    // 4. ICONO PERSONALIZADO
    // --------------------------------------------------

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

    // --------------------------------------------------
    // 5. MARCADORES ADICIONALES
    // --------------------------------------------------

    val locations = listOf(
        "José Luis Bustamante y Rivero" to
                LatLng(-16.433415, -71.5442652),

        "Paucarpata" to
                LatLng(-16.4205151, -71.4945209),

        "Zamacola" to
                LatLng(-16.3524187, -71.5675994)
    )

    // --------------------------------------------------
    // 6. POLÍGONOS
    // --------------------------------------------------

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

    // --------------------------------------------------
    // 7. ESTADOS DE LA APLICACIÓN
    // --------------------------------------------------

    var tipoMapa by remember {
        mutableStateOf(MapType.NORMAL)
    }

    var mensaje by remember {
        mutableStateOf("Mapa de Arequipa")
    }

    var textoBusqueda by remember {
        mutableStateOf("")
    }

    var resultadosBusqueda by remember {
        mutableStateOf<List<AutocompletePrediction>>(
            emptyList()
        )
    }

    var marcadorBuscado by remember {
        mutableStateOf<LatLng?>(null)
    }

    var nombreBuscado by remember {
        mutableStateOf("")
    }

    var buscando by remember {
        mutableStateOf(false)
    }

    // Token utilizado para asociar las consultas de una búsqueda
    var sessionToken by remember {
        mutableStateOf(AutocompleteSessionToken.newInstance())
    }

    // --------------------------------------------------
    // 8. MOVIMIENTO DE CÁMARA
    // --------------------------------------------------

    fun moverCamara(destino: LatLng, zoom: Float) {
        scope.launch {
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(
                    destino,
                    zoom
                ),
                durationMs = 3000
            )
        }
    }

    // --------------------------------------------------
    // 9. PERMISOS DE UBICACIÓN
    // --------------------------------------------------

    val finePermission =
        Manifest.permission.ACCESS_FINE_LOCATION

    val coarsePermission =
        Manifest.permission.ACCESS_COARSE_LOCATION

    fun tienePermiso(): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            finePermission
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            coarsePermission
        ) == PackageManager.PERMISSION_GRANTED

        return fineGranted || coarseGranted
    }

    var permisoUbicacion by remember {
        mutableStateOf(tienePermiso())
    }

    val fusedLocationClient = remember(context) {
        LocationServices.getFusedLocationProviderClient(context)
    }

    fun obtenerUbicacionActual() {

        if (!tienePermiso()) {
            permisoUbicacion = false
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
                mensaje = "Error al obtener ubicación"
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

    // --------------------------------------------------
    // 10. BUSCAR LUGARES CON GOOGLE PLACES
    // --------------------------------------------------

    fun buscarLugares() {

        val consulta = textoBusqueda.trim()

        if (consulta.isBlank()) {
            mensaje = "Escribe un lugar para buscar"
            resultadosBusqueda = emptyList()
            return
        }

        buscando = true
        mensaje = "Buscando lugares..."
        resultadosBusqueda = emptyList()

        val request = FindAutocompletePredictionsRequest
            .builder()
            .setQuery(consulta)
            .setSessionToken(sessionToken)
            .build()

        placesClient.findAutocompletePredictions(request)
            .addOnSuccessListener { response ->

                resultadosBusqueda =
                    response.autocompletePredictions

                mensaje = if (resultadosBusqueda.isEmpty()) {
                    "No se encontraron lugares"
                } else {
                    "Selecciona uno de los resultados"
                }

                buscando = false
            }
            .addOnFailureListener { error ->

                mensaje = "Error de Places: ${error.message}"
                buscando = false
            }
    }

    // --------------------------------------------------
    // 11. SELECCIONAR LUGAR DEL BUSCADOR
    // --------------------------------------------------

    fun seleccionarLugar(
        prediction: AutocompletePrediction
    ) {

        mensaje = "Cargando lugar..."

        val fields = listOf(
            Place.Field.ID,
            Place.Field.DISPLAY_NAME,
            Place.Field.LOCATION
        )

        val request = FetchPlaceRequest.builder(
            prediction.placeId,
            fields
        )
            .setSessionToken(sessionToken)
            .build()

        placesClient.fetchPlace(request)
            .addOnSuccessListener { response ->

                val lugar = response.place
                val posicion = lugar.location

                if (posicion != null) {

                    marcadorBuscado = posicion

                    nombreBuscado =
                        lugar.displayName ?: textoBusqueda

                    textoBusqueda = nombreBuscado

                    resultadosBusqueda = emptyList()

                    moverCamara(posicion, 16f)

                    mensaje = "Lugar encontrado: $nombreBuscado"

                    // Nueva sesión para búsquedas posteriores
                    sessionToken =
                        AutocompleteSessionToken.newInstance()

                } else {
                    mensaje = "El lugar no tiene coordenadas"
                }
            }
            .addOnFailureListener { error ->
                mensaje = "Error: ${error.message}"
            }
    }

    // --------------------------------------------------
    // 12. INTERFAZ DE USUARIO
    // --------------------------------------------------

    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        // BUSCADOR DE LUGARES
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {

            OutlinedTextField(
                value = textoBusqueda,
                onValueChange = {
                    textoBusqueda = it
                    resultadosBusqueda = emptyList()
                },
                label = {
                    Text("Buscar lugar")
                },
                placeholder = {
                    Text("Ej. Plaza de Armas Arequipa")
                },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )

            Button(
                enabled = !buscando,
                onClick = {
                    buscarLugares()
                }
            ) {
                Text(if (buscando) "..." else "Buscar")
            }
        }

        // RESULTADOS DE GOOGLE PLACES
        if (resultadosBusqueda.isNotEmpty()) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 180.dp)
                    .verticalScroll(rememberScrollState())
            ) {

                resultadosBusqueda.take(5).forEach { resultado ->

                    key(resultado.placeId) {

                        Button(
                            onClick = {
                                seleccionarLugar(resultado)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                        ) {
                            Text(
                                resultado.getPrimaryText(null).toString()
                            )
                        }
                    }
                }
            }
        }

        // CAMBIO DE TIPO DE MAPA
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {

            Button(
                onClick = {
                    tipoMapa = MapType.NORMAL
                }
            ) {
                Text("Normal")
            }

            Button(
                onClick = {
                    tipoMapa = MapType.HYBRID
                }
            ) {
                Text("Híbrido")
            }

            Button(
                onClick = {
                    tipoMapa = MapType.TERRAIN
                }
            ) {
                Text("Terreno")
            }

            Button(
                onClick = {
                    tipoMapa = MapType.SATELLITE
                }
            ) {
                Text("Satélite")
            }
        }

        // BOTONES DE NAVEGACIÓN Y UBICACIÓN
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {

            Button(
                onClick = {
                    moverCamara(yura, 12f)
                    mensaje = "Moviendo cámara a Yura"
                }
            ) {
                Text("Ir a Yura")
            }

            Button(
                onClick = {
                    moverCamara(arequipa, 12f)
                    mensaje = "Mapa de Arequipa"
                }
            ) {
                Text("Arequipa")
            }

            Button(
                onClick = {
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
                }
            ) {
                Text("Mi ubicación")
            }
        }

        // MENSAJE DE ESTADO
        Text(
            text = mensaje,
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = 4.dp
            )
        )

        // --------------------------------------------------
        // 13. GOOGLE MAPS
        // --------------------------------------------------

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

            // MARCADOR AZUL DE AREQUIPA
            Marker(
                state = rememberMarkerState(
                    position = arequipa
                ),
                title = "Arequipa, Perú",
                icon = BitmapDescriptorFactory.defaultMarker(
                    BitmapDescriptorFactory.HUE_BLUE
                )
            )

            // MARCADOR PERSONALIZADO DE MONTAÑA
            Marker(
                state = rememberMarkerState(
                    position = LatLng(
                        -16.398866,
                        -71.536961
                    )
                ),
                title = "Plaza de Armas",
                snippet = "Marcador personalizado",
                icon = iconoMontana
            )

            // MARCADORES ADICIONALES
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

            // POLÍGONO: MALL AVENTURA
            Polygon(
                points = mallAventuraPolygon,
                strokeColor = Color.Red,
                fillColor = Color.Blue.copy(alpha = 0.25f),
                strokeWidth = 5f
            )

            // POLÍGONO: PARQUE LAMBRAMANI
            Polygon(
                points = parqueLambramaniPolygon,
                strokeColor = Color.Red,
                fillColor = Color.Blue.copy(alpha = 0.25f),
                strokeWidth = 5f
            )

            // POLÍGONO: PLAZA DE ARMAS
            Polygon(
                points = plazaDeArmasPolygon,
                strokeColor = Color.Red,
                fillColor = Color.Blue.copy(alpha = 0.25f),
                strokeWidth = 5f
            )

            // POLILÍNEA ROSADA
            Polyline(
                points = listOf(
                    LatLng(-16.398866, -71.536961),
                    LatLng(-16.422704, -71.530830),
                    LatLng(-16.432292, -71.509145)
                ),
                color = Color.Magenta,
                width = 8f
            )

            // MARCADOR VERDE DE GOOGLE PLACES
            marcadorBuscado?.let { posicion ->

                key(posicion) {

                    Marker(
                        state = rememberMarkerState(
                            position = posicion
                        ),
                        title = nombreBuscado,
                        snippet = "Resultado de Google Places",
                        icon = BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_GREEN
                        )
                    )
                }
            }
        }
    }
}
