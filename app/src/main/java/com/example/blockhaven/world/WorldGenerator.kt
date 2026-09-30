package com.example.blockhaven.world

import java.util.Random

class WorldGenerator(val seed: Long) {
    private val terrainNoise = FastNoise(seed)
    private val detailNoise = FastNoise(seed xor 0x5555AAAA)
    private val caveNoise = FastNoise(seed xor 0x3333CCCC)
    private val tempNoise = FastNoise(seed xor 0x0F0F0F0F)
    private val moistureNoise = FastNoise(seed xor 0xF0F0F0F0)
    private val riverNoise = FastNoise(seed xor 0x7777BBBB)

    fun getBiomeAt(worldX: Int, worldZ: Int): BiomeType {
        val t = (tempNoise.fractal2D(worldX * 0.002f, worldZ * 0.002f, 2) + 1f) * 0.5f
        val m = (moistureNoise.fractal2D(worldX * 0.002f, worldZ * 0.002f, 2) + 1f) * 0.5f
        return BiomeType.fromClimate(t.coerceIn(0f, 1f), m.coerceIn(0f, 1f))
    }

    fun generateChunk(chunk: Chunk) {
        val startX = chunk.chunkX * Chunk.WIDTH
        val startZ = chunk.chunkZ * Chunk.DEPTH

        // Heightmap and biome cache
        val heightMap = IntArray(Chunk.WIDTH * Chunk.DEPTH)
        val biomeMap = Array(Chunk.WIDTH * Chunk.DEPTH) { BiomeType.PLAINS }

        for (x in 0 until Chunk.WIDTH) {
            for (z in 0 until Chunk.DEPTH) {
                val wx = startX + x
                val wz = startZ + z
                val biome = getBiomeAt(wx, wz)
                val idx = z * Chunk.WIDTH + x
                biomeMap[idx] = biome

                val baseN = terrainNoise.fractal2D(wx * 0.006f, wz * 0.006f, 4)
                val detN = detailNoise.fractal2D(wx * 0.025f, wz * 0.025f, 2) * 0.25f

                val rawH = biome.baseHeight + (baseN * biome.heightVariation) + (detN * 8f)
                var h = rawH.toInt().coerceIn(4, Chunk.HEIGHT - 8)

                // Winding river valley carving
                val rSample = kotlin.math.abs(riverNoise.fractal2D(wx * 0.0035f, wz * 0.0035f, 2))
                val riverThreshold = 0.040f
                if (rSample < riverThreshold) {
                    val riverRatio = rSample / riverThreshold // 0 at center, 1 at edge
                    val carveDepth = (1f - riverRatio) * (1f - riverRatio) * 16f
                    val carvedH = (h - carveDepth.toInt()).coerceAtLeast(Chunk.SEA_LEVEL - 3)
                    h = kotlin.math.min(h, carvedH)
                }
                heightMap[idx] = h
            }
        }

        // Fill blocks: Bedrock, Stone, Subsurface, Top, Water
        for (x in 0 until Chunk.WIDTH) {
            for (z in 0 until Chunk.DEPTH) {
                val wx = startX + x
                val wz = startZ + z
                val idx = z * Chunk.WIDTH + x
                val surfaceY = heightMap[idx]
                val biome = biomeMap[idx]

                // Bedrock at y=0
                chunk.setBlock(x, 0, z, BlockType.BEDROCK, recordMod = false)

                for (y in 1 until Chunk.HEIGHT) {
                    when {
                        y > surfaceY -> {
                            // Above surface: Water if below sea level, else Air
                            if (y <= Chunk.SEA_LEVEL) {
                                chunk.setBlock(x, y, z, BlockType.WATER, recordMod = false)
                            } else {
                                chunk.setBlock(x, y, z, BlockType.AIR, recordMod = false)
                            }
                        }
                        y == surfaceY -> {
                            // Surface block
                            if (surfaceY < Chunk.SEA_LEVEL - 1) {
                                chunk.setBlock(x, y, z, BlockType.SAND, recordMod = false)
                            } else if (surfaceY in (Chunk.SEA_LEVEL - 1)..Chunk.SEA_LEVEL) {
                                chunk.setBlock(x, y, z, BlockType.SAND, recordMod = false)
                            } else {
                                chunk.setBlock(x, y, z, biome.topBlock, recordMod = false)
                            }
                        }
                        y in (surfaceY - 3) until surfaceY -> {
                            // Subsurface layer
                            if (surfaceY <= Chunk.SEA_LEVEL + 1) {
                                chunk.setBlock(x, y, z, BlockType.SAND, recordMod = false)
                            } else {
                                chunk.setBlock(x, y, z, biome.subsurfaceBlock, recordMod = false)
                            }
                        }
                        else -> {
                            // Deep stone layer
                            chunk.setBlock(x, y, z, BlockType.STONE, recordMod = false)
                        }
                    }
                }
            }
        }

        // Carve 3D Caves with 3D Perlin noise
        for (x in 0 until Chunk.WIDTH) {
            for (z in 0 until Chunk.DEPTH) {
                val wx = startX + x
                val wz = startZ + z
                val surfaceY = heightMap[z * Chunk.WIDTH + x]
                val maxCaveY = (surfaceY - 4).coerceAtMost(Chunk.HEIGHT - 10)

                for (y in 2..maxCaveY) {
                    val caveVal = caveNoise.fractal3D(wx * 0.035f, y * 0.045f, wz * 0.035f, 2)
                    // Worm tunnel density
                    if (caveVal > 0.48f) {
                        val curr = chunk.getBlock(x, y, z)
                        if (curr != BlockType.BEDROCK && curr != BlockType.WATER) {
                            chunk.setBlock(x, y, z, BlockType.AIR, recordMod = false)
                        }
                    }
                }
            }
        }

        // Decorate with Ores using deterministic random for this chunk
        val chunkRand = Random(seed xor (chunk.chunkX.toLong() * 341873128712L + chunk.chunkZ.toLong() * 132897987541L))
        generateOreVeins(chunk, chunkRand)

        // Decorate surface: Trees, Cacti, Flowers, Tall Grass
        for (x in 2 until Chunk.WIDTH - 2) {
            for (z in 2 until Chunk.DEPTH - 2) {
                val idx = z * Chunk.WIDTH + x
                val surfaceY = heightMap[idx]
                if (surfaceY < Chunk.SEA_LEVEL) continue // underwater

                val topBlock = chunk.getBlock(x, surfaceY, z)
                val biome = biomeMap[idx]

                val rVal = chunkRand.nextFloat()
                if (topBlock == BlockType.GRASS) {
                    if (rVal < biome.treeDensity) {
                        generateTree(chunk, x, surfaceY + 1, z, biome == BiomeType.SNOWY_MOUNTAINS)
                    } else if (rVal < biome.treeDensity + biome.floraDensity) {
                        val flora = when (chunkRand.nextInt(3)) {
                            0 -> BlockType.TALL_GRASS
                            1 -> BlockType.FLOWER_CYAN
                            else -> BlockType.FLOWER_GOLDEN
                        }
                        chunk.setBlock(x, surfaceY + 1, z, flora, recordMod = false)
                    }
                } else if (topBlock == BlockType.SAND && biome == BiomeType.DESERT) {
                    if (rVal < 0.015f) {
                        val cactusH = 1 + chunkRand.nextInt(3)
                        for (cy in 1..cactusH) {
                            if (surfaceY + cy < Chunk.HEIGHT) {
                                chunk.setBlock(x, surfaceY + cy, z, BlockType.CACTUS, recordMod = false)
                            }
                        }
                    }
                }
            }
        }

        // Calculate sunlight propagation for this chunk
        computeSunlight(chunk)

        chunk.isGenerated = true
        chunk.isMeshDirty = true
    }

