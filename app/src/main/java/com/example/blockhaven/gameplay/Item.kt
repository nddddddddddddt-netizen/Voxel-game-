package com.example.blockhaven.gameplay

import com.example.blockhaven.world.BlockType

enum class ToolType {
    NONE, PICKAXE, AXE, SHOVEL, SWORD
}

data class ItemDefinition(
    val id: Int,
    val name: String,
    val isBlock: Boolean = true,
    val blockId: Short = BlockType.AIR,
    val toolType: ToolType = ToolType.NONE,
    val harvestTier: Int = 0, // 0 = Wood, 1 = Stone, 2 = Iron, 3 = Diamond, 4 = Etherite
    val miningSpeedMultiplier: Float = 1.0f,
    val attackDamage: Float = 1.0f,
    val maxStackSize: Int = 64
)

data class ItemStack(
    val itemId: Int,
    var count: Int = 1
)

object ItemRegistry {
    // Blocks use their BlockType ID (1..99)
    // Custom non-block items use 100+
    const val STICK = 100
    const val WOOD_PICKAXE = 101
    const val STONE_PICKAXE = 102
    const val IRON_PICKAXE = 103
    const val DIAMOND_PICKAXE = 104
    const val ETHERITE_PICKAXE = 105

    const val WOOD_SWORD = 106
    const val STONE_SWORD = 107
    const val IRON_SWORD = 108
    const val DIAMOND_SWORD = 109
    const val ETHERITE_BLADE = 110

    const val WOOD_AXE = 111
    const val STONE_AXE = 112
    const val IRON_AXE = 113
    const val DIAMOND_AXE = 114

    const val IRON_INGOT = 115
    const val GOLD_INGOT = 116
    const val DIAMOND_GEM = 117
    const val ETHERITE_CRYSTAL = 118
    const val COAL = 119
    const val RAW_MEAT = 120
    const val COOKED_STEAK = 121

    private val ITEMS = mutableMapOf<Int, ItemDefinition>()

    init {
        // Register block items
        for (bId in 1..30) {
            val def = BlockType.get(bId.toShort())
            ITEMS[bId] = ItemDefinition(
                id = bId,
                name = def.name,
                isBlock = true,
                blockId = bId.toShort()
            )
        }

        // Tools and materials
        register(ItemDefinition(STICK, "Wooden Stick", isBlock = false))
        register(ItemDefinition(COAL, "Lump of Coal", isBlock = false))
        register(ItemDefinition(IRON_INGOT, "Iron Ingot", isBlock = false))
        register(ItemDefinition(GOLD_INGOT, "Gold Ingot", isBlock = false))
        register(ItemDefinition(DIAMOND_GEM, "Diamond Gem", isBlock = false))
        register(ItemDefinition(ETHERITE_CRYSTAL, "Etherite Crystal", isBlock = false))
        register(ItemDefinition(RAW_MEAT, "Raw Meat", isBlock = false))
        register(ItemDefinition(COOKED_STEAK, "Cooked Steak", isBlock = false))

        // Pickaxes
        register(ItemDefinition(WOOD_PICKAXE, "Wooden Pickaxe", isBlock = false, toolType = ToolType.PICKAXE, harvestTier = 0, miningSpeedMultiplier = 2.0f, attackDamage = 2f, maxStackSize = 1))
        register(ItemDefinition(STONE_PICKAXE, "Stone Pickaxe", isBlock = false, toolType = ToolType.PICKAXE, harvestTier = 1, miningSpeedMultiplier = 4.0f, attackDamage = 3f, maxStackSize = 1))
        register(ItemDefinition(IRON_PICKAXE, "Iron Pickaxe", isBlock = false, toolType = ToolType.PICKAXE, harvestTier = 2, miningSpeedMultiplier = 6.0f, attackDamage = 4f, maxStackSize = 1))
        register(ItemDefinition(DIAMOND_PICKAXE, "Diamond Pickaxe", isBlock = false, toolType = ToolType.PICKAXE, harvestTier = 3, miningSpeedMultiplier = 9.0f, attackDamage = 5f, maxStackSize = 1))
        register(ItemDefinition(ETHERITE_PICKAXE, "Etherite Pickaxe", isBlock = false, toolType = ToolType.PICKAXE, harvestTier = 4, miningSpeedMultiplier = 14.0f, attackDamage = 7f, maxStackSize = 1))

        // Swords
        register(ItemDefinition(WOOD_SWORD, "Wooden Sword", isBlock = false, toolType = ToolType.SWORD, attackDamage = 4f, maxStackSize = 1))
        register(ItemDefinition(STONE_SWORD, "Stone Sword", isBlock = false, toolType = ToolType.SWORD, attackDamage = 5f, maxStackSize = 1))
        register(ItemDefinition(IRON_SWORD, "Iron Sword", isBlock = false, toolType = ToolType.SWORD, attackDamage = 6f, maxStackSize = 1))
        register(ItemDefinition(DIAMOND_SWORD, "Diamond Sword", isBlock = false, toolType = ToolType.SWORD, attackDamage = 7f, maxStackSize = 1))
        register(ItemDefinition(ETHERITE_BLADE, "Etherite Blade", isBlock = false, toolType = ToolType.SWORD, attackDamage = 10f, maxStackSize = 1))

        // Axes
        register(ItemDefinition(WOOD_AXE, "Wooden Axe", isBlock = false, toolType = ToolType.AXE, miningSpeedMultiplier = 2.5f, attackDamage = 3f, maxStackSize = 1))
        register(ItemDefinition(STONE_AXE, "Stone Axe", isBlock = false, toolType = ToolType.AXE, miningSpeedMultiplier = 4.5f, attackDamage = 4f, maxStackSize = 1))
        register(ItemDefinition(IRON_AXE, "Iron Axe", isBlock = false, toolType = ToolType.AXE, miningSpeedMultiplier = 7.0f, attackDamage = 5f, maxStackSize = 1))
        register(ItemDefinition(DIAMOND_AXE, "Diamond Axe", isBlock = false, toolType = ToolType.AXE, miningSpeedMultiplier = 10.0f, attackDamage = 6f, maxStackSize = 1))
    }

    fun register(def: ItemDefinition) {
        ITEMS[def.id] = def
    }

    fun get(id: Int): ItemDefinition {
        return ITEMS[id] ?: ItemDefinition(id, "Item #$id", isBlock = false)
    }
}
