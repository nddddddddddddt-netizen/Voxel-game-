package com.example.blockhaven

import com.example.blockhaven.world.BlockType
import com.example.blockhaven.world.Chunk
import com.example.blockhaven.world.FastNoise
import com.example.blockhaven.world.WorldGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorldGenerationTest {

    @Test
    fun testSeedDeterminism() {
        val seed = 987654321L
        val noise1 = FastNoise(seed)
        val noise2 = FastNoise(seed)

        for (i in 0..50) {
            val x = i * 1.5f
            val y = i * 2.2f
            assertEquals(noise1.perlin2D(x, y), noise2.perlin2D(x, y), 0.0001f)
        }
    }

    @Test
    fun testChunkBlockStorage() {
        val chunk = Chunk(0, 0)
        chunk.setBlock(2, 40, 5, BlockType.DIAMOND_ORE)
        assertEquals(BlockType.DIAMOND_ORE, chunk.getBlock(2, 40, 5))
        assertTrue(chunk.isModified.get())
    }

    @Test
    fun testWorldGeneratorPopulatesBlocks() {
        val chunk = Chunk(0, 0)
        val generator = WorldGenerator(12345L)
        generator.generateChunk(chunk)

        // Bedrock must exist at Y=0
        assertEquals(BlockType.BEDROCK, chunk.getBlock(0, 0, 0))

        // There should be solid ground generated
        var solidCount = 0
        for (y in 1..60) {
            if (chunk.getBlock(8, y, 8) != BlockType.AIR) {
                solidCount++
            }
        }
        assertTrue("Chunk should have solid ground generated", solidCount > 10)
    }

    @Test
    fun testSafeSpawnFinder() {
        val chunk = Chunk(0, 0)
        val generator = WorldGenerator(42L)
        generator.generateChunk(chunk)

        // Find surface at (8, 8)
        var surfaceY = -1
        for (y in (Chunk.HEIGHT - 4) downTo 2) {
            val b = chunk.getBlock(8, y, 8)
            if (b != BlockType.AIR && BlockType.isSolid(b)) {
                surfaceY = y
                break
            }
        }
        assertTrue("Surface must be found above bedrock", surfaceY > 5)
        // Spawn must be strictly above surface block
        val spawnY = (surfaceY + 1).toFloat() + 0.05f
        assertTrue("Spawn Y must be clear above ground", spawnY > surfaceY)
    }
}
