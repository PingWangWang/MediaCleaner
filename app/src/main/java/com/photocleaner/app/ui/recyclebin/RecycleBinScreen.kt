package com.photocleaner.app.ui.recyclebin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.photocleaner.core.database.entity.RecycleItemEntity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecycleBinScreen(
    viewModel: RecycleBinViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val items by viewModel.items.collectAsState()
    val itemCount by viewModel.itemCount.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showClearAllConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf<Long?>(null) }

    if (showClearAllConfirm) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirm = false },
            title = { Text("清空回收站") },
            text = { Text("确定要清空回收站吗？删除后将无法恢复。") },
            confirmButton = {
                Button(
                    onClick = {
                        showClearAllConfirm = false
                        viewModel.clearAll()
                        scope.launch {
                            snackbarHostState.showSnackbar("已清空回收站")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("清空") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearAllConfirm = false }) { Text("取消") }
            }
        )
    }

    showDeleteConfirm?.let { deleteId ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("永久删除") },
            text = { Text("确定要永久删除该文件吗？此操作不可撤销。") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.permanentlyDelete(deleteId)
                        showDeleteConfirm = null
                        scope.launch {
                            snackbarHostState.showSnackbar("已永久删除")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("删除") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirm = null }) { Text("取消") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("回收站")
                        if (itemCount > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = "$itemCount",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (itemCount > 0) {
                        IconButton(onClick = { showClearAllConfirm = true }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "清空")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (items.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "回收站为空",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "删除的图片会在这里保留30天，过期自动清理",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    RecycleItemCard(
                        item = item,
                        onRestore = {
                            viewModel.restoreItem(item.id)
                            scope.launch {
                                snackbarHostState.showSnackbar("已恢复")
                            }
                        },
                        onDelete = { showDeleteConfirm = item.id }
                    )
                }
            }
        }
    }
}

@Composable
private fun RecycleItemCard(
    item: RecycleItemEntity,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    val daysLeft = ((item.expireTime - System.currentTimeMillis()) / (24 * 60 * 60 * 1000)).coerceAtLeast(0)

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail placeholder
            Surface(
                modifier = Modifier.size(56.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = item.name.take(2),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                Text(
                    text = formatSize(item.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (daysLeft > 0) "${daysLeft}天后自动清理" else "即将过期",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (daysLeft > 0) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.error
                )
            }

            IconButton(onClick = onRestore) {
                Icon(
                    Icons.Default.Restore,
                    contentDescription = "恢复",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "永久删除",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun formatSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${"%.1f".format(bytes.toDouble() / (1024 * 1024))} MB"
    }
}
