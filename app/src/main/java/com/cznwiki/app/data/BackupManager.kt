package com.cznwiki.app.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.cznwiki.app.data.database.AppDatabase
import com.cznwiki.app.data.entity.TeamEntity
import com.cznwiki.app.data.entity.UserCollectionEntity
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BackupManager(private val context: Context) {
    companion object {
        private const val TAG = "BackupManager"
        private const val FILE_EXTENSION = ".cznbackup"
    }

    data class BackupData(
        val version: Int = 2,
        @SerializedName("exportedAt") val exportedAt: String = "",
        @SerializedName("dataVersion") val dataVersion: Int = 0,
        @SerializedName("ownedCharacters") val ownedCharacters: List<CharacterBackupEntry> = emptyList(),
        @SerializedName("teams") val teams: List<TeamBackupEntry> = emptyList()
    )

    data class CharacterBackupEntry(
        val characterId: Int,
        val name: String = "",
        val owned: Boolean = false,
        val constellation: Int = 0,
        val potential: Int = 0,
        val customTier: String = ""
    )

    data class TeamBackupEntry(
        val id: Int = 0,
        val name: String,
        val description: String = "",
        val member1Id: Int? = null,
        val member2Id: Int? = null,
        val member3Id: Int? = null
    )

    private val gson = Gson()

    suspend fun exportBackup(database: AppDatabase): String? = withContext(Dispatchers.IO) {
        try {
            val userCollections = database.userCollectionDao().getAllSync()
            val allCharacters = withContext(Dispatchers.IO) {
                // get character names for readability
                userCollections.map { uc ->
                    val char = database.characterDao().getCharacterById(uc.characterId)
                    CharacterBackupEntry(
                        characterId = uc.characterId,
                        name = char?.name ?: "",
                        owned = uc.owned,
                        constellation = uc.constellation,
                        potential = uc.potential,
                        customTier = uc.customTier
                    )
                }
            }

            val allTeams = database.teamDao().getAllTeamsSync()
            val teamEntries = allTeams.map { t ->
                TeamBackupEntry(
                    id = t.id,
                    name = t.name,
                    description = t.description,
                    member1Id = t.member1Id,
                    member2Id = t.member2Id,
                    member3Id = t.member3Id
                )
            }

            val backup = BackupData(
                version = 2,
                exportedAt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                    .format(java.util.Date()),
                dataVersion = try {
                    context.assets.open("data/version.json").bufferedReader().use {
                        val obj = gson.fromJson(it.readText(), Map::class.java)
                        (obj["version_code"] as? Double)?.toInt() ?: 0
                    }
                } catch (e: Exception) { 0 },
                ownedCharacters = allCharacters,
                teams = teamEntries
            )

            val json = gson.toJson(backup)
            val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
                .format(java.util.Date())
            val fileName = "czn_backup_$timestamp$FILE_EXTENSION"

            context.openFileOutput(fileName, Context.MODE_PRIVATE).use {
                it.write(json.toByteArray())
            }

            val file = context.getFileStreamPath(fileName)
            Log.i(TAG, "Backup exported to: ${file.absolutePath}, size=${json.length}")
            return@withContext file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Export failed", e)
            return@withContext null
        }
    }

    suspend fun importBackup(uri: Uri, database: AppDatabase): Int = withContext(Dispatchers.IO) {
        try {
            val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: return@withContext -1

            val backup = gson.fromJson(json, BackupData::class.java)

            var restoredCount = 0

            // Restore owned characters
            if (backup.ownedCharacters.isNotEmpty()) {
                val entities = backup.ownedCharacters.map { entry ->
                    UserCollectionEntity(
                        characterId = entry.characterId,
                        owned = entry.owned,
                        constellation = entry.constellation,
                        potential = entry.potential,
                        customTier = entry.customTier
                    )
                }
                database.userCollectionDao().insertAll(entities)
                restoredCount += entities.size
            }

            // Restore teams
            if (backup.teams.isNotEmpty()) {
                backup.teams.forEach { team ->
                    database.teamDao().insert(
                        TeamEntity(
                            name = team.name,
                            description = team.description,
                            member1Id = team.member1Id,
                            member2Id = team.member2Id,
                            member3Id = team.member3Id
                        )
                    )
                    restoredCount++
                }
            }

            Log.i(TAG, "Backup imported: $restoredCount items restored")
            return@withContext restoredCount
        } catch (e: Exception) {
            Log.e(TAG, "Import failed", e)
            return@withContext -1
        }
    }

    fun createExportIntent(): Intent {
        return Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, "czn_backup_${
                java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
                    .format(java.util.Date())
            }.cznbackup")
        }
    }

    fun createImportIntent(): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
    }
}