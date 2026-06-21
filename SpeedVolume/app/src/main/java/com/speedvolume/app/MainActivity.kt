package com.speedvolume.app

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var tvSpeed: TextView
    private lateinit var tvVolume: TextView
    private lateinit var tvStatus: TextView
    private lateinit var btnToggle: Button
    private lateinit var tvSpeedLabel: TextView

    private var speedVolumeService: SpeedVolumeService? = null
    private var isServiceBound = false
    private var isTracking = false

    companion object {
        const val LOCATION_PERMISSION_REQUEST = 100
    }

    // اتصال به سرویس پس‌زمینه
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as SpeedVolumeService.LocalBinder
            speedVolumeService = localBinder.getService()
            isServiceBound = true

            // دریافت آپدیت از سرویس
            speedVolumeService?.setUpdateListener { speedKmh, volumePercent ->
                runOnUiThread {
                    updateUI(speedKmh, volumePercent)
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            speedVolumeService = null
            isServiceBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupClickListeners()
    }

    private fun initViews() {
        tvSpeed = findViewById(R.id.tvSpeed)
        tvVolume = findViewById(R.id.tvVolume)
        tvStatus = findViewById(R.id.tvStatus)
        btnToggle = findViewById(R.id.btnToggle)
        tvSpeedLabel = findViewById(R.id.tvSpeedLabel)
    }

    private fun setupClickListeners() {
        btnToggle.setOnClickListener {
            if (isTracking) {
                stopTracking()
            } else {
                checkPermissionsAndStart()
            }
        }
    }

    private fun checkPermissionsAndStart() {
        when {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                startTracking()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) -> {
                Toast.makeText(
                    this,
                    "برای استفاده از این برنامه، دسترسی به موقعیت مکانی لازم است",
                    Toast.LENGTH_LONG
                ).show()
                requestLocationPermission()
            }
            else -> requestLocationPermission()
        }
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            LOCATION_PERMISSION_REQUEST
        )
    }

    private fun startTracking() {
        isTracking = true
        btnToggle.text = getString(R.string.btn_stop)
        tvStatus.text = getString(R.string.status_active)
        tvStatus.setTextColor(ContextCompat.getColor(this, R.color.green))

        // شروع سرویس foreground
        val intent = Intent(this, SpeedVolumeService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        // اتصال به سرویس
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun stopTracking() {
        isTracking = false
        btnToggle.text = getString(R.string.btn_start)
        tvStatus.text = getString(R.string.status_inactive)
        tvStatus.setTextColor(ContextCompat.getColor(this, R.color.red))
        tvSpeed.text = "---"
        tvVolume.text = "---%"

        // قطع اتصال و توقف سرویس
        if (isServiceBound) {
            unbindService(serviceConnection)
            isServiceBound = false
        }
        stopService(Intent(this, SpeedVolumeService::class.java))
    }

    private fun updateUI(speedKmh: Float, volumePercent: Int) {
        tvSpeed.text = "%.1f".format(speedKmh)
        tvVolume.text = "$volumePercent%"
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startTracking()
            } else {
                Toast.makeText(
                    this,
                    "بدون دسترسی به موقعیت مکانی برنامه کار نمی‌کند",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isServiceBound) {
            unbindService(serviceConnection)
            isServiceBound = false
        }
    }
}
