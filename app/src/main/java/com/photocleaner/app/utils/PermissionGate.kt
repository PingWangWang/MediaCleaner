/*
 * Copyright (C) 2025 PhotoCleaner
 *
 * 权限门组件 — 简化版
 *
 * 启动时自动请求存储权限：
 * - 授予 → 自动进入首页
 * - 拒绝 → 自动退出应用
 * - 下次启动再次询问，不记录永久拒绝状态
 *
 * @author PhotoCleaner
 */
package com.photocleaner.app.utils

import android.Manifest
import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * 权限门组件。
 *
 * 启动时自动检测并请求存储权限：
 * - 已授予 → 直接渲染 [content]
 * - 未授予 → 自动弹出系统权限对话框
 * - 用户授予 → 渲染 [content]
 * - 用户拒绝 → 退出应用
 *
 * 不检测永久拒绝状态，每次启动都重新询问。
 */
@Composable
fun PermissionGate(content: @Composable () -> Unit) {
    val context = LocalContext.current

    // 确定要请求的权限
    val permission = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    // 当前权限状态
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, permission) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    // 是否已尝试过请求
    var hasRequested by remember { mutableStateOf(false) }

    // 权限请求 launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasRequested = true
        if (granted) {
            hasPermission = true
        } else {
            // 用户拒绝 → 退出应用
            (context as? Activity)?.finish()
        }
    }

    // 未授予且还未请求过 → 自动发起请求
    LaunchedEffect(Unit) {
        if (!hasPermission && !hasRequested) {
            permissionLauncher.launch(permission)
        }
    }

    if (hasPermission) {
        content()
    } else {
        // 等待权限请求结果时显示加载指示
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(progress = 0f)
        }
    }
}
