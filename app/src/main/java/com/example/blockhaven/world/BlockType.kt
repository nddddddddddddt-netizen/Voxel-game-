package com.example.blockhaven.world

enum class BlockSoundType {
    GRASS, STONE, WOOD, SAND, GLASS, WATER, METAL, DIRT
}

data class BlockDefinition(
    val id: Short,
    val name: String,
    val hardness: Float,
    val isSolid: Boolean = true,
    val isTransparent: Boolean = false,
    val isLiquid: Boolean = false,
    val isPlant: Boolean = false,
    val lightEmission: Int = 0,
    val textureTop: Int,
    val textureSide: Int,
    val textureBottom: Int,
    val soundType: BlockSoundType = BlockSoundType.STONE,
    val dropItem: Short = id
)

object BlockType {
    const val AIR: Short = 0
    const val GRASS: Short = 1
    const val DIRT: Short = 2
    const val STONE: Short = 3
    const val COBBLESTONE: Short = 4
    const val SAND: Short = 5
    const val SNOW: Short = 6
    const val WOOD_LOG: Short = 7
    const val WOOD_PLANKS: Short = 8
    const val LEAVES: Short = 9
    const val GLASS: Short = 10
    const val WATER: Short = 11
    const val BEDROCK: Short = 12
    const val COAL_ORE: Short = 13
    const val IRON_ORE: Short = 14
    const val GOLD_ORE: Short = 15
    const val DIAMOND_ORE: Short = 16
    const val ETHERITE_ORE: Short = 17
    const val PYRITE_ORE: Short = 18
    const val LUMINITE_ORE: Short = 19
    const val BRICKS: Short = 20
    const val CRAFTING_TABLE: Short = 21
    const val FURNACE: Short = 22
    const val TORCH: Short = 23
    const val BOOKSHELF: Short = 24
    const val OBSIDIAN: Short = 25
    const val CLAY: Short = 26
    const val CACTUS: Short = 27
    const val TALL_GRASS: Short = 28
    const val FLOWER_CYAN: Short = 29
    const val FLOWER_GOLDEN: Short = 30

    // Dynamic registry map to allow mods to register custom blocks seamlessly!
    private val REGISTRY = mutableMapOf<Short, BlockDefinition>()

    init {
        register(BlockDefinition(AIR, "Air", 0f, isSolid = false, isTransparent = true, textureTop = 0, textureSide = 0, textureBottom = 0))
        register(BlockDefinition(GRASS, "Grass Block", 0.6f, textureTop = 0, textureSide = 1, textureBottom = 2, soundType = BlockSoundType.GRASS, dropItem = DIRT))
        register(BlockDefinition(DIRT, "Dirt", 0.5f, textureTop = 2, textureSide = 2, textureBottom = 2, soundType = BlockSoundType.DIRT))
        register(BlockDefinition(STONE, "Stone", 1.5f, textureTop = 3, textureSide = 3, textureBottom = 3, soundType = BlockSoundType.STONE, dropItem = COBBLESTONE))
        register(BlockDefinition(COBBLESTONE, "Cobblestone", 2.0f, textureTop = 4, textureSide = 4, textureBottom = 4, soundType = BlockSoundType.STONE))
        register(BlockDefinition(SAND, "Sand", 0.5f, textureTop = 5, textureSide = 5, textureBottom = 5, soundType = BlockSoundType.SAND))
        register(BlockDefinition(SNOW, "Snow Block", 0.3f, textureTop = 6, textureSide = 6, textureBottom = 6, soundType = BlockSoundType.GRASS))
        register(BlockDefinition(WOOD_LOG, "Wood Log", 2.0f, textureTop = 7, textureSide = 8, textureBottom = 7, soundType = BlockSoundType.WOOD))
        register(BlockDefinition(WOOD_PLANKS, "Wood Planks", 1.5f, textureTop = 9, textureSide = 9, textureBottom = 9, soundType = BlockSoundType.WOOD))
        register(BlockDefinition(LEAVES, "Leaves", 0.2f, isTransparent = true, textureTop = 10, textureSide = 10, textureBottom = 10, soundType = BlockSoundType.GRASS))
        register(BlockDefinition(GLASS, "Glass", 0.3f, isTransparent = true, textureTop = 11, textureSide = 11, textureBottom = 11, soundType = BlockSoundType.GLASS, dropItem = AIR))
        register(BlockDefinition(WATER, "Water", 100f, isSolid = false, isTransparent = true, isLiquid = true, textureTop = 12, textureSide = 12, textureBottom = 12, soundType = BlockSoundType.WATER, dropItem = AIR))
        register(BlockDefinition(BEDROCK, "Bedrock", -1f, textureTop = 13, textureSide = 13, textureBottom = 13, soundType = BlockSoundType.STONE, dropItem = AIR))
        register(BlockDefinition(COAL_ORE, "Coal Ore", 3.0f, textureTop = 14, textureSide = 14, textureBottom = 14, soundType = BlockSoundType.STONE))
        register(BlockDefinition(IRON_ORE, "Iron Ore", 3.5f, textureTop = 15, textureSide = 15, textureBottom = 15, soundType = BlockSoundType.STONE))
        register(BlockDefinition(GOLD_ORE, "Gold Ore", 4.0f, textureTop = 16, textureSide = 16, textureBottom = 16, soundType = BlockSoundType.STONE))
        register(BlockDefinition(DIAMOND_ORE, "Diamond Ore", 5.0f, textureTop = 17, textureSide = 17, textureBottom = 17, soundType = BlockSoundType.STONE))
        register(BlockDefinition(ETHERITE_ORE, "Etherite Ore", 6.0f, lightEmission = 4, textureTop = 18, textureSide = 18, textureBottom = 18, soundType = BlockSoundType.STONE))
        register(BlockDefinition(PYRITE_ORE, "Pyrite Ore", 4.5f, lightEmission = 3, textureTop = 19, textureSide = 19, textureBottom = 19, soundType = BlockSoundType.STONE))
        register(BlockDefinition(LUMINITE_ORE, "Luminite Ore", 3.0f, lightEmission = 12, textureTop = 20, textureSide = 20, textureBottom = 20, soundType = BlockSoundType.GLASS))
        register(BlockDefinition(BRICKS, "Stone Bricks", 2.0f, textureTop = 21, textureSide = 21, textureBottom = 21, soundType = BlockSoundType.STONE))
        register(BlockDefinition(CRAFTING_TABLE, "Crafting Table", 2.0f, textureTop = 22, textureSide = 23, textureBottom = 9, soundType = BlockSoundType.WOOD))
        register(BlockDefinition(FURNACE, "Furnace", 3.5f, textureTop = 4, textureSide = 24, textureBottom = 4, soundType = BlockSoundType.STONE))
        register(BlockDefinition(TORCH, "Torch", 0.05f, isSolid = false, isTransparent = true, lightEmission = 14, textureTop = 25, textureSide = 25, textureBottom = 25, soundType = BlockSoundType.WOOD))
        register(BlockDefinition(BOOKSHELF, "Bookshelf", 1.5f, textureTop = 9, textureSide = 26, textureBottom = 9, soundType = BlockSoundType.WOOD))
        register(BlockDefinition(OBSIDIAN, "Obsidian", 15.0f, textureTop = 27, textureSide = 27, textureBottom = 27, soundType = BlockSoundType.STONE))
        register(BlockDefinition(CLAY, "Clay Block", 0.6f, textureTop = 28, textureSide = 28, textureBottom = 28, soundType = BlockSoundType.DIRT))
        register(BlockDefinition(CACTUS, "Cactus", 0.4f, isTransparent = true, textureTop = 29, textureSide = 30, textureBottom = 29, soundType = BlockSoundType.GRASS))
        register(BlockDefinition(TALL_GRASS, "Tall Grass", 0.1f, isSolid = false, isTransparent = true, isPlant = true, textureTop = 31, textureSide = 31, textureBottom = 31, soundType = BlockSoundType.GRASS, dropItem = AIR))
        register(BlockDefinition(FLOWER_CYAN, "Cyan Flower", 0.1f, isSolid = false, isTransparent = true, isPlant = true, textureTop = 32, textureSide = 32, textureBottom = 32, soundType = BlockSoundType.GRASS))
        register(BlockDefinition(FLOWER_GOLDEN, "Golden Flower", 0.1f, isSolid = false, isTransparent = true, isPlant = true, textureTop = 33, textureSide = 33, textureBottom = 33, soundType = BlockSoundType.GRASS))
    }

