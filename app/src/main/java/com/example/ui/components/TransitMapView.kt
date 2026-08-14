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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

    // Configure Osmdroid User Agent
    remember {
        Configuration.getInstance().userAgentValue = context.packageName
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
    }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(13.5)
            // Center around Tunis (36.8065, 10.1815)
            controller.setCenter(GeoPoint(36.8064948, 10.1815316))
        }
    }

    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { mapView },
            update = { map ->
                map.overlays.clear()

                // Draw Streets
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

                // Draw POI Markers
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

                // If a POI is selected, center map on it
                if (selectedPoi != null && selectedPoi.latitude != 0.0) {
                    map.controller.animateTo(GeoPoint(selectedPoi.latitude, selectedPoi.longitude))
                }

                map.invalidate()
            }
        )

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
