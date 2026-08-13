package com.cznwiki.app.data.entity

import androidx.room.*
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

/**
 * 解析 characters.json 为 CharacterEntity 列表，并对缺失/为 null 的字段做兜底清洗。
 *
 * 背景：Gson 反序列化 Kotlin data class 时，若 JSON 缺少某字段（如远程 OTA 数据
 * 缺 tier/role/nameEn/egoSkill），会通过 Unsafe 直接写入 null，绕过 Kotlin 非空
 * 类型检查，导致 Room 非空列被写入 NULL，读取时 DAO 映射崩溃，图鉴/收藏显示为空。
 * 这里改用 JsonObject 逐字段取值，缺失一律回退默认值，从源头杜绝 NULL 污染。
 */
fun parseCharacters(json: String, gson: Gson): List<CharacterEntity> {
    val arr: JsonArray = try {
        gson.fromJson(json, JsonArray::class.java)
    } catch (e: Exception) {
        return emptyList()
    } ?: return emptyList()

    return arr.mapNotNull { el ->
        if (!el.isJsonObject) return@mapNotNull null
        val o: JsonObject = el.asJsonObject

        fun str(key: String): String =
            o.get(key)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString ?: ""
        fun int(key: String): Int =
            o.get(key)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }?.asInt ?: 0

        CharacterEntity(
            id = int("id"),
            name = str("name"),
            nameEn = str("nameEn"),
            stars = int("stars"),
            element = str("element"),
            job = str("class"),
            faction = str("faction"),
            rarity = str("rarity"),
            cv = str("cv"),
            race = str("race"),
            birthday = str("birthday"),
            ability = str("ability"),
            description = str("description"),
            tier = str("tier"),
            role = str("role"),
            baseAtk = int("baseAtk"),
            baseDef = int("baseDef"),
            baseHp = int("baseHp"),
            imageUrl = str("imageUrl"),
            thumbUrl = str("thumbUrl"),
            egoSkill = str("egoSkill")
        )
    }
}

@Entity(tableName = "characters")
data class CharacterEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val nameEn: String = "",
    val stars: Int = 0,
    val element: String = "",
    @SerializedName("class")
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
    val thumbUrl: String = "",
    val egoSkill: String = ""
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
    val character: String = "",
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
