package com.example.blockhaven.world

enum class BiomeType(
    val biomeName: String,
    val baseHeight: Int,
    val heightVariation: Int,
    val topBlock: Short,
    val subsurfaceBlock: Short,
    val treeDensity: Float,
    val floraDensity: Float,
    val skyTint: Int
) {
    PLAINS("Plains", 52, 14, BlockType.GRASS, BlockType.DIRT, 0.015f, 0.06f, 0x87CEEB),
    FOREST("Dense Forest", 55, 18, BlockType.GRASS, BlockType.DIRT, 0.08f, 0.04f, 0x7AB8D6),
    DESERT("Desert Dunes", 50, 10, BlockType.SAND, BlockType.SAND, 0.005f, 0.01f, 0xB0E0E6),
    SNOWY_MOUNTAINS("Snowy Mountains", 70, 38, BlockType.SNOW, BlockType.STONE, 0.03f, 0.01f, 0xC4D8E2),
    SWAMP("Murky Swamp", 48, 6, BlockType.GRASS, BlockType.CLAY, 0.04f, 0.08f, 0x5D7E6B),
    ISLANDS("Tropical Islands", 46, 12, BlockType.SAND, BlockType.SAND, 0.02f, 0.03f, 0x64B5F6);

    companion object {
        fun fromClimate(temp: Float, moisture: Float): BiomeType {
            return when {
                temp < 0.25f -> SNOWY_MOUNTAINS
                temp > 0.75f && moisture < 0.35f -> DESERT
                moisture > 0.7f && temp > 0.4f -> SWAMP
                moisture > 0.5f -> FOREST
                temp > 0.6f && moisture > 0.55f -> ISLANDS
                else -> PLAINS
            }
        }
    }
}
