package com.cznwiki.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cznwiki.app.BuildConfig
import com.cznwiki.app.CznApplication
import com.cznwiki.app.data.entity.BannerEntity
import com.cznwiki.app.data.entity.EventEntity
import com.cznwiki.app.network.RemoteUpdateManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToCharacter: (Int) -> Unit,
    onNavigateToCharacterList: () -> Unit,
    onNavigateToEvents: () -> Unit = {},
    onNavigateToBanners: () -> Unit = {},
    onNavigateToTeams: () -> Unit = {},
    onNavigateToBackup: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val app = context.applicationContext as CznApplication
    val db = app.database
    val updateManager = remember { RemoteUpdateManager.getInstance(context, db) }

    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateMessage by remember { mutableStateOf("") }
    var isChecking by remember { mutableStateOf(false) }
    var updateStatusText by remember { mutableStateOf("") }
    var dataVersion by remember { mutableIntStateOf(updateManager.getLocalVersion()) }
    var remoteVersion by remember { mutableStateOf("") }
    val appVersion = BuildConfig.VERSION_NAME

    // Load events and banners
    var events by remember { mutableStateOf<List<EventEntity>>(emptyList()) }
    var currentBanners by remember { mutableStateOf<List<BannerEntity>>(emptyList()) }
    val today = "2026-06-10"

    LaunchedEffect(Unit) {
        events = db.eventDao().getAllEventsSync()
        currentBanners = db.bannerDao().getAllBannersSync().filter { it.type == "current" }
    }

    val activeEvents = remember(events) {
        events.filter { it.endDate >= today }.sortedBy { it.endDate }
    }
    val allEvents = remember(events) {
        events.sortedByDescending { it.endDate }
    }
    var showAllEvents by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // === Hero Area ===
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
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
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width; val h = size.height
                drawLine(Color.White.copy(alpha = 0.08f), Offset(0f, h * 0.3f), Offset(w * 0.4f, h * 0.9f), 2f)
                drawLine(Color.White.copy(alpha = 0.05f), Offset(w * 0.6f, h * 0.2f), Offset(w, h * 0.7f), 1.5f)
                drawCircle(Color.White.copy(alpha = 0.04f), w * 0.15f, Offset(w * 0.85f, h * 0.25f))
            }
            Column(Modifier.fillMaxSize().padding(24.dp), Arrangement.Center) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text("卡厄思梦境 Wiki", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(Modifier.height(8.dp))
                        Text("战斗员图鉴 · 卡牌查询 · 阵容推荐", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f))
                    }
                    IconButton(onClick = {
                        if (!isChecking) {
                            isChecking = true; updateStatusText = "正在检查版本..."
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
                                    if (result.charsUpdated > 0 || result.cardsUpdated > 0 || result.saUpdated > 0 || result.userCollUpdated > 0)
                                        append("\n更新: ${result.charsUpdated}角色, ${result.cardsUpdated}卡牌, ${result.saUpdated}命座, ${result.userCollUpdated}收藏")
                                }
                                dataVersion = result.version; remoteVersion = result.remoteVersion; isChecking = false; showUpdateDialog = true
                            }
                        }
                    }, enabled = !isChecking) {
                        if (isChecking) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                                if (updateStatusText.isNotEmpty()) Text(updateStatusText, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                            }
                        } else Icon(Icons.Default.Refresh, "检查更新", tint = Color.White)
                    }
                }
            }
        }

        // === Current Banner Countdown ===
        if (currentBanners.isNotEmpty()) {
            Text("当前卡池", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(currentBanners.take(3)) { banner ->
                    val endDate = try { dateFormat.parse(banner.endDate) } catch (_: Exception) { null }
                    val remainDays = if (endDate != null) ((endDate.time - System.currentTimeMillis()) / (1000 * 60 * 60 * 24) + 1).toInt() else -1
                    BannerCountdownCard(
                        name = banner.name,
                        stars = banner.stars,
                        element = banner.element,
                        server = banner.server,
                        remainDays = remainDays,
                        onClick = onNavigateToBanners
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
        }

        // === Quick Entry Cards ===
        Text("快速入口", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                QuickEntryCard("角色图鉴", "浏览全部战斗员资料", onClick = onNavigateToCharacterList, tint = MaterialTheme.colorScheme.primary)
            }
            item {
                QuickEntryCard("当期活动", "活动倒计时与详情", onClick = onNavigateToEvents, tint = MaterialTheme.colorScheme.secondary)
            }
            item {
                QuickEntryCard("队伍构筑", "创建与管理我的队伍", onClick = onNavigateToTeams, tint = MaterialTheme.colorScheme.tertiary)
            }
            item {
                QuickEntryCard("数据备份", "导入/导出本地数据", onClick = onNavigateToBackup, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
            }
        }

        Spacer(Modifier.height(8.dp))

        // === Activity Countdown Section ===
        if (activeEvents.isNotEmpty()) {
            Text("最新活动", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

            val displayEvents = if (showAllEvents) allEvents else activeEvents

            displayEvents.forEach { event ->
                val endDate = try { dateFormat.parse(event.endDate) } catch (_: Exception) { null }
                val remainDays = if (endDate != null) ((endDate.time - System.currentTimeMillis()) / (1000 * 60 * 60 * 24) + 1).toInt() else -1
                val isActive = event.endDate >= today
                val urgencyColor = when {
                    !isActive -> MaterialTheme.colorScheme.outline
                    remainDays <= 3 -> MaterialTheme.colorScheme.error
                    remainDays <= 7 -> Color(0xFFFF9800)
                    else -> MaterialTheme.colorScheme.primary
                }

                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(event.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            if (event.description.isNotBlank())
                                Text(event.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                        Surface(shape = RoundedCornerShape(8.dp), color = urgencyColor.copy(alpha = 0.15f)) {
                            Text(
                                if (remainDays > 0) "${remainDays}天后结束" else if (remainDays == 0) "今日结束" else "已结束",
                                style = MaterialTheme.typography.labelMedium,
                                color = urgencyColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            if (!showAllEvents && allEvents.size > activeEvents.size) {
                TextButton(onClick = { showAllEvents = true }, modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text("查看更多 (${allEvents.size}项活动)", color = MaterialTheme.colorScheme.primary)
                }
            }
            if (showAllEvents) {
                TextButton(onClick = { showAllEvents = false }, modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text("收起", color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(Modifier.height(8.dp))
        }

        // === T0 Recommended Characters ===
        Text("T0 推荐角色", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        val t0Chars = listOf(
            Triple(17, "奥尔莱亚", "全能辅助"),
            Triple(13, "维若妮卡", "副C/辅助"),
            Triple(3, "黛安娜", "弃牌主C"),
            Triple(5, "蒂菲拉", "辅助"),
            Triple(19, "凯西乌斯", "士气发动机"),
        )
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(t0Chars) { (id, name, role) ->
                T0CharacterCard(name = name, role = role, onClick = { onNavigateToCharacter(id) })
            }
        }

        Spacer(Modifier.height(8.dp))

        // === Banner History Quick View ===
        val historyBanners = remember { mutableStateListOf<BannerEntity>() }
        LaunchedEffect(Unit) {
            historyBanners.clear()
            historyBanners.addAll(db.bannerDao().getAllBannersSync().filter { it.type == "history" }.take(2))
        }
        if (historyBanners.isNotEmpty()) {
            Text("往期卡池", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(historyBanners.toList()) { banner ->
                    Card(
                        modifier = Modifier.width(180.dp).clickable(onClick = onNavigateToBanners),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text(banner.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text("${banner.startDate} ~ ${banner.endDate}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            if (banner.server.isNotEmpty()) Text(banner.server, style = MaterialTheme.typography.labelSmall, color = if (banner.server == "国际服") Color(0xFF4FC3F7) else Color(0xFFFF7043))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))
        Text("数据版本: v$dataVersion", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 8.dp))
    }

    if (showUpdateDialog) {
        val needsAppUpdate = appVersion != remoteVersion && remoteVersion.isNotEmpty()
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = { Text("数据更新") },
            text = {
                Column {
                    Text(updateMessage)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "当前软件版本: v$appVersion  |  最新版本: v${remoteVersion.ifEmpty { "未知" }}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    if (needsAppUpdate) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "软件版本不是最新，建议更新软件以获取完整数据",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showUpdateDialog = false }) { Text("确定") } }
        )
    }
}

@Composable
fun QuickEntryCard(title: String, description: String, onClick: () -> Unit = {}, tint: Color) {
    Card(modifier = Modifier.width(150.dp).clickable(onClick = onClick), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = tint.copy(alpha = 0.12f))) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = tint)
            Spacer(Modifier.height(4.dp))
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
    }
}

@Composable
fun T0CharacterCard(name: String, role: String, onClick: () -> Unit) {
    Card(modifier = Modifier.width(130.dp).clickable(onClick = onClick), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(56.dp).clip(CircleShape).background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))), contentAlignment = Alignment.Center) {
                Text(name.take(1), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(Modifier.height(10.dp))
            Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(2.dp))
            Text(role, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)) {
                Text("T0", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }
        }
    }
}

@Composable
fun BannerCountdownCard(name: String, stars: Int, element: String, server: String, remainDays: Int, onClick: () -> Unit) {
    val urgencyColor = when {
        remainDays <= 3 -> MaterialTheme.colorScheme.error
        remainDays <= 7 -> Color(0xFFFF9800)
        else -> MaterialTheme.colorScheme.primary
    }
    Card(modifier = Modifier.width(170.dp).clickable(onClick = onClick), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)) {
                    Text("${stars}星", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                }
            }
            if (element.isNotBlank()) {
                Text(element, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
            if (server.isNotEmpty()) {
                Text(server, style = MaterialTheme.typography.labelSmall, color = if (server == "国际服") Color(0xFF4FC3F7) else Color(0xFFFF7043))
            }
            Spacer(Modifier.height(8.dp))
            Surface(shape = RoundedCornerShape(6.dp), color = urgencyColor.copy(alpha = 0.15f)) {
                Text(
                    if (remainDays > 0) "剩余 ${remainDays} 天" else if (remainDays == 0) "今日结束" else "已结束",
                    style = MaterialTheme.typography.labelMedium, color = urgencyColor, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
