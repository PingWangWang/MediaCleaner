package com.photocleaner.app.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.photocleaner.app.utils.ScanRecord
import com.photocleaner.core.common.model.DuplicateGroup
import com.photocleaner.feature.fileops.model.DeleteResult
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val selectedIds by viewModel.selectedGroupIds.collectAsState()

    var showDeleteConfirm by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val pulseAlpha = rememberInfiniteTransition(label = "pulse").animateFloat(
        0.6f, 1.0f, infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "pulse"
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (state is HomeUiState.Complete && selectedIds.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { showDeleteConfirm = true },
                    icon = { Icon(Icons.Default.DeleteSweep, contentDescription = null) },
                    text = { Text("清理 ${selectedIds.size}") }
                )
            }
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (val s = state) {
                is HomeUiState.Idle -> IdleContent(
                    records = s.records,
                    onStartScan = { viewModel.startScan() }
                )
                is HomeUiState.Starting -> CircularProgressIndicator(progress = 0f)
                is HomeUiState.Scanning -> ScanningContent(s, pulseAlpha.value)
                is HomeUiState.ScanCompleted -> ScanCompletedContent(
                    totalCount = s.totalCount,
                    onStartDetection = { viewModel.startDetection() },
                    onGoHome = { viewModel.goHome() }
                )
                is HomeUiState.Detecting -> DetectingContent(
                    s,
                    onPause = { viewModel.pauseDetection() },
                    onResume = { viewModel.resumeDetection() },
                    onCancel = { viewModel.cancelDetection() }
                )
                is HomeUiState.Complete -> CompleteContent(
                    groups = s.groups,
                    selectedIds = selectedIds,
                    viewModel = viewModel,
                    onGoHome = { viewModel.goHome() }
                )
                is HomeUiState.Error -> ErrorContent(s.message) { viewModel.startScan() }
            }
        }
    }

    if (showDeleteConfirm && state is HomeUiState.Complete) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除选中的 ${selectedIds.size} 组重复图片吗？") },
            confirmButton = {
                Button(onClick = {
                    showDeleteConfirm = false
                    scope.launch {
                        viewModel.deleteSelected().collect { result ->
                            if (result is DeleteResult.SUCCESS) {
                                snackbarHostState.showSnackbar("删除成功")
                            }
                        }
                        viewModel.clearSelection()
                    }
                }) { Text("删除") }
            },
            dismissButton = { OutlinedButton(onClick = { showDeleteConfirm = false }) { Text("取消") } }
        )
    }
}

