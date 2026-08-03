package com.cznwiki.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cznwiki.app.BuildConfig
import com.cznwiki.app.CznApplication
import com.cznwiki.app.data.entity.BannerEntity
import com.cznwiki.app.data.entity.EventEntity
import com.cznwiki.app.network.RemoteUpdateManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

// === Accent Colors ===
private val Gold = Color(0xFFFFD700)
private val Cyan = Color(0xFF00E5FF)
private val DeepPurple = Color(0xFF1A0533)
private val DeepBlue = Color(0xFF0A1628)

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

    var events by remember { mutableStateOf<List<EventEntity>>(emptyList()) }
    var currentBanners by remember { mutableStateOf<List<BannerEntity>>(emptyList()) }
    val today = LocalDate.now().toString()

    LaunchedEffect(Unit) {
        events = db.eventDao().getAllEventsSync()
        currentBanners = db.bannerDao().getAllBannersSync().filter { it.endDate >= today && it.server == "Global" }
    }

    // Unified sorted events: active (asc by endDate) -> ended (desc by endDate)
    val sortedEvents = remember(events, today) {
        val active = events.filter { it.endDate >= today }.sortedBy { it.endDate }
        val ended = events.filter { it.endDate < today }.sortedByDescending { it.endDate }
        active + ended
    }
    val activeEvents = remember(sortedEvents) {
        sortedEvents.filter { it.endDate >= today }
    }
    var showAllEvents by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    // Hero particle animation state
    val infiniteTransition = rememberInfiniteTransition(label = "hero")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // === Hero Area with Animated Particles ===
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            DeepPurple,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            DeepBlue,
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        ) {
            // Animated particles
            val particleCount = 20
            val particles = remember {
                val rng = Random(42)
                List(particleCount) {
                    Triple(rng.nextFloat(), rng.nextFloat(), 2f + rng.nextFloat() * 4f)
                }
            }

            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width; val h = size.height
                // Static decorative lines
                drawLine(Color.White.copy(alpha = 0.06f), Offset(0f, h * 0.3f), Offset(w * 0.4f, h * 0.9f), 2f)
                drawLine(Color.White.copy(alpha = 0.04f), Offset(w * 0.6f, h * 0.2f), Offset(w, h * 0.7f), 1.5f)
            }

            // Animated particles overlay
            val particleAlpha by infiniteTransition.animateFloat(
                initialValue = 0.15f, targetValue = 0.4f,
                animationSpec = infiniteRepeatable(
                    animation = tween(3000, easing = EaseInOutSine),
                    repeatMode = RepeatMode.Reverse
                ), label = "particleAlpha"
            )
            val particleOffset by infiniteTransition.animateFloat(
                initialValue = 0f, targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(8000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ), label = "particleOffset"
            )

            Canvas(modifier = Modifier.fillMaxSize()) {
                for ((x, y, r) in particles) {
                    val offsetY = (y + particleOffset * 0.3f) % 1.2f - 0.1f
                    val px = x * size.width
                    val py = offsetY * size.height
                    val radius = r
                    val alpha = particleAlpha * (0.5f + 0.5f * sin(particleOffset * 6.28f + x * 6.28f).toFloat())
                    drawCircle(Color.Cyan.copy(alpha = maxOf(alpha * 0.4f, 0.02f)), radius, Offset(px, py))
                }
            }

            Column(Modifier.fillMaxSize().padding(24.dp), Arrangement.Center) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "卡厄思梦境 Wiki",
                            style = TextStyle(
                                fontSize = MaterialTheme.typography.headlineLarge.fontSize,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                shadow = Shadow(
                                    color = Cyan.copy(alpha = 0.5f),
                                    blurRadius = 20f
                                )
                            )
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "战斗员图鉴 · 卡牌查询 · 阵容推荐",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                    IconButton(onClick = {
                        if (!isChecking) {
                            isChecking = true; updateStatusText = "正在检查版本..."
                            scope.launch {
                                val result = updateManager.checkForUpdate { status ->
                                    updateStatusText = when (status) {
                                        is RemoteUpdateManager.UpdateStatus.Checking -> "正在检查版本..."
                                        is RemoteUpdateManager.UpdateStatus.Progress -> "下载中 ${status.progress.filesDone}/${status.progress.totalFiles}"
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

        // === Current Banner Countdown with staggered animation ===
        if (currentBanners.isNotEmpty()) {
            Text(
                "当前卡池",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.height(IntrinsicSize.Max)
            ) {
                itemsIndexed(currentBanners.take(3)) { index, banner ->
                    Column(Modifier.fillMaxHeight()) {
                    val endDate = try { dateFormat.parse(banner.endDate) } catch (_: Exception) { null }
                    val remainDays = if (endDate != null) ((endDate.time - System.currentTimeMillis()) / (1000 * 60 * 60 * 24) + 1).toInt() else -1
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        delay(index * 100L)
                        visible = true
                    }
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(animationSpec = tween(400)) + slideInHorizontally(
                            animationSpec = tween(400, delayMillis = 0),
                            initialOffsetX = { it / 2 }
                        )
                    ) {
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
                }
            }
            Spacer(Modifier.height(4.dp))
        }

        // === Quick Entry Cards with press animation ===
        Text(
            "快速入口",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.height(IntrinsicSize.Max)
        ) {
            item {
                Box(Modifier.fillMaxHeight()) {
                QuickEntryCard("角色图鉴", "浏览全部战斗员资料", onClick = onNavigateToCharacterList, tint = MaterialTheme.colorScheme.primary)
                }
            }
            item {
                Box(Modifier.fillMaxHeight()) {
                QuickEntryCard("当期活动", "活动倒计时与详情", onClick = onNavigateToEvents, tint = MaterialTheme.colorScheme.secondary)
                }
            }
            item {
                Box(Modifier.fillMaxHeight()) {
                QuickEntryCard("卡池一览", "当期与往期卡池", onClick = onNavigateToBanners, tint = MaterialTheme.colorScheme.tertiary)
                }
            }
            item {
                Box(Modifier.fillMaxHeight()) {
                QuickEntryCard("队伍构筑", "创建与管理我的队伍", onClick = onNavigateToTeams, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                }
            }
            item {
                Box(Modifier.fillMaxHeight()) {
                QuickEntryCard("数据备份", "导入/导出本地数据", onClick = onNavigateToBackup, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // === Activity Countdown Section with staggered animation ===
        if (activeEvents.isNotEmpty()) {
            Text(
                "最新活动",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            val displayEvents = if (showAllEvents) sortedEvents else activeEvents

            displayEvents.forEachIndexed { index, event ->
                val endDate = try { dateFormat.parse(event.endDate) } catch (_: Exception) { null }
                val remainDays = if (endDate != null) ((endDate.time - System.currentTimeMillis()) / (1000 * 60 * 60 * 24) + 1).toInt() else -1
                val isActive = event.endDate >= today
                val urgencyColor = when {
                    !isActive -> MaterialTheme.colorScheme.outline
                    remainDays <= 3 -> MaterialTheme.colorScheme.error
                    remainDays <= 7 -> Color(0xFFFF9800)
                    else -> MaterialTheme.colorScheme.primary
                }

                var visible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    delay(index * 60L)
                    visible = true
                }
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(300)) + slideInHorizontally(
                        animationSpec = tween(300),
                        initialOffsetX = { it / 3 }
                    )
                ) {
                    EventCard(
                        event = event,
                        remainDays = remainDays,
                        isActive = isActive,
                        urgencyColor = urgencyColor
                    )
                }
            }

            if (!showAllEvents && sortedEvents.size > activeEvents.size) {
                TextButton(onClick = { showAllEvents = true }, modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text("查看更多 (${sortedEvents.size}项活动)", color = MaterialTheme.colorScheme.primary)
                }
            }
            if (showAllEvents) {
                TextButton(onClick = { showAllEvents = false }, modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text("收起", color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(Modifier.height(8.dp))
        }

        // === T0 Recommended Characters with animated border ===
        Text(
            "T0 推荐角色",
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
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.height(IntrinsicSize.Max)
        ) {
            items(t0Chars) { (id, name, role) ->
                Box(Modifier.fillMaxHeight()) {
                T0CharacterCard(name = name, role = role, onClick = { onNavigateToCharacter(id) })
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // === Banner History Quick View ===
        val historyBanners = remember { mutableStateListOf<BannerEntity>() }
        LaunchedEffect(Unit) {
            historyBanners.clear()
            historyBanners.addAll(db.bannerDao().getAllBannersSync().filter { it.endDate < today && it.server == "Global" }.take(2))
        }
        if (historyBanners.isNotEmpty()) {
            Text(
                "往期卡池",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.height(IntrinsicSize.Max)
            ) {
                items(historyBanners.toList()) { banner ->
                    Box(Modifier.fillMaxHeight()) {
                    Card(
                        modifier = Modifier.width(180.dp).clickable(onClick = onNavigateToBanners),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text(banner.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${banner.startDate} ~ ${banner.endDate}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            if (banner.server.isNotEmpty()) Text(
                                banner.server,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (banner.server == "Global") Color(0xFF4FC3F7) else Color(0xFFFF7043)
                            )
                        }
                    }
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))
        Text(
            "数据版本: v$dataVersion",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 8.dp)
        )
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

// === Event Card with colored left indicator ===
@Composable
fun EventCard(
    event: EventEntity,
    remainDays: Int,
    isActive: Boolean,
    urgencyColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(Modifier.padding(0.dp)) {
            // Colored left indicator bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .defaultMinSize(minHeight = 60.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(urgencyColor, urgencyColor.copy(alpha = 0.3f))
                        )
                    )
            )
            Row(
                Modifier
                    .weight(1f)
                    .padding(start = 12.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        event.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (event.description.isNotBlank())
                        Text(
                            event.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                }
                // Capsule-style countdown
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = urgencyColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        if (remainDays > 0) "${remainDays}天" else if (remainDays == 0) "今天" else "已结束",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (remainDays > 0 && remainDays <= 3) Gold else urgencyColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

// === Quick Entry Card with press scale ===
@Composable
fun QuickEntryCard(title: String, description: String, onClick: () -> Unit = {}, tint: Color) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "quickEntryScale"
    )

    Card(
        modifier = Modifier
            .width(150.dp)
            .scale(scale)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = tint.copy(alpha = 0.12f))
    ) {
        Column(
            Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon circle background
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(tint.copy(alpha = 0.3f), tint.copy(alpha = 0.05f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    title.take(2),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = tint,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = tint,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}

// === T0 Character Card with animated gradient ring ===
@Composable
fun T0CharacterCard(name: String, role: String, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "t0Border")
    val borderAngle by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "t0BorderAngle"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "t0Scale"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    Card(
        modifier = Modifier
            .width(130.dp)
            .scale(scale)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Column(
            Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar with animated gradient border
            Box(
                Modifier.size(62.dp),
                contentAlignment = Alignment.Center
            ) {
                // Rotating gradient ring
                val sweepColors = listOf(Cyan, primaryColor, Gold, Cyan)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val sweep = Brush.sweepGradient(sweepColors)
                    drawArc(
                        brush = sweep,
                        startAngle = borderAngle,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 3.dp.toPx())
                    )
                }
                // Inner circle with initials
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(primaryColor, secondaryColor)
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
            }
            Spacer(Modifier.height(10.dp))
            Text(
                name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                role,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(4.dp))
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

// === Banner Countdown Card with pulse and shimmer ===
@Composable
fun BannerCountdownCard(
    name: String,
    stars: Int,
    element: String,
    server: String,
    remainDays: Int,
    onClick: () -> Unit
) {
    val urgencyColor = when {
        remainDays <= 3 -> MaterialTheme.colorScheme.error
        remainDays <= 7 -> Color(0xFFFF9800)
        else -> MaterialTheme.colorScheme.primary
    }

    // Pulse animation for urgent (<=3 days)
    val isUrgent = remainDays in 1..3
    val pulseScale by animateFloatAsState(
        targetValue = if (isUrgent) 1.08f else 1f,
        animationSpec = if (isUrgent) {
            infiniteRepeatable(
                animation = tween(800, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            )
        } else {
            tween(200)
        },
        label = "bannerPulse"
    )

    // Shimmer border animation
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "shimmerOffset"
    )

    Card(
        modifier = Modifier
            .width(170.dp)
            .scale(pulseScale)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Cyan.copy(alpha = 0.15f),
                        Cyan.copy(alpha = 0.4f),
                        Cyan.copy(alpha = 0.15f)
                    ),
                    start = Offset(shimmerOffset * 340f, 0f),
                    end = Offset(shimmerOffset * 340f + 340f, 0f)
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                ) {
                    Text(
                        "${stars}星",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
            if (element.isNotBlank()) {
                Text(
                    element,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            if (server.isNotEmpty()) {
                Text(
                    server,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (server == "Global") Color(0xFF4FC3F7) else Color(0xFFFF7043)
                )
            }
            Spacer(Modifier.height(8.dp))
            // Large bold countdown
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = urgencyColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        if (remainDays > 0) "剩余 ${remainDays} 天" else if (remainDays == 0) "今日结束" else "已结束",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isUrgent) Gold else urgencyColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}
