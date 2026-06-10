package com.cznwiki.app.data.database

import android.content.Context
import androidx.room.*
import com.cznwiki.app.data.entity.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*

@Database(
    entities = [
        CharacterEntity::class, CardEntity::class, SelfAwarenessEntity::class,
        UserCollectionEntity::class, EventEntity::class, BannerEntity::class,
        TeamEntity::class
    ],
    version = 4,
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

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(context, AppDatabase::class.java, "czn_wiki.db")
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

fun seedDatabaseFromAssets(context: Context, database: AppDatabase) {
    val gson = Gson()
    val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    coroutineScope.launch {
        try {
            val charJson = context.assets.open("data/characters.json").bufferedReader().use { it.readText() }
            val charList: List<CharacterEntity> = gson.fromJson(
                charJson,
                object : TypeToken<List<CharacterEntity>>() {}.type
            )
            if (charList.isNotEmpty()) database.characterDao().insertAll(charList)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            val cardsJson = context.assets.open("data/cards.json").bufferedReader().use { it.readText() }
            val cardList: List<CardEntity> = gson.fromJson(
                cardsJson,
                object : TypeToken<List<CardEntity>>() {}.type
            )
            if (cardList.isNotEmpty()) database.cardDao().insertAll(cardList)
        } catch (_: Exception) {}

        try {
            val saJson = context.assets.open("data/self_awareness.json").bufferedReader().use { it.readText() }
            val saList: List<SelfAwarenessEntity> = gson.fromJson(
                saJson,
                object : TypeToken<List<SelfAwarenessEntity>>() {}.type
            )
            if (saList.isNotEmpty()) database.selfAwarenessDao().insertAll(saList)
        } catch (_: Exception) {}

        try {
            val ucJson = context.assets.open("data/user_collection.json").bufferedReader().use { it.readText() }
            val ucList: List<UserCollectionEntity> = gson.fromJson(
                ucJson,
                object : TypeToken<List<UserCollectionEntity>>() {}.type
            )
            if (ucList.isNotEmpty()) database.userCollectionDao().insertAll(ucList)
        } catch (_: Exception) {}

        try {
            val eventsJson = context.assets.open("data/events.json").bufferedReader().use { it.readText() }
            val eventList: List<EventEntity> = gson.fromJson(
                eventsJson,
                object : TypeToken<List<EventEntity>>() {}.type
            )
            if (eventList.isNotEmpty()) database.eventDao().insertAll(eventList)
        } catch (_: Exception) {}

        try {
            val bannerJson = context.assets.open("data/banners.json").bufferedReader().use { it.readText() }
            val bannerList: List<BannerEntity> = gson.fromJson(
                bannerJson,
                object : TypeToken<List<BannerEntity>>() {}.type
            )
            if (bannerList.isNotEmpty()) database.bannerDao().insertAll(bannerList)
        } catch (_: Exception) {}
    }
}