    fun register(definition: BlockDefinition) {
        REGISTRY[definition.id] = definition
    }

    fun get(id: Short): BlockDefinition {
        return REGISTRY[id] ?: REGISTRY[DIRT]!!
    }

    fun isSolid(id: Short): Boolean = get(id).isSolid
    fun isTransparent(id: Short): Boolean = get(id).isTransparent
    fun isLiquid(id: Short): Boolean = get(id).isLiquid
    fun isPlant(id: Short): Boolean = get(id).isPlant
    fun getLight(id: Short): Int = get(id).lightEmission

    fun getParticleColor(id: Short): FloatArray {
        return when (id) {
            GRASS -> floatArrayOf(0.28f, 0.65f, 0.22f)
            DIRT -> floatArrayOf(0.50f, 0.35f, 0.20f)
            STONE -> floatArrayOf(0.50f, 0.50f, 0.52f)
            COBBLESTONE -> floatArrayOf(0.42f, 0.42f, 0.44f)
            SAND -> floatArrayOf(0.88f, 0.80f, 0.55f)
            SNOW -> floatArrayOf(0.95f, 0.96f, 0.99f)
            WOOD_LOG, WOOD_PLANKS -> floatArrayOf(0.60f, 0.44f, 0.28f)
            LEAVES -> floatArrayOf(0.18f, 0.58f, 0.16f)
            GLASS -> floatArrayOf(0.85f, 0.95f, 0.98f)
            WATER -> floatArrayOf(0.20f, 0.55f, 0.90f)
            COAL_ORE -> floatArrayOf(0.18f, 0.18f, 0.18f)
            IRON_ORE -> floatArrayOf(0.78f, 0.60f, 0.45f)
            GOLD_ORE -> floatArrayOf(0.98f, 0.82f, 0.15f)
            DIAMOND_ORE -> floatArrayOf(0.32f, 0.92f, 0.92f)
            ETHERITE_ORE -> floatArrayOf(0.75f, 0.35f, 0.98f)
            PYRITE_ORE -> floatArrayOf(0.98f, 0.45f, 0.12f)
            LUMINITE_ORE -> floatArrayOf(1.0f, 0.92f, 0.35f)
            TORCH -> floatArrayOf(1.0f, 0.68f, 0.18f)
            BRICKS -> floatArrayOf(0.62f, 0.55f, 0.50f)
            CRAFTING_TABLE, BOOKSHELF -> floatArrayOf(0.55f, 0.38f, 0.25f)
            OBSIDIAN -> floatArrayOf(0.15f, 0.12f, 0.22f)
            CACTUS -> floatArrayOf(0.25f, 0.58f, 0.20f)
            FLOWER_CYAN -> floatArrayOf(0.15f, 0.80f, 0.95f)
            FLOWER_GOLDEN -> floatArrayOf(0.98f, 0.82f, 0.12f)
            else -> floatArrayOf(0.5f, 0.5f, 0.5f)
        }
    }
}
