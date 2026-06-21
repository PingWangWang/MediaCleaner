package com.photocleaner.app.ui.result

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.photocleaner.core.common.model.DuplicateGroup
import com.photocleaner.core.common.model.GroupType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    groups: List<DuplicateGroup>,
    onItemClick: (Long) -> Unit,
    onBack: () -> Unit
) {
    var selectedGroups by remember { mutableStateOf(setOf<Long>()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("去重结果") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.DoneAll, contentDescription = "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            if (groups.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { /* Perform cleanup */ },
                    icon = { Icon(Icons.Default.CleaningServices, contentDescription = null) },
                    text = { Text("一键清理") }
                )
            }
        }
    ) { padding ->
        if (groups.isEmpty()) {
            // Empty state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DoneAll,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "未发现重复图片",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "你的相册很干净，暂时不需要清理",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            // Summary header
            Column(modifier = Modifier.padding(padding)) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "发现 ${groups.size} 组重复图片",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val totalSize = groups.sumOf { it.size }
                        Text(
                            text = "可释放空间：${formatSize(totalSize)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(groups) { group ->
                        DuplicateGroupCard(
                            group = group,
                            isSelected = group.groupId in selectedGroups,
                            onClick = { onItemClick(group.groupId) },
                            onToggleSelect = {
                                selectedGroups = if (group.groupId in selectedGroups) {
                                    selectedGroups - group.groupId
                                } else {
                                    selectedGroups + group.groupId
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DuplicateGroupCard(
    group: DuplicateGroup,
    isSelected: Boolean,
    onClick: () -> Unit,
    onToggleSelect: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelect() }
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = group.groupType.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = "${(group.similarity * 100).toInt()}%",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${group.images.size} 张图片 · ${formatSize(group.size)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Image thumbnails row
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    group.images.take(5).forEach { image ->
                        Surface(
                            modifier = Modifier.size(48.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.small
                        ) {
                            // In real app, use Coil AsyncImage
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = image.name.take(2),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    if (group.images.size > 5) {
                        Box(
                            modifier = Modifier.size(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+${group.images.size - 5}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
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

private val GroupType.displayName: String
    get() = when (this) {
        GroupType.EXACT_DUPLICATE -> "完全重复"
        GroupType.HIGH_SIMILARITY -> "高度相似"
        GroupType.MEDIUM_SIMILARITY -> "中等相似"
    }
