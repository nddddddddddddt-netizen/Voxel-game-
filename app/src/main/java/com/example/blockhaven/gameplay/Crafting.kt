package com.example.blockhaven.gameplay

import com.example.blockhaven.world.BlockType

data class CraftingRecipe(
    val width: Int,
    val height: Int,
    val ingredients: List<Int?>, // Item ID or null for empty slot
    val result: ItemStack
) {
    fun matches(grid: Array<ItemStack?>, gridWidth: Int, gridHeight: Int): Boolean {
        // Search if pattern matches anywhere inside the grid (sliding window)
        for (offsetX in 0..(gridWidth - width)) {
            for (offsetY in 0..(gridHeight - height)) {
                if (checkMatchAt(grid, gridWidth, gridHeight, offsetX, offsetY)) {
                    return true
                }
            }
        }
        return false
    }

    private fun checkMatchAt(grid: Array<ItemStack?>, gridW: Int, gridH: Int, offX: Int, offY: Int): Boolean {
        for (gx in 0 until gridW) {
            for (gy in 0 until gridH) {
                val gridIdx = gy * gridW + gx
                val stack = grid[gridIdx]

                val inPatternX = gx - offX
                val inPatternY = gy - offY

                if (inPatternX in 0 until width && inPatternY in 0 until height) {
                    val patternIdx = inPatternY * width + inPatternX
                    val expectedId = ingredients[patternIdx]
                    if (expectedId == null) {
                        if (stack != null) return false
                    } else {
                        if (stack == null || stack.itemId != expectedId) return false
                    }
                } else {
                    // Must be empty outside the recipe bounds
                    if (stack != null) return false
                }
            }
        }
        return true
    }
}

object CraftingManager {
    val recipes = mutableListOf<CraftingRecipe>()

