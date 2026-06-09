package com.cznwiki.app.data.database

import androidx.room.*
import com.cznwiki.app.data.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CharacterDao {
    @Query("SELECT * FROM characters ORDER BY tier ASC, stars DESC, name ASC")
    fun getAllCharacters(): Flow<List<CharacterEntity>>

    @Query("SELECT * FROM characters WHERE id = :id")
    suspend fun getCharacterById(id: Int): CharacterEntity?

    @Query("SELECT * FROM characters WHERE name LIKE '%' || :query || '%' OR nameEn LIKE '%' || :query || '%'")
    fun searchCharacters(query: String): Flow<List<CharacterEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(characters: List<CharacterEntity>)

    @Query("SELECT COUNT(*) FROM characters")
    suspend fun getCount(): Int

    @Query("DELETE FROM characters")
    suspend fun deleteAll()
}

@Dao
interface CardDao {
    @Query("SELECT * FROM cards WHERE characterId = :characterId AND type = '基础卡牌' ORDER BY sortOrder")
    fun getBaseCards(characterId: Int): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE characterId = :characterId AND type = '灵光一闪' ORDER BY sortOrder")
    fun getEpiphanyCards(characterId: Int): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE characterId = :characterId ORDER BY sortOrder")
    fun getAllCards(characterId: Int): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE characterId = :characterId ORDER BY sortOrder")
    suspend fun getAllCardsSync(characterId: Int): List<CardEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(cards: List<CardEntity>)

    @Query("DELETE FROM cards WHERE characterId = :characterId")
    suspend fun deleteByCharacter(characterId: Int)

    @Query("DELETE FROM cards")
    suspend fun deleteAll()
}

@Dao
interface SelfAwarenessDao {
    @Query("SELECT * FROM self_awareness WHERE characterId = :characterId ORDER BY stage")
    fun getByCharacter(characterId: Int): Flow<List<SelfAwarenessEntity>>

    @Query("SELECT * FROM self_awareness WHERE characterId = :characterId ORDER BY stage")
    suspend fun getByCharacterSync(characterId: Int): List<SelfAwarenessEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SelfAwarenessEntity>)

    @Query("DELETE FROM self_awareness WHERE characterId = :characterId")
    suspend fun deleteByCharacter(characterId: Int)

    @Query("DELETE FROM self_awareness")
    suspend fun deleteAll()
}

@Dao
interface UserCollectionDao {
    @Query("SELECT * FROM user_collection WHERE owned = 1")
    fun getOwnedCharacters(): Flow<List<UserCollectionEntity>>

    @Query("SELECT * FROM user_collection WHERE characterId = :characterId")
    suspend fun getByCharacterId(characterId: Int): UserCollectionEntity?

    @Query("SELECT * FROM user_collection")
    suspend fun getAllSync(): List<UserCollectionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: UserCollectionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<UserCollectionEntity>)

    @Query("UPDATE user_collection SET constellation = :value WHERE characterId = :characterId")
    suspend fun updateConstellation(characterId: Int, value: Int)

    @Query("UPDATE user_collection SET potential = :value WHERE characterId = :characterId")
    suspend fun updatePotential(characterId: Int, value: Int)

    @Query("UPDATE user_collection SET partnerId = :value WHERE characterId = :characterId")
    suspend fun updatePartner(characterId: Int, value: Int)

    @Query("UPDATE user_collection SET customTier = :value WHERE characterId = :characterId")
    suspend fun updateTier(characterId: Int, value: String)
}
