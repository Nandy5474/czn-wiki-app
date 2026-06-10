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
import com.cznwiki.app.data.entity.EventEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen() {
    val context = LocalContext.current
    val db = (context.applicationContext as CznApplication).database
    var events by remember { mutableStateOf<List<EventEntity>>(emptyList()) }

    LaunchedEffect(Unit) {
        events = db.eventDao().getAllEventsSync()
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("最新活动", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )

        if (events.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无活动数据", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(events) { event ->
                    EventDetailCard(event)
                }
            }
        }
    }
}

@Composable
fun EventDetailCard(event: EventEntity) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val endDate = try { dateFormat.parse(event.endDate) } catch (_: Exception) { null }
    val remainDays = if (endDate != null) ((endDate.time - System.currentTimeMillis()) / (1000 * 60 * 60 * 24) + 1).toInt() else -1
    val urgencyColor = when {
        remainDays <= 0 -> MaterialTheme.colorScheme.error
        remainDays <= 3 -> MaterialTheme.colorScheme.error
        remainDays <= 7 -> androidx.compose.ui.graphics.Color(0xFFFF9800)
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(event.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Surface(shape = RoundedCornerShape(8.dp), color = urgencyColor.copy(alpha = 0.15f)) {
                    Text(
                        when {
                            remainDays > 0 -> "剩余 ${remainDays} 天"
                            remainDays == 0 -> "今日结束"
                            else -> "已结束"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = urgencyColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            if (event.description.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(event.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
            Spacer(Modifier.height(12.dp))
            Text("结束日期: ${event.endDate}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
    }
}