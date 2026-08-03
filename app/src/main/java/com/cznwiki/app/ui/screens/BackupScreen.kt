package com.cznwiki.app.ui.screens

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
import com.cznwiki.app.network.RemoteUpdateManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val app = context.applicationContext as CznApplication
    val db = app.database
    val remoteUpdateMgr = app.remoteUpdateManager
    val localDataMgr = app.localDataManager
    val backupManager = remember { BackupManager(context) }

    var statusMessage by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }

    // Remote update state
    var updateProgressDesc by remember { mutableStateOf("") }
    var updateProgressPct by remember { mutableStateOf(0f) }
    var updateResult by remember { mutableStateOf<RemoteUpdateManager.UpdateResult?>(null) }
    var isCheckingRemote by remember { mutableStateOf(false) }
    var localVersion by remember { mutableIntStateOf(localDataMgr.getLocalVersion()) }

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
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            context.openFileInput(path.substringAfterLast("/")).use { inp ->
                                inp.copyTo(out)
                            }
                        }
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

    fun triggerRemoteUpdate() {
        isCheckingRemote = true
        updateProgressDesc = ""
        updateProgressPct = 0f
        updateResult = null
        statusMessage = ""
        scope.launch {
            val result = remoteUpdateMgr.checkForUpdate { status ->
                when (status) {
                    is RemoteUpdateManager.UpdateStatus.Checking -> {
                        updateProgressDesc = "正在检查远程版本..."
                        updateProgressPct = 0f
                    }
                    is RemoteUpdateManager.UpdateStatus.Downloading -> {
                        updateProgressDesc = "正在下载${status.step}..."
                    }
                    is RemoteUpdateManager.UpdateStatus.Progress -> {
                        val p = status.progress
                        updateProgressDesc = "下载 ${p.fileLabel} (${p.filesDone}/${p.totalFiles})"
                        updateProgressPct = p.filesDone.toFloat() / p.totalFiles
                    }
                    is RemoteUpdateManager.UpdateStatus.Done -> {
                        updateProgressPct = 1f
                        updateResult = status.result
                        localVersion = localDataMgr.getLocalVersion()
                        statusMessage = "已更新至 ${status.result.remoteVersion}"
                    }
                    is RemoteUpdateManager.UpdateStatus.Error -> {
                        statusMessage = status.message
                    }
                }
            }
            // checkForUpdate returns result directly; if Done wasn't emitted, use the return value
            if (updateResult == null && result.success) {
                updateResult = result
                localVersion = localDataMgr.getLocalVersion()
                statusMessage = if (result.remoteVersion.isNotBlank()) "已更新至 ${result.remoteVersion}" else result.message
            }
            if (updateResult == null && !result.success && statusMessage.isBlank()) {
                statusMessage = result.message
            }
            isCheckingRemote = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        TopAppBar(
            title = { Text("数据管理", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )

        // ── 远程数据更新 ──
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudSync, "更新", tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("数据更新", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("当前数据版本：v$localVersion", style = MaterialTheme.typography.bodyMedium)
                    updateResult?.let { res ->
                        if (res.version > localVersion && res.remoteVersion.isNotBlank()) {
                            Text("最新版本：${res.remoteVersion}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                if (updateProgressDesc.isNotEmpty() && isCheckingRemote) {
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { updateProgressPct },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(updateProgressDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }

                updateResult?.let { res ->
                    if (res.message.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(res.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }

                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { triggerRemoteUpdate() },
                    enabled = !isCheckingRemote && !isProcessing,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isCheckingRemote) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Icon(Icons.Default.CloudSync, "检查更新")
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(if (isCheckingRemote) "正在更新..." else "检查远程更新")
                }
            }
        }

        // ── 备份说明 ──
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, "说明", tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("备份说明", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                Text("备份文件包含：已拥有角色、命座信息、自定义队伍配置。", style = MaterialTheme.typography.bodySmall)
                Text("数据更新后可通过导入备份恢复以上信息。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        }

        // ── 导出备份 ──
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
                    enabled = !isProcessing && !isCheckingRemote,
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

        // ── 导入备份 ──
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
                    enabled = !isProcessing && !isCheckingRemote,
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
                    containerColor = if (statusMessage.contains("成功") || statusMessage.contains("最新"))
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    else MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                )
            ) {
                Text(
                    statusMessage,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (statusMessage.contains("成功") || statusMessage.contains("最新"))
                        MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
