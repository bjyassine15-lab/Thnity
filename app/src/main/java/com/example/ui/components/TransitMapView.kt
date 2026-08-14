package com.example.ui.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.Poi
import com.example.data.model.Street
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@Composable
fun TransitMapView(
    pois: List<Poi>,
    streets: List<Street>,
    selectedPoi: Poi?,
    onPoiClick: (Poi) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var mapError by remember { mutableStateOf<String?>(null) }
    val mapResult = remember(context) {
        runCatching {
            // Load persistent cache settings first, then identify the app with
            // a stable, contactable User-Agent required by OSM tile servers.
            Configuration.getInstance().load(
                context,
                context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
            )
            Configuration.getInstance().userAgentValue =
                "Thnity/1.0 (+https://github.com/bjyassine15-lab/Thnity)"
            MapView(context).apply {
                // MAPNIK resolves to https://tile.openstreetmap.org/ in the
                // bundled osmdroid version and keeps normal disk caching.
                setTileSource(TileSourceFactory.MAPNIK)
                setUseDataConnection(true)
                setMultiTouchControls(true)
                controller.setZoom(13.5)
                controller.setCenter(GeoPoint(36.8064948, 10.1815316))
            }
        }
    }
    val mapView = mapResult.getOrNull()

    LaunchedEffect(mapResult) {
        mapResult.exceptionOrNull()?.let {
            mapError = "تعذر تشغيل الخريطة حالياً"
        }
    }

    if (mapView == null) {
        Box(
            modifier = modifier.background(Color(0xFF0F172A)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = mapError ?: "الخريطة غير متاحة حالياً، ويمكنك استخدام بقية أقسام التطبيق.",
                color = Color.White
            )
        }
        return
    }

    DisposableEffect(mapView) {
        runCatching { mapView.onResume() }
            .onFailure { mapError = "تعذر تشغيل الخريطة حالياً" }
        onDispose {
            runCatching {
                mapView.onPause()
                mapView.onDetach()
            }
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { mapView },
            update = { map ->
                runCatching {
                    map.overlays.clear()

                    streets.forEach { street ->
                        if (street.coordinates.isNotEmpty()) {
                            val polyline = Polyline(map).apply {
                                title = street.name
                                val geoPoints = street.coordinates.map { pt ->
                                    val lon = pt.getOrNull(0) ?: 0.0
                                    val lat = pt.getOrNull(1) ?: 0.0
                                    GeoPoint(lat, lon)
                                }
                                setPoints(geoPoints)
                                outlinePaint.color = android.graphics.Color.parseColor("#4338CA")
                                outlinePaint.strokeWidth = 10f
                            }
                            map.overlays.add(polyline)
                        }
                    }

                    pois.forEach { poi ->
                        val lat = poi.latitude
                        val lon = poi.longitude
                        if (lat != 0.0 && lon != 0.0) {
                            val marker = Marker(map).apply {
                                position = GeoPoint(lat, lon)
                                title = poi.name
                                snippet = poi.address ?: poi.type ?: poi.getDisplayName("en")
                                setOnMarkerClickListener { _, _ ->
                                    onPoiClick(poi)
                                    showInfoWindow()
                                    true
                                }
                            }
                            map.overlays.add(marker)
                        }
                    }

                    if (selectedPoi != null && selectedPoi.latitude != 0.0) {
                        map.controller.animateTo(GeoPoint(selectedPoi.latitude, selectedPoi.longitude))
                    }
                    map.invalidate()
                }.onFailure {
                    mapError = "تعذر عرض بيانات الخريطة"
                }
            }
        )

        if (mapError != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(12.dp)
            ) {
                Text(
                    text = mapError!!,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        // Required visible OpenStreetMap attribution.
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "© OpenStreetMap contributors",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Overlay Map badge
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = "خريطة شبكة النقل المباشرة (OpenStreetMap)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
