package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.model.GpsSessionEntity
import com.example.data.model.GpsTrackEntity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LocationTrackingService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var database: AppDatabase? = null
    private var currentRouteId: Long = -1
    private var currentDateString: String = ""
    private var sessionId: Long = -1
    private var sessionInsertJob: Deferred<Long>? = null
    private var pointCount: Int = 0
    private var lastLocation: Location? = null
    private var totalDistance: Float = 0f

    companion object {
        const val CHANNEL_ID = "gps_tracking_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.example.START_TRACKING"
        const val ACTION_STOP = "com.example.STOP_TRACKING"
        const val EXTRA_ROUTE_ID = "route_id"

        private var _isRunning = false
        val isRunning: Boolean get() = _isRunning
        private var _currentRouteId: Long = -1
        val currentTrackingRouteId: Long get() = _currentRouteId

        private val _trackingState = MutableStateFlow(TrackingState())
        val trackingState: StateFlow<TrackingState> = _trackingState.asStateFlow()
    }

    data class TrackingState(
        val isRunning: Boolean = false,
        val routeId: Long = -1L
    )

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        database = AppDatabase.getDatabase(applicationContext, CoroutineScope(SupervisorJob()))
        createNotificationChannel()
        setupLocationCallback()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val routeId = intent.getLongExtra(EXTRA_ROUTE_ID, -1)
                if (routeId != -1L) {
                    startTracking(routeId)
                }
            }
            ACTION_STOP -> {
                stopTracking()
            }
        }
        return START_STICKY
    }

    private fun startTracking(routeId: Long) {
        if (_isRunning && _currentRouteId == routeId) return

        currentRouteId = routeId
        _currentRouteId = routeId
        pointCount = 0
        totalDistance = 0f
        lastLocation = null
        currentDateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val hasFineLocation = ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFineLocation) {
            stopSelf()
            return
        }

        // Create session
        sessionInsertJob = scope.async {
            val session = GpsSessionEntity(
                routeId = routeId,
                dateString = currentDateString,
                startTime = System.currentTimeMillis(),
                isActive = true
            )
            val id = database?.gpsSessionDao()?.insertSession(session) ?: -1
            sessionId = id
            id
        }

        val notification = buildNotification("Mulai tracking...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateDistanceMeters(0f)
            .setMinUpdateIntervalMillis(1000L)
            .setWaitForAccurateLocation(false)
            .build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        _isRunning = true
        updateNotification("Tracking aktif • 0 titik")
        _trackingState.value = TrackingState(isRunning = true, routeId = routeId)
    }

    private fun stopTracking() {
        fusedLocationClient.removeLocationUpdates(locationCallback)

        val sessionJob = sessionInsertJob
        val finalPointCount = pointCount
        val finalTotalDistance = totalDistance
        scope.launch {
            // The session insert is asynchronous. Wait for it so a quick Stop
            // cannot leave an active session behind in Room.
            val finalSessionId = sessionJob?.await() ?: sessionId
            if (finalSessionId != -1L) {
                database?.gpsSessionDao()?.stopSession(
                    id = finalSessionId,
                    endTime = System.currentTimeMillis(),
                    totalPoints = finalPointCount,
                    totalDistance = finalTotalDistance
                )
            }
        }

        _isRunning = false
        _currentRouteId = -1
        _trackingState.value = TrackingState()
        pointCount = 0
        totalDistance = 0f
        lastLocation = null
        sessionId = -1
        sessionInsertJob = null

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return

                // Calculate distance from last point
                if (lastLocation != null) {
                    totalDistance += lastLocation!!.distanceTo(location)
                }
                lastLocation = location
                pointCount++

                val point = GpsTrackEntity(
                    routeId = currentRouteId,
                    dateString = currentDateString,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    speed = location.speed,
                    timestamp = System.currentTimeMillis()
                )

                scope.launch {
                    database?.gpsTrackDao()?.insertPoint(point)
                    // Update session stats periodically
                    if (pointCount % 10 == 0) {
                        database?.gpsSessionDao()?.updateSessionStats(
                            id = sessionId,
                            totalPoints = pointCount,
                            totalDistance = totalDistance
                        )
                    }
                }

                val distStr = if (totalDistance >= 1000) {
                    String.format("%.1f km", totalDistance / 1000)
                } else {
                    "${totalDistance.toInt()} m"
                }
                updateNotification("Tracking aktif • $pointCount titik • $distStr")
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "GPS Tracking",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Tracking lokasi rute"
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(contentText: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, LocationTrackingService::class.java).apply {
                action = ACTION_STOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val routeName = getRouteName(currentRouteId)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🗺️ $routeName")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .addAction(0, "Stop", stopIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun updateNotification(contentText: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(contentText))
    }

    private fun getRouteName(routeId: Long): String {
        return try {
            val db = database ?: return "Rute #$routeId"
            val route = kotlinx.coroutines.runBlocking {
                db.routeDao().getRouteById(routeId)
            }
            route?.name ?: "Rute #$routeId"
        } catch (e: Exception) {
            "Rute #$routeId"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
