package com.example.myapplication.service

import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class NavigationBoundService : Service(), SensorEventListener {
    private val binder = LocalBinder()
    private lateinit var sensorManager : SensorManager
    private var rotationSencor : Sensor? = null
    private lateinit var fusedLocationClient : FusedLocationProviderClient
    var targetAngle : Float = 0f
    private var angleFlow = MutableStateFlow(0f)
    val targetAngleFlow = angleFlow.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        rotationSencor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (rotationSencor == null) {
            Log.e("CompassService", "ВНИМАНИЕ: Датчик TYPE_ROTATION_VECTOR отсутствует на этом устройстве!")
        } else {
            Log.i("CompassService", "Датчик TYPE_ROTATION_VECTOR успешно найден")
        }
    }

    override fun onBind(p0: Intent?): IBinder {
        rotationSencor?.let { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        }
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        sensorManager.unregisterListener(this)
        return super.onUnbind(intent)
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {

    }

    override fun onSensorChanged(p0: SensorEvent?) {
        var rotationMatrix = FloatArray(9)
        SensorManager.getRotationMatrixFromVector(rotationMatrix, p0?.values)

        var orientation = FloatArray(3)
        SensorManager.getOrientation(rotationMatrix, orientation)

        var angle = Math.toDegrees(orientation.get(0).toDouble()).toFloat()
        if(angle < 0) {
            angle += 360
        }
        angleFlow.value = angle

    }

    inner class LocalBinder : Binder(){
        fun getService() : NavigationBoundService =
            this@NavigationBoundService

        }
}