package com.example.blockhaven

import com.example.blockhaven.gameplay.CraftingManager
import com.example.blockhaven.gameplay.Inventory
import com.example.blockhaven.gameplay.ItemRegistry
import com.example.blockhaven.gameplay.ItemStack
import com.example.blockhaven.gameplay.SmeltingManager
import com.example.blockhaven.world.BlockType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameplayTest {

    @Test
    fun testCraftingPlanksFromLog() {
        val grid = arrayOfNulls<ItemStack>(9)
        grid[0] = ItemStack(BlockType.WOOD_LOG.toInt(), 1)

        val result = CraftingManager.findMatchingResult(grid, 3, 3)
        assertNotNull(result)
        assertEquals(BlockType.WOOD_PLANKS.toInt(), result?.itemId)
        assertEquals(4, result?.count)
    }

    @Test
    fun testSmeltingIronOre() {
        val output = SmeltingManager.getResult(BlockType.IRON_ORE.toInt())
        assertNotNull(output)
        assertEquals(ItemRegistry.IRON_INGOT, output?.itemId)
    }

    @Test
    fun testInventoryAddAndConsume() {
        val inv = Inventory()
        assertTrue(inv.addItem(ItemStack(ItemRegistry.IRON_INGOT, 5)))
        assertEquals(ItemRegistry.IRON_INGOT, inv.getSlot(0)?.itemId)
        assertEquals(5, inv.getSlot(0)?.count)

        inv.selectedHotbarIndex = 0
        inv.consumeSelectedItem(2)
        assertEquals(3, inv.getSelectedItem()?.count)
    }
}
