/*
 * Copyright (C) 2025 PhotoCleaner
 *
 * 设置页面 Compose UI
 *
 * @author PhotoCleaner
 */
package com.photocleaner.feature.settings.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────────────────────────────────────
// Item models for the settings list
// ─────────────────────────────────────────────────────────────────────────────
private sealed interface SettingsItem {
    val id: String
}

private data class ToggleItem(
    override val id: String,
    val title: String,
    val subtitle: String = "",
    val checked: Boolean,
    val onCheckedChange: (Boolean) -> Unit
) : SettingsItem

private data class DropdownItem(
    override val id: String,
    val title: String,
    val subtitle: String = "",
    val selectedValue: String,
    val options: List<String>,
    val onOptionSelected: (String) -> Unit
) : SettingsItem

private data class RadioGroupItem(
    override val id: String,
    val title: String,
    val selectedValue: String,
    val options: List<String>,
    val onOptionSelected: (String) -> Unit
) : SettingsItem

private data class InfoItem(
    override val id: String,
    val title: String,
    val value: String
) : SettingsItem

private data class ActionItem(
    override val id: String,
    val title: String,
    val subtitle: String = "",
    val onClick: () -> Unit
) : SettingsItem

// ─────────────────────────────────────────────────────────────────────────────
// SettingsGroup — groups settings into a collapsible section
// ─────────────────────────────────────────────────────────────────────────────
private data class SettingsGroup(
    val id: String,
    val title: String,
    val items: List<SettingsItem>
)

