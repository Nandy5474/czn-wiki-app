package com.cznwiki.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cznwiki.app.CznApplication
import com.cznwiki.app.data.LocalDataManager
import com.cznwiki.app.data.database.AppDatabase
import com.cznwiki.app.data.entity.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CharacterDisplayItem(
    val entity: CharacterEntity,
    val effectiveTier: String
)

class CharacterListViewModel(application: Application) : AndroidViewModel(application) {
    private val db = (application as CznApplication).database

    val characters: StateFlow<List<CharacterEntity>> = db.characterDao()
        .getAllCharacters()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val userCollections: StateFlow<Map<Int, String>> = db.userCollectionDao()
        .getOwnedCharacters()
        .map { list -> list.associate { it.characterId to it.customTier } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterElement = MutableStateFlow<String?>(null)
    val filterElement: StateFlow<String?> = _filterElement.asStateFlow()

    private val _filterJob = MutableStateFlow<String?>(null)
    val filterJob: StateFlow<String?> = _filterJob.asStateFlow()

    private val _filterStars = MutableStateFlow<Int?>(null)
    val filterStars: StateFlow<Int?> = _filterStars.asStateFlow()

    @Suppress("UNCHECKED_CAST")
    val filteredCharacters = combine(
        characters, userCollections, searchQuery, filterElement, filterJob, filterStars
    ) { values: Array<Any?> ->
        val chars = values[0] as List<CharacterEntity>
        val customTiers = values[1] as Map<Int, String>
        val query = values[2] as String
        val element = values[3] as String?
        val job = values[4] as String?
        val stars = values[5] as Int?
        chars.filter { c ->
            (query.isBlank() || c.name.contains(query, ignoreCase = true) || c.nameEn.contains(query, ignoreCase = true)) &&
            (element == null || c.element == element) &&
            (job == null || c.job == job) &&
            (stars == null || c.stars == stars)
        }.map { c ->
            val customTier = customTiers[c.id] ?: ""
            CharacterDisplayItem(
                entity = c,
                effectiveTier = customTier.ifBlank { c.tier }
            )
        }.sortedWith(
            compareBy<CharacterDisplayItem> { tierPriority(it.effectiveTier) }
                .thenByDescending { it.entity.stars }
                .thenBy { it.entity.name }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearch(query: String) { _searchQuery.value = query }
    fun updateElementFilter(element: String?) { _filterElement.value = element }
    fun updateJobFilter(job: String?) { _filterJob.value = job }
    fun updateStarsFilter(stars: Int?) { _filterStars.value = stars }

    private fun tierPriority(tier: String): Int = when {
        tier.startsWith("T0") -> 0
        tier.startsWith("T1") -> 1
        tier.startsWith("T2") -> 2
        tier.startsWith("T3") -> 3
        tier.startsWith("T4") -> 4
        tier.isNotBlank() -> 5
        else -> 6
    }

    val elements = listOf("热情", "虚无", "本能", "秩序", "正义")
    val jobs = listOf("决斗家", "先锋", "前锋", "游侠", "猎人", "操控师", "心灵术士", "控制师", "奥义师", "守卫")
    val starOptions = listOf(0, 1, 2, 3, 4, 5, 6)
}

class CharacterDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val db = (application as CznApplication).database
    private val localDataMgr = (application as CznApplication).localDataManager
    private var characterId: Int = 0

    val character: StateFlow<CharacterEntity?> = MutableStateFlow(null)
    val allCards: StateFlow<List<CardEntity>> = MutableStateFlow(emptyList())
    val selfAwareness: StateFlow<List<SelfAwarenessEntity>> = MutableStateFlow(emptyList())
    val collectionStatus: StateFlow<UserCollectionEntity?> = MutableStateFlow(null)
    val allCharacterIds: StateFlow<List<Int>> = MutableStateFlow(emptyList())

    fun loadCharacter(id: Int) {
        characterId = id
        viewModelScope.launch {
            (character as MutableStateFlow).value = db.characterDao().getCharacterById(id)
            (allCards as MutableStateFlow).value = db.cardDao().getAllCardsSync(id)
            (selfAwareness as MutableStateFlow).value = db.selfAwarenessDao().getByCharacterSync(id)
            (collectionStatus as MutableStateFlow).value = db.userCollectionDao().getByCharacterId(id)
            (allCharacterIds as MutableStateFlow).value = db.characterDao().getAllCharacterIdsSync()
        }
    }

    fun updateOwnership(owned: Boolean) {
        viewModelScope.launch {
            val existing = db.userCollectionDao().getByCharacterId(characterId)
            val entity = if (existing != null) {
                existing.copy(owned = owned)
            } else {
                UserCollectionEntity(characterId = characterId, owned = owned)
            }
            db.userCollectionDao().upsert(entity)
            // 持久化到用户修改层
            localDataMgr.saveUserModification(characterId, "owned", owned)
            (collectionStatus as MutableStateFlow).value = db.userCollectionDao().getByCharacterId(characterId)
        }
    }

    fun updateConstellation(value: Int) {
        viewModelScope.launch {
            db.userCollectionDao().updateConstellation(characterId, value)
            localDataMgr.saveUserModification(characterId, "constellation", value)
            (collectionStatus as MutableStateFlow).value = db.userCollectionDao().getByCharacterId(characterId)
        }
    }

    fun updatePotential(value: Int) {
        viewModelScope.launch {
            db.userCollectionDao().updatePotential(characterId, value)
            localDataMgr.saveUserModification(characterId, "potential", value)
            (collectionStatus as MutableStateFlow).value = db.userCollectionDao().getByCharacterId(characterId)
        }
    }

    fun updateCustomTier(value: String) {
        viewModelScope.launch {
            db.userCollectionDao().updateTier(characterId, value)
            localDataMgr.saveUserModification(characterId, "customTier", value)
            (collectionStatus as MutableStateFlow).value = db.userCollectionDao().getByCharacterId(characterId)
        }
    }
}

class CollectionViewModel(application: Application) : AndroidViewModel(application) {
    private val db = (application as CznApplication).database
    private val localDataMgr = (application as CznApplication).localDataManager

    val ownedCharacters: StateFlow<List<UserCollectionEntity>> = db.userCollectionDao()
        .getOwnedCharacters()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val collectionWithCharacters: StateFlow<List<CollectionCharacterData>> =
        db.userCollectionDao().getOwnedCharacters()
            .map { ucList ->
                ucList.mapNotNull { uc ->
                    val char = db.characterDao().getCharacterById(uc.characterId) ?: return@mapNotNull null
                    val sa = db.selfAwarenessDao().getByCharacterSync(uc.characterId)
                    CollectionCharacterData(
                        characterId = char.id,
                        name = char.name,
                        element = char.element,
                        job = char.job,
                        stars = char.stars,
                        tier = char.tier,
                        customTier = uc.customTier,
                        imageUrl = char.imageUrl,
                        thumbUrl = char.thumbUrl,
                        owned = uc.owned,
                        constellation = uc.constellation,
                        potential = uc.potential,
                        partnerId = uc.partnerId,
                        selfAwareness = sa
                    )
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateOwnership(characterId: Int, owned: Boolean) {
        viewModelScope.launch {
            db.userCollectionDao().upsert(UserCollectionEntity(characterId = characterId, owned = owned))
            localDataMgr.saveUserModification(characterId, "owned", owned)
        }
    }

    fun updateConstellation(characterId: Int, value: Int) {
        viewModelScope.launch {
            db.userCollectionDao().updateConstellation(characterId, value)
            localDataMgr.saveUserModification(characterId, "constellation", value)
        }
    }

    fun updatePotential(characterId: Int, value: Int) {
        viewModelScope.launch {
            db.userCollectionDao().updatePotential(characterId, value)
            localDataMgr.saveUserModification(characterId, "potential", value)
        }
    }

    fun updatePartner(characterId: Int, value: Int) {
        viewModelScope.launch {
            db.userCollectionDao().updatePartner(characterId, value)
            localDataMgr.saveUserModification(characterId, "partnerId", value)
        }
    }

    fun updateTier(characterId: Int, value: String) {
        viewModelScope.launch {
            db.userCollectionDao().updateTier(characterId, value)
            localDataMgr.saveUserModification(characterId, "customTier", value)
        }
    }
}

data class CollectionCharacterData(
    val characterId: Int,
    val name: String,
    val element: String,
    val job: String,
    val stars: Int,
    val tier: String,
    val customTier: String,
    val imageUrl: String,
    val thumbUrl: String,
    val owned: Boolean,
    val constellation: Int,
    val potential: Int,
    val partnerId: Int?,
    val selfAwareness: List<SelfAwarenessEntity>
)