@Composable
private fun IdleContent(records: List<ScanRecord>, onStartScan: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        Text("一键去重，释放存储空间", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onStartScan, modifier = Modifier.fillMaxWidth().height(48.dp)) {
            Icon(Icons.Default.DeleteSweep, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("开始扫描", fontSize = 16.sp)
        }
        Spacer(Modifier.height(24.dp))

        if (records.isEmpty()) {
            Text("暂无扫描记录", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text("扫描记录", fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
            Spacer(Modifier.height(8.dp))
            records.forEachIndexed { index, record ->
                ScanRecordCard(record = record, isFirst = index == 0)
                if (index < records.lastIndex) Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScanRecordCard(record: ScanRecord, isFirst: Boolean) {
    var expanded by remember { mutableStateOf(isFirst) }
    val date = java.text.SimpleDateFormat("MM/dd HH:mm", java.util.Locale.getDefault())
        .format(java.util.Date(record.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        onClick = { expanded = !expanded }
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("扫描了 ${record.totalImages} 张图片", fontSize = 14.sp)
                    Text(date, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(Modifier.padding(top = 8.dp)) {
                    Divider(modifier = Modifier.padding(bottom = 8.dp))
                    if (!record.hasDetected) {
                        Text("未检测", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else if (record.duplicateGroups > 0) {
                        Text("发现 ${record.duplicateGroups} 组重复图片", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    } else {
                        Text("未发现重复图片", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanningContent(s: HomeUiState.Scanning, pulseAlpha: Float) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(180.dp).alpha(pulseAlpha)) {
        CircularProgressIndicator(progress = s.progress, modifier = Modifier.fillMaxSize())
        Text("${(s.progress * 100).toInt()}%", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
    Spacer(Modifier.height(16.dp))
    Text("正在扫描...", fontSize = 18.sp, fontWeight = FontWeight.Medium)
    Text("已扫描 ${s.scannedCount}/${s.totalCount} 张图片", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ScanCompletedContent(totalCount: Int, onStartDetection: () -> Unit, onGoHome: () -> Unit) {
    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(16.dp))
    Text("扫描完成", fontSize = 20.sp, fontWeight = FontWeight.Bold)
    Text("共扫描 $totalCount 张图片", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(24.dp))
    Button(onClick = onStartDetection, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("开始检测重复图片", fontSize = 16.sp) }
    Spacer(Modifier.height(8.dp))
    OutlinedButton(onClick = onGoHome, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("返回首页", fontSize = 16.sp) }
}

@Composable
private fun DetectingContent(
    s: HomeUiState.Detecting,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit
) {
    if (!s.paused) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(180.dp)) {
            CircularProgressIndicator(progress = 0f, modifier = Modifier.fillMaxSize())
        }
    } else {
        Icon(
            Icons.Default.PauseCircle,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Spacer(Modifier.height(16.dp))
    Text(
        text = if (s.paused) "检测已暂停" else "正在检测重复图片...",
        fontSize = 18.sp,
        fontWeight = FontWeight.Medium
    )
    Text("已发现 ${s.foundGroups} 组重复", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(24.dp))
    Button(
        onClick = if (s.paused) onResume else onPause,
        modifier = Modifier.fillMaxWidth().height(48.dp)
    ) {
        Text(if (s.paused) "继续检测" else "暂停检测", fontSize = 16.sp)
    }
    Spacer(Modifier.height(8.dp))
    OutlinedButton(
        onClick = onCancel,
        modifier = Modifier.fillMaxWidth().height(48.dp)
    ) {
        Text("结束检测", fontSize = 16.sp)
    }
}

@Composable
private fun CompleteContent(groups: List<DuplicateGroup>, selectedIds: Set<Long>, viewModel: HomeViewModel,
                            onGoHome: () -> Unit) {
    if (groups.isEmpty()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text("未发现重复图片", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("您的相册很干净", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("发现 ${groups.size} 组重复图片", fontWeight = FontWeight.Bold)
            TextButton(onClick = {
                if (selectedIds.size < groups.size) viewModel.selectAll() else viewModel.clearSelection()
            }) { Text(if (selectedIds.size < groups.size) "全选" else "取消全选") }
        }
        OutlinedButton(
            onClick = onGoHome,
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) { Text("返回首页", fontSize = 16.sp) }
        Spacer(Modifier.height(8.dp))
        LazyColumn(Modifier.weight(1f)) {
            items(groups) { group ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (group.groupId in selectedIds)
                            MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = group.groupId in selectedIds,
                            onCheckedChange = { viewModel.toggleGroupSelection(group.groupId) }
                        )
                        Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                            Text("相似度: ${(group.similarity * 100).toInt()}%", fontWeight = FontWeight.Medium)
                            Text("${group.images.size} 张图片 · ${formatSize(group.size)}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Icon(Icons.Default.Error, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
    Spacer(Modifier.height(16.dp))
    Text("扫描失败", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
    Text(message, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    Spacer(Modifier.height(24.dp))
    Button(onClick = onRetry) { Text("重试") }
}

private fun formatSize(bytes: Long): String = when {
    bytes >= 1_073_741_824L -> "${"%.1f".format(bytes.toDouble() / 1_073_741_824)} GB"
    bytes >= 1_048_576L -> "${"%.1f".format(bytes.toDouble() / 1_048_576)} MB"
    bytes >= 1024L -> "${bytes / 1024} KB"
    else -> "$bytes B"
}
