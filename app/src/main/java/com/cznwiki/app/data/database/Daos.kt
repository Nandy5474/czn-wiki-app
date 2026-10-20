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

    @Query("SELECT id FROM characters ORDER BY id ASC")
    suspend fun getAllCharacterIdsSync(): List<Int>

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

    @Query("SELECT * FROM user_collection WHERE favorite = 1")
    fun getFavoriteCharacters(): Flow<List<UserCollectionEntity>>

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

    @Query("UPDATE user_collection SET favorite = :value WHERE characterId = :characterId")
    suspend fun updateFavorite(characterId: Int, value: Boolean)
}

@Dao
interface EventDao {
    @Query("SELECT * FROM events WHERE server='Global' ORDER BY endDate ASC")
    fun getAllEvents(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE server='Global' ORDER BY endDate ASC")
    suspend fun getAllEventsSync(): List<EventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<EventEntity>)

    @Query("DELETE FROM events")
    suspend fun deleteAll()
}

@Dao
interface BannerDao {
    @Query("SELECT * FROM banners WHERE status IN ('current','future') AND server='Global' ORDER BY endDate ASC")
    fun getCurrentBanners(): Flow<List<BannerEntity>>

    @Query("SELECT * FROM banners WHERE status = 'history' AND server='Global' ORDER BY endDate DESC")
    fun getHistoryBanners(): Flow<List<BannerEntity>>

    @Query("SELECT * FROM banners WHERE server='Global' ORDER BY endDate DESC")
    suspend fun getAllBannersSync(): List<BannerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(banners: List<BannerEntity>)

    @Query("DELETE FROM banners")
    suspend fun deleteAll()
}

@Dao
interface TeamDao {
    @Query("SELECT * FROM teams ORDER BY createdAt DESC")
    fun getAllTeams(): Flow<List<TeamEntity>>

    @Query("SELECT * FROM teams ORDER BY createdAt DESC")
    suspend fun getAllTeamsSync(): List<TeamEntity>

    @Query("SELECT * FROM teams WHERE id = :id")
    suspend fun getTeamById(id: Int): TeamEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(team: TeamEntity): Long

    @Update
    suspend fun update(team: TeamEntity)

    @Query("DELETE FROM teams WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Delete
    suspend fun delete(team: TeamEntity)
}

@Dao
interface PartnerDao {
    @Query("SELECT * FROM partners ORDER BY CAST(id AS INTEGER) ASC")
    fun getAllPartners(): Flow<List<PartnerEntity>>

    @Query("SELECT * FROM partners ORDER BY CAST(id AS INTEGER) ASC")
    suspend fun getAllPartnersSync(): List<PartnerEntity>

    @Query("SELECT * FROM partners WHERE id = :id")
    suspend fun getPartnerById(id: String): PartnerEntity?

    @Query("SELECT * FROM partners WHERE name LIKE '%' || :query || '%' OR nameEn LIKE '%' || :query || '%' OR slug LIKE '%' || :query || '%'")
    fun searchPartners(query: String): Flow<List<PartnerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(partners: List<PartnerEntity>)

    @Query("SELECT COUNT(*) FROM partners")
    suspend fun getCount(): Int

    @Query("SELECT id FROM partners ORDER BY CAST(id AS INTEGER) ASC")
    suspend fun getAllPartnerIdsSync(): List<String>

    @Query("DELETE FROM partners")
    suspend fun deleteAll()
}
