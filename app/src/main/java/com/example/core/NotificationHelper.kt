package com.example.core

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity

class NotificationHelper(private val context: Context) {
    private val sharedPrefs = context.getSharedPreferences("emulator_prefs", Context.MODE_PRIVATE)

    companion object {
        const val CHANNEL_SCANS = "channel_scans"
        const val CHANNEL_SAVES = "channel_saves"
        const val CHANNEL_PERF = "channel_perf"
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val scanChannel = NotificationChannel(
                CHANNEL_SCANS,
                "Scans & Bibliothèque",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications lors du scan et de la détection de nouveaux jeux"
            }

            val saveChannel = NotificationChannel(
                CHANNEL_SAVES,
                "Sauvegardes Émulation",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications de confirmation de sauvegardes d'états (Save States)"
            }

            val perfChannel = NotificationChannel(
                CHANNEL_PERF,
                "Performances & Profils",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications sur les profils de performance ou le mode Turbo"
            }

            notificationManager.createNotificationChannel(scanChannel)
            notificationManager.createNotificationChannel(saveChannel)
            notificationManager.createNotificationChannel(perfChannel)
        }
    }

    fun isCategoryEnabled(channelId: String): Boolean {
        return when (channelId) {
            CHANNEL_SCANS -> sharedPrefs.getBoolean("notify_scans_enabled", true)
            CHANNEL_SAVES -> sharedPrefs.getBoolean("notify_saves_enabled", true)
            CHANNEL_PERF -> sharedPrefs.getBoolean("notify_perf_enabled", true)
            else -> true
        }
    }

    fun setCategoryEnabled(channelId: String, enabled: Boolean) {
        val prefKey = when (channelId) {
            CHANNEL_SCANS -> "notify_scans_enabled"
            CHANNEL_SAVES -> "notify_saves_enabled"
            CHANNEL_PERF -> "notify_perf_enabled"
            else -> return
        }
        sharedPrefs.edit().putBoolean(prefKey, enabled).apply()
    }

    @SuppressLint("MissingPermission")
    fun sendNotification(channelId: String, notificationId: Int, title: String, message: String) {
        if (!isCategoryEnabled(channelId)) return

        // Check overall app-level notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            val granted = context.checkSelfPermission(permission) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_save) // Use a system fallback or custom icon
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