    init {
        // 1 Wood Log -> 4 Wood Planks
        register(CraftingRecipe(1, 1, listOf(BlockType.WOOD_LOG.toInt()), ItemStack(BlockType.WOOD_PLANKS.toInt(), 4)))

        // 2 Wood Planks -> 4 Sticks
        register(CraftingRecipe(1, 2, listOf(BlockType.WOOD_PLANKS.toInt(), BlockType.WOOD_PLANKS.toInt()), ItemStack(ItemRegistry.STICK, 4)))

        // 4 Planks (2x2) -> Crafting Table
        register(CraftingRecipe(2, 2, listOf(
            BlockType.WOOD_PLANKS.toInt(), BlockType.WOOD_PLANKS.toInt(),
            BlockType.WOOD_PLANKS.toInt(), BlockType.WOOD_PLANKS.toInt()
        ), ItemStack(BlockType.CRAFTING_TABLE.toInt(), 1)))

        // 8 Cobblestone -> Furnace
        register(CraftingRecipe(3, 3, listOf(
            BlockType.COBBLESTONE.toInt(), BlockType.COBBLESTONE.toInt(), BlockType.COBBLESTONE.toInt(),
            BlockType.COBBLESTONE.toInt(), null, BlockType.COBBLESTONE.toInt(),
            BlockType.COBBLESTONE.toInt(), BlockType.COBBLESTONE.toInt(), BlockType.COBBLESTONE.toInt()
        ), ItemStack(BlockType.FURNACE.toInt(), 1)))

        // 1 Coal + 1 Stick -> 4 Torches
        register(CraftingRecipe(1, 2, listOf(ItemRegistry.COAL, ItemRegistry.STICK), ItemStack(BlockType.TORCH.toInt(), 4)))
        // Also allow with Coal Ore if mined directly
        register(CraftingRecipe(1, 2, listOf(BlockType.COAL_ORE.toInt(), ItemRegistry.STICK), ItemStack(BlockType.TORCH.toInt(), 4)))

        // Wooden Pickaxe
        register(CraftingRecipe(3, 3, listOf(
            BlockType.WOOD_PLANKS.toInt(), BlockType.WOOD_PLANKS.toInt(), BlockType.WOOD_PLANKS.toInt(),
            null, ItemRegistry.STICK, null,
            null, ItemRegistry.STICK, null
        ), ItemStack(ItemRegistry.WOOD_PICKAXE, 1)))

        // Stone Pickaxe
        register(CraftingRecipe(3, 3, listOf(
            BlockType.COBBLESTONE.toInt(), BlockType.COBBLESTONE.toInt(), BlockType.COBBLESTONE.toInt(),
            null, ItemRegistry.STICK, null,
            null, ItemRegistry.STICK, null
        ), ItemStack(ItemRegistry.STONE_PICKAXE, 1)))

        // Iron Pickaxe
        register(CraftingRecipe(3, 3, listOf(
            ItemRegistry.IRON_INGOT, ItemRegistry.IRON_INGOT, ItemRegistry.IRON_INGOT,
            null, ItemRegistry.STICK, null,
            null, ItemRegistry.STICK, null
        ), ItemStack(ItemRegistry.IRON_PICKAXE, 1)))

        // Diamond Pickaxe
        register(CraftingRecipe(3, 3, listOf(
            ItemRegistry.DIAMOND_GEM, ItemRegistry.DIAMOND_GEM, ItemRegistry.DIAMOND_GEM,
            null, ItemRegistry.STICK, null,
            null, ItemRegistry.STICK, null
        ), ItemStack(ItemRegistry.DIAMOND_PICKAXE, 1)))

        // Etherite Pickaxe
        register(CraftingRecipe(3, 3, listOf(
            ItemRegistry.ETHERITE_CRYSTAL, ItemRegistry.ETHERITE_CRYSTAL, ItemRegistry.ETHERITE_CRYSTAL,
            null, ItemRegistry.STICK, null,
            null, ItemRegistry.STICK, null
        ), ItemStack(ItemRegistry.ETHERITE_PICKAXE, 1)))

        // Wooden Sword
        register(CraftingRecipe(1, 3, listOf(
            BlockType.WOOD_PLANKS.toInt(),
            BlockType.WOOD_PLANKS.toInt(),
            ItemRegistry.STICK
        ), ItemStack(ItemRegistry.WOOD_SWORD, 1)))

        // Stone Sword
        register(CraftingRecipe(1, 3, listOf(
            BlockType.COBBLESTONE.toInt(),
            BlockType.COBBLESTONE.toInt(),
            ItemRegistry.STICK
        ), ItemStack(ItemRegistry.STONE_SWORD, 1)))

        // Iron Sword
        register(CraftingRecipe(1, 3, listOf(
            ItemRegistry.IRON_INGOT,
            ItemRegistry.IRON_INGOT,
            ItemRegistry.STICK
        ), ItemStack(ItemRegistry.IRON_SWORD, 1)))

        // Diamond Sword
        register(CraftingRecipe(1, 3, listOf(
            ItemRegistry.DIAMOND_GEM,
            ItemRegistry.DIAMOND_GEM,
            ItemRegistry.STICK
        ), ItemStack(ItemRegistry.DIAMOND_SWORD, 1)))

        // Etherite Blade
        register(CraftingRecipe(1, 3, listOf(
            ItemRegistry.ETHERITE_CRYSTAL,
            ItemRegistry.ETHERITE_CRYSTAL,
            ItemRegistry.STICK
        ), ItemStack(ItemRegistry.ETHERITE_BLADE, 1)))

        // Stone Bricks (2x2 Stone)
        register(CraftingRecipe(2, 2, listOf(
            BlockType.STONE.toInt(), BlockType.STONE.toInt(),
            BlockType.STONE.toInt(), BlockType.STONE.toInt()
        ), ItemStack(BlockType.BRICKS.toInt(), 4)))
    }

    fun register(recipe: CraftingRecipe) {
        recipes.add(recipe)
    }

    fun findMatchingResult(grid: Array<ItemStack?>, gridW: Int, gridH: Int): ItemStack? {
        for (recipe in recipes) {
            if (recipe.matches(grid, gridW, gridH)) {
                return recipe.result.copy()
            }
        }
        return null
    }
}
