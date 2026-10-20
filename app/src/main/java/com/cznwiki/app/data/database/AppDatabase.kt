package com.cznwiki.app.data.database

import android.content.Context
import android.util.Log
import androidx.room.*
import androidx.room.migration.Migration
import com.cznwiki.app.data.entity.*
import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.reflect.TypeToken

@Database(
    entities = [
        CharacterEntity::class, CardEntity::class, SelfAwarenessEntity::class,
        UserCollectionEntity::class, EventEntity::class, BannerEntity::class,
        TeamEntity::class, PartnerEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun characterDao(): CharacterDao
    abstract fun cardDao(): CardDao
    abstract fun selfAwarenessDao(): SelfAwarenessDao
    abstract fun userCollectionDao(): UserCollectionDao
    abstract fun eventDao(): EventDao
    abstract fun bannerDao(): BannerDao
    abstract fun teamDao(): TeamDao
    abstract fun partnerDao(): PartnerDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `partners` (`id` TEXT NOT NULL, " +
                        "`name` TEXT NOT NULL, `nameEn` TEXT NOT NULL, `slug` TEXT NOT NULL, " +
                        "`rarity` INTEGER NOT NULL, `className` TEXT NOT NULL, `classEn` TEXT NOT NULL, " +
                        "`faction` TEXT NOT NULL, `factionEn` TEXT NOT NULL, `stats` TEXT NOT NULL, " +
                        "`passiveName` TEXT NOT NULL, `passiveDesc` TEXT NOT NULL, `egoName` TEXT NOT NULL, " +
                        "`egoDesc` TEXT NOT NULL, `egoCost` INTEGER NOT NULL, `thumbUrl` TEXT NOT NULL, " +
                        "`imageUrl` TEXT NOT NULL, `source` TEXT NOT NULL, PRIMARY KEY(`id`))"
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(context, AppDatabase::class.java, "czn_wiki.db")
                    .addMigrations(MIGRATION_5_6)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

suspend fun seedDatabaseFromAssets(context: Context, database: AppDatabase) {
    val gson = Gson()

    try {
        val charJson = context.assets.open("data/characters.json").bufferedReader().use { it.readText() }
        val charList: List<CharacterEntity> = try {
            gson.fromJson(charJson, object : TypeToken<List<CharacterEntity>>() {}.type)
        } catch (e: JsonParseException) {
            // Fallback: try warpped object format {"characters": [...]}
            Log.w("AppDatabase", "characters.json is not a top-level array, trying wrapped object format", e)
            try {
                val wrapped = gson.fromJson(charJson, Map::class.java)
                val innerList = wrapped["characters"]
                gson.fromJson(gson.toJson(innerList), object : TypeToken<List<CharacterEntity>>() {}.type)
            } catch (e2: Exception) {
                Log.e("AppDatabase", "Failed to parse characters.json in both formats", e2)
                throw e
            }
        }
        if (charList.isNotEmpty()) {
            database.characterDao().insertAll(charList)
            Log.i("AppDatabase", "Seeded ${charList.size} characters from assets")
        } else {
            Log.w("AppDatabase", "characters.json parsed but returned empty list")
        }
    } catch (e: Exception) {
        Log.e("AppDatabase", "FATAL: Failed to seed characters from assets", e)
    }

    try {
        val cardsJson = context.assets.open("data/cards.json").bufferedReader().use { it.readText() }
        val cardList: List<CardEntity> = gson.fromJson(
            cardsJson,
            object : TypeToken<List<CardEntity>>() {}.type
        )
        if (cardList.isNotEmpty()) database.cardDao().insertAll(cardList)
    } catch (e: Exception) {
        Log.e("AppDatabase", "Failed to seed cards from assets", e)
    }

    try {
        val saJson = context.assets.open("data/self_awareness.json").bufferedReader().use { it.readText() }
        val saList: List<SelfAwarenessEntity> = gson.fromJson(
            saJson,
            object : TypeToken<List<SelfAwarenessEntity>>() {}.type
        )
        if (saList.isNotEmpty()) database.selfAwarenessDao().insertAll(saList)
    } catch (e: Exception) {
        Log.e("AppDatabase", "Failed to seed self_awareness from assets", e)
    }

    try {
        val ucJson = context.assets.open("data/user_collection.json").bufferedReader().use { it.readText() }
        val ucList: List<UserCollectionEntity> = gson.fromJson(
            ucJson,
            object : TypeToken<List<UserCollectionEntity>>() {}.type
        )
        if (ucList.isNotEmpty()) database.userCollectionDao().insertAll(ucList)
    } catch (e: Exception) {
        Log.e("AppDatabase", "Failed to seed user_collection from assets", e)
    }

    try {
        val eventsJson = context.assets.open("data/events.json").bufferedReader().use { it.readText() }
        val eventList: List<EventEntity> = gson.fromJson(
            eventsJson,
            object : TypeToken<List<EventEntity>>() {}.type
        )
        if (eventList.isNotEmpty()) database.eventDao().insertAll(eventList)
    } catch (e: Exception) {
        Log.e("AppDatabase", "Failed to seed events from assets", e)
    }

    try {
        val bannerJson = context.assets.open("data/banners.json").bufferedReader().use { it.readText() }
        val bannerList: List<BannerEntity> = gson.fromJson(
            bannerJson,
            object : TypeToken<List<BannerEntity>>() {}.type
        )
        if (bannerList.isNotEmpty()) database.bannerDao().insertAll(bannerList)
    } catch (e: Exception) {
        Log.e("AppDatabase", "Failed to seed banners from assets", e)
    }

    try {
        val partnersJson = context.assets.open("data/partners.json").bufferedReader().use { it.readText() }
        val partnersList: List<PartnerEntity> = parsePartners(partnersJson, gson)
        if (partnersList.isNotEmpty()) database.partnerDao().insertAll(partnersList)
    } catch (e: Exception) {
        Log.e("AppDatabase", "Failed to seed partners from assets", e)
    }
}
