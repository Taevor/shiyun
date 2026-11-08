package com.childe.shi.ui.log

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.childe.shi.common.log.AppLogger
import com.childe.shi.common.log.LogExportResult
import com.childe.shi.common.log.LogStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 格式化字节大小显示
 */
private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format(java.util.Locale.CHINA, "%.1f KB", bytes / 1024.0)
        else -> String.format(java.util.Locale.CHINA, "%.2f MB", bytes / (1024.0 * 1024.0))
    }
}

/**
 * 运行日志管理与导出对话框
 */
@Composable
fun LogManagementDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var logStats by remember { mutableStateOf<LogStats?>(null) }
    var isExporting by remember { mutableStateOf(false) }
    var lastExportResult by remember { mutableStateOf<LogExportResult?>(null) }

    fun refreshStats() {
        logStats = AppLogger.getLogStats()
    }

    LaunchedEffect(Unit) {
        refreshStats()
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "运行日志管理",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // 1. 日志状态卡片
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "系统状态",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.outline,
                            )
                            Text(
                                text = "实时记录中",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }

                        val stats = logStats
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "本地占用",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.outline,
                            )
                            Text(
                                text = if (stats != null) {
                                    "${stats.fileCount} 个文件（${formatFileSize(stats.totalSizeBytes)}）"
                                } else "统计中...",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "轮换规则",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.outline,
                            )
                            Text(
                                text = "单文件上限 2MB，最多 5 卷",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                    }
                }

                Text(
                    text = "日志包含应用冷启动、诗词数据加载、搜索耗时与异常报错，不记录任何个人隐私或搜索内容全文。可直接导出并发送给开发者排查问题。",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline,
                    lineHeight = 17.sp,
                )

                // 2. 最近一次导出结果反馈
                val result = lastExportResult
                if (result != null) {
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )
                    if (result.success) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = "导出成功！",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    text = "已保存至: ${result.relativePath}/${result.fileName}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = "文件大小: ${formatFileSize(result.totalBytes)}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.outline,
                                )

                                if (result.uri != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Button(
                                        onClick = { shareExportedLog(context, result) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                    ) {
                                        Text(text = "分享此日志文件", fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "导出失败: ${result.errorMessage ?: "未知错误"}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isExporting) return@Button
                    isExporting = true
                    scope.launch {
                        val exportRes = withContext(Dispatchers.IO) {
                            AppLogger.exportLogsToDownloads(context)
                        }
                        lastExportResult = exportRes
                        isExporting = false
                        if (exportRes.success) {
                            Toast.makeText(context, "日志已成功保存至 Download/诗云日志", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "导出失败: ${exportRes.errorMessage}", Toast.LENGTH_SHORT).show()
                        }
                        refreshStats()
                    }
                },
                enabled = !isExporting,
                shape = RoundedCornerShape(10.dp),
            ) {
                if (isExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(text = "正在导出...", fontSize = 14.sp)
                } else {
                    Text(text = "导出运行日志", fontSize = 14.sp)
                }
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        val success = AppLogger.clearLogs()
                        refreshStats()
                        lastExportResult = null
                        if (success) {
                            Toast.makeText(context, "已清空本地所有日志", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "清空日志失败", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text(text = "清空本地日志", fontSize = 13.sp)
                }
                TextButton(
                    onClick = onDismissRequest,
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text(text = "关闭", fontSize = 13.sp)
                }
            }
        },
    )
}

/**
 * 分享已导出的日志文件给其他应用（微信、QQ、邮件、文档等）
 */
private fun shareExportedLog(context: Context, result: LogExportResult) {
    val uri = result.uri ?: return
    try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "分享诗云运行日志")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    } catch (e: Exception) {
        Toast.makeText(context, "无法唤起分享: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
