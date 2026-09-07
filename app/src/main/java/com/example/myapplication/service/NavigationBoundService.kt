package com.example.myapplication.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Binder
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class NavigationBoundService : Service(), SensorEventListener {

    private val binder = LocalBinder()
    private lateinit var sensorManager: SensorManager
    private var rotationSensor: Sensor? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private val angleFlow = MutableStateFlow(0f)
    val targetAngleFlow = angleFlow.asStateFlow()

    var targetLatitude: Double = 55.7558
    var targetLongitude: Double = 37.6173

    private var currentLocation: Location? = null
    private var currentAzimuth: Float = 0f

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    currentLocation = location
                    Log.d("CompassService", "Новая локация: ${location.latitude}, ${location.longitude}")
                    updateArrowAngle()
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onBind(intent: Intent?): IBinder {
        rotationSensor?.let { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
            .setMinUpdateIntervalMillis(1000)
            .build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        // Отписываемся от ресурсов при отвязке
        sensorManager.unregisterListener(this)
        fusedLocationClient.removeLocationUpdates(locationCallback)
        return super.onUnbind(intent)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return

        val rotationMatrix = FloatArray(9)
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

        val orientation = FloatArray(3)
        SensorManager.getOrientation(rotationMatrix, orientation)

        var azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
        if (azimuth < 0) azimuth += 360f

        currentAzimuth = azimuth
        updateArrowAngle()
    }

    private fun updateArrowAngle() {
        val userLoc = currentLocation

        userLoc?.let { loc ->
            val targetLoc = Location("").apply {
                latitude = targetLatitude
                longitude = targetLongitude
            }

            var bearing = loc.bearingTo(targetLoc)
            if (bearing < 0) bearing += 360f

            var arrowAngle = bearing - currentAzimuth
            if (arrowAngle < 0) arrowAngle += 360f

            angleFlow.value = arrowAngle
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): NavigationBoundService = this@NavigationBoundService
    }
}