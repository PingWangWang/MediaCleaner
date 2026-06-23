/*
 * Copyright (C) 2025 PhotoCleaner
 *
 * 扫描前台服务，保活扫描任务
 *
 * @author PhotoCleaner
 */
package com.photocleaner.feature.scanner.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.photocleaner.feature.scanner.R

/**
 * 前台扫描服务。
 *
 * 在图片扫描期间保持前台运行，防止被系统杀死，
 * 并通过持续通知向用户展示扫描进度。
 *
 * @author PhotoCleaner
 */
class ScanForegroundService : Service() {

    companion object {
        /** 通知 ID */
        const val NOTIFICATION_ID = 1001

        /** 通知渠道 ID */
        const val CHANNEL_ID = "scan_service"

        /** 通知渠道名称 */
        private const val CHANNEL_NAME = "图片扫描"

        /** 通知渠道描述 */
        private const val CHANNEL_DESCRIPTION = "显示图片扫描进度"

        /** Action: 启动扫描 */
        private const val ACTION_START_SCAN = "com.photocleaner.feature.scanner.action.START_SCAN"

        /** Action: 停止扫描 */
        private const val ACTION_STOP_SCAN = "com.photocleaner.feature.scanner.action.STOP_SCAN"

        /**
         * 启动扫描服务的便捷方法。
         *
         * @param context 上下文
         */
        fun start(context: Context) {
            val intent = Intent(context, ScanForegroundService::class.java).apply {
                action = ACTION_START_SCAN
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /**
         * 停止扫描服务的便捷方法。
         *
         * @param context 上下文
         */
        fun stop(context: Context) {
            val intent = Intent(context, ScanForegroundService::class.java).apply {
                action = ACTION_STOP_SCAN
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SCAN -> {
                startForeground(NOTIFICATION_ID, createNotification())
            }
            ACTION_STOP_SCAN -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * 创建通知渠道（API 26+ 必需）。
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = CHANNEL_DESCRIPTION
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * 创建前台服务通知。
     */
    private fun createNotification(): Notification {
        // 使用 PendingIntent 让通知可点击返回应用
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val packageName = packageName
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            pendingIntentFlags
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("PhotoCleaner")
            .setContentText("正在进行图片扫描...")
            .setSmallIcon(android.R.drawable.ic_menu_gallery)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
