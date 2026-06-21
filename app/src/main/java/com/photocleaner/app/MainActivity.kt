package com.photocleaner.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.photocleaner.app.navigation.AppNavGraph
import com.photocleaner.app.ui.theme.PhotoCleanerTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * 清图大师主 Activity。
 *
 * 使用 Hilt 注解 @AndroidEntryPoint 启用依赖注入，
 * 在 onCreate 中设置边缘到边缘显示并加载 Compose 界面。
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PhotoCleanerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavGraph()
                }
            }
        }
    }
}
