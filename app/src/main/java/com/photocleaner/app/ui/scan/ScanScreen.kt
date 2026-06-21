package com.photocleaner.app.ui.scan

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.photocleaner.core.common.model.DuplicateGroup
import kotlinx.coroutines.delay

@Composable
fun ScanScreen(
    onScanComplete: (List<DuplicateGroup>) -> Unit,
    onCancel: () -> Unit
) {
    var scanProgress by remember { mutableStateOf(0f) }
    var scanPhase by remember { mutableStateOf("正在扫描图片...") }
    var scannedCount by remember { mutableIntStateOf(0) }

    // Pulsing animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // Simulate scan progress
    LaunchedEffect(Unit) {
        // In real app, this would call ScanImageUseCase
        val totalSteps = 100
        for (i in 1..totalSteps) {
            scanProgress = i / 100f
            scannedCount = (i * 50)
            when {
                i < 30 -> scanPhase = "正在扫描图片..."
                i < 60 -> scanPhase = "正在计算哈希值..."
                i < 85 -> scanPhase = "正在分组对比..."
                else -> scanPhase = "正在整理结果..."
            }
            delay(50) // Simulate work
        }
        onScanComplete(emptyList())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("正在扫描") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = "取消")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Circular progress
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(200.dp)
                    .alpha(pulseAlpha)
            ) {
                CircularProgressIndicator(
                    progress = { scanProgress },
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 8.dp,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Text(
                    text = "${(scanProgress * 100).toInt()}%",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = scanPhase,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "已扫描 $scannedCount 张图片",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "请保持应用在前台运行",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(onClick = onCancel) {
                Text("取消扫描")
            }
        }
    }
}
