package com.cznwiki.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cznwiki.app.CznApplication
import com.cznwiki.app.network.RemoteUpdateManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onNavigateToCharacter: (Int) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = (context.applicationContext as CznApplication).database
    val updateManager = remember { RemoteUpdateManager.getInstance(context, db) }

    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateMessage by remember { mutableStateOf("") }
    var isChecking by remember { mutableStateOf(false) }
    var updateStatusText by remember { mutableStateOf("") }
    var dataVersion by remember { mutableIntStateOf(updateManager.getLocalVersion()) }
    val prefs = context.getSharedPreferences("czn_remote_update", Context.MODE_PRIVATE)
    val lastCheckTime = prefs.getLong("last_check_time", 0L)
    val lastCheckDays = if (lastCheckTime > 0) {
        ((System.currentTimeMillis() - lastCheckTime) / (1000 * 60 * 60 * 24)).toInt()
    } else -1

    Column(modifier = Modifier.fillMaxSize()) {
        // === Hero Area ===
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        ) {
            // Decorative geometric lines
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                // Line 1
                drawLine(
                    color = Color.White.copy(alpha = 0.08f),
                    start = Offset(0f, h * 0.3f),
                    end = Offset(w * 0.4f, h * 0.9f),
                    strokeWidth = 2f
                )
                // Line 2
                drawLine(
                    color = Color.White.copy(alpha = 0.05f),
                    start = Offset(w * 0.6f, h * 0.2f),
                    end = Offset(w, h * 0.7f),
                    strokeWidth = 1.5f
                )
                // Circle
                drawCircle(
                    color = Color.White.copy(alpha = 0.04f),
                    radius = w * 0.15f,
                    center = Offset(w * 0.85f, h * 0.25f)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "卡厄思梦境 Wiki",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "战斗员图鉴 · 卡牌查询 · 阵容推荐",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                    IconButton(
                        onClick = {
                            if (!isChecking) {
                                isChecking = true
                                updateStatusText = "正在检查版本..."
                                scope.launch {
                                    val result = updateManager.checkForUpdate { status ->
                                        updateStatusText = when (status) {
                                            is RemoteUpdateManager.UpdateStatus.Checking -> "正在检查版本..."
                                            is RemoteUpdateManager.UpdateStatus.Downloading -> "正在下载${status.step}..."
                                            is RemoteUpdateManager.UpdateStatus.Done -> "更新完成"
                                            is RemoteUpdateManager.UpdateStatus.Error -> "更新失败: ${status.message}"
                                        }
                                    }
                                    updateMessage = buildString {
                                        append(result.message)
                                        if (result.charsUpdated > 0 || result.cardsUpdated > 0 ||
                                            result.saUpdated > 0 || result.userCollUpdated > 0) {
                                            append("\n更新: ${result.charsUpdated}角色, ${result.cardsUpdated}卡牌, " +
                                                "${result.saUpdated}命座, ${result.userCollUpdated}收藏")
                                        }
                                    }
                                    dataVersion = result.version
                                    isChecking = false
                                    showUpdateDialog = true
                                }
                            }
                        },
                        enabled = !isChecking
                    ) {
                        if (isChecking) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                                if (updateStatusText.isNotEmpty()) {
                                    Text(
                                        updateStatusText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        } else {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "检查更新",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }

        // === Quick Entry Cards ===
        Text(
            text = "快速入口",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                QuickEntryCard(
                    title = "角色图鉴",
                    description = "浏览全部战斗员资料",
                    onClick = { onNavigateToCharacter(1) },
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            item {
                QuickEntryCard(
                    title = "当期活动",
                    description = "全力战 BOSS · 赛季奖励",
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
            item {
                QuickEntryCard(
                    title = "阵容推荐",
                    description = "探索 · 全力战 · 螺旋塔",
                    tint = MaterialTheme.colorScheme.tertiary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // === T0 Recommended Characters ===
        Text(
            text = "T0 推荐角色",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        val t0Chars = listOf(
            Triple(17, "奥尔莱亚", "全能辅助"),
            Triple(13, "维若妮卡", "副C/辅助"),
            Triple(3, "黛安娜", "弃牌主C"),
            Triple(5, "蒂菲拉", "辅助"),
            Triple(19, "凯西乌斯", "士气发动机"),
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(t0Chars) { (id, name, role) ->
                T0CharacterCard(
                    name = name,
                    role = role,
                    onClick = { onNavigateToCharacter(id) }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // === Data version at bottom ===
        Text(
            text = "数据版本: v$dataVersion",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 8.dp)
        )
    }

    if (showUpdateDialog) {
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = { Text("数据更新") },
            text = { Text(updateMessage) },
            confirmButton = {
                TextButton(onClick = { showUpdateDialog = false }) {
                    Text("确定")
                }
            }
        )
    }
}

@Composable
fun QuickEntryCard(
    title: String,
    description: String,
    onClick: () -> Unit = {},
    tint: Color
) {
    Card(
        modifier = Modifier
            .width(180.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = tint.copy(alpha = 0.12f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = tint
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun T0CharacterCard(
    name: String,
    role: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(130.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar circle with gradient
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    name.take(1),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                role,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            ) {
                Text(
                    "T0",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}
