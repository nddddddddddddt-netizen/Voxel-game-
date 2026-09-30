package com.example.blockhaven.render

import android.graphics.Bitmap
import android.graphics.Color
import android.opengl.GLES30
import android.opengl.GLUtils
import java.util.Random
import kotlin.math.cos
import kotlin.math.sin

object TextureAtlas {
    const val TILE_SIZE = 32
    const val TILES_PER_ROW = 16
    const val ATLAS_SIZE = TILE_SIZE * TILES_PER_ROW // 512x512

    var textureId: Int = 0
        private set

    fun getUV(tileIndex: Int): FloatArray {
        val col = tileIndex % TILES_PER_ROW
        val row = tileIndex / TILES_PER_ROW

        val u0 = (col * TILE_SIZE).toFloat() / ATLAS_SIZE
        val v0 = (row * TILE_SIZE).toFloat() / ATLAS_SIZE
        val u1 = ((col + 1) * TILE_SIZE).toFloat() / ATLAS_SIZE
        val v1 = ((row + 1) * TILE_SIZE).toFloat() / ATLAS_SIZE

        return floatArrayOf(u0, v0, u1, v1)
    }

    fun init() {
        if (textureId != 0) return

        val pixels = IntArray(ATLAS_SIZE * ATLAS_SIZE)
        generateHighFidelityTextures(pixels)

        val bitmap = Bitmap.createBitmap(pixels, ATLAS_SIZE, ATLAS_SIZE, Bitmap.Config.ARGB_8888)

        val textures = IntArray(1)
        GLES30.glGenTextures(1, textures, 0)
        textureId = textures[0]

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST_MIPMAP_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)

        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0)
        GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D)

        bitmap.recycle()
    }

    private inline fun setTexel(pixels: IntArray, ox: Int, oy: Int, x: Int, y: Int, color: Int) {
        if (x in 0 until TILE_SIZE && y in 0 until TILE_SIZE) {
            pixels[(oy + y) * ATLAS_SIZE + (ox + x)] = color
        }
    }

    private inline fun rgb(r: Int, g: Int, b: Int): Int {
        return (0xFF shl 24) or ((r.coerceIn(0, 255)) shl 16) or ((g.coerceIn(0, 255)) shl 8) or (b.coerceIn(0, 255))
    }

    private inline fun rgba(r: Int, g: Int, b: Int, a: Int): Int {
        return ((a.coerceIn(0, 255)) shl 24) or ((r.coerceIn(0, 255)) shl 16) or ((g.coerceIn(0, 255)) shl 8) or (b.coerceIn(0, 255))
    }

    private fun generateHighFidelityTextures(pixels: IntArray) {
        val rand = Random(424242L)

        for (tile in 0..33) {
            val ox = (tile % TILES_PER_ROW) * TILE_SIZE
            val oy = (tile / TILES_PER_ROW) * TILE_SIZE

            when (tile) {
                0 -> drawGrassTop(pixels, ox, oy, rand)
                1 -> drawGrassSide(pixels, ox, oy, rand)
                2 -> drawDirt(pixels, ox, oy, rand)
                3 -> drawStone(pixels, ox, oy, rand)
                4 -> drawCobblestone(pixels, ox, oy, rand)
                5 -> drawSand(pixels, ox, oy, rand)
                6 -> drawSnow(pixels, ox, oy, rand)
                7 -> drawWoodLogTop(pixels, ox, oy)
                8 -> drawWoodLogSide(pixels, ox, oy, rand)
                9 -> drawWoodPlanks(pixels, ox, oy)
                10 -> drawLeaves(pixels, ox, oy, rand)
                11 -> drawGlass(pixels, ox, oy)
                12 -> drawWater(pixels, ox, oy, rand)
                13 -> drawBedrock(pixels, ox, oy, rand)
                14 -> drawOre(pixels, ox, oy, 28, 28, 30, rand) // Coal Ore
                15 -> drawOre(pixels, ox, oy, 218, 160, 115, rand) // Iron Ore
                16 -> drawOre(pixels, ox, oy, 255, 215, 30, rand) // Gold Ore
                17 -> drawOre(pixels, ox, oy, 70, 240, 240, rand) // Diamond Ore
                18 -> drawOre(pixels, ox, oy, 195, 80, 255, rand) // Etherite Ore
                19 -> drawOre(pixels, ox, oy, 255, 110, 25, rand) // Pyrite Ore
                20 -> drawLuminite(pixels, ox, oy, rand) // Luminite Crystal
                21 -> drawStoneBricks(pixels, ox, oy)
                22 -> drawCraftingTableTop(pixels, ox, oy)
                23 -> drawCraftingTableSide(pixels, ox, oy)
                24 -> drawFurnaceFront(pixels, ox, oy)
                25 -> drawTorch(pixels, ox, oy)
                26 -> drawBookshelf(pixels, ox, oy, rand)
                27 -> drawObsidian(pixels, ox, oy, rand)
                28 -> drawClay(pixels, ox, oy, rand)
                29 -> drawCactusTop(pixels, ox, oy)
                30 -> drawCactusSide(pixels, ox, oy, rand)
                31 -> drawTallGrass(pixels, ox, oy, rand)
                32 -> drawFlower(pixels, ox, oy, 30, 205, 245)
                33 -> drawFlower(pixels, ox, oy, 255, 215, 25)
            }
        }
    }

    private fun drawGrassTop(pixels: IntArray, ox: Int, oy: Int, rand: Random) {
        val baseR = 72; val baseG = 158; val baseB = 48
        for (y in 0 until TILE_SIZE) {
            for (x in 0 until TILE_SIZE) {
                val blade = if ((x + y) % 3 == 0) 14 else if ((x * 3 + y * 5) % 7 == 0) -10 else 0
                val noise = rand.nextInt(18) - 9 + blade
                // Subtle edge bevel
                val edgeShade = if (x == 0 || y == 0) -12 else if (x == TILE_SIZE - 1 || y == TILE_SIZE - 1) 8 else 0
                setTexel(pixels, ox, oy, x, y, rgb(baseR + noise + edgeShade, baseG + noise * 2 + edgeShade, baseB + noise + edgeShade))
            }
        }
    }

    private fun drawGrassSide(pixels: IntArray, ox: Int, oy: Int, rand: Random) {
        drawDirt(pixels, ox, oy, rand)
        // Hanging grass blades with subtle variation
        for (x in 0 until TILE_SIZE) {
            val depth = 6 + ((sin(x * 0.8f) * 2f).toInt()) + rand.nextInt(3)
            for (y in 0 until depth) {
                val shade = (y * 5) - 10
                val r = (72 + rand.nextInt(15) + shade).coerceIn(40, 120)
                val g = (158 + rand.nextInt(20) + shade).coerceIn(100, 210)
                val b = (48 + rand.nextInt(15) + shade).coerceIn(30, 90)
                setTexel(pixels, ox, oy, x, y, rgb(r, g, b))
            }
        }
    }

    private fun drawDirt(pixels: IntArray, ox: Int, oy: Int, rand: Random) {
        val baseR = 120; val baseG = 82; val baseB = 46
        for (y in 0 until TILE_SIZE) {
            for (x in 0 until TILE_SIZE) {
                val pebble = if ((x * 7 + y * 11) % 19 == 0) 25 else if ((x * 13 + y * 3) % 23 == 0) -20 else 0
                val noise = rand.nextInt(20) - 10 + pebble
                setTexel(pixels, ox, oy, x, y, rgb(baseR + noise, baseG + (noise * 0.7f).toInt(), baseB + (noise * 0.5f).toInt()))
            }
        }
    }

    private fun drawStone(pixels: IntArray, ox: Int, oy: Int, rand: Random) {
        val base = 126
        for (y in 0 until TILE_SIZE) {
            for (x in 0 until TILE_SIZE) {
                val vein = (sin(x * 0.35f + y * 0.2f) * 12).toInt()
                val grain = rand.nextInt(16) - 8 + vein
                val v = (base + grain).coerceIn(0, 255)
                // Micro-contrast for realistic granite/stone feel
                val blueTint = if ((x + y) % 4 == 0) 4 else 0
                setTexel(pixels, ox, oy, x, y, rgb(v - 2, v, v + blueTint))
            }
        }
    }

    private fun drawCobblestone(pixels: IntArray, ox: Int, oy: Int, rand: Random) {
        drawStone(pixels, ox, oy, rand)
        // Mortar lines and stone segment highlights
        val mortarColor = rgb(55, 55, 60)
        val highlightColor = rgb(150, 150, 155)

        for (x in 0 until TILE_SIZE) {
            setTexel(pixels, ox, oy, x, 0, mortarColor)
            setTexel(pixels, ox, oy, x, 15, mortarColor)
            setTexel(pixels, ox, oy, x, 16, mortarColor)
            setTexel(pixels, ox, oy, x, 31, mortarColor)
            // Bevel highlights
            if (x !in listOf(0, 15, 16, 31)) {
                setTexel(pixels, ox, oy, x, 1, highlightColor)
                setTexel(pixels, ox, oy, x, 17, highlightColor)
            }
        }
        for (y in 0 until 16) {
            setTexel(pixels, ox, oy, 15, y, mortarColor)
            setTexel(pixels, ox, oy, 16, y, mortarColor)
        }
        for (y in 16 until 32) {
            setTexel(pixels, ox, oy, 7, y, mortarColor)
            setTexel(pixels, ox, oy, 8, y, mortarColor)
            setTexel(pixels, ox, oy, 23, y, mortarColor)
            setTexel(pixels, ox, oy, 24, y, mortarColor)
        }
    }

    private fun drawSand(pixels: IntArray, ox: Int, oy: Int, rand: Random) {
        val baseR = 226; val baseG = 202; val baseB = 138
        for (y in 0 until TILE_SIZE) {
            for (x in 0 until TILE_SIZE) {
                val wave = (sin(x * 0.45f + y * 0.25f) * 10).toInt()
                val sparkle = if (rand.nextInt(40) == 0) 18 else 0
                val noise = rand.nextInt(14) - 7 + wave + sparkle
                setTexel(pixels, ox, oy, x, y, rgb(baseR + noise, baseG + (noise * 0.9f).toInt(), baseB + (noise * 0.6f).toInt()))
            }
        }
    }

    private fun drawSnow(pixels: IntArray, ox: Int, oy: Int, rand: Random) {
        for (y in 0 until TILE_SIZE) {
            for (x in 0 until TILE_SIZE) {
                val sparkle = if (rand.nextInt(25) == 0) 12 else 0
                val noise = rand.nextInt(10) - 5 + sparkle
                val r = (245 + noise).coerceIn(220, 255)
                val g = (248 + noise).coerceIn(225, 255)
                val b = (255).coerceIn(230, 255)
                setTexel(pixels, ox, oy, x, y, rgb(r, g, b))
            }
        }
    }

    private fun drawWoodLogTop(pixels: IntArray, ox: Int, oy: Int) {
        val bark = rgb(86, 56, 28)
        val outerWood = rgb(178, 136, 84)
        val ringDark = rgb(142, 102, 60)
        val innerCore = rgb(125, 88, 50)

        for (y in 0 until TILE_SIZE) {
            for (x in 0 until TILE_SIZE) {
                val dx = x - 15.5f
                val dy = y - 15.5f
                val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                val col = when {
                    dist > 14.5f -> bark
                    dist in 11.5f..13.0f -> ringDark
                    dist in 7.5f..9.0f -> ringDark
                    dist in 3.5f..5.0f -> ringDark
                    dist <= 2.5f -> innerCore
                    else -> outerWood
                }
                setTexel(pixels, ox, oy, x, y, col)
            }
        }
    }

    private fun drawWoodLogSide(pixels: IntArray, ox: Int, oy: Int, rand: Random) {
        for (y in 0 until TILE_SIZE) {
            for (x in 0 until TILE_SIZE) {
                val furrow = if ((x % 8 == 0) || ((x + 3) % 8 == 0 && (y % 6 == 0))) -25 else 0
                val grain = (sin(x * 0.9f) * 14).toInt()
                val noise = rand.nextInt(12) - 6 + furrow + grain
                val r = (92 + noise).coerceIn(40, 160)
                val g = (62 + (noise * 0.7f).toInt()).coerceIn(25, 120)
                val b = (32 + (noise * 0.4f).toInt()).coerceIn(15, 80)
                setTexel(pixels, ox, oy, x, y, rgb(r, g, b))
            }
        }
    }

    private fun drawWoodPlanks(pixels: IntArray, ox: Int, oy: Int) {
        val baseR = 178; val baseG = 132; val baseB = 78
        val seamColor = rgb(88, 58, 28)
        val highlightColor = rgb(205, 158, 102)

        for (y in 0 until TILE_SIZE) {
            val plankIndex = y / 8
            val isSeamY = y % 8 == 7
            val isHighlightY = y % 8 == 0

            for (x in 0 until TILE_SIZE) {
                if (isSeamY) {
                    setTexel(pixels, ox, oy, x, y, seamColor)
                } else if (isHighlightY) {
                    setTexel(pixels, ox, oy, x, y, highlightColor)
                } else {
                    // Vertical seams staggered per row
                    val seamX = when (plankIndex) {
                        0 -> 14
                        1 -> 24
                        2 -> 8
                        else -> 20
                    }
                    if (x == seamX) {
                        setTexel(pixels, ox, oy, x, y, seamColor)
                    } else if (x == seamX + 1) {
                        setTexel(pixels, ox, oy, x, y, highlightColor)
                    } else {
                        val grain = ((x * 3 + y * 7) % 5) * 4 - 8
                        setTexel(pixels, ox, oy, x, y, rgb(baseR + grain, baseG + grain, baseB + (grain * 0.6f).toInt()))
                    }
                }
            }
        }
    }

    private fun drawLeaves(pixels: IntArray, ox: Int, oy: Int, rand: Random) {
        for (y in 0 until TILE_SIZE) {
            for (x in 0 until TILE_SIZE) {
                // Transparent openings for realistic volumetric foliage
                if ((x * 5 + y * 13) % 17 == 0 || (x * 7 + y * 3) % 23 == 0) {
                    setTexel(pixels, ox, oy, x, y, rgba(0, 0, 0, 0))
                } else {
                    val leafVar = if ((x + y) % 3 == 0) 22 else -16
                    val noise = rand.nextInt(24) - 12 + leafVar
                    val r = (38 + noise).coerceIn(20, 90)
                    val g = (142 + noise * 2).coerceIn(80, 220)
                    val b = (32 + noise).coerceIn(15, 80)
                    setTexel(pixels, ox, oy, x, y, rgba(r, g, b, 255))
                }
            }
        }
    }

    private fun drawGlass(pixels: IntArray, ox: Int, oy: Int) {
        val frameColor = rgb(190, 225, 235)
        val glintColor = rgba(255, 255, 255, 180)
        val paneColor = rgba(180, 220, 240, 55)

        for (y in 0 until TILE_SIZE) {
            for (x in 0 until TILE_SIZE) {
                val isFrame = (x <= 1 || x >= TILE_SIZE - 2 || y <= 1 || y >= TILE_SIZE - 2)
                val isGlint = (x + y in 8..10) || (x + y in 24..26)
                val col = when {
                    isFrame -> frameColor
                    isGlint -> glintColor
                    else -> paneColor
                }
                setTexel(pixels, ox, oy, x, y, col)
            }
        }
    }

    private fun drawWater(pixels: IntArray, ox: Int, oy: Int, rand: Random) {
        val baseR = 35; val baseG = 120; val baseB = 225
        for (y in 0 until TILE_SIZE) {
            for (x in 0 until TILE_SIZE) {
                val ripple = (sin(x * 0.4f + y * 0.3f) * 20).toInt()
                val sparkle = if ((x * 7 + y * 13) % 19 == 0) 40 else 0
                val r = (baseR + ripple + sparkle).coerceIn(15, 140)
                val g = (baseG + ripple + sparkle).coerceIn(80, 210)
                val b = (baseB + (ripple * 0.5f).toInt()).coerceIn(170, 255)
                setTexel(pixels, ox, oy, x, y, rgba(r, g, b, 195))
            }
        }
    }

    private fun drawBedrock(pixels: IntArray, ox: Int, oy: Int, rand: Random) {
        for (y in 0 until TILE_SIZE) {
            for (x in 0 until TILE_SIZE) {
                val noise = rand.nextInt(75)
                setTexel(pixels, ox, oy, x, y, rgb(noise, noise, noise + 4))
            }
        }
    }

    private fun drawOre(pixels: IntArray, ox: Int, oy: Int, gemR: Int, gemG: Int, gemB: Int, rand: Random) {
        drawStone(pixels, ox, oy, rand)

        // Multiple realistic ore crystal clusters
        val spots = listOf(
            Pair(7, 8), Pair(8, 9), Pair(9, 8), Pair(8, 7), Pair(9, 9),
            Pair(20, 21), Pair(21, 22), Pair(22, 21), Pair(21, 20),
            Pair(21, 8), Pair(22, 9), Pair(7, 22), Pair(8, 23)
        )

        for (pt in spots) {
            val hx = pt.first
            val hy = pt.second
            val highlight = rgb(
                (gemR + 50).coerceIn(0, 255),
                (gemG + 50).coerceIn(0, 255),
                (gemB + 50).coerceIn(0, 255)
            )
            val shadow = rgb(
                (gemR - 40).coerceIn(0, 255),
                (gemG - 40).coerceIn(0, 255),
                (gemB - 40).coerceIn(0, 255)
            )
            val baseGem = rgb(gemR, gemG, gemB)

            setTexel(pixels, ox, oy, hx, hy, baseGem)
            setTexel(pixels, ox, oy, hx - 1, hy, shadow)
            setTexel(pixels, ox, oy, hx, hy - 1, highlight)
        }
    }

    private fun drawLuminite(pixels: IntArray, ox: Int, oy: Int, rand: Random) {
        drawStone(pixels, ox, oy, rand)
        // Glowing cyan-gold core
        for (y in 8 until 24) {
            for (x in 8 until 24) {
                val dx = x - 15.5f
                val dy = y - 15.5f
                val d = kotlin.math.sqrt(dx * dx + dy * dy)
                if (d < 7f) {
                    val bright = ((7f - d) * 35).toInt()
                    setTexel(pixels, ox, oy, x, y, rgb(255, 235 - bright / 2, 80 + bright))
                }
            }
        }
    }

    private fun drawStoneBricks(pixels: IntArray, ox: Int, oy: Int) {
        val baseR = 140; val baseG = 140; val baseB = 145
        val mortar = rgb(65, 65, 70)
        val highlight = rgb(175, 175, 180)

        for (y in 0 until TILE_SIZE) {
            val row = y / 8
            val isSeamY = y % 8 == 7
            val isHighlightY = y % 8 == 0

            for (x in 0 until TILE_SIZE) {
                if (isSeamY) {
                    setTexel(pixels, ox, oy, x, y, mortar)
                } else if (isHighlightY) {
                    setTexel(pixels, ox, oy, x, y, highlight)
                } else {
                    val seamX = if (row % 2 == 0) 15 else 7
                    val seamX2 = if (row % 2 == 0) 31 else 23
                    if (x == seamX || x == seamX2) {
                        setTexel(pixels, ox, oy, x, y, mortar)
                    } else if (x == seamX + 1 || x == seamX2 + 1) {
                        setTexel(pixels, ox, oy, x, y, highlight)
                    } else {
                        val n = ((x * 5 + y * 11) % 7) * 4 - 12
                        setTexel(pixels, ox, oy, x, y, rgb(baseR + n, baseG + n, baseB + n))
                    }
                }
            }
        }
    }

    private fun drawCraftingTableTop(pixels: IntArray, ox: Int, oy: Int) {
        drawWoodPlanks(pixels, ox, oy)
        // Grid square outline and tools
        val gridColor = rgb(65, 45, 25)
        for (i in 4 until 28) {
            setTexel(pixels, ox, oy, i, 4, gridColor)
            setTexel(pixels, ox, oy, i, 16, gridColor)
            setTexel(pixels, ox, oy, i, 28, gridColor)
            setTexel(pixels, ox, oy, 4, i, gridColor)
            setTexel(pixels, ox, oy, 16, i, gridColor)
            setTexel(pixels, ox, oy, 28, i, gridColor)
        }
    }

    private fun drawCraftingTableSide(pixels: IntArray, ox: Int, oy: Int) {
        drawWoodPlanks(pixels, ox, oy)
        // Hammer and saw silhouette
        val iron = rgb(215, 215, 220)
        val handle = rgb(110, 75, 40)
        for (i in 8..14) {
            setTexel(pixels, ox, oy, 10, i, handle)
            setTexel(pixels, ox, oy, 22, i, handle)
        }
        for (i in 8..13) {
            setTexel(pixels, ox, oy, i, 7, iron)
            setTexel(pixels, ox, oy, i + 12, 7, iron)
        }
    }

    private fun drawFurnaceFront(pixels: IntArray, ox: Int, oy: Int) {
        drawCobblestone(pixels, ox, oy, Random(123L))
        // Smelting fire aperture
        val fireR = 255; val fireG = 145; val fireB = 25
        val coalR = 30; val coalG = 30; val coalB = 30
        for (y in 14 until 26) {
            for (x in 8 until 24) {
                val isFire = y >= 20 || (x in 12..20 && y >= 17)
                val c = if (isFire) rgb(fireR, fireG, fireB) else rgb(coalR, coalG, coalB)
                setTexel(pixels, ox, oy, x, y, c)
            }
        }
    }

    private fun drawTorch(pixels: IntArray, ox: Int, oy: Int) {
        for (y in 0 until TILE_SIZE) {
            for (x in 0 until TILE_SIZE) {
                setTexel(pixels, ox, oy, x, y, rgba(0, 0, 0, 0))
            }
        }
        // Wooden handle
        val handle = rgb(135, 95, 50)
        val handleShadow = rgb(95, 65, 35)
        for (y in 14 until 30) {
            for (x in 14..17) {
                val col = if (x == 17) handleShadow else handle
                setTexel(pixels, ox, oy, x, y, col)
            }
        }
        // Glowing flame with warm yellow/orange/red gradient
        for (y in 4..13) {
            for (x in 12..19) {
                val dx = x - 15.5f
                val dy = y - 9.5f
                if (dx * dx + dy * dy <= 16f) {
                    val isCenter = (dx * dx + dy * dy <= 4f)
                    val isTop = y <= 6
                    val col = when {
                        isCenter -> rgb(255, 255, 180)
                        isTop -> rgb(255, 120, 20)
                        else -> rgb(255, 195, 30)
                    }
                    setTexel(pixels, ox, oy, x, y, col)
                }
            }
        }
    }

    private fun drawBookshelf(pixels: IntArray, ox: Int, oy: Int, rand: Random) {
        drawWoodPlanks(pixels, ox, oy)
        val spineColors = listOf(
            rgb(180, 45, 45), // Crimson
            rgb(45, 110, 180), // Sapphire
            rgb(45, 160, 65), // Emerald
            rgb(160, 140, 45), // Gold
            rgb(120, 50, 150)  // Amethyst
        )
        // Two shelves of books
        for (shelf in listOf(4, 18)) {
            var bx = 3
            var colorIdx = 0
            while (bx < 28) {
                val bookWidth = 2 + (bx % 2)
                val bookColor = spineColors[colorIdx % spineColors.size]
                colorIdx++
                for (y in shelf until (shelf + 10)) {
                    for (w in 0 until bookWidth) {
                        setTexel(pixels, ox, oy, bx + w, y, bookColor)
                    }
                }
                bx += bookWidth + 1
            }
        }
    }

    private fun drawObsidian(pixels: IntArray, ox: Int, oy: Int, rand: Random) {
        val base = 25
        for (y in 0 until TILE_SIZE) {
            for (x in 0 until TILE_SIZE) {
                val crystal = if ((x * 11 + y * 7) % 19 == 0) 35 else 0
                val n = rand.nextInt(16) + crystal
                setTexel(pixels, ox, oy, x, y, rgb(base + n, (base * 0.7f + n * 0.8f).toInt(), (base * 1.5f + n * 1.6f).toInt()))
            }
        }
    }

    private fun drawClay(pixels: IntArray, ox: Int, oy: Int, rand: Random) {
        val baseR = 155; val baseG = 160; val baseB = 168
        for (y in 0 until TILE_SIZE) {
            for (x in 0 until TILE_SIZE) {
                val n = rand.nextInt(14) - 7
                setTexel(pixels, ox, oy, x, y, rgb(baseR + n, baseG + n, baseB + n))
            }
        }
    }

    private fun drawCactusTop(pixels: IntArray, ox: Int, oy: Int) {
        val green = rgb(55, 135, 40)
        val rim = rgb(35, 95, 25)
        for (y in 0 until TILE_SIZE) {
            for (x in 0 until TILE_SIZE) {
                val isRim = x <= 2 || x >= 29 || y <= 2 || y >= 29
                setTexel(pixels, ox, oy, x, y, if (isRim) rim else green)
            }
        }
    }

    private fun drawCactusSide(pixels: IntArray, ox: Int, oy: Int, rand: Random) {
        val green = rgb(65, 145, 48)
        val rib = rgb(45, 110, 32)
        val spine = rgb(240, 240, 210)

        for (y in 0 until TILE_SIZE) {
            for (x in 0 until TILE_SIZE) {
                val isRib = x % 6 == 0
                val isSpine = isRib && (y % 7 == 3)
                val col = when {
                    isSpine -> spine
                    isRib -> rib
                    else -> green
                }
                setTexel(pixels, ox, oy, x, y, col)
            }
        }
    }

    private fun drawTallGrass(pixels: IntArray, ox: Int, oy: Int, rand: Random) {
        for (y in 0 until TILE_SIZE) {
            for (x in 0 until TILE_SIZE) {
                setTexel(pixels, ox, oy, x, y, rgba(0, 0, 0, 0))
            }
        }
        val grassColor = rgb(75, 175, 50)
        val stalkColor = rgb(55, 135, 35)

        for (x in 4..27 step 3) {
            val height = 18 + rand.nextInt(10)
            for (y in (32 - height) until 32) {
                setTexel(pixels, ox, oy, x, y, if (y > 26) stalkColor else grassColor)
                if (y % 4 == 0 && x > 0) {
                    setTexel(pixels, ox, oy, x - 1, y, grassColor)
                }
            }
        }
    }

    private fun drawFlower(pixels: IntArray, ox: Int, oy: Int, petalR: Int, petalG: Int, petalB: Int) {
        for (y in 0 until TILE_SIZE) {
            for (x in 0 until TILE_SIZE) {
                setTexel(pixels, ox, oy, x, y, rgba(0, 0, 0, 0))
            }
        }
        val stem = rgb(65, 150, 45)
        val petal = rgb(petalR, petalG, petalB)
        val center = rgb(255, 235, 50)

        // Stem
        for (y in 16 until 31) {
            setTexel(pixels, ox, oy, 15, y, stem)
            setTexel(pixels, ox, oy, 16, y, stem)
        }
        // Flower Petals
        val cx = 15.5f
        val cy = 13.5f
        for (y in 8..19) {
            for (x in 10..21) {
                val dx = x - cx
                val dy = y - cy
                val dist = dx * dx + dy * dy
                if (dist <= 24f) {
                    val col = if (dist <= 4f) center else petal
                    setTexel(pixels, ox, oy, x, y, col)
                }
            }
        }
    }
}
