package com.example.blockhaven.gameplay

import com.example.blockhaven.world.BlockType

data class SmeltingRecipe(
    val inputId: Int,
    val result: ItemStack,
    val cookTimeTicks: Int = 200 // 10 seconds at 20 ticks/sec
)

object SmeltingManager {
    val recipes = mutableMapOf<Int, SmeltingRecipe>()
    val fuels = mutableMapOf<Int, Int>() // Item ID to burn time in ticks

    init {
        // Smelting outputs
        register(SmeltingRecipe(BlockType.IRON_ORE.toInt(), ItemStack(ItemRegistry.IRON_INGOT, 1)))
        register(SmeltingRecipe(BlockType.GOLD_ORE.toInt(), ItemStack(ItemRegistry.GOLD_INGOT, 1)))
        register(SmeltingRecipe(BlockType.SAND.toInt(), ItemStack(BlockType.GLASS.toInt(), 1)))
        register(SmeltingRecipe(BlockType.COBBLESTONE.toInt(), ItemStack(BlockType.STONE.toInt(), 1)))
        register(SmeltingRecipe(BlockType.CLAY.toInt(), ItemStack(BlockType.BRICKS.toInt(), 1)))
        register(SmeltingRecipe(ItemRegistry.RAW_MEAT, ItemStack(ItemRegistry.COOKED_STEAK, 1)))

        // Fuels
        fuels[ItemRegistry.COAL] = 1600         // 80 seconds
        fuels[BlockType.COAL_ORE.toInt()] = 1600
        fuels[BlockType.WOOD_LOG.toInt()] = 300 // 15 seconds
        fuels[BlockType.WOOD_PLANKS.toInt()] = 300
        fuels[ItemRegistry.STICK] = 100         // 5 seconds
    }

    fun register(recipe: SmeltingRecipe) {
        recipes[recipe.inputId] = recipe
    }

    fun getResult(inputId: Int): ItemStack? {
        return recipes[inputId]?.result?.copy()
    }

    fun getFuelBurnTime(fuelId: Int): Int {
        return fuels[fuelId] ?: 0
    }
}