    private fun generateOreVeins(chunk: Chunk, rand: Random) {
        // Coal Ore: 18 veins per chunk, y: 5..85
        spawnVein(chunk, rand, 18, BlockType.COAL_ORE, 5, 85, 6)
        // Iron Ore: 12 veins per chunk, y: 5..60
        spawnVein(chunk, rand, 12, BlockType.IRON_ORE, 5, 60, 5)
        // Gold Ore: 4 veins per chunk, y: 4..32
        spawnVein(chunk, rand, 4, BlockType.GOLD_ORE, 4, 32, 4)
        // Diamond Ore: 2 veins per chunk, y: 2..16
        spawnVein(chunk, rand, 2, BlockType.DIAMOND_ORE, 2, 16, 3)
        // Etherite Ore (Mystic rare): 1 vein, y: 2..12
        spawnVein(chunk, rand, 1, BlockType.ETHERITE_ORE, 2, 12, 2)
        // Pyrite Ore: 3 veins, y: 6..28
        spawnVein(chunk, rand, 3, BlockType.PYRITE_ORE, 6, 28, 3)
        // Luminite Ore (Glowing crystal): 3 veins, y: 8..40
        spawnVein(chunk, rand, 3, BlockType.LUMINITE_ORE, 8, 40, 4)
    }

