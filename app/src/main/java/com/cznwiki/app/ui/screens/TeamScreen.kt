package com.cznwiki.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cznwiki.app.CznApplication
import com.cznwiki.app.data.entity.CharacterEntity
import com.cznwiki.app.data.entity.TeamEntity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamScreen(onNavigateToDetail: (Int) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = (context.applicationContext as CznApplication).database
    val teamDao = db.teamDao()
    val characterDao = db.characterDao()

    var teams by remember { mutableStateOf<List<TeamEntity>>(emptyList()) }
    var allCharacters by remember { mutableStateOf<List<CharacterEntity>>(emptyList()) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<TeamEntity?>(null) }
    var newTeamName by remember { mutableStateOf("") }
    var selectedMember1 by remember { mutableIntStateOf(-1) }
    var selectedMember2 by remember { mutableIntStateOf(-1) }
    var selectedMember3 by remember { mutableIntStateOf(-1) }
    var editingTeam by remember { mutableStateOf<TeamEntity?>(null) }

    LaunchedEffect(Unit) {
        teams = teamDao.getAllTeamsSync()
        allCharacters = characterDao.getAllCharacterIdsSync().let { ids ->
            ids.mapNotNull { characterDao.getCharacterById(it) }
        }
    }

    fun refreshTeams() {
        scope.launch {
            teams = teamDao.getAllTeamsSync()
        }
    }

    fun getCharName(id: Int?): String {
        if (id == null) return "未选择"
        return allCharacters.find { it.id == id }?.name ?: "未知角色"
    }

    fun getCharElement(id: Int?): String {
        if (id == null) return ""
        return allCharacters.find { it.id == id }?.element ?: ""
    }

    Column(Modifier.fillMaxSize()) {
        // Header
        TopAppBar(
            title = { Text("队伍构筑", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )

        // Create button
        Button(
            onClick = {
                newTeamName = ""
                selectedMember1 = -1; selectedMember2 = -1; selectedMember3 = -1
                editingTeam = null
                showCreateDialog = true
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Add, "创建队伍")
            Spacer(Modifier.width(8.dp))
            Text("创建新队伍")
        }

        if (teams.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("暂无队伍", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Spacer(Modifier.height(8.dp))
                    Text("点击上方按钮创建你的第一个队伍", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                }
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(teams, key = { it.id }) { team ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                Text(team.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Row {
                                    IconButton(onClick = {
                                        newTeamName = team.name
                                        selectedMember1 = team.member1Id ?: -1
                                        selectedMember2 = team.member2Id ?: -1
                                        selectedMember3 = team.member3Id ?: -1
                                        editingTeam = team
                                        showCreateDialog = true
                                    }) {
                                        Icon(Icons.Default.Edit, "编辑", tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(onClick = { showDeleteDialog = team }) {
                                        Icon(Icons.Default.Delete, "删除", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                            if (team.description.isNotBlank()) {
                                Text(team.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                Spacer(Modifier.height(8.dp))
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                listOf(team.member1Id to "主力", team.member2Id to "副手", team.member3Id to "支援").forEach { (id, label) ->
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(Modifier.size(48.dp).clip(CircleShape).background(
                                            if (id != null) Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))
                                            else Brush.linearGradient(listOf(Color.Gray, Color.LightGray))
                                        ), contentAlignment = Alignment.Center) {
                                            Text(
                                                if (id != null) getCharName(id).take(1) else "?",
                                                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White
                                            )
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text(getCharName(id), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
                                        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Create / Edit Dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text(if (editingTeam != null) "编辑队伍" else "创建新队伍") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newTeamName,
                        onValueChange = { newTeamName = it },
                        label = { Text("队伍名称") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Text("选择三位角色", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

                    // Member 1
                    TeamMemberSelector("主力角色", allCharacters, selectedMember1, onSelect = { selectedMember1 = it }, excludeIds = listOfNotNull(
                        selectedMember2.takeIf { it > 0 }, selectedMember3.takeIf { it > 0 }
                    ))
                    TeamMemberSelector("副手角色", allCharacters, selectedMember2, onSelect = { selectedMember2 = it }, excludeIds = listOfNotNull(
                        selectedMember1.takeIf { it > 0 }, selectedMember3.takeIf { it > 0 }
                    ))
                    TeamMemberSelector("支援角色", allCharacters, selectedMember3, onSelect = { selectedMember3 = it }, excludeIds = listOfNotNull(
                        selectedMember1.takeIf { it > 0 }, selectedMember2.takeIf { it > 0 }
                    ))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newTeamName.isNotBlank()) {
                            scope.launch {
                                if (editingTeam != null) {
                                    teamDao.update(editingTeam!!.copy(
                                        name = newTeamName,
                                        member1Id = selectedMember1.takeIf { it > 0 },
                                        member2Id = selectedMember2.takeIf { it > 0 },
                                        member3Id = selectedMember3.takeIf { it > 0 }
                                    ))
                                } else {
                                    teamDao.insert(TeamEntity(
                                        name = newTeamName,
                                        member1Id = selectedMember1.takeIf { it > 0 },
                                        member2Id = selectedMember2.takeIf { it > 0 },
                                        member3Id = selectedMember3.takeIf { it > 0 }
                                    ))
                                }
                                showCreateDialog = false
                                refreshTeams()
                            }
                        }
                    },
                    enabled = newTeamName.isNotBlank()
                ) { Text(if (editingTeam != null) "保存" else "创建") }
            },
            dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text("取消") } }
        )
    }

    // Delete Confirmation
    showDeleteDialog?.let { team ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("删除队伍") },
            text = { Text("确定要删除队伍「${team.name}」吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        teamDao.deleteById(team.id)
                        showDeleteDialog = null
                        refreshTeams()
                    }
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = null }) { Text("取消") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamMemberSelector(
    label: String,
    allCharacters: List<CharacterEntity>,
    selectedId: Int,
    onSelect: (Int) -> Unit,
    excludeIds: List<Int> = emptyList()
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedChar = allCharacters.find { it.id == selectedId }
    val availableChars = allCharacters.filter { it.id !in excludeIds }

    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                value = if (selectedChar != null) "${selectedChar.name} (${selectedChar.element})" else "选择角色",
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                singleLine = true
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                availableChars.forEach { char ->
                    DropdownMenuItem(
                        text = { Text("${char.name} (${char.element} · ${char.job})") },
                        onClick = {
                            onSelect(char.id)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}