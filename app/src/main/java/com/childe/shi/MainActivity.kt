package com.childe.shi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.childe.shi.common.log.AppLogger
import com.childe.shi.data.repository.LocalJsonWorkRepository
import com.childe.shi.data.repository.WorkRepository
import com.childe.shi.ui.ShiApp
import com.childe.shi.ui.theme.诗云Theme

/**
 * 诗云应用主入口 Activity
 *
 * 遵循极简职责划分：
 * 1. 负责窗口与 Edge-to-Edge 全屏沉浸式配置；
 * 2. 唯一定义并持有本地数据仓库 LocalJsonWorkRepository 实例；
 * 3. 挂载根 UI 组件 ShiApp，不承担任何具体页面逻辑与业务排版。
 */
class MainActivity : ComponentActivity() {

    private val workRepository: WorkRepository by lazy {
        LocalJsonWorkRepository(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 初始化应用日志系统
        AppLogger.init(applicationContext)
        AppLogger.i("MainActivity", "应用主 Activity 启动 (onCreate)")

        enableEdgeToEdge()
        setContent {
            诗云Theme {
                ShiApp(repository = workRepository)
            }
        }
    }
}
