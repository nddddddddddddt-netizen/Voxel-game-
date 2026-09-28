package com.example.blockhaven.storage

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "worlds")
data class WorldEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val seed: Long,
    val gameMode: String,
    val lastPlayed: Long = System.currentTimeMillis(),
    val playerX: Float = 8f,
    val playerY: Float = 66f,
    val playerZ: Float = 8f,
    val playerYaw: Float = 0f,
    val playerPitch: Float = 0f,
    val playerHealth: Float = 20f,
    val playerHunger: Float = 20f,
    val worldTime: Float = 6000f
)

@Entity(tableName = "block_deltas", primaryKeys = ["worldId", "chunkX", "chunkZ", "blockIndex"])
data class BlockDeltaEntity(
    val worldId: Long,
    val chunkX: Int,
    val chunkZ: Int,
    val blockIndex: Int,
    val blockId: Short
)

@Dao
interface WorldDao {
    @Query("SELECT * FROM worlds ORDER BY lastPlayed DESC")
    fun getAllWorlds(): Flow<List<WorldEntity>>

    @Query("SELECT * FROM worlds WHERE id = :id")
    suspend fun getWorldById(id: Long): WorldEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorld(world: WorldEntity): Long

    @Update
    suspend fun updateWorld(world: WorldEntity)

    @Query("DELETE FROM worlds WHERE id = :id")
    suspend fun deleteWorld(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlockDeltas(deltas: List<BlockDeltaEntity>)

    @Query("SELECT * FROM block_deltas WHERE worldId = :worldId AND chunkX = :cx AND chunkZ = :cz")
    suspend fun getDeltasForChunk(worldId: Long, cx: Int, cz: Int): List<BlockDeltaEntity>

    @Query("DELETE FROM block_deltas WHERE worldId = :worldId")
    suspend fun deleteDeltasForWorld(worldId: Long)
}

@Database(entities = [WorldEntity::class, BlockDeltaEntity::class], version = 1, exportSchema = false)
abstract class BlockhavenDatabase : RoomDatabase() {
    abstract fun worldDao(): WorldDao

    companion object {
        @Volatile
        private var INSTANCE: BlockhavenDatabase? = null

        fun getDatabase(context: Context): BlockhavenDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BlockhavenDatabase::class.java,
                    "blockhaven_world_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
