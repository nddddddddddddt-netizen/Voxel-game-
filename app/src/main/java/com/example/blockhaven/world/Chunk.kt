package com.example.blockhaven.world

import com.example.blockhaven.core.math.Vec3i
import java.util.concurrent.atomic.AtomicBoolean

class Chunk(val chunkX: Int, val chunkZ: Int) {
    companion object {
        const val WIDTH = 16
        const val DEPTH = 16
        const val HEIGHT = 128
        const val VOLUME = WIDTH * DEPTH * HEIGHT
        const val SEA_LEVEL = 48

        fun getIndex(x: Int, y: Int, z: Int): Int {
            return (y shl 8) or (z shl 4) or x
        }

        fun isValidPos(x: Int, y: Int, z: Int): Boolean {
            return x in 0 until WIDTH && z in 0 until DEPTH && y in 0 until HEIGHT
        }
    }

    val blocks = ShortArray(VOLUME)
    val lightMap = ByteArray(VOLUME) // Lower 4 bits: Block Light, Upper 4 bits: Sunlight

    var isMeshDirty = true
    var isLightDirty = true
    val isModified = AtomicBoolean(false)
    var isGenerated = false

    // Record of block edits within this chunk for delta persistence and multiplayer sync
    val blockModifications = mutableMapOf<Int, Short>()

    fun getBlock(x: Int, y: Int, z: Int): Short {
        if (!isValidPos(x, y, z)) return BlockType.AIR
        return blocks[getIndex(x, y, z)]
    }

    fun setBlock(x: Int, y: Int, z: Int, blockId: Short, recordMod: Boolean = true): Boolean {
        if (!isValidPos(x, y, z)) return false
        val idx = getIndex(x, y, z)
        val old = blocks[idx]
        if (old == blockId) return false

        blocks[idx] = blockId
        isMeshDirty = true
        isLightDirty = true
        if (recordMod) {
            isModified.set(true)
            synchronized(blockModifications) {
                blockModifications[idx] = blockId
            }
        }
        return true
    }

    fun getSunlight(x: Int, y: Int, z: Int): Int {
        if (!isValidPos(x, y, z)) return 15
        return (lightMap[getIndex(x, y, z)].toInt() ushr 4) and 0x0F
    }

    fun setSunlight(x: Int, y: Int, z: Int, level: Int) {
        if (!isValidPos(x, y, z)) return
        val idx = getIndex(x, y, z)
        val current = lightMap[idx].toInt() and 0x0F
        val clamped = level.coerceIn(0, 15)
        lightMap[idx] = ((clamped shl 4) or current).toByte()
    }

    fun getBlockLight(x: Int, y: Int, z: Int): Int {
        if (!isValidPos(x, y, z)) return 0
        return lightMap[getIndex(x, y, z)].toInt() and 0x0F
    }

    fun setBlockLight(x: Int, y: Int, z: Int, level: Int) {
        if (!isValidPos(x, y, z)) return
        val idx = getIndex(x, y, z)
        val current = lightMap[idx].toInt() and 0xF0
        val clamped = level.coerceIn(0, 15)
        lightMap[idx] = (current or clamped).toByte()
    }

    fun worldOrigin(): Vec3i {
        return Vec3i(chunkX * WIDTH, 0, chunkZ * DEPTH)
    }

    fun clear() {
        blocks.fill(BlockType.AIR)
        lightMap.fill(0)
        isMeshDirty = true
        isModified.set(false)
        synchronized(blockModifications) {
            blockModifications.clear()
        }
    }
}
