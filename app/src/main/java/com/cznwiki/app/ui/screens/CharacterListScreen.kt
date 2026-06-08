package com.cznwiki.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.cznwiki.app.R
import com.cznwiki.app.ui.viewmodel.CharacterDisplayItem
import com.cznwiki.app.ui.viewmodel.CharacterListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterListScreen(
    onNavigateToDetail: (Int) -> Unit,
    viewModel: CharacterListViewModel = viewModel()
) {
    val characters by viewModel.filteredCharacters.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filterElement by viewModel.filterElement.collectAsState()
    val filterJob by viewModel.filterJob.collectAsState()
    val filterStars by viewModel.filterStars.collectAsState()
    var isGridView by remember { mutableStateOf(true) }
    var showFilterSheet by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top bar
        TopAppBar(
            title = { Text("角色图鉴") },
            actions = {
                IconButton(onClick = { isGridView = !isGridView }) {
                    Icon(
                        if (isGridView) Icons.Default.List else Icons.Default.GridView,
                        contentDescription = "切换视图"
                    )
                }
                IconButton(onClick = { showFilterSheet = true }) {
                    Text("筛选", style = MaterialTheme.typography.labelLarge)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                actionIconContentColor = MaterialTheme.colorScheme.onSurface,
            )
        )

        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.updateSearch(it) },
            placeholder = { Text("搜索角色名称...") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Active filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filterElement?.let {
                FilterChip(selected = true, onClick = { viewModel.updateElementFilter(null) }, label = { Text(it) })
            }
            filterJob?.let {
                FilterChip(selected = true, onClick = { viewModel.updateJobFilter(null) }, label = { Text(it) })
            }
            filterStars?.let {
                FilterChip(selected = true, onClick = { viewModel.updateStarsFilter(null) }, label = { Text("${it}星") })
            }
        }

        // Character grid/list
        if (isGridView) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(characters, key = { it.entity.id }) { item ->
                    CharacterGridItem(item = item, onClick = { onNavigateToDetail(item.entity.id) })
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(characters, key = { it.entity.id }) { item ->
                    CharacterListItem(item = item, onClick = { onNavigateToDetail(item.entity.id) })
                }
            }
        }
    }

    // Filter bottom sheet
    if (showFilterSheet) {
        FilterSheet(
            elements = viewModel.elements,
            jobs = viewModel.jobs,
            starOptions = viewModel.starOptions,
            selectedElement = filterElement,
            selectedJob = filterJob,
            selectedStars = filterStars,
            onElementSelected = { viewModel.updateElementFilter(it) },
            onJobSelected = { viewModel.updateJobFilter(it) },
            onStarsSelected = { viewModel.updateStarsFilter(it) },
            onDismiss = { showFilterSheet = false }
        )
    }
}

@Composable
fun CharacterGridItem(item: CharacterDisplayItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.entity.thumbUrl.ifBlank { item.entity.imageUrl })
                    .placeholder(R.drawable.placeholder_portrait)
                    .error(R.drawable.placeholder_portrait)
                    .crossfade(300)
                    .allowHardware(false)
                    .build(),
                contentDescription = item.entity.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(300f / 462f)
                    .clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                item.entity.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (item.effectiveTier.isNotBlank()) {
                Text(
                    item.effectiveTier,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun CharacterListItem(item: CharacterDisplayItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.entity.thumbUrl.ifBlank { item.entity.imageUrl })
                    .placeholder(R.drawable.placeholder_portrait)
                    .error(R.drawable.placeholder_portrait)
                    .crossfade(300)
                    .allowHardware(false)
                    .build(),
                contentDescription = item.entity.name,
                modifier = Modifier
                    .height(72.dp)
                    .aspectRatio(300f / 462f)
                    .clip(MaterialTheme.shapes.small),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.entity.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                if (item.entity.element.isNotBlank() || item.entity.job.isNotBlank()) {
                    Text(
                        "${item.entity.element} ${item.entity.job}".trim(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            if (item.effectiveTier.isNotBlank()) {
                AssistChip(
                    onClick = {},
                    label = { Text(item.effectiveTier, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSheet(
    elements: List<String>,
    jobs: List<String>,
    starOptions: List<Int>,
    selectedElement: String?,
    selectedJob: String?,
    selectedStars: Int?,
    onElementSelected: (String?) -> Unit,
    onJobSelected: (String?) -> Unit,
    onStarsSelected: (Int?) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("按属性", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                elements.forEach { element ->
                    FilterChip(
                        selected = selectedElement == element,
                        onClick = { onElementSelected(if (selectedElement == element) null else element) },
                        label = { Text(element) }
                    )
                }
            }

            Text("按职业", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                jobs.forEach { job ->
                    FilterChip(
                        selected = selectedJob == job,
                        onClick = { onJobSelected(if (selectedJob == job) null else job) },
                        label = { Text(job) }
                    )
                }
            }

            Text("按星级", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                starOptions.forEach { star ->
                    FilterChip(
                        selected = selectedStars == star,
                        onClick = { onStarsSelected(if (selectedStars == star) null else star) },
                        label = { Text("${star}星") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
