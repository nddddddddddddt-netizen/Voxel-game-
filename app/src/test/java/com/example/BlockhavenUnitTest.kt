package com.example

import com.example.blockhaven.core.math.Vec3f
import com.example.blockhaven.core.math.Vec3i
import com.example.blockhaven.entity.GameMode
import com.example.blockhaven.entity.Player
import com.example.blockhaven.gameplay.CraftingManager
import com.example.blockhaven.gameplay.Inventory
import com.example.blockhaven.gameplay.ItemRegistry
import com.example.blockhaven.gameplay.ItemStack
import com.example.blockhaven.modding.ModEventListener
import com.example.blockhaven.modding.ModScriptEngine
import com.example.blockhaven.world.BlockType
import com.example.blockhaven.world.Chunk
import com.example.blockhaven.world.FastNoise
import com.example.blockhaven.world.WorldGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockhavenUnitTest {

    @Test
    fun testNoiseDeterministicSeedRepeatability() {
        val seed = 987654321L
        val noise1 = FastNoise(seed)
        val noise2 = FastNoise(seed)

        for (x in -5..5) {
            for (y in -5..5) {
                val val1 = noise1.perlin2D(x * 0.1f, y * 0.1f)
                val val2 = noise2.perlin2D(x * 0.1f, y * 0.1f)
                assertEquals("Perlin 2D noise must match exactly for identical seed", val1, val2, 0.000001f)
            }
        }
    }

    @Test
    fun testWorldGeneratorProducesIdenticalChunksForSameSeed() {
        val seed = 42424242L
        val gen1 = WorldGenerator(seed)
        val gen2 = WorldGenerator(seed)

        val chunk1 = Chunk(2, -3)
        val chunk2 = Chunk(2, -3)

        gen1.generateChunk(chunk1)
        gen2.generateChunk(chunk2)

        for (x in 0 until Chunk.WIDTH) {
            for (z in 0 until Chunk.DEPTH) {
                for (y in 0 until Chunk.HEIGHT) {
                    assertEquals(
                        "Block at ($x, $y, $z) must match for identical seed",
                        chunk1.getBlock(x, y, z),
                        chunk2.getBlock(x, y, z)
                    )
                }
            }
        }
    }

    @Test
    fun testCraftingPlanksAndSticks() {
        // 1 Wood Log -> 4 Planks
        val logGrid = arrayOf<ItemStack?>(
            ItemStack(BlockType.WOOD_LOG.toInt(), 1), null, null,
            null, null, null,
            null, null, null
        )
        val planksResult = CraftingManager.findMatchingResult(logGrid, 3, 3)
        assertNotNull("Crafting 1 wood log should yield planks", planksResult)
        assertEquals(BlockType.WOOD_PLANKS.toInt(), planksResult!!.itemId)
        assertEquals(4, planksResult.count)

        // 2 Planks (vertical) -> 4 Sticks
        val stickGrid = arrayOf<ItemStack?>(
            ItemStack(BlockType.WOOD_PLANKS.toInt(), 1), null, null,
            ItemStack(BlockType.WOOD_PLANKS.toInt(), 1), null, null,
            null, null, null
        )
        val stickResult = CraftingManager.findMatchingResult(stickGrid, 3, 3)
        assertNotNull("Crafting 2 vertical planks should yield sticks", stickResult)
        assertEquals(ItemRegistry.STICK, stickResult!!.itemId)
        assertEquals(4, stickResult.count)
    }

    @Test
    fun testInventoryAddAndConsume() {
        val inv = Inventory()
        assertTrue(inv.addItem(ItemStack(BlockType.DIRT.toInt(), 10)))
        assertEquals(BlockType.DIRT.toInt(), inv.getSlot(0)?.itemId)
        assertEquals(10, inv.getSlot(0)?.count)

        // Add 5 more of same item, should stack
        inv.addItem(ItemStack(BlockType.DIRT.toInt(), 5))
        assertEquals(15, inv.getSlot(0)?.count)

        // Select slot 0 and consume 3
        inv.selectedHotbarIndex = 0
        inv.consumeSelectedItem(3)
        assertEquals(12, inv.getSlot(0)?.count)
    }

    @Test
    fun testChunkModificationTracking() {
        val chunk = Chunk(0, 0)
        chunk.setBlock(4, 20, 7, BlockType.BRICKS)

        assertTrue(chunk.isModified.get())
        assertEquals(BlockType.BRICKS, chunk.getBlock(4, 20, 7))

        val idx = Chunk.getIndex(4, 20, 7)
        assertEquals(BlockType.BRICKS, chunk.blockModifications[idx])
    }

    @Test
    fun testModScriptEngineDispatch() {
        val engine = ModScriptEngine()
        var receivedCommand = ""

        engine.registerListener("test_mod", object : ModEventListener {
            override fun onCommand(
                command: String,
                args: List<String>,
                player: Player,
                world: com.example.blockhaven.world.World
            ): String {
                receivedCommand = command
                return "Handled: $command"
            }
        })

        val player = Player()
        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default)
        val world = com.example.blockhaven.world.World(1234L, scope)

        val resp = engine.dispatchCommand("ping", player, world)
        assertEquals("[test_mod] Handled: ping", resp)
        assertEquals("ping", receivedCommand)
    }
}
