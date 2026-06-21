package com.speedvolume.app

import android.animation.ValueAnimator
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.media.AudioManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class SpeedVolumeService : Service() {

    // ---- Binder برای ارتباط با Activity ----
    private val binder = LocalBinder()
    private var updateListener: ((Float, Int) -> Unit)? = null

    inner class LocalBinder : Binder() {
        fun getService(): SpeedVolumeService = this@SpeedVolumeService
    }

    // ---- متغیرهای اصلی ----
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var audioManager: AudioManager
    private var currentVolumePercent = 20
    private var volumeAnimator: ValueAnimator? = null

    companion object {
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "SpeedVolumeChannel"

        // جدول سرعت → درصد صدا (km/h)
        val SPEED_VOLUME_TABLE = listOf(
            Triple(0f,   10f,  20),   // زیر ۱۰  → ۲۰٪
            Triple(10f,  30f,  30),   // ۱۰–۳۰   → ۳۰٪
            Triple(30f,  60f,  50),   // ۳۰–۶۰   → ۵۰٪
            Triple(60f,  90f,  70),   // ۶۰–۹۰   → ۷۰٪
            Triple(90f,  120f, 85),   // ۹۰–۱۲۰  → ۸۵٪
            Triple(120f, Float.MAX_VALUE, 100) // بالای ۱۲۰ → ۱۰۰٪
        )
    }

    // ---- Location Callback با FusedLocationProvider ----
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                processLocation(location)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification("در حال دریافت GPS..."))
        startLocationUpdates()
        return START_STICKY // سرویس بعد از کشته شدن دوباره راه‌اندازی شود
    }

    override fun onBind(intent: Intent?): IBinder = binder

    // ---- شروع دریافت موقعیت مکانی ----
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, // دقت بالا برای GPS
            1000L // هر ۱ ثانیه
        ).apply {
            setMinUpdateIntervalMillis(500L)      // حداقل هر ۰.۵ ثانیه
            setMinUpdateDistanceMeters(5f)         // حداقل ۵ متر جابجایی
        }.build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            stopSelf()
        }
    }

    // ---- پردازش موقعیت و تغییر صدا ----
    private fun processLocation(location: Location) {
        val speedKmh = location.speed * 3.6f  // تبدیل m/s به km/h
        val targetVolume = getVolumeForSpeed(speedKmh)

        // تغییر صدا با انیمیشن روان
        animateVolumeChange(currentVolumePercent, targetVolume)
        currentVolumePercent = targetVolume

        // آپدیت notification
        updateNotification(speedKmh, targetVolume)

        // اطلاع‌رسانی به Activity
        updateListener?.invoke(speedKmh, targetVolume)
    }

    // ---- تعیین درصد صدا بر اساس سرعت ----
    private fun getVolumeForSpeed(speedKmh: Float): Int {
        for ((min, max, volume) in SPEED_VOLUME_TABLE) {
            if (speedKmh >= min && speedKmh < max) return volume
        }
        return 100
    }

    // ---- تغییر تدریجی صدا با ValueAnimator ----
    private fun animateVolumeChange(from: Int, to: Int) {
        if (from == to) return

        volumeAnimator?.cancel()
        volumeAnimator = ValueAnimator.ofInt(from, to).apply {
            duration = 800L // مدت انتقال: ۰.۸ ثانیه
            addUpdateListener { animator ->
                val animatedValue = animator.animatedValue as Int
                applyVolume(animatedValue)
            }
            start()
        }
    }

    // ---- اعمال صدا روی سیستم ----
    private fun applyVolume(percent: Int) {
        // تغییر صدای موسیقی
        val maxMusic = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audioManager.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            (maxMusic * percent / 100.0).toInt(),
            0
        )

        // تغییر صدای زنگ
        val maxRing = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
        audioManager.setStreamVolume(
            AudioManager.STREAM_RING,
            (maxRing * percent / 100.0).toInt(),
            0
        )
    }

    // ---- Notification ----
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "کنترل صدا بر اساس سرعت",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "نمایش وضعیت سرعت و صدا"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SpeedVolume فعال است")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setOngoing(true) // قابل بستن نیست
            .build()
    }

    private fun updateNotification(speedKmh: Float, volume: Int) {
        val text = "سرعت: %.0f km/h | صدا: %d%%".format(speedKmh, volume)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(text))
    }

    // ---- ثبت listener از Activity ----
    fun setUpdateListener(listener: (Float, Int) -> Unit) {
        updateListener = listener
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        volumeAnimator?.cancel()
        updateListener = null
    }
}
