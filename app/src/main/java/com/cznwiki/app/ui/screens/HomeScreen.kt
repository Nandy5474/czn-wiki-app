package com.cznwiki.app.ui.screens

import android.util.Log
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cznwiki.app.CznApplication
import com.cznwiki.app.data.entity.BannerEntity
import com.cznwiki.app.data.entity.EventEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.math.sin
import kotlin.random.Random

// === HomeScreen — production version with error boundary ===

private val DeepPurple = Color(0xFF1A0533)
private val Cyan = Color(0xFF00E5FF)
private val Gold = Color(0xFFFFD700)

// Data wrapper for T0 character items
private data class T0Item(val id: Int, val name: String, val role: String, val imageUrl: String)

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

    var diagState by remember { mutableStateOf("加载中...") }
    var events by remember { mutableStateOf<List<EventEntity>>(emptyList()) }
    var currentBanners by remember { mutableStateOf<List<BannerEntity>>(emptyList()) }
    var historyBanners by remember { mutableStateOf<List<BannerEntity>>(emptyList()) }
    var charCount by remember { mutableIntStateOf(0) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val app = context.applicationContext as CznApplication
            val db = app.database
            charCount = runBlocking { db.characterDao().getCount() }
            val today = java.time.LocalDate.now().toString()
            events = runBlocking { db.eventDao().getAllEventsSync() }.filter { it.endDate >= today || it.endDate.isEmpty() }
            val allBanners = runBlocking { db.bannerDao().getAllBannersSync() }
            currentBanners = allBanners.filter { (it.endDate ?: "") >= today && (it.server ?: "") == "Global" }
            historyBanners = allBanners.filter { (it.endDate ?: "") < today && (it.server ?: "") == "Global" }.take(2)
            diagState = "渲染成功"
        } catch (e: Exception) {
            Log.e("HomeDiag", "init failed", e)
            errorMsg = "${e.javaClass.simpleName}: ${e.message}"
        }
    }

    if (errorMsg != null) {
        Box(Modifier.fillMaxSize().background(DeepPurple), contentAlignment = Alignment.Center) {
            Text("错误: $errorMsg", color = Color.White, fontSize = 14.sp)
        }
        return
    }

    val infiniteTransition = rememberInfiniteTransition(label = "hero")
    val particleAlpha by infiniteTransition.animateFloat(0.15f, 0.4f,
        infiniteRepeatable(tween(3000, easing = EaseInOutSine), RepeatMode.Reverse), "pa")
    val particleOffset by infiniteTransition.animateFloat(0f, 1f,
        infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart), "po")
    val particles = remember { val rng = Random(42); List(20) { Triple(rng.nextFloat(), rng.nextFloat(), 2f + rng.nextFloat() * 4f) } }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {

        // === Hero Area ===
        Box(Modifier.fillMaxWidth().height(220.dp).background(
            Brush.verticalGradient(listOf(DeepPurple, Color(0xFF0A1628)))
        )) {
            Canvas(Modifier.fillMaxSize()) {
                for ((x, y, r) in particles) {
                    val oy = (y + particleOffset * 0.3f) % 1.2f - 0.1f
                    val a = particleAlpha * (0.5f + 0.5f * sin(particleOffset * 6.28f + x * 6.28f).toFloat())
                    drawCircle(Color.Cyan.copy(alpha = maxOf(a * 0.4f, 0.02f)), r, Offset(x * size.width, oy * size.height))
                }
                drawLine(Color.White.copy(alpha = 0.06f), Offset(0f, size.height * 0.3f), Offset(size.width * 0.4f, size.height * 0.9f), 2f)
            }
            Column(Modifier.fillMaxSize().padding(24.dp), Arrangement.Center) {
                Text("卡厄思梦境 Wiki", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text("战斗员图鉴 \u00b7 卡牌查询 \u00b7 阵容推荐", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Text("角色图鉴 | 卡池查询 | 阵容推荐",
                    color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
            }
        }

        // === Banner Cards ===
        if (currentBanners.isNotEmpty()) {
            Text("当前卡池", fontWeight = FontWeight.Bold, color = Color.White,
                modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 4.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.height(140.dp)
            ) {
                items(currentBanners.take(3).size) { index ->
                    val banner = currentBanners[index]
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { delay(index * 100L); visible = true }
                    AnimatedVisibility(visible, enter = fadeIn(tween(400)) + slideInHorizontally(tween(400)) { it / 2 }) {
                        val remainDays = banner.endDate?.let {
                            val df = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                            try { ((df.parse(it)!!.time - System.currentTimeMillis()) / 86400000 + 1).toInt() } catch (_: Exception) { -1 }
                        } ?: -1
                        val urgencyColor = when {
                            remainDays <= 3 -> MaterialTheme.colorScheme.error
                            remainDays <= 7 -> Color(0xFFFF9800)
                            else -> MaterialTheme.colorScheme.primary
                        }
                        val isUrgent = remainDays in 1..3
                        Card(
                            Modifier.width(170.dp)
                                .border(1.dp, Cyan.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                                .clickable(onClick = onNavigateToBanners),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(banner.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                    Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)) {
                                        Text("${banner.stars}星", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                    }
                                }
                                if (banner.element.isNotEmpty())
                                    Text(banner.element, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                                Spacer(Modifier.height(8.dp))
                                Surface(shape = RoundedCornerShape(8.dp), color = urgencyColor.copy(alpha = 0.15f)) {
                                    Text(if (remainDays > 0) "剩余 ${remainDays} 天" else if (remainDays == 0) "今日结束" else "已结束",
                                        color = if (isUrgent) Gold else urgencyColor, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }

        // === Quick Entry Cards ===
        Text("快速入口", fontWeight = FontWeight.Bold, color = Color.White,
            modifier = Modifier.padding(16.dp, 8.dp, 16.dp, 4.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.height(130.dp)
        ) {
            item { QuickEntryCard("角色图鉴", "浏览全部战斗员", onClick = onNavigateToCharacterList, tint = MaterialTheme.colorScheme.primary) }
            item { QuickEntryCard("当期活动", "活动倒计时详情", onClick = onNavigateToEvents, tint = MaterialTheme.colorScheme.secondary) }
            item { QuickEntryCard("卡池一览", "当期与往期卡池", onClick = onNavigateToBanners, tint = MaterialTheme.colorScheme.tertiary) }
            item { QuickEntryCard("队伍构筑", "创建管理我的队伍", onClick = onNavigateToTeams, tint = Color(0xFFFF7043)) }
            item { QuickEntryCard("数据管理", "检查更新与备份", onClick = onNavigateToBackup, tint = Color(0xFF26A69A)) }
        }

        Spacer(Modifier.height(8.dp))

        // === T0 Recommended Characters ===
        Text("T0 推荐角色", fontWeight = FontWeight.Bold, color = Color.White,
            modifier = Modifier.padding(16.dp, 8.dp, 16.dp, 4.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.height(170.dp)
        ) {
            val t0Chars = listOf(
                T0Item(17, "奥尔莱亚", "全能辅助", "https://raw.githubusercontent.com/czn-gg/czn-wiki-data/main/images/characters/17_thumb.png"),
                T0Item(13, "维若妮卡", "副C/辅助", "https://raw.githubusercontent.com/czn-gg/czn-wiki-data/main/images/characters/13_thumb.png"),
                T0Item(3, "黛安娜", "弃牌主C", "https://raw.githubusercontent.com/czn-gg/czn-wiki-data/main/images/characters/3_thumb.png"),
                T0Item(5, "蒂菲拉", "辅助", "https://raw.githubusercontent.com/czn-gg/czn-wiki-data/main/images/characters/5_thumb.png"),
                T0Item(19, "凯西乌斯", "士气发动机", "https://raw.githubusercontent.com/czn-gg/czn-wiki-data/main/images/characters/19_thumb.png"),
            )
            items(t0Chars) { item ->
                T0CharacterCard(name = item.name, role = item.role, imageUrl = item.imageUrl, onClick = { onNavigateToCharacter(item.id) })
            }
        }

        Spacer(Modifier.height(8.dp))

        // === Events (forEachIndexed, NOT LazyColumn) ===
        if (events.isNotEmpty()) {
            Text("最新活动", fontWeight = FontWeight.Bold, color = Color.White,
                modifier = Modifier.padding(16.dp, 8.dp, 16.dp, 4.dp))
            val today = java.time.LocalDate.now().toString()
            events.forEachIndexed { index, event ->
                val endDate = try { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).parse(event.endDate) } catch (_: Exception) { null }
                val remainDays = if (endDate != null) ((endDate.time - System.currentTimeMillis()) / 86400000 + 1).toInt() else -1
                val isActive = event.endDate >= today
                val urgencyColor = when {
                    !isActive -> MaterialTheme.colorScheme.outline
                    remainDays <= 3 -> MaterialTheme.colorScheme.error
                    remainDays <= 7 -> Color(0xFFFF9800)
                    else -> MaterialTheme.colorScheme.primary
                }
                var visible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { delay(index * 60L); visible = true }
                AnimatedVisibility(visible, enter = fadeIn(tween(300)) + slideInHorizontally(tween(300)) { it / 3 }) {
                    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f))
                    ) {
                        Row(Modifier.padding(0.dp)) {
                            Box(Modifier.width(4.dp).defaultMinSize(minHeight = 60.dp)
                                .background(Brush.verticalGradient(listOf(urgencyColor, urgencyColor.copy(alpha = 0.3f)))))
                            Row(Modifier.weight(1f).padding(12.dp), Arrangement.SpaceBetween) {
                                Column(Modifier.weight(1f)) {
                                    Text(event.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    if (event.description.isNotBlank())
                                        Text(event.description, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                                }
                                Surface(shape = RoundedCornerShape(20.dp), color = urgencyColor.copy(alpha = 0.15f)) {
                                    Text(if (remainDays > 0) "${remainDays}天" else if (remainDays == 0) "今天" else "已结束",
                                        color = if (remainDays in 1..3) Gold else urgencyColor,
                                        fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // === History Banners ===
        if (historyBanners.isNotEmpty()) {
            Text("往期卡池", fontWeight = FontWeight.Bold, color = Color.White,
                modifier = Modifier.padding(16.dp, 8.dp, 16.dp, 4.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.height(100.dp)
            ) {
                items(historyBanners) { banner ->
                    Card(
                        Modifier.width(180.dp).clickable(onClick = onNavigateToBanners),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f))
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text(banner.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("${banner.startDate} ~ ${banner.endDate}", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

// === Quick Entry Card ===
@Composable
fun QuickEntryCard(title: String, description: String, onClick: () -> Unit, tint: Color) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.93f else 1f, spring(0.3f, 20f), label = "qeScale")
    Card(Modifier.width(150.dp).scale(scale).clickable(interactionSource, null, onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = tint.copy(alpha = 0.12f))
    ) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(
                Brush.radialGradient(listOf(tint.copy(alpha = 0.3f), tint.copy(alpha = 0.05f)))),
                contentAlignment = Alignment.Center) {
                Text(title.take(2), color = tint, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Spacer(Modifier.height(10.dp))
            Text(title, color = tint, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(description, color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
        }
    }
}

// === T0 Character Card (animated gradient ring + AsyncImage) ===
@Composable
fun T0CharacterCard(name: String, role: String, imageUrl: String, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "t0brd")
    val borderAngle by infiniteTransition.animateFloat(0f, 360f,
        infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart), "ba")
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.93f else 1f, spring(0.3f, 20f), label = "t0sc")
    val primaryColor = MaterialTheme.colorScheme.primary
    Card(Modifier.width(130.dp).scale(scale).clickable(interactionSource, null, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f))
    ) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(62.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val sweep = Brush.sweepGradient(listOf(Cyan, primaryColor, Gold, Cyan))
                    drawArc(sweep, borderAngle, 360f, false, style = Stroke(width = 3.dp.toPx()))
                }
                Box(Modifier.size(52.dp).clip(CircleShape).background(
                    Brush.linearGradient(listOf(primaryColor, MaterialTheme.colorScheme.secondary))),
                    contentAlignment = Alignment.Center) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUrl)
                            .crossfade(true)
                            .size(52)
                            .build(),
                        contentDescription = name,
                        modifier = Modifier.size(52.dp).clip(CircleShape)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(role, color = MaterialTheme.colorScheme.primary, fontSize = 10.sp)
            Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)) {
                Text("T0", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }
        }
    }
}
