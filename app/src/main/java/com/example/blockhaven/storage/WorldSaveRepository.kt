package com.example.blockhaven.storage

import com.example.blockhaven.entity.GameMode
import com.example.blockhaven.entity.Player
import com.example.blockhaven.world.Chunk
import com.example.blockhaven.world.World
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WorldSaveRepository(private val dao: WorldDao) {
    val allWorlds = dao.getAllWorlds()

    suspend fun createWorld(name: String, seed: Long, gameMode: GameMode): Long = withContext(Dispatchers.IO) {
        val entity = WorldEntity(
            name = name,
            seed = seed,
            gameMode = gameMode.name
        )
        dao.insertWorld(entity)
    }

    suspend fun saveWorldState(worldId: Long, world: World, player: Player) = withContext(Dispatchers.IO) {
        val existing = dao.getWorldById(worldId) ?: return@withContext
        val updated = existing.copy(
            lastPlayed = System.currentTimeMillis(),
            playerX = player.pos.x,
            playerY = player.pos.y,
            playerZ = player.pos.z,
            playerYaw = player.yaw,
            playerPitch = player.pitch,
            playerHealth = player.health,
            playerHunger = player.hunger,
            worldTime = world.timeOfDay
        )
        dao.updateWorld(updated)

        // Save any modified chunks atomically
        val deltas = mutableListOf<BlockDeltaEntity>()
        for ((_, chunk) in world.chunks) {
            if (chunk.isModified.compareAndSet(true, false)) {
                synchronized(chunk.blockModifications) {
                    for ((idx, blockId) in chunk.blockModifications) {
                        deltas.add(BlockDeltaEntity(worldId, chunk.chunkX, chunk.chunkZ, idx, blockId))
                    }
                }
            }
        }
        if (deltas.isNotEmpty()) {
            dao.insertBlockDeltas(deltas)
        }
    }

    suspend fun loadWorld(worldId: Long, world: World, player: Player) = withContext(Dispatchers.IO) {
        val entity = dao.getWorldById(worldId) ?: return@withContext
        player.pos = com.example.blockhaven.core.math.Vec3f(entity.playerX, entity.playerY, entity.playerZ)
        player.yaw = entity.playerYaw
        player.pitch = entity.playerPitch
        player.health = entity.playerHealth
        player.hunger = entity.playerHunger
        player.gameMode = if (entity.gameMode == GameMode.CREATIVE.name) GameMode.CREATIVE else GameMode.SURVIVAL
        world.timeOfDay = entity.worldTime
        player.ensureSafeGrounded(world)
    }

    suspend fun saveChunkDeltaDirect(worldId: Long, chunk: Chunk) = withContext(Dispatchers.IO) {
        if (chunk.isModified.compareAndSet(true, false)) {
            val deltas = mutableListOf<BlockDeltaEntity>()
            synchronized(chunk.blockModifications) {
                for ((idx, blockId) in chunk.blockModifications) {
                    deltas.add(BlockDeltaEntity(worldId, chunk.chunkX, chunk.chunkZ, idx, blockId))
                }
            }
            if (deltas.isNotEmpty()) {
                dao.insertBlockDeltas(deltas)
            }
        }
    }

    suspend fun loadChunkDeltas(worldId: Long, chunk: Chunk) = withContext(Dispatchers.IO) {
        val deltas = dao.getDeltasForChunk(worldId, chunk.chunkX, chunk.chunkZ)
        for (d in deltas) {
            val idx = d.blockIndex
            val x = idx and 0x0F
            val z = (idx ushr 4) and 0x0F
            val y = idx ushr 8
            chunk.setBlock(x, y, z, d.blockId, recordMod = false)
        }
    }

    suspend fun deleteWorld(worldId: Long) = withContext(Dispatchers.IO) {
        dao.deleteDeltasForWorld(worldId)
        dao.deleteWorld(worldId)
    }
}