    private fun spawnVein(chunk: Chunk, rand: Random, count: Int, oreType: Short, minY: Int, maxY: Int, veinSize: Int) {
        for (i in 0 until count) {
            val ox = rand.nextInt(Chunk.WIDTH)
            val oz = rand.nextInt(Chunk.DEPTH)
            val oy = minY + rand.nextInt(maxY - minY + 1)

            for (v in 0 until veinSize) {
                val bx = (ox + rand.nextInt(3) - 1).coerceIn(0, Chunk.WIDTH - 1)
                val by = (oy + rand.nextInt(3) - 1).coerceIn(1, Chunk.HEIGHT - 2)
                val bz = (oz + rand.nextInt(3) - 1).coerceIn(0, Chunk.DEPTH - 1)

                if (chunk.getBlock(bx, by, bz) == BlockType.STONE) {
                    chunk.setBlock(bx, by, bz, oreType, recordMod = false)
                }
            }
        }
    }

    private fun generateTree(chunk: Chunk, x: Int, y: Int, z: Int, isPine: Boolean) {
        val treeHeight = if (isPine) 6 else 4
        if (y + treeHeight + 3 >= Chunk.HEIGHT) return

        // Trunk
        for (ty in 0 until treeHeight) {
            chunk.setBlock(x, y + ty, z, BlockType.WOOD_LOG, recordMod = false)
        }

        // Foliage
        val top = y + treeHeight
        if (isPine) {
            for (ly in (top - 3)..top) {
                val radius = if (ly == top) 1 else 2
                for (dx in -radius..radius) {
                    for (dz in -radius..radius) {
                        if (dx == 0 && dz == 0 && ly < top) continue
                        val bx = x + dx
                        val bz = z + dz
                        if (Chunk.isValidPos(bx, ly, bz) && chunk.getBlock(bx, ly, bz) == BlockType.AIR) {
                            chunk.setBlock(bx, ly, bz, BlockType.LEAVES, recordMod = false)
                        }
                    }
                }
            }
            if (Chunk.isValidPos(x, top + 1, z)) {
                chunk.setBlock(x, top + 1, z, BlockType.LEAVES, recordMod = false)
            }
        } else {
            for (ly in (top - 2)..top) {
                val radius = if (ly == top) 1 else 2
                for (dx in -radius..radius) {
                    for (dz in -radius..radius) {
                        if (dx * dx + dz * dz > radius * radius + 1) continue
                        val bx = x + dx
                        val bz = z + dz
                        if (Chunk.isValidPos(bx, ly, bz) && chunk.getBlock(bx, ly, bz) == BlockType.AIR) {
                            chunk.setBlock(bx, ly, bz, BlockType.LEAVES, recordMod = false)
                        }
                    }
                }
            }
            if (Chunk.isValidPos(x, top + 1, z)) {
                chunk.setBlock(x, top + 1, z, BlockType.LEAVES, recordMod = false)
            }
        }
    }

    private fun computeSunlight(chunk: Chunk) {
        for (x in 0 until Chunk.WIDTH) {
            for (z in 0 until Chunk.DEPTH) {
                var sunLevel = 15
                for (y in (Chunk.HEIGHT - 1) downTo 0) {
                    val block = chunk.getBlock(x, y, z)
                    if (block == BlockType.AIR || BlockType.isTransparent(block)) {
                        chunk.setSunlight(x, y, z, sunLevel)
                        if (block == BlockType.WATER) {
                            sunLevel = (sunLevel - 2).coerceAtLeast(0)
                        }
                    } else {
                        chunk.setSunlight(x, y, z, 0)
                        sunLevel = 0
                    }
                }
            }
        }
    }
}
