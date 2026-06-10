package com.cznwiki.app.data.entity

import androidx.room.*

@Entity(tableName = "characters")
data class CharacterEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val nameEn: String = "",
    val stars: Int = 0,
    val element: String = "",
    val job: String = "",
    val faction: String = "",
    val rarity: String = "",
    val cv: String = "",
    val race: String = "",
    val birthday: String = "",
    val ability: String = "",
    val description: String = "",
    val tier: String = "",
    val role: String = "",
    val baseAtk: Int = 0,
    val baseDef: Int = 0,
    val baseHp: Int = 0,
    val imageUrl: String = "",
    val thumbUrl: String = ""
)

@Entity(tableName = "cards")
data class CardEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val characterId: Int,
    val name: String,
    val type: String = "",
    val cost: Int = 0,
    val effect: String = "",
    val isUnique: Boolean = false,
    val isRetain: Boolean = false,
    val isQuick: Boolean = false,
    val isBless: Boolean = false,
    val hasEpiphany: Boolean = false,
    val sortOrder: Int = 0
)

@Entity(tableName = "self_awareness")
data class SelfAwarenessEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val characterId: Int,
    val stage: Int,
    val name: String = "",
    val effect: String = ""
)

@Entity(tableName = "user_collection")
data class UserCollectionEntity(
    @PrimaryKey val characterId: Int,
    val owned: Boolean = false,
    val constellation: Int = 0,
    val potential: Int = 0,
    val partnerId: Int? = null,
    val customTier: String = ""
)

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val description: String = "",
    val endDate: String = "",
    val url: String = "",
    val server: String = ""
)

@Entity(tableName = "banners")
data class BannerEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val stars: Int = 0,
    val element: String = "",
    val className: String = "",
    val type: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val url: String = "",
    val server: String = ""
)

@Entity(tableName = "teams")
data class TeamEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String = "",
    val member1Id: Int? = null,
    val member2Id: Int? = null,
    val member3Id: Int? = null,
    val createdAt: Long = System.currentTimeMillis()
)
