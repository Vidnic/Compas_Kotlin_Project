package com.example.myapplication.activity

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.myapplication.databinding.ActivityMainBinding
import com.example.myapplication.service.NavigationBoundService
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navigationBoundService: NavigationBoundService
    private var isBound: Boolean = false
    private val requestLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted && coarseGranted) {
            startAndBindService()
        }
    }
    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName?, service: IBinder?) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance.
            val binder = service as NavigationBoundService.LocalBinder
            navigationBoundService = binder.getService()
            isBound = true
            Log.i("CompassService", "NavigationBoundService is bound")
            onRotationStart(navigationBoundService)
        }

        override fun onServiceDisconnected(arg0: ComponentName?) {
            isBound = false
            Log.i("CompassService", "NavigationBoundService unbound")
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun onStart() {
        super.onStart()
        askLocationPermissions()
        startAndBindService()
    }

    override fun onStop() {
        super.onStop()
        unbindService(connection)
        isBound = false
    }

    private fun startAndBindService() {
        if (!isBound) {
            Intent(this, NavigationBoundService::class.java).also { intent ->
                bindService(intent, connection, BIND_AUTO_CREATE)
            }
        }
    }
    private fun askLocationPermissions() {
        if (!checkLocationPermissions()) {
            val showRationale =
                shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) ||
                        shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (!showRationale) {
                requestLocationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            } else {
                openAppSettings()
            }
        }
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }

    private fun checkLocationPermissions(): Boolean {
        val resultFine = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        val resultCoarse = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        return resultFine == PackageManager.PERMISSION_GRANTED &&
                resultCoarse == PackageManager.PERMISSION_GRANTED
    }

    private fun onRotationStart(service: NavigationBoundService) {
        Log.d("CompassService", "onRotationStart запущен")
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                service.targetAngleFlow.collect { angle ->
                    binding.compassArrow.animate()
                        .rotation(angle)
                        .setDuration(60)
                        .start()
                    Log.d("CompassService", "onSensorChanged сработал! Значение: $angle")
                }
            }
        }
    }
}