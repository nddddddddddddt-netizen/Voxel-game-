package com.example.blockhaven.world

import com.example.blockhaven.core.math.RaycastHit
import com.example.blockhaven.core.math.Vec3f
import com.example.blockhaven.core.math.Vec3i
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.floor
import kotlin.math.sqrt

class World(val seed: Long = 123456789L, val worldScope: CoroutineScope) {
    val generator = WorldGenerator(seed)
    val chunks = ConcurrentHashMap<Long, Chunk>()

    var renderDistance = 4
    var timeOfDay = 6000f // 0 = dawn, 6000 = noon, 12000 = sunset, 18000 = midnight
    val dayDurationTicks = 24000f

    // Sky illumination factor based on time of day (0.15 at night to 1.0 at high noon)
    val daylightFactor: Float
        get() {
            val angle = (timeOfDay / dayDurationTicks) * 2f * Math.PI.toFloat()
            val cosVal = kotlin.math.cos(angle - Math.PI.toFloat() * 0.5f)
            return (cosVal * 0.45f + 0.55f).coerceIn(0.18f, 1.0f)
        }

    fun chunkKey(cx: Int, cz: Int): Long = (cx.toLong() shl 32) or (cz.toLong() and 0xFFFFFFFFL)

    fun getChunk(cx: Int, cz: Int): Chunk? = chunks[chunkKey(cx, cz)]

    fun getOrCreateChunk(cx: Int, cz: Int): Chunk {
        val key = chunkKey(cx, cz)
        return chunks.computeIfAbsent(key) {
            val c = Chunk(cx, cz)
            generator.generateChunk(c)
            c
        }
    }

    /**
     * Determines a guaranteed safe spawn location atop dry ground with 2 air blocks clearance
     */
    fun findSafeSpawn(targetX: Float = 8.5f, targetZ: Float = 8.5f): Vec3f {
        val cx = floor(targetX / Chunk.WIDTH).toInt()
        val cz = floor(targetZ / Chunk.DEPTH).toInt()

        // Ensure 3x3 chunks around spawn exist synchronously
        for (dx in -1..1) {
            for (dz in -1..1) {
                getOrCreateChunk(cx + dx, cz + dz)
            }
        }

        val sx = floor(targetX).toInt()
        val sz = floor(targetZ).toInt()

        // Search outward in rings for the highest solid, non-water block
        for (r in 0..6) {
            for (dx in -r..r) {
                for (dz in -r..r) {
                    val testX = sx + dx
                    val testZ = sz + dz
                    for (y in (Chunk.HEIGHT - 4) downTo 4) {
                        val block = getBlock(testX, y, testZ)
                        if (block != BlockType.AIR && block != BlockType.WATER && BlockType.isSolid(block)) {
                            val above1 = getBlock(testX, y + 1, testZ)
                            val above2 = getBlock(testX, y + 2, testZ)
                            if ((above1 == BlockType.AIR || BlockType.isPlant(above1)) && above2 == BlockType.AIR) {
                                return Vec3f(testX + 0.5f, (y + 1).toFloat() + 0.05f, testZ + 0.5f)
                            }
                        }
                    }
                }
            }
        }
        return Vec3f(targetX, 75f, targetZ)
    }

    fun getBlock(x: Int, y: Int, z: Int): Short {
        if (y !in 0 until Chunk.HEIGHT) return BlockType.AIR
        val cx = floor(x.toFloat() / Chunk.WIDTH).toInt()
        val cz = floor(z.toFloat() / Chunk.DEPTH).toInt()
        val chunk = getChunk(cx, cz) ?: return BlockType.AIR
        val lx = (x % Chunk.WIDTH + Chunk.WIDTH) % Chunk.WIDTH
        val lz = (z % Chunk.DEPTH + Chunk.DEPTH) % Chunk.DEPTH
        return chunk.getBlock(lx, y, lz)
    }

    fun setBlock(x: Int, y: Int, z: Int, blockId: Short): Boolean {
        if (y !in 0 until Chunk.HEIGHT) return false
        val cx = floor(x.toFloat() / Chunk.WIDTH).toInt()
        val cz = floor(z.toFloat() / Chunk.DEPTH).toInt()
        val chunk = getOrCreateChunk(cx, cz)
        val lx = (x % Chunk.WIDTH + Chunk.WIDTH) % Chunk.WIDTH
        val lz = (z % Chunk.DEPTH + Chunk.DEPTH) % Chunk.DEPTH

        val changed = chunk.setBlock(lx, y, lz, blockId)
        if (changed) {
            // Also dirty adjacent chunks if on edge
            if (lx == 0) getChunk(cx - 1, cz)?.isMeshDirty = true
            if (lx == Chunk.WIDTH - 1) getChunk(cx + 1, cz)?.isMeshDirty = true
            if (lz == 0) getChunk(cx, cz - 1)?.isMeshDirty = true
            if (lz == Chunk.DEPTH - 1) getChunk(cx, cz + 1)?.isMeshDirty = true
        }
        return changed
    }

    fun updatePlayerPosition(playerPos: Vec3f) {
        val centerCX = floor(playerPos.x / Chunk.WIDTH).toInt()
        val centerCZ = floor(playerPos.z / Chunk.DEPTH).toInt()

        // Asynchronously load chunks in concentric radius around player
        worldScope.launch(Dispatchers.Default) {
            val r = renderDistance
            for (dx in -r..r) {
                for (dz in -r..r) {
                    if (dx * dx + dz * dz <= (r + 1) * (r + 1)) {
                        val cx = centerCX + dx
                        val cz = centerCZ + dz
                        if (!chunks.containsKey(chunkKey(cx, cz))) {
                            getOrCreateChunk(cx, cz)
                        }
                    }
                }
            }

            // Unload chunks that are too far away (with margin)
            val maxUnloadDistSq = (r + 2) * (r + 2)
            val keysToRemove = mutableListOf<Long>()
            for ((key, chunk) in chunks) {
                val dx = chunk.chunkX - centerCX
                val dz = chunk.chunkZ - centerCZ
                if (dx * dx + dz * dz > maxUnloadDistSq) {
                    keysToRemove.add(key)
                }
            }
            for (key in keysToRemove) {
                chunks.remove(key)
            }
        }
    }

    fun tick(deltaTime: Float) {
        // 24000 ticks in full day, e.g. 60 ticks per sec in standard rate
        timeOfDay = (timeOfDay + deltaTime * 20f) % dayDurationTicks
    }

    /**
     * Fast voxel raymarching using DDA (Digital Differential Analyzer)
     */
    fun raycast(origin: Vec3f, direction: Vec3f, maxDistance: Float = 6.0f): RaycastHit? {
        val dir = direction.normalize()
        var currX = floor(origin.x).toInt()
        var currY = floor(origin.y).toInt()
        var currZ = floor(origin.z).toInt()

        val stepX = if (dir.x > 0) 1 else -1
        val stepY = if (dir.y > 0) 1 else -1
        val stepZ = if (dir.z > 0) 1 else -1

        val tDeltaX = if (dir.x != 0f) kotlin.math.abs(1f / dir.x) else Float.MAX_VALUE
        val tDeltaY = if (dir.y != 0f) kotlin.math.abs(1f / dir.y) else Float.MAX_VALUE
        val tDeltaZ = if (dir.z != 0f) kotlin.math.abs(1f / dir.z) else Float.MAX_VALUE

        val nextVoxelX = if (stepX > 0) currX + 1f else currX.toFloat()
        val nextVoxelY = if (stepY > 0) currY + 1f else currY.toFloat()
        val nextVoxelZ = if (stepZ > 0) currZ + 1f else currZ.toFloat()

        var tMaxX = if (dir.x != 0f) (nextVoxelX - origin.x) / dir.x else Float.MAX_VALUE
        var tMaxY = if (dir.y != 0f) (nextVoxelY - origin.y) / dir.y else Float.MAX_VALUE
        var tMaxZ = if (dir.z != 0f) (nextVoxelZ - origin.z) / dir.z else Float.MAX_VALUE

        var normal = Vec3i.ZERO
        var distance = 0f

        while (distance <= maxDistance) {
            val b = getBlock(currX, currY, currZ)
            if (b != BlockType.AIR && b != BlockType.WATER && !BlockType.isPlant(b)) {
                val blockPos = Vec3i(currX, currY, currZ)
                val placePos = blockPos + normal
                return RaycastHit(blockPos, placePos, normal, distance)
            }

            if (tMaxX < tMaxY) {
                if (tMaxX < tMaxZ) {
                    currX += stepX
                    distance = tMaxX
                    tMaxX += tDeltaX
                    normal = Vec3i(-stepX, 0, 0)
                } else {
                    currZ += stepZ
                    distance = tMaxZ
                    tMaxZ += tDeltaZ
                    normal = Vec3i(0, 0, -stepZ)
                }
            } else {
                if (tMaxY < tMaxZ) {
                    currY += stepY
                    distance = tMaxY
                    tMaxY += tDeltaY
                    normal = Vec3i(0, -stepY, 0)
                } else {
                    currZ += stepZ
                    distance = tMaxZ
                    tMaxZ += tDeltaZ
                    normal = Vec3i(0, 0, -stepZ)
                }
            }
        }
        return null
    }
}
