/*
 * Copyright (C) 2025 PhotoCleaner
 *
 * 隐私政策与服务条款弹窗组件
 *
 * @author PhotoCleaner
 */
package com.photocleaner.app.utils

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 隐私政策文本 */
val PRIVACY_POLICY_TEXT = """
清图大师（PhotoCleaner）尊重并保护您的隐私...

1. 信息收集
本应用为纯本地工具，不会收集、存储或上传任何个人身份信息、图片内容、文件路径等敏感数据。

2. 权限说明
- 存储权限（READ_MEDIA_IMAGES / READ_EXTERNAL_STORAGE）：仅用于扫描和识别重复图片。
- 通知权限（POST_NOTIFICATIONS）：仅用于扫描完成和清理提醒。

3. 数据安全
所有图片数据仅在您的设备本地处理，不会上传至任何服务器。

4. 第三方服务
本应用不集成任何第三方分析、广告或统计SDK。

5. 隐私政策更新
我们可能会不时更新本隐私政策。更新后的政策将在应用内展示。
""".trimIndent()

/** 服务条款文本 */
val TERMS_OF_SERVICE_TEXT = """
欢迎使用清图大师（PhotoCleaner）。使用本应用即表示您同意以下条款。

1. 服务说明
本应用提供图片去重、清理功能，所有操作在本地完成。

2. 使用限制
- 您不得将本应用用于任何非法目的。
- 您不得反向工程、反编译或破解本应用。

3. 免责声明
- 本应用按"现状"提供，不提供任何明示或暗示的保证。
- 因删除操作导致的任何数据损失，开发者不承担责任。

4. 知识产权
本应用的所有权利、所有权和利益归开发者所有。

5. 适用法律
本条款受中华人民共和国法律管辖。
""".trimIndent()

@Composable
fun PrivacyPolicyDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("隐私政策", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                Text(PRIVACY_POLICY_TEXT, fontSize = 14.sp, lineHeight = 22.sp)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}

@Composable
fun TermsOfServiceDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("服务条款", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                Text(TERMS_OF_SERVICE_TEXT, fontSize = 14.sp, lineHeight = 22.sp)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}

@Composable
fun AgreementScreen(onAccept: () -> Unit, onShowPrivacy: () -> Unit, onShowTerms: () -> Unit) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("欢迎使用清图大师", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("PhotoCleaner", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(32.dp))
            Text("请阅读并同意以下条款以继续使用", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onShowPrivacy, modifier = Modifier.fillMaxWidth()) { Text("查看隐私政策") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onShowTerms, modifier = Modifier.fillMaxWidth()) { Text("查看服务条款") }
            Spacer(Modifier.height(32.dp))
            Button(onClick = onAccept, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("同意并继续", fontSize = 16.sp) }
        }
    }
}
