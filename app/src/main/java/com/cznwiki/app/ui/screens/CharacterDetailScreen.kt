package com.cznwiki.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.cznwiki.app.ui.viewmodel.CharacterDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterDetailScreen(
    characterId: Int,
    onNavigateBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("基本信息", "卡牌列表", "灵光一闪", "自我意识")

    val viewModel: CharacterDetailViewModel = viewModel()
    LaunchedEffect(characterId) {
        viewModel.loadCharacter(characterId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("角色详情") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> BasicInfoTab(viewModel)
                1 -> CardListTab(viewModel)
                2 -> EpiphanyTab(viewModel)
                3 -> SelfAwarenessTab(viewModel)
            }
        }
    }
}

@Composable
fun BasicInfoTab(viewModel: CharacterDetailViewModel) {
    val character by viewModel.character.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        character?.let { char ->
            // Character portrait
            val portraitUrl = char.thumbUrl.ifBlank { char.imageUrl }
            AsyncImage(
                model = if (portraitUrl.isNotBlank()) {
                    ImageRequest.Builder(LocalContext.current)
                        .data(portraitUrl)
                        .placeholder(R.drawable.placeholder_portrait)
                        .error(R.drawable.placeholder_portrait)
                        .crossfade(300)
                        .allowHardware(false)
                        .size(800)
                        .build()
                } else {
                    R.drawable.placeholder_portrait
                },
                contentDescription = char.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(12.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(char.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${char.element} | ${char.job} | ${"★".repeat(char.stars)}", style = MaterialTheme.typography.bodyMedium)
                    if (char.tier.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("梯度: ${char.tier}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                    if (char.role.isNotBlank()) {
                        Text("定位: ${char.role}", style = MaterialTheme.typography.bodySmall)
                    }
                    if (char.ability.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("特性: ${char.ability}", style = MaterialTheme.typography.bodySmall)
                    }
                    if (char.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(char.description, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("基础属性", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoRow("攻击力", "${char.baseAtk}")
                    InfoRow("防御力", "${char.baseDef}")
                    InfoRow("生命值", "${char.baseHp}")
                    if (char.race.isNotBlank()) InfoRow("种族", char.race)
                    if (char.birthday.isNotBlank()) InfoRow("生日", char.birthday)
                    if (char.cv.isNotBlank()) InfoRow("CV", char.cv)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 收藏管理卡片
            val collectionStatus by viewModel.collectionStatus.collectAsState()

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("收藏管理", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    // 拥有开关
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("已拥有", modifier = Modifier.weight(1f))
                        Switch(
                            checked = collectionStatus?.owned == true,
                            onCheckedChange = { viewModel.updateOwnership(it) }
                        )
                    }

                    if (collectionStatus?.owned == true) {
                        // 自定义强度
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("强度评级 (自定义)", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("T0", "T1", "T2", "T3", "T4").forEach { tier ->
                                FilterChip(
                                    selected = collectionStatus?.customTier == tier,
                                    onClick = { viewModel.updateCustomTier(if (collectionStatus?.customTier == tier) "" else tier) },
                                    label = { Text(tier) }
                                )
                            }
                        }

                        // 命座/星座等级
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("命座等级: E${collectionStatus?.constellation ?: 0}", style = MaterialTheme.typography.bodyMedium)
                        Slider(
                            value = (collectionStatus?.constellation ?: 0).toFloat(),
                            onValueChange = { viewModel.updateConstellation(it.toInt()) },
                            valueRange = 0f..6f,
                            steps = 5
                        )

                        // 潜能等级
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("潜能等级: ${collectionStatus?.potential ?: 0}", style = MaterialTheme.typography.bodyMedium)
                        Slider(
                            value = (collectionStatus?.potential ?: 0).toFloat(),
                            onValueChange = { viewModel.updatePotential(it.toInt()) },
                            valueRange = 0f..6f,
                            steps = 5
                        )
                    }
                }
            }
        } ?: run {
            Text("加载中...", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun CardListTab(viewModel: CharacterDetailViewModel) {
    val cards by viewModel.allCards.collectAsState()
    val baseCards = cards.filter { it.type == "基础卡牌" }
    val uniqueCards = cards.filter { it.type == "独特卡牌" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        if (baseCards.isNotEmpty()) {
            Text("基础卡牌", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            baseCards.forEach { card ->
                CardItem(card)
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        if (uniqueCards.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("独特卡牌", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            uniqueCards.forEach { card ->
                CardItem(card)
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        if (cards.isEmpty()) {
            Text("暂无卡牌数据", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun EpiphanyTab(viewModel: CharacterDetailViewModel) {
    val cards by viewModel.allCards.collectAsState()
    val epiphanyCards = cards.filter { it.type == "灵光一闪" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("灵光一闪卡牌", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        if (epiphanyCards.isNotEmpty()) {
            epiphanyCards.forEach { card ->
                CardItem(card)
                Spacer(modifier = Modifier.height(6.dp))
            }
        } else {
            Text("暂无灵光一闪数据", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun SelfAwarenessTab(viewModel: CharacterDetailViewModel) {
    val stages by viewModel.selfAwareness.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("自我意识", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        if (stages.isNotEmpty()) {
            stages.forEach { sa ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "E${sa.stage}${if (sa.name.isNotBlank()) " - ${sa.name}" else ""}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(sa.effect, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
        } else {
            Text("暂无命座数据", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun CardItem(card: com.cznwiki.app.data.entity.CardEntity) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(card.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    if (card.type.isNotBlank()) {
                        Text(card.type, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
                if (card.effect.isNotBlank()) {
                    Text(card.effect, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (card.cost >= 0) {
                Text("${card.cost}费", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