// ─────────────────────────────────────────────────────────────────────────────
// Public composable
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel? = null,
    onNavigateBack: () -> Unit,
    onShowPrivacyPolicy: () -> Unit = {},
    onShowTermsOfService: () -> Unit = {}
) {
    val state = viewModel?.uiState?.collectAsState()?.value ?: SettingsState()

    val groups = remember(state) {
        listOf(
            // ════════════════════════════════════════════════════════════════
            // 1️⃣ 扫描设置
            // ════════════════════════════════════════════════════════════════
            SettingsGroup(
                id = "scan",
                title = "扫描设置",
                items = listOf(
                    ToggleItem(
                        id = "high_precision",
                        title = "高精度模式",
                        subtitle = "使用更精确的算法检测重复（速度较慢）",
                        checked = state.highPrecisionEnabled,
                        onCheckedChange = { viewModel?.updateHighPrecision(it) }
                    ),
                    DropdownItem(
                        id = "scan_concurrency",
                        title = "扫描并发数",
                        subtitle = "同时扫描的文件数量，越高越快但越耗电",
                        selectedValue = state.scanConcurrency.toString(),
                        options = listOf("2", "4", "6"),
                        onOptionSelected = { value ->
                            viewModel?.updateScanConcurrency(value.toInt())
                        }
                    ),
                    DropdownItem(
                        id = "min_image_size",
                        title = "最小图片大小",
                        subtitle = "仅扫描大于此值的图片（KB）",
                        selectedValue = "${state.minImageSizeKb} KB",
                        options = listOf("50 KB", "100 KB", "200 KB", "500 KB"),
                        onOptionSelected = { value ->
                            viewModel?.updateMinImageSizeKb(value.removeSuffix(" KB").toInt())
                        }
                    ),
                    ToggleItem(
                        id = "ignore_screenshots",
                        title = "忽略截屏图片",
                        subtitle = "扫描时跳过截图文件夹中的图片",
                        checked = state.ignoreScreenshots,
                        onCheckedChange = { viewModel?.updateIgnoreScreenshots(it) }
                    ),
                    ToggleItem(
                        id = "ignore_gif",
                        title = "忽略 GIF 图片",
                        subtitle = "扫描时跳过 GIF 格式文件",
                        checked = state.ignoreGif,
                        onCheckedChange = { viewModel?.updateIgnoreGif(it) }
                    )
                )
            ),

            // ════════════════════════════════════════════════════════════════
            // 2️⃣ 去重设置
            // ════════════════════════════════════════════════════════════════
            SettingsGroup(
                id = "dedup",
                title = "去重设置",
                items = listOf(
                    DropdownItem(
                        id = "similarity_threshold",
                        title = "相似度阈值",
                        subtitle = "dHash 汉明距离阈值，越小越严格",
                        selectedValue = state.similarityThreshold.toString(),
                        options = listOf("5", "10", "15", "20", "25"),
                        onOptionSelected = { value ->
                            viewModel?.updateSimilarityThreshold(value.toInt())
                        }
                    ),
                    ToggleItem(
                        id = "auto_retain_best",
                        title = "自动保留最佳图片",
                        subtitle = "自动选择分辨率最高的图片保留",
                        checked = state.autoRetainBest,
                        onCheckedChange = { viewModel?.updateAutoRetainBest(it) }
                    )
                )
            ),

            // ════════════════════════════════════════════════════════════════
            // 3️⃣ 删除设置
            // ════════════════════════════════════════════════════════════════
            SettingsGroup(
                id = "delete",
                title = "删除设置",
                items = listOf(
                    ToggleItem(
                        id = "confirm_before_delete",
                        title = "删除前确认",
                        subtitle = "删除重复图片前弹出确认对话框",
                        checked = state.confirmBeforeDelete,
                        onCheckedChange = { viewModel?.updateConfirmBeforeDelete(it) }
                    ),
                    ToggleItem(
                        id = "recycle_bin",
                        title = "启用回收站",
                        subtitle = "删除的图片先移入回收站而非永久删除",
                        checked = state.recycleBinEnabled,
                        onCheckedChange = { viewModel?.updateRecycleBinEnabled(it) }
                    ),
                    ToggleItem(
                        id = "auto_clear_recycle",
                        title = "自动清空回收站",
                        subtitle = "清理完成后自动清空回收站",
                        checked = state.autoClearRecycle,
                        onCheckedChange = { viewModel?.updateAutoClearRecycle(it) }
                    ),
                    DropdownItem(
                        id = "auto_clear_cycle",
                        title = "自动清理周期",
                        subtitle = "回收站文件保留天数后自动清理",
                        selectedValue = "${state.autoClearRecycleDays} 天",
                        options = listOf("7 天", "14 天", "30 天", "60 天"),
                        onOptionSelected = { value ->
                            viewModel?.updateAutoClearRecycleDays(value.removeSuffix(" 天").toInt())
                        }
                    )
                )
            ),

            // ════════════════════════════════════════════════════════════════
            // 4️⃣ 升级设置
            // ════════════════════════════════════════════════════════════════
            SettingsGroup(
                id = "update",
                title = "升级设置",
                items = listOf(
                    ToggleItem(
                        id = "auto_check_update",
                        title = "自动检查更新",
                        subtitle = "启动时自动检查新版本",
                        checked = state.autoCheckUpdate,
                        onCheckedChange = { viewModel?.updateAutoCheckUpdate(it) }
                    ),
                    ToggleItem(
                        id = "wifi_only_download",
                        title = "仅 WiFi 下载",
                        subtitle = "仅在 WiFi 环境下下载更新包",
                        checked = state.scanOnWifiOnly,
                        onCheckedChange = { viewModel?.updateScanOnWifiOnly(it) }
                    )
                )
            ),

            // ════════════════════════════════════════════════════════════════
            // 5️⃣ 通用设置
            // ════════════════════════════════════════════════════════════════
            SettingsGroup(
                id = "general",
                title = "通用设置",
                items = listOf(
                    RadioGroupItem(
                        id = "theme_mode",
                        title = "主题模式",
                        selectedValue = state.themeMode.name,
                        options = ThemeMode.entries.map { it.name },
                        onOptionSelected = { value ->
                            viewModel?.updateThemeMode(ThemeMode.valueOf(value))
                        }
                    ),
                    ToggleItem(
                        id = "notification",
                        title = "启用通知",
                        subtitle = "扫描完成和清理提醒",
                        checked = state.notificationEnabled,
                        onCheckedChange = { viewModel?.updateNotification(it) }
                    ),
                    ToggleItem(
                        id = "shake_to_feedback",
                        title = "摇动反馈",
                        subtitle = "摇动手机时发送反馈",
                        checked = state.shakeToFeedback,
                        onCheckedChange = { viewModel?.updateShakeToFeedback(it) }
                    )
                )
            ),

            // ════════════════════════════════════════════════════════════════
            // 6️⃣ 关于
            // ════════════════════════════════════════════════════════════════
            SettingsGroup(
                id = "about",
                title = "关于",
                items = listOf(
                    InfoItem(
                        id = "app_version",
                        title = "应用版本",
                        value = "v${state.appVersion} (${state.appVersionCode})"
                    ),
                    InfoItem(
                        id = "device_tier",
                        title = "设备性能等级",
                        value = when (state.deviceTier) {
                            "HIGH" -> "高端"
                            "MEDIUM" -> "中端"
                            else -> "入门"
                        }
                    ),
                    ActionItem(
                        id = "privacy_policy",
                        title = "隐私政策",
                        subtitle = "查看隐私政策",
                        onClick = { onShowPrivacyPolicy() }
                    ),
                    ActionItem(
                        id = "terms_of_service",
                        title = "服务条款",
                        subtitle = "查看服务条款",
                        onClick = { onShowTermsOfService() }
                    )
                )
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(groups, key = { it.id }) { group ->
                GroupView(group = group)
            }

            // Bottom spacer so last item isn't flush with the edge
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Group composable — clickable header row + expandable items
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupView(group: SettingsGroup) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        // ── Clickable header row ────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            onClick = { expanded = !expanded }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = group.title,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowDown
                                  else Icons.Default.KeyboardArrowRight,
                    contentDescription = if (expanded) "折叠" else "展开",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ── Animated item list ──────────────────────────
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(expandFrom = Alignment.Top),
            exit = shrinkVertically(shrinkTowards = Alignment.Top)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                group.items.forEach { item ->
                    when (item) {
                        is ToggleItem -> ToggleItemView(item)
                        is DropdownItem -> DropdownItemView(item)
                        is RadioGroupItem -> RadioGroupItemView(item)
                        is InfoItem -> InfoItemView(item)
                        is ActionItem -> ActionItemView(item)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Private composable views for each item type
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ToggleItemView(item: ToggleItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (item.subtitle.isNotBlank()) {
                    Text(
                        text = item.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
                checked = item.checked,
                onCheckedChange = item.onCheckedChange
            )
        }
    }
}

@Composable
private fun DropdownItemView(item: DropdownItem) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge
            )
            if (item.subtitle.isNotBlank()) {
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.selectedValue,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            item.options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        item.onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun RadioGroupItemView(item: RadioGroupItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            item.options.forEach { option ->
                val displayName = when (option) {
                    "SYSTEM" -> "跟随系统"
                    "LIGHT" -> "浅色模式"
                    "DARK" -> "深色模式"
                    else -> option
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { item.onOptionSelected(option) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = item.selectedValue == option,
                        onClick = { item.onOptionSelected(option) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoItemView(item: InfoItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = item.value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ActionItemView(item: ActionItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { item.onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                if (item.subtitle.isNotBlank()) {
                    Text(
                        text = item.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
