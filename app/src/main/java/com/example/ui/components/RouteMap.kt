package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.data.model.GpsTrackEntity
import com.example.data.model.StoreEntity
import com.example.service.LocationTrackingService
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@Composable
fun RouteMapCard(
    routeId: Long,
    routeName: String,
    gpsPoints: List<GpsTrackEntity>,
    stores: List<StoreEntity>,
    isTrackingActive: Boolean,
    currentTrackingRouteId: Long,
    todayDateString: String,
    onStartTracking: () -> Unit,
    onStopTracking: () -> Unit,
    onDeleteHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isThisRouteTracking = isTrackingActive && currentTrackingRouteId == routeId

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isThisRouteTracking) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            // Map
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                OsmMapView(
                    routeId = routeId,
                    gpsPoints = gpsPoints,
                    stores = stores,
                    modifier = Modifier.fillMaxSize()
                )

                // Point count badge
                if (gpsPoints.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .padding(8.dp)
                            .align(Alignment.TopEnd)
                    ) {
                        Text(
                            text = "📍 ${gpsPoints.size} titik",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Controls
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🗺️ GPS Tracking",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    if (gpsPoints.isNotEmpty() && !isThisRouteTracking) {
                        IconButton(onClick = onDeleteHistory) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "Hapus",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (isThisRouteTracking) {
                            onStopTracking()
                        } else {
                            onStartTracking()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isThisRouteTracking)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = if (isThisRouteTracking) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isThisRouteTracking) "Stop Tracking" else "Mulai Tracking"
                    )
                }
            }
        }
    }
}

@Composable
fun OsmMapView(
    routeId: Long,
    gpsPoints: List<GpsTrackEntity>,
    stores: List<StoreEntity>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Initialize osmdroid config once
    remember {
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = context.packageName
    }

    var hasAutoFitted by remember { mutableStateOf(false) }

    LaunchedEffect(routeId) {
        hasAutoFitted = false
    }

    AndroidView(
        factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(15.0)
            }
        },
        update = { map ->
            val ctx = map.context
            map.overlays.clear()

            // Add GPS polyline
            if (gpsPoints.size >= 2) {
                val polyline = Polyline().apply {
                    outlinePaint.color = Color(0xFF3ECF8E).toArgb()
                    outlinePaint.strokeWidth = 6f
                    setPoints(gpsPoints.map { GeoPoint(it.latitude, it.longitude) })
                }
                map.overlays.add(polyline)

                // Start marker
                val start = gpsPoints.first()
                addMarker(map, ctx, start.latitude, start.longitude, "Start", Color(0xFF22C55E))

                // End marker
                val end = gpsPoints.last()
                addMarker(map, ctx, end.latitude, end.longitude, "Sekarang", Color(0xFFEF4444))

                // Frame the route once. Re-fitting on every GPS point would
                // continuously override the user's zoom and pan.
                if (!hasAutoFitted) {
                    val boundingBox = BoundingBox.fromGeoPoints(
                        gpsPoints.map { GeoPoint(it.latitude, it.longitude) }
                    )
                    map.zoomToBoundingBox(boundingBox.increaseByScale(1.3f), true)
                    hasAutoFitted = true
                }
            } else if (gpsPoints.size == 1) {
                val point = gpsPoints.first()
                if (!hasAutoFitted) {
                    map.controller.setCenter(GeoPoint(point.latitude, point.longitude))
                    hasAutoFitted = true
                }
                addMarker(map, ctx, point.latitude, point.longitude, "Posisi", Color(0xFF3ECF8E))
            }

            // Add store markers
            stores.forEach { store ->
                if (store.latitude != null && store.longitude != null) {
                    addMarker(
                        map, ctx,
                        store.latitude, store.longitude,
                        store.name,
                        Color(0xFF3B82F6)
                    )
                }
            }

            // Default center (Jakarta) if no data
            if (gpsPoints.isEmpty() && stores.none { it.latitude != null }) {
                map.controller.setCenter(GeoPoint(-6.2088, 106.8456))
                map.controller.setZoom(12.0)
            }
        },
        modifier = modifier
    )
}

private fun addMarker(map: MapView, context: Context, lat: Double, lng: Double, title: String, color: Color) {
    val marker = Marker(map).apply {
        position = GeoPoint(lat, lng)
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        this.title = title
        this.snippet = String.format("%.6f, %.6f", lat, lng)

        // Create colored circle bitmap
        val size = 32
        val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint().apply {
            this.color = color.toArgb()
            isAntiAlias = true
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        val whitePaint = android.graphics.Paint().apply {
            this.color = android.graphics.Color.WHITE
            isAntiAlias = true
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 4f, whitePaint)

        icon = BitmapDrawable(context.resources, bitmap)
    }
    map.overlays.add(marker)
}
