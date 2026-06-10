package com.cznwiki.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cznwiki.app.CznApplication
import com.cznwiki.app.data.entity.BannerEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BannersScreen() {
    val context = LocalContext.current
    val db = (context.applicationContext as CznApplication).database
    var currentBanners by remember { mutableStateOf<List<BannerEntity>>(emptyList()) }
    var historyBanners by remember { mutableStateOf<List<BannerEntity>>(emptyList()) }

    LaunchedEffect(Unit) {
        currentBanners = db.bannerDao().getAllBannersSync().filter { it.type == "current" }
        historyBanners = db.bannerDao().getAllBannersSync().filter { it.type == "history" }
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("卡池一览", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )

        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Current Banners
            if (currentBanners.isNotEmpty()) {
                item {
                    Text("当期卡池", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                }
                items(currentBanners) { banner ->
                    BannerDetailCard(banner, isCurrent = true)
                }
            }

            // History Banners
            if (historyBanners.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text("往期卡池", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                }
                items(historyBanners) { banner ->
                    BannerDetailCard(banner, isCurrent = false)
                }
            }

            if (currentBanners.isEmpty() && historyBanners.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("暂无卡池数据", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    }
                }
            }
        }
    }
}

@Composable
fun BannerDetailCard(banner: BannerEntity, isCurrent: Boolean) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val endDate = try { dateFormat.parse(banner.endDate) } catch (_: Exception) { null }
    val remainDays = if (isCurrent && endDate != null) {
        ((endDate.time - System.currentTimeMillis()) / (1000 * 60 * 60 * 24) + 1).toInt()
    } else -1

    val urgencyColor = when {
        remainDays <= 3 -> MaterialTheme.colorScheme.error
        remainDays <= 7 -> androidx.compose.ui.graphics.Color(0xFFFF9800)
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isCurrent) 0.6f else 0.3f))
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(banner.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)) {
                    Text("${banner.stars}星", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
            if (banner.className.isNotBlank() || banner.element.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                val tags = listOfNotNull(
                    banner.className.takeIf { it.isNotBlank() },
                    banner.element.takeIf { it.isNotBlank() },
                    binderIn(banner.type).takeIf { it.isNotBlank() }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    tags.forEach { tag ->
                        Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)) {
                            Text(tag, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("${banner.startDate} ~ ${banner.endDate}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            if (isCurrent && remainDays > 0) {
                Spacer(Modifier.height(8.dp))
                Surface(shape = RoundedCornerShape(6.dp), color = urgencyColor.copy(alpha = 0.15f)) {
                    Text("剩余 ${remainDays} 天", style = MaterialTheme.typography.labelMedium, color = urgencyColor, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                }
            }
        }
    }
}

private fun binderIn(type: String): String = when (type) {
    "current" -> "当期UP"
    "history" -> "往期"
    else -> ""
}