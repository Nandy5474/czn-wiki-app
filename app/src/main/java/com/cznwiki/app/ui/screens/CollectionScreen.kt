package com.cznwiki.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cznwiki.app.R
import com.cznwiki.app.data.entity.SelfAwarenessEntity
import com.cznwiki.app.ui.viewmodel.CollectionCharacterData
import com.cznwiki.app.ui.viewmodel.CollectionViewModel

@Composable
fun CollectionScreen(
    onNavigateToDetail: (Int) -> Unit
) {
    val viewModel: CollectionViewModel = viewModel()
    val collectionData by viewModel.collectionWithCharacters.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        Text(
            text = "我的收藏",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onBackground
        )

        if (collectionData.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Card(
                    modifier = Modifier.padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "暂无收藏角色",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "在角色详情页点击「已拥有」即可添加到收藏",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(collectionData, key = { it.characterId }) { data ->
                    CollectionCharacterCard(
                        data = data,
                        onTierChange = { viewModel.updateTier(data.characterId, it) },
                        onConstellationChange = { viewModel.updateConstellation(data.characterId, it) },
                        onPotentialChange = { viewModel.updatePotential(data.characterId, it) },
                        onClick = { onNavigateToDetail(data.characterId) }
                    )
                }
            }
        }
    }
}

@Composable
fun CollectionCharacterCard(
    data: CollectionCharacterData,
    onTierChange: (String) -> Unit,
    onConstellationChange: (Int) -> Unit,
    onPotentialChange: (Int) -> Unit,
    onClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(data.thumbUrl.ifBlank { data.imageUrl })
                        .placeholder(R.drawable.placeholder_portrait)
                        .error(R.drawable.placeholder_portrait)
                        .crossfade(300)
                        .allowHardware(false)
                        .size(72)
                        .build(),
                    contentDescription = data.name,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(MaterialTheme.shapes.small),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        data.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "${data.element} | ${data.job} | E${data.constellation}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                val displayTier = data.customTier.ifBlank { data.tier }
                if (displayTier.isNotBlank()) {
                    AssistChip(
                        onClick = { expanded = !expanded },
                        label = { Text(displayTier) }
                    )
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(8.dp))

                Text("强度评级", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("T0", "T1", "T2", "T3", "T4").forEach { t ->
                        FilterChip(
                            selected = data.customTier == t,
                            onClick = { onTierChange(if (data.customTier == t) "" else t) },
                            label = { Text(t, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("命座等级: E${data.constellation}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                Slider(
                    value = data.constellation.toFloat(),
                    onValueChange = { onConstellationChange(it.toInt()) },
                    valueRange = 0f..6f,
                    steps = 5,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))
                Text("潜能等级: ${data.potential}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                Slider(
                    value = data.potential.toFloat(),
                    onValueChange = { onPotentialChange(it.toInt()) },
                    valueRange = 0f..6f,
                    steps = 5,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                    )
                )

                if (data.selfAwareness.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "自我意识",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    data.selfAwareness.forEach { sa ->
                        Text(
                            "E${sa.stage} ${sa.name}: ${sa.effect}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
