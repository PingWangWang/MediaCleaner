package com.photocleaner.feature.settings.ui

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
// Section model for the settings list
// ─────────────────────────────────────────────────────────────────────────────
private sealed interface SettingsItem {
    val id: String
}

private data class SectionHeader(
    override val id: String,
    val title: String
) : SettingsItem

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
    val onClick: () -> Unit
) : SettingsItem

// ─────────────────────────────────────────────────────────────────────────────
// Public composable
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel? = null,
    onNavigateBack: () -> Unit
) {
    val state = viewModel?.uiState?.collectAsState()?.value ?: SettingsState()

    val items = remember(state) {
        buildList {
            // ── 扫描设置 (Scan Settings) ────────────────────────────────────
            add(SectionHeader(id = "scan_header", title = "扫描设置"))

            add(
                DropdownItem(
                    id = "scan_concurrency",
                    title = "扫描并发数",
                    subtitle = "同时扫描的文件数量",
                    selectedValue = state.scanConcurrency.toString(),
                    options = listOf("2", "4", "6"),
                    onOptionSelected = { value ->
                        viewModel?.updateScanConcurrency(value.toInt())
                    }
                )
            )

            add(
                ToggleItem(
                    id = "high_precision",
                    title = "高精度扫描",
                    subtitle = "更精确地识别重复文件（速度较慢）",
                    checked = state.highPrecisionEnabled,
                    onCheckedChange = { viewModel?.updateHighPrecision(it) }
                )
            )

            // ── 清理设置 (Cleanup Settings) ────────────────────────────────
            add(SectionHeader(id = "cleanup_header", title = "清理设置"))

            add(
                ToggleItem(
                    id = "auto_clear_recycle",
                    title = "自动清空回收站",
                    subtitle = "清理完成后自动清空回收站",
                    checked = state.autoClearRecycle,
                    onCheckedChange = { viewModel?.updateAutoClearRecycle(it) }
                )
            )

            // ── 显示设置 (Display Settings) ────────────────────────────────
            add(SectionHeader(id = "display_header", title = "显示设置"))

            add(
                RadioGroupItem(
                    id = "theme_mode",
                    title = "主题模式",
                    selectedValue = state.themeMode.name,
                    options = ThemeMode.entries.map { it.name },
                    onOptionSelected = { value ->
                        viewModel?.updateThemeMode(ThemeMode.valueOf(value))
                    }
                )
            )

            add(
                RadioGroupItem(
                    id = "language",
                    title = "语言",
                    selectedValue = state.language,
                    options = listOf("zh", "en"),
                    onOptionSelected = { value ->
                        viewModel?.updateLanguage(value)
                    }
                )
            )

            // ── 通知设置 (Notification Settings) ───────────────────────────
            add(SectionHeader(id = "notification_header", title = "通知设置"))

            add(
                ToggleItem(
                    id = "notification",
                    title = "启用通知",
                    subtitle = "扫描完成和清理提醒",
                    checked = state.notificationEnabled,
                    onCheckedChange = { viewModel?.updateNotification(it) }
                )
            )

            // ── 隐私设置 (Privacy Settings) ────────────────────────────────
            add(SectionHeader(id = "privacy_header", title = "隐私设置"))

            add(
                ToggleItem(
                    id = "privacy_lock",
                    title = "隐私锁",
                    subtitle = "进入应用时需要验证",
                    checked = state.privacyLock,
                    onCheckedChange = { viewModel?.updatePrivacyLock(it) }
                )
            )

            add(
                ToggleItem(
                    id = "shake_to_feedback",
                    title = "摇动反馈",
                    subtitle = "摇动手机时发送反馈",
                    checked = state.shakeToFeedback,
                    onCheckedChange = { viewModel?.updateShakeToFeedback(it) }
                )
            )

            // ── 关于 (About) ────────────────────────────────────────────────
            add(SectionHeader(id = "about_header", title = "关于"))

            add(
                InfoItem(
                    id = "app_version",
                    title = "应用版本",
                    value = "v${state.appVersion} (${state.appVersionCode})"
                )
            )

            add(
                ActionItem(
                    id = "check_update",
                    title = "检查更新"
                ) {
                    // Placeholder — will wire up navigation / use-case later
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
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
            items(items, key = { it.id }) { item ->
                when (item) {
                    is SectionHeader -> SectionHeaderView(item)
                    is ToggleItem -> ToggleItemView(item)
                    is DropdownItem -> DropdownItemView(item)
                    is RadioGroupItem -> RadioGroupItemView(item)
                    is InfoItem -> InfoItemView(item)
                    is ActionItem -> ActionItemView(item)
                }
            }

            // Bottom spacer so last item isn't flush with the edge
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Private composable views for each item type
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionHeaderView(item: SectionHeader) {
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = item.title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

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
                    "zh" -> "中文"
                    "en" -> "English"
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
    OutlinedButton(
        onClick = item.onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = item.title)
    }
}
