/*
 * Copyright (C) 2025 PhotoCleaner
 *
 * 权限与隐私协议门组件
 *
 * 启动时依次处理：
 * 1. 请求存储权限 → 拒绝则退出
 * 2. 检查隐私协议是否已同意 → 未同意则展示同意页面
 * 3. 全部通过后渲染 [content]
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import com.photocleaner.app.utils.AgreementScreen
import com.photocleaner.app.utils.PrivacyPolicyDialog
import com.photocleaner.app.utils.TermsOfServiceDialog
import kotlinx.coroutines.launch

/** 使用与 SettingsPreferences 相同的 DataStore 文件 */
private val android.content.Context.agreementDataStore by preferencesDataStore(name = "photo_cleaner_settings")

/** 隐私协议已同意的 Key */
private val PRIVACY_ACCEPTED_KEY = booleanPreferencesKey("privacy_policy_accepted")

/**
 * 权限与隐私协议门组件。
 *
 * 启动时自动执行：
 * 1. 检查并请求存储权限
 * 2. 检查隐私协议是否已同意
 * 3. 全部通过后渲染 [content]
 */
@Composable
fun PermissionGate(content: @Composable () -> Unit) {
    val context = LocalContext.current

    // ── 权限状态 ──────────────────────────────────────────────
    val permission = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, permission) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    var hasRequested by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasRequested = true
        if (granted) {
            hasPermission = true
        } else {
            (context as? Activity)?.finish()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasPermission && !hasRequested) {
            permissionLauncher.launch(permission)
        }
    }

    // ── 隐私协议状态 ───────────────────────────────────────────
    val agreementAccepted by context.agreementDataStore.data
        .map { it[PRIVACY_ACCEPTED_KEY] ?: false }
        .collectAsState(initial = null)

    // ── 等待权限结果 ───────────────────────────────────────────
    if (!hasPermission) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(progress = 0f)
        }
        return
    }

    // ── 等待协议加载 ───────────────────────────────────────────
    if (agreementAccepted == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(progress = 0f)
        }
        return
    }

    // ── 协议未同意 ────────────────────────────────────────────
    if (!agreementAccepted!!) {
        var showPrivacy by remember { mutableStateOf(false) }
        var showTerms by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        if (showPrivacy) {
            PrivacyPolicyDialog(onDismiss = { showPrivacy = false })
        }
        if (showTerms) {
            TermsOfServiceDialog(onDismiss = { showTerms = false })
        }

        AgreementScreen(
            onAccept = {
                scope.launch {
                    context.agreementDataStore.edit { prefs ->
                        prefs[PRIVACY_ACCEPTED_KEY] = true
                    }
                }
            },
            onShowPrivacy = { showPrivacy = true },
            onShowTerms = { showTerms = true }
        )
        return
    }

    // ── 全部通过 → 渲染内容 ───────────────────────────────────
    content()
}
