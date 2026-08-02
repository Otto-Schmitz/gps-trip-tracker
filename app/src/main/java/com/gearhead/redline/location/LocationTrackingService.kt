package com.gearhead.redline.location

import android.Manifest
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
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.gearhead.redline.MainActivity
import com.gearhead.redline.R
import com.gearhead.redline.data.local.entity.LocationPointEntity
import com.gearhead.redline.data.repository.TripRepository
import com.gearhead.redline.di.ServiceLocator
import com.gearhead.redline.util.Formatters
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Foreground service that owns the recording lifecycle: it creates the trip row,
 * streams high-accuracy fixes from [FusedLocationProviderClient], filters noise,
 * accumulates metrics, persists each accepted point, and finalizes the trip on
 * stop. Runs as a `location`-typed foreground service so the OS keeps delivering
 * fixes with the screen off (Android 14+ requires the manifest type + permission).
 *
 * The GPS callback thread is the single writer of [filter]/[accumulator]; only DB
 * writes are dispatched to a coroutine.
 */
class LocationTrackingService : Service() {

    private val serviceJob = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Default + serviceJob)

    private lateinit var repository: TripRepository
    private lateinit var fusedClient: FusedLocationProviderClient

    private val filter = GpsFilter()
    private var accumulator: TripMetricsAccumulator? = null
    private var tripId: Long = -1L
    private var startedAt: Long = 0L
    private var tickerJob: Job? = null

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let(::handleLocation)
        }
    }

    override fun onCreate() {
        super.onCreate()
        repository = ServiceLocator.from(this).tripRepository
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRecording()
            ACTION_STOP -> stopRecording()
        }
        // If the OS kills us under memory pressure while recording, restart so a
        // ride isn't silently dropped; onStartCommand re-enters with a null intent
        // and we simply keep the existing session alive via the sticky notification.
        return START_STICKY
    }

    private fun startRecording() {
        if (RecordingState.isRecording) return
        if (!hasLocationPermission()) {
            stopSelf()
            return
        }

        startedAt = System.currentTimeMillis()
        accumulator = TripMetricsAccumulator(startedAt)

        // Must reach startForeground within a few seconds of service start.
        val initial = LiveTripState(isRecording = true, startedAt = startedAt)
        startAsForeground(initial)
        RecordingState.update(initial)

        scope.launch {
            tripId = repository.startTrip(startedAt)
            RecordingState.update(accumulator?.toLiveState(tripId, System.currentTimeMillis()) ?: initial)
            withContext(Dispatchers.Main) { requestLocationUpdates() }
            startTicker()
        }
    }

    private fun requestLocationUpdates() {
        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            TrackingConfig.UPDATE_INTERVAL_MS,
        )
            .setMinUpdateIntervalMillis(TrackingConfig.FASTEST_INTERVAL_MS)
            .setMinUpdateDistanceMeters(0f)
            .setWaitForAccurateLocation(false)
            .build()

        if (!hasLocationPermission()) {
            stopRecording()
            return
        }
        fusedClient.requestLocationUpdates(request, locationCallback, mainLooper())
    }

    private fun mainLooper() = android.os.Looper.getMainLooper()

    /** Runs on the GPS callback thread (main looper). Single writer of filter/accumulator. */
    private fun handleLocation(location: Location) {
        val acc = accumulator ?: return
        val sample = GpsSample.from(location)
        when (val result = filter.process(sample)) {
            is GpsFilter.Result.Accept -> {
                acc.onAccepted(result)
                val point = LocationPointEntity(
                    tripId = tripId,
                    latitude = sample.latitude,
                    longitude = sample.longitude,
                    timestamp = sample.timestamp,
                    speedMps = result.speedMps,
                    altitude = if (sample.hasAltitude) sample.altitude else null,
                )
                scope.launch { repository.appendPoint(point) }
                RecordingState.update(acc.toLiveState(tripId, System.currentTimeMillis()))
            }
            is GpsFilter.Result.Reject -> {
                // Dropped as noise; nothing to persist.
            }
        }
    }

    /** Ticks the elapsed clock and refreshes the notification once per second. */
    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive) {
                val acc = accumulator ?: break
                val live = acc.toLiveState(tripId, System.currentTimeMillis())
                RecordingState.update(live)
                withContext(Dispatchers.Main) { updateNotification(live) }
                delay(1_000)
            }
        }
    }

    private fun stopRecording() {
        tickerJob?.cancel()
        if (::fusedClient.isInitialized) {
            fusedClient.removeLocationUpdates(locationCallback)
        }
        val acc = accumulator
        val id = tripId
        val ended = System.currentTimeMillis()

        scope.launch {
            withContext(NonCancellable) {
                if (acc != null && id > 0) {
                    if (acc.pointCount == 0) {
                        repository.deleteTripIfEmpty(id)
                    } else {
                        repository.getTrip(id)?.let { trip ->
                            repository.finishTrip(
                                trip.copy(
                                    endedAt = ended,
                                    distanceMeters = acc.distanceMeters,
                                    avgSpeedMps = acc.avgSpeedMps,
                                    maxSpeedMps = acc.maxSpeedMps.toDouble(),
                                    durationMillis = ended - startedAt,
                                    movingMillis = acc.movingMillis,
                                    pointCount = acc.pointCount,
                                )
                            )
                        }
                    }
                }
            }
            accumulator = null
            tripId = -1L
            RecordingState.reset()
            withContext(Dispatchers.Main) {
                ServiceCompat.stopForeground(this@LocationTrackingService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    override fun onDestroy() {
        if (::fusedClient.isInitialized) {
            fusedClient.removeLocationUpdates(locationCallback)
        }
        serviceJob.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // --- Notification -------------------------------------------------------

    private fun startAsForeground(state: LiveTripState) {
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(state),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            } else {
                0
            },
        )
    }

    private fun updateNotification(state: LiveTripState) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(state))
    }

    private fun buildNotification(state: LiveTripState): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, LocationTrackingService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val speed = Formatters.speedKmh(state.currentSpeedMps)
        val elapsed = Formatters.duration(state.elapsedMillis)
        val distance = Formatters.distance(state.distanceMeters)
        val content = if (state.hasFix) {
            "$speed km/h  •  $elapsed  •  $distance"
        } else {
            "Acquiring GPS…  •  $elapsed"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.notif_recording_title))
            .setContentText(content)
            .setContentIntent(openIntent)
            .addAction(0, "Stop", stopIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notif_channel_recording_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.notif_channel_recording_desc)
            setShowBadge(false)
        }
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    companion object {
        private const val CHANNEL_ID = "trip_recording"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.gearhead.redline.action.START"
        const val ACTION_STOP = "com.gearhead.redline.action.STOP"

        fun start(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = ACTION_START
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = ACTION_STOP
            }
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
