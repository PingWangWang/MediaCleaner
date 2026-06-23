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

@Composable
fun PermissionGate(
    agreementAccepted: Boolean? = null,
    onAcceptAgreement: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    // ── 权限状态 ──
    val permission = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE
    }
    var hasPermission by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, permission) == android.content.pm.PackageManager.PERMISSION_GRANTED) }
    var hasRequested by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasRequested = true
        if (granted) hasPermission = true else (context as? Activity)?.finish()
    }
    LaunchedEffect(Unit) { if (!hasPermission && !hasRequested) permissionLauncher.launch(permission) }

    if (!hasPermission) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(progress = 0f) }
        return
    }

    // ── 协议状态 ──
    if (agreementAccepted == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(progress = 0f) }
        return
    }

    if (!agreementAccepted) {
        var showPrivacy by remember { mutableStateOf(false) }
        var showTerms by remember { mutableStateOf(false) }
        if (showPrivacy) PrivacyPolicyDialog(onDismiss = { showPrivacy = false })
        if (showTerms) TermsOfServiceDialog(onDismiss = { showTerms = false })
        AgreementScreen(
            onAccept = onAcceptAgreement,
            onShowPrivacy = { showPrivacy = true },
            onShowTerms = { showTerms = true }
        )
        return
    }

    content()
}
