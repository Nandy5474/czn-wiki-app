package com.cznwiki.app.data.database

import android.content.Context
import androidx.room.*
import com.cznwiki.app.data.entity.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*

@Database(
    entities = [CharacterEntity::class, CardEntity::class, SelfAwarenessEntity::class, UserCollectionEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun characterDao(): CharacterDao
    abstract fun cardDao(): CardDao
    abstract fun selfAwarenessDao(): SelfAwarenessDao
    abstract fun userCollectionDao(): UserCollectionDao

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
    }
}
