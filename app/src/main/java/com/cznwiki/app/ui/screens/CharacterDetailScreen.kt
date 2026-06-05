package com.cznwiki.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cznwiki.app.R
import com.cznwiki.app.ui.viewmodel.CharacterDetailViewModel
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterDetailScreen(
    characterId: Int,
    onNavigateBack: () -> Unit
) {
    val viewModel: CharacterDetailViewModel = viewModel()
    LaunchedEffect(characterId) {
        viewModel.loadCharacter(characterId)
    }

    val character by viewModel.character.collectAsState()
    val cards by viewModel.allCards.collectAsState()
    val selfAwareness by viewModel.selfAwareness.collectAsState()
    val collectionStatus by viewModel.collectionStatus.collectAsState()

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(character) {
        visible = character != null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                character?.let { char ->
                    // === Hero Image ===
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(300f / 462f)
                    ) {
                        // Character image full width
                        AsyncImage(
                            model = if (char.imageUrl.isNotBlank()) {
                                ImageRequest.Builder(LocalContext.current)
                                    .data(char.imageUrl)
                                    .placeholder(R.drawable.placeholder_portrait)
                                    .error(R.drawable.placeholder_portrait)
                                    .crossfade(300)
                                    .allowHardware(false)
                                    .build()
                            } else {
                                R.drawable.placeholder_portrait
                            },
                            contentDescription = char.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        // Gradient overlay (transparent -> background)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.Transparent,
                                            MaterialTheme.colorScheme.background.copy(alpha = 0.3f),
                                            MaterialTheme.colorScheme.background
                                        ),
                                        startY = 0f,
                                        endY = Float.POSITIVE_INFINITY
                                    )
                                )
                        )

                        // Character name + element/job tags on gradient
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(20.dp)
                        ) {
                            Text(
                                char.name,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (char.element.isNotBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                    ) {
                                        Text(
                                            char.element,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                if (char.job.isNotBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
                                    ) {
                                        Text(
                                            char.job,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                if (char.stars > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFFFD700).copy(alpha = 0.7f)
                                    ) {
                                        Text(
                                            "★".repeat(char.stars),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // === Section 1: Basic Info ===
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("基本信息", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            if (char.tier.isNotBlank()) {
                                Text("梯度: ${char.tier}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            if (char.role.isNotBlank()) {
                                Text("定位: ${char.role}", style = MaterialTheme.typography.bodySmall)
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            if (char.ability.isNotBlank()) {
                                Text("特性: ${char.ability}", style = MaterialTheme.typography.bodySmall)
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            if (char.description.isNotBlank()) {
                                Text(char.description, style = MaterialTheme.typography.bodySmall)
                            }
                            if (char.race.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("种族: ${char.race}", style = MaterialTheme.typography.bodySmall)
                            }
                            if (char.birthday.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("生日: ${char.birthday}", style = MaterialTheme.typography.bodySmall)
                            }
                            if (char.cv.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("CV: ${char.cv}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    // === Section 2: Attribute Radar Chart ===
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("属性雷达图", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(12.dp))
                            AttributeRadarChart(
                                atk = char.baseAtk,
                                def = char.baseDef,
                                hp = char.baseHp,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // === Section 3: Card List ===
                    val baseCards = cards.filter { it.type == "基础卡牌" }
                    val uniqueCards = cards.filter { it.type == "独特卡牌" }

                    if (baseCards.isNotEmpty() || uniqueCards.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                if (baseCards.isNotEmpty()) {
                                    Text("基础卡牌", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    baseCards.forEach { card ->
                                        CardItem(card)
                                        Spacer(modifier = Modifier.height(6.dp))
                                    }
                                }
                                if (uniqueCards.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("独特卡牌", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    uniqueCards.forEach { card ->
                                        CardItem(card)
                                        Spacer(modifier = Modifier.height(6.dp))
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // === Section 4: Epiphany Cards ===
                    val epiphanyCards = cards.filter { it.type == "灵光一闪" }
                    if (epiphanyCards.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("灵光一闪", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                epiphanyCards.forEach { card ->
                                    CardItem(card)
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // === Section 5: Self Awareness ===
                    if (selfAwareness.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("自我意识", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                selfAwareness.forEach { sa ->
                                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                        Text(
                                            "E${sa.stage}${if (sa.name.isNotBlank()) " - ${sa.name}" else ""}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(sa.effect, style = MaterialTheme.typography.bodySmall)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // === Section 6: Collection Management ===
                    var collExpanded by remember { mutableStateOf(false) }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clickable { collExpanded = !collExpanded }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("收藏管理", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Icon(
                                    if (collExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (collExpanded) "收起" else "展开"
                                )
                            }

                            if (collExpanded) {
                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(12.dp))

                                // Ownership switch
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                    Text("已拥有", modifier = Modifier.weight(1f))
                                    Switch(
                                        checked = collectionStatus?.owned == true,
                                        onCheckedChange = { viewModel.updateOwnership(it) }
                                    )
                                }

                                if (collectionStatus?.owned == true) {
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

                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("命座等级: E${collectionStatus?.constellation ?: 0}", style = MaterialTheme.typography.bodyMedium)
                                    Slider(
                                        value = (collectionStatus?.constellation ?: 0).toFloat(),
                                        onValueChange = { viewModel.updateConstellation(it.toInt()) },
                                        valueRange = 0f..6f,
                                        steps = 5
                                    )

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
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                } ?: run {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("加载中...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun AttributeRadarChart(
    atk: Int,
    def: Int,
    hp: Int,
) {
    // Normalize values to 0f-1f range
    val maxAtk = 200f
    val maxDef = 200f
    val maxHp = 5000f

    val values = listOf(
        (atk / maxAtk).coerceIn(0f, 1f),       // 攻击
        (def / maxDef).coerceIn(0f, 1f),       // 防御
        (hp / maxHp).coerceIn(0f, 1f),         // 生命
        0.5f,                                    // 速度 (default)
        0.5f,                                    // 暴击 (default)
        0.5f,                                     // 能量 (default)
    )

    val labels = listOf("攻击", "防御", "生命", "速度", "暴击", "能量")

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(
            modifier = Modifier.size(220.dp)
        ) {
            drawRadarChart(values)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            labels.forEach { label ->
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        }
    }
}

fun DrawScope.drawRadarChart(values: List<Float>) {
    val centerX = size.width / 2f
    val centerY = size.height / 2f
    val radius = size.minDimension / 2.5f
    val sides = values.size

    val primaryColor = Color(0xFF9C7CF4)
    val gridColor = Color.White.copy(alpha = 0.15f)

    // Draw grid (5 levels)
    for (level in 1..5) {
        val r = radius * level / 5f
        val path = Path()
        for (i in 0 until sides) {
            val angle = (Math.PI / 2) + (2 * Math.PI / sides) * i - Math.PI / 2
            val x = centerX + r * cos(angle).toFloat()
            val y = centerY + r * sin(angle).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        drawPath(path, color = gridColor, style = Stroke(width = 1f))
    }

    // Draw axis lines
    for (i in 0 until sides) {
        val angle = (Math.PI / 2) + (2 * Math.PI / sides) * i - Math.PI / 2
        drawLine(
            color = gridColor,
            start = Offset(centerX, centerY),
            end = Offset(
                centerX + radius * cos(angle).toFloat(),
                centerY + radius * sin(angle).toFloat()
            ),
            strokeWidth = 1f
        )
    }

    // Draw data polygon
    val dataPath = Path()
    for (i in 0 until sides) {
        val angle = (Math.PI / 2) + (2 * Math.PI / sides) * i - Math.PI / 2
        val r = radius * values[i]
        val x = centerX + r * cos(angle).toFloat()
        val y = centerY + r * sin(angle).toFloat()
        if (i == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
    }
    dataPath.close()
    drawPath(dataPath, color = primaryColor.copy(alpha = 0.3f))
    drawPath(dataPath, color = primaryColor, style = Stroke(width = 2f))

    // Draw data points
    for (i in 0 until sides) {
        val angle = (Math.PI / 2) + (2 * Math.PI / sides) * i - Math.PI / 2
        val r = radius * values[i]
        val x = centerX + r * cos(angle).toFloat()
        val y = centerY + r * sin(angle).toFloat()
        drawCircle(
            color = primaryColor,
            radius = 4f,
            center = Offset(x, y)
        )
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
