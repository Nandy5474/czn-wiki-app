package com.cznwiki.app.ui.screens

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cznwiki.app.CznApplication
import com.cznwiki.app.data.BackupManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = (context.applicationContext as CznApplication).database
    val backupManager = remember { BackupManager(context) }

    var statusMessage by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var lastExportPath by remember { mutableStateOf<String?>(null) }

    // Export launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            isProcessing = true
            statusMessage = "正在导出..."
            scope.launch {
                val path = backupManager.exportBackup(db)
                if (path != null) {
                    // Copy internal file to user-selected URI
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            context.openFileInput(path.substringAfterLast("/")).use { inp ->
                                inp.copyTo(out)
                            }
                        }
                        lastExportPath = uri.toString()
                        statusMessage = "导出成功！文件已保存"
                    } catch (e: Exception) {
                        statusMessage = "导出失败: ${e.message}"
                    }
                } else {
                    statusMessage = "导出失败：无法生成备份数据"
                }
                isProcessing = false
            }
        }
    }

    // Import launcher
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            isProcessing = true
            statusMessage = "正在导入..."
            scope.launch {
                val count = backupManager.importBackup(uri, db)
                statusMessage = if (count >= 0) "导入成功！恢复了 $count 条数据" else "导入失败：文件格式不正确"
                isProcessing = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        TopAppBar(
            title = { Text("数据备份", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )

        // Info card
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, "说明", tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("备份说明", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(12.dp))
                Text("备份文件包含以下数据：", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Text("• 已拥有角色及命座信息", style = MaterialTheme.typography.bodySmall)
                Text("• 自定义队伍配置", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                Text("数据更新后可通过导入备份文件恢复以上信息。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        }

        // Export section
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Upload, "导出", tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("导出备份", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                Text("将当前数据导出为 .cznbackup 文件，可保存到任意位置。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { exportLauncher.launch("czn_backup.czbackup") },
                    enabled = !isProcessing,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isProcessing) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    else Icon(Icons.Default.Upload, "导出")
                    Spacer(Modifier.width(8.dp))
                    Text("导出备份文件")
                }
            }
        }

        // Import section
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Download, "导入", tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(8.dp))
                    Text("导入备份", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                Text("选择之前导出的 .cznbackup 文件，恢复角色和队伍数据。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { importLauncher.launch(arrayOf("*/*")) },
                    enabled = !isProcessing,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    if (isProcessing) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onSecondary)
                    else Icon(Icons.Default.Download, "导入")
                    Spacer(Modifier.width(8.dp))
                    Text("导入备份文件")
                }
            }
        }

        // Status
        if (statusMessage.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (statusMessage.contains("成功")) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    else MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                )
            ) {
                Text(
                    statusMessage,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (statusMessage.contains("成功")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}