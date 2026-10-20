package com.cznwiki.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cznwiki.app.CznApplication
import com.cznwiki.app.R
import com.cznwiki.app.data.entity.PartnerEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartnerScreen() {
    val context = LocalContext.current
    val db = (context.applicationContext as CznApplication).database
    var partners by remember { mutableStateOf<List<PartnerEntity>>(emptyList()) }

    LaunchedEffect(Unit) {
        partners = db.partnerDao().getAllPartnersSync()
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("伙伴 / 辅战员", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )

        if (partners.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无伙伴数据", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(partners, key = { it.id }) { partner ->
                    PartnerCard(partner)
                }
            }
        }
    }
}

@Composable
fun PartnerCard(partner: PartnerEntity) {
    val imageUrl = partner.thumbUrl.ifBlank { partner.imageUrl }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .placeholder(R.drawable.placeholder_portrait)
                        .error(R.drawable.placeholder_portrait)
                        .crossfade(300)
                        .allowHardware(false)
                        .build(),
                    contentDescription = partner.name,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(partner.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (partner.rarity > 0) {
                            Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)) {
                                Text("${partner.rarity}星", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                    Text("职业: ${partner.className} | 阵营: ${partner.faction}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
            if (partner.passiveName.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text("被动 · ${partner.passiveName}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                if (partner.passiveDesc.isNotBlank()) {
                    Text(partner.passiveDesc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
            }
            if (partner.egoName.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text("自我意识 · ${partner.egoName}${if (partner.egoCost > 0) "（EP ${partner.egoCost}）" else ""}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                if (partner.egoDesc.isNotBlank()) {
                    Text(partner.egoDesc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
            }
        }
    }
}
