package com.example.blockhaven.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.opengl.GLES30
import android.opengl.GLUtils
import java.util.Random

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

        val bitmap = Bitmap.createBitmap(ATLAS_SIZE, ATLAS_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Generate procedural pixel art for all tiles
        generateAllTiles(canvas, paint)

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

    private fun generateAllTiles(canvas: Canvas, paint: Paint) {
        val rand = Random(424242L)

        for (tile in 0..33) {
            val ox = (tile % TILES_PER_ROW) * TILE_SIZE
            val oy = (tile / TILES_PER_ROW) * TILE_SIZE

            when (tile) {
                0 -> drawGrassTop(canvas, ox, oy, rand)
                1 -> drawGrassSide(canvas, ox, oy, rand)
                2 -> drawDirt(canvas, ox, oy, rand)
                3 -> drawStone(canvas, ox, oy, rand)
                4 -> drawCobblestone(canvas, ox, oy, rand)
                5 -> drawSand(canvas, ox, oy, rand)
                6 -> drawSnow(canvas, ox, oy, rand)
                7 -> drawWoodLogTop(canvas, ox, oy)
                8 -> drawWoodLogSide(canvas, ox, oy, rand)
                9 -> drawWoodPlanks(canvas, ox, oy)
                10 -> drawLeaves(canvas, ox, oy, rand)
                11 -> drawGlass(canvas, ox, oy)
                12 -> drawWater(canvas, ox, oy, rand)
                13 -> drawBedrock(canvas, ox, oy, rand)
                14 -> drawOre(canvas, ox, oy, Color.rgb(20, 20, 20), rand) // Coal
                15 -> drawOre(canvas, ox, oy, Color.rgb(215, 150, 105), rand) // Iron
                16 -> drawOre(canvas, ox, oy, Color.rgb(255, 215, 0), rand) // Gold
                17 -> drawOre(canvas, ox, oy, Color.rgb(80, 235, 235), rand) // Diamond
                18 -> drawOre(canvas, ox, oy, Color.rgb(180, 70, 255), rand) // Etherite
                19 -> drawOre(canvas, ox, oy, Color.rgb(255, 95, 20), rand) // Pyrite
                20 -> drawLuminite(canvas, ox, oy, rand) // Luminite
                21 -> drawStoneBricks(canvas, ox, oy)
                22 -> drawCraftingTableTop(canvas, ox, oy)
                23 -> drawCraftingTableSide(canvas, ox, oy)
                24 -> drawFurnaceFront(canvas, ox, oy)
                25 -> drawTorch(canvas, ox, oy)
                26 -> drawBookshelf(canvas, ox, oy, rand)
                27 -> drawObsidian(canvas, ox, oy, rand)
                28 -> drawClay(canvas, ox, oy, rand)
                29 -> drawCactusTop(canvas, ox, oy)
                30 -> drawCactusSide(canvas, ox, oy, rand)
                31 -> drawTallGrass(canvas, ox, oy, rand)
                32 -> drawFlower(canvas, ox, oy, Color.rgb(30, 200, 240))
                33 -> drawFlower(canvas, ox, oy, Color.rgb(255, 210, 20))
            }
        }
    }

    private fun drawGrassTop(canvas: Canvas, ox: Int, oy: Int, rand: Random) {
        val baseR = 75; val baseG = 165; val baseB = 55
        for (y in 0 until TILE_SIZE) {
            for (x in 0 until TILE_SIZE) {
                val noise = rand.nextInt(25) - 12
                val c = Color.rgb(
                    (baseR + noise).coerceIn(0, 255),
                    (baseG + noise * 2).coerceIn(0, 255),
                    (baseB + noise).coerceIn(0, 255)
                )
                canvas.drawPoint((ox + x).toFloat(), (oy + y).toFloat(), Paint().apply { color = c })
            }
        }
    }

    private fun drawGrassSide(canvas: Canvas, ox: Int, oy: Int, rand: Random) {
        drawDirt(canvas, ox, oy, rand)
        // Green fringe on top 6-8 pixels
        for (x in 0 until TILE_SIZE) {
            val hang = 6 + (x % 3) + rand.nextInt(3)
            for (y in 0 until hang) {
                val c = Color.rgb(75 + rand.nextInt(20), 160 + rand.nextInt(30), 50 + rand.nextInt(20))
                canvas.drawPoint((ox + x).toFloat(), (oy + y).toFloat(), Paint().apply { color = c })
            }
        }
    }

    private fun drawDirt(canvas: Canvas, ox: Int, oy: Int, rand: Random) {
        val baseR = 125; val baseG = 85; val baseB = 50
        for (y in 0 until TILE_SIZE) {
            for (x in 0 until TILE_SIZE) {
                val noise = rand.nextInt(30) - 15
                val c = Color.rgb(
                    (baseR + noise).coerceIn(0, 255),
                    (baseG + noise).coerceIn(0, 255),
                    (baseB + noise).coerceIn(0, 255)
                )
                canvas.drawPoint((ox + x).toFloat(), (oy + y).toFloat(), Paint().apply { color = c })
            }
        }
    }

    private fun drawStone(canvas: Canvas, ox: Int, oy: Int, rand: Random) {
        val base = 128
        for (y in 0 until TILE_SIZE) {
            for (x in 0 until TILE_SIZE) {
                val noise = rand.nextInt(36) - 18
                val v = (base + noise).coerceIn(0, 255)
                canvas.drawPoint((ox + x).toFloat(), (oy + y).toFloat(), Paint().apply { color = Color.rgb(v, v, v + 2) })
            }
        }
    }

    private fun drawCobblestone(canvas: Canvas, ox: Int, oy: Int, rand: Random) {
        drawStone(canvas, ox, oy, rand)
        // Mortar lines
        val paint = Paint().apply { color = Color.rgb(60, 60, 65) }
        canvas.drawRect(ox.toFloat(), oy.toFloat(), (ox + TILE_SIZE).toFloat(), (oy + 1).toFloat(), paint)
        canvas.drawRect(ox.toFloat(), (oy + 16).toFloat(), (ox + TILE_SIZE).toFloat(), (oy + 17).toFloat(), paint)
        canvas.drawRect((ox + 16).toFloat(), oy.toFloat(), (ox + 17).toFloat(), (oy + 16).toFloat(), paint)
        canvas.drawRect((ox + 8).toFloat(), (oy + 16).toFloat(), (ox + 9).toFloat(), (oy + 32).toFloat(), paint)
        canvas.drawRect((ox + 24).toFloat(), (oy + 16).toFloat(), (ox + 25).toFloat(), (oy + 32).toFloat(), paint)
    }

    private fun drawSand(canvas: Canvas, ox: Int, oy: Int, rand: Random) {
        val baseR = 225; val baseG = 205; val baseB = 140
        for (y in 0 until TILE_SIZE) {
            for (x in 0 until TILE_SIZE) {
                val wave = (kotlin.math.sin(x * 0.4f + y * 0.2f) * 8).toInt()
                val noise = rand.nextInt(16) - 8 + wave
                val c = Color.rgb(
                    (baseR + noise).coerceIn(0, 255),
                    (baseG + noise).coerceIn(0, 255),
                    (baseB + noise).coerceIn(0, 255)
                )
                canvas.drawPoint((ox + x).toFloat(), (oy + y).toFloat(), Paint().apply { color = c })
            }
        }
    }

    private fun drawSnow(canvas: Canvas, ox: Int, oy: Int, rand: Random) {
        for (y in 0 until TILE_SIZE) {
            for (x in 0 until TILE_SIZE) {
                val noise = rand.nextInt(12) - 6
                val c = Color.rgb(
                    (245 + noise).coerceIn(0, 255),
                    (248 + noise).coerceIn(0, 255),
                    (255 + noise).coerceIn(0, 255)
                )
                canvas.drawPoint((ox + x).toFloat(), (oy + y).toFloat(), Paint().apply { color = c })
            }
        }
    }

    private fun drawWoodLogTop(canvas: Canvas, ox: Int, oy: Int) {
        val p = Paint()
        // Bark border
        p.color = Color.rgb(90, 60, 30)
        canvas.drawRect(ox.toFloat(), oy.toFloat(), (ox + TILE_SIZE).toFloat(), (oy + TILE_SIZE).toFloat(), p)
        // Growth rings
        p.color = Color.rgb(180, 140, 90)
        canvas.drawRect((ox + 3).toFloat(), (oy + 3).toFloat(), (ox + TILE_SIZE - 3).toFloat(), (oy + TILE_SIZE - 3).toFloat(), p)
        p.color = Color.rgb(150, 110, 70)
        canvas.drawRect((ox + 7).toFloat(), (oy + 7).toFloat(), (ox + TILE_SIZE - 7).toFloat(), (oy + TILE_SIZE - 7).toFloat(), p)
        p.color = Color.rgb(180, 140, 90)
        canvas.drawRect((ox + 11).toFloat(), (oy + 11).toFloat(), (ox + TILE_SIZE - 11).toFloat(), (oy + TILE_SIZE - 11).toFloat(), p)
        p.color = Color.rgb(130, 95, 60)
        canvas.drawRect((ox + 14).toFloat(), (oy + 14).toFloat(), (ox + 18).toFloat(), (oy + 18).toFloat(), p)
    }

    private fun drawWoodLogSide(canvas: Canvas, ox: Int, oy: Int, rand: Random) {
        for (y in 0 until TILE_SIZE) {
            for (x in 0 until TILE_SIZE) {
                val grain = (kotlin.math.sin(x * 0.7f) * 15).toInt()
                val noise = rand.nextInt(14) - 7 + grain
                val c = Color.rgb(
                    (95 + noise).coerceIn(0, 255),
                    (65 + noise).coerceIn(0, 255),
                    (35 + noise).coerceIn(0, 255)
                )
                canvas.drawPoint((ox + x).toFloat(), (oy + y).toFloat(), Paint().apply { color = c })
            }
        }
    }

    private fun drawWoodPlanks(canvas: Canvas, ox: Int, oy: Int) {
        val p = Paint()
        p.color = Color.rgb(175, 130, 80)
        canvas.drawRect(ox.toFloat(), oy.toFloat(), (ox + TILE_SIZE).toFloat(), (oy + TILE_SIZE).toFloat(), p)
        // 4 plank lines
        val lineP = Paint().apply { color = Color.rgb(90, 60, 30) }
        for (i in 1..3) {
            val ly = oy + i * 8
            canvas.drawRect(ox.toFloat(), ly.toFloat(), (ox + TILE_SIZE).toFloat(), (ly + 1).toFloat(), lineP)
        }
        // Vertical seams
        canvas.drawRect((ox + 12).toFloat(), oy.toFloat(), (ox + 13).toFloat(), (oy + 8).toFloat(), lineP)
        canvas.drawRect((ox + 22).toFloat(), (oy + 8).toFloat(), (ox + 23).toFloat(), (oy + 16).toFloat(), lineP)
        canvas.drawRect((ox + 8).toFloat(), (oy + 16).toFloat(), (ox + 9).toFloat(), (oy + 24).toFloat(), lineP)
        canvas.drawRect((ox + 20).toFloat(), (oy + 24).toFloat(), (ox + 21).toFloat(), (oy + 32).toFloat(), lineP)
    }

    private fun drawLeaves(canvas: Canvas, ox: Int, oy: Int, rand: Random) {
        for (y in 0 until TILE_SIZE) {
            for (x in 0 until TILE_SIZE) {
                if (rand.nextFloat() < 0.12f) {
                    canvas.drawPoint((ox + x).toFloat(), (oy + y).toFloat(), Paint().apply { color = Color.TRANSPARENT })
                } else {
                    val noise = rand.nextInt(35) - 17
                    val c = Color.argb(
                        255,
                        (40 + noise).coerceIn(0, 255),
                        (140 + noise * 2).coerceIn(0, 255),
                        (35 + noise).coerceIn(0, 255)
                    )
                    canvas.drawPoint((ox + x).toFloat(), (oy + y).toFloat(), Paint().apply { color = c })
                }
            }
        }
    }

    private fun drawGlass(canvas: Canvas, ox: Int, oy: Int) {
        val p = Paint()
        // Translucent frosted base
        p.color = Color.argb(45, 220, 240, 255)
        canvas.drawRect(ox.toFloat(), oy.toFloat(), (ox + TILE_SIZE).toFloat(), (oy + TILE_SIZE).toFloat(), p)
        // Outer frame
        p.color = Color.argb(200, 240, 250, 255)
        canvas.drawRect(ox.toFloat(), oy.toFloat(), (ox + TILE_SIZE).toFloat(), (oy + 2).toFloat(), p)
        canvas.drawRect(ox.toFloat(), (oy + TILE_SIZE - 2).toFloat(), (ox + TILE_SIZE).toFloat(), (oy + TILE_SIZE).toFloat(), p)
        canvas.drawRect(ox.toFloat(), oy.toFloat(), (ox + 2).toFloat(), (oy + TILE_SIZE).toFloat(), p)
        canvas.drawRect((ox + TILE_SIZE - 2).toFloat(), oy.toFloat(), (ox + TILE_SIZE).toFloat(), (oy + TILE_SIZE).toFloat(), p)
        // Diagonal reflection glint
        for (i in 0..10) {
            canvas.drawPoint((ox + 6 + i).toFloat(), (oy + 6 + i).toFloat(), p)
            canvas.drawPoint((ox + 16 + i).toFloat(), (oy + 16 + i).toFloat(), p)
        }
    }

    private fun drawWater(canvas: Canvas, ox: Int, oy: Int, rand: Random) {
        for (y in 0 until TILE_SIZE) {
            for (x in 0 until TILE_SIZE) {
                val wave = (kotlin.math.sin(x * 0.35f + y * 0.25f) * 20).toInt()
                val c = Color.argb(
                    180,
                    (20 + wave / 2).coerceIn(0, 255),
                    (110 + wave).coerceIn(0, 255),
                    (230 + wave).coerceIn(0, 255)
                )
                canvas.drawPoint((ox + x).toFloat(), (oy + y).toFloat(), Paint().apply { color = c })
            }
        }
    }

    private fun drawBedrock(canvas: Canvas, ox: Int, oy: Int, rand: Random) {
        for (y in 0 until TILE_SIZE) {
            for (x in 0 until TILE_SIZE) {
                val v = rand.nextInt(60)
                canvas.drawPoint((ox + x).toFloat(), (oy + y).toFloat(), Paint().apply { color = Color.rgb(v, v, v) })
            }
        }
    }

    private fun drawOre(canvas: Canvas, ox: Int, oy: Int, oreColor: Int, rand: Random) {
        drawStone(canvas, ox, oy, rand)
        val p = Paint().apply { color = oreColor }
        // Gem / ore specks
        for (g in 0..7) {
            val gx = ox + 4 + rand.nextInt(TILE_SIZE - 8)
            val gy = oy + 4 + rand.nextInt(TILE_SIZE - 8)
            canvas.drawRect(gx.toFloat(), gy.toFloat(), (gx + 3).toFloat(), (gy + 3).toFloat(), p)
        }
    }

    private fun drawLuminite(canvas: Canvas, ox: Int, oy: Int, rand: Random) {
        for (y in 0 until TILE_SIZE) {
            for (x in 0 until TILE_SIZE) {
                val noise = rand.nextInt(40)
                val c = Color.rgb(30 + noise, 230 - noise, 220 - noise)
                canvas.drawPoint((ox + x).toFloat(), (oy + y).toFloat(), Paint().apply { color = c })
            }
        }
    }

    private fun drawStoneBricks(canvas: Canvas, ox: Int, oy: Int) {
        val p = Paint().apply { color = Color.rgb(135, 135, 140) }
        canvas.drawRect(ox.toFloat(), oy.toFloat(), (ox + TILE_SIZE).toFloat(), (oy + TILE_SIZE).toFloat(), p)
        val seam = Paint().apply { color = Color.rgb(70, 70, 75) }
        canvas.drawRect(ox.toFloat(), (oy + 16).toFloat(), (ox + TILE_SIZE).toFloat(), (oy + 17).toFloat(), seam)
        canvas.drawRect((ox + 16).toFloat(), oy.toFloat(), (ox + 17).toFloat(), (oy + 16).toFloat(), seam)
        canvas.drawRect((ox + 8).toFloat(), (oy + 16).toFloat(), (ox + 9).toFloat(), (oy + 32).toFloat(), seam)
        canvas.drawRect((ox + 24).toFloat(), (oy + 16).toFloat(), (ox + 25).toFloat(), (oy + 32).toFloat(), seam)
    }

    private fun drawCraftingTableTop(canvas: Canvas, ox: Int, oy: Int) {
        val p = Paint().apply { color = Color.rgb(175, 125, 75) }
        canvas.drawRect(ox.toFloat(), oy.toFloat(), (ox + TILE_SIZE).toFloat(), (oy + TILE_SIZE).toFloat(), p)
        // 3x3 grid etched into wood
        val line = Paint().apply { color = Color.rgb(85, 55, 30) }
        canvas.drawRect((ox + 4).toFloat(), (oy + 4).toFloat(), (ox + 28).toFloat(), (oy + 28).toFloat(), Paint().apply {
            color = Color.rgb(140, 95, 55)
        })
        canvas.drawRect((ox + 12).toFloat(), (oy + 4).toFloat(), (ox + 13).toFloat(), (oy + 28).toFloat(), line)
        canvas.drawRect((ox + 20).toFloat(), (oy + 4).toFloat(), (ox + 21).toFloat(), (oy + 28).toFloat(), line)
        canvas.drawRect((ox + 4).toFloat(), (oy + 12).toFloat(), (ox + 28).toFloat(), (oy + 13).toFloat(), line)
        canvas.drawRect((ox + 4).toFloat(), (oy + 20).toFloat(), (ox + 28).toFloat(), (oy + 21).toFloat(), line)
    }

    private fun drawCraftingTableSide(canvas: Canvas, ox: Int, oy: Int) {
        drawWoodPlanks(canvas, ox, oy)
        // Crossed hammer and chisel emblem
        val p = Paint().apply { color = Color.rgb(60, 60, 65) }
        canvas.drawRect((ox + 10).toFloat(), (oy + 10).toFloat(), (ox + 22).toFloat(), (oy + 22).toFloat(), p)
    }

    private fun drawFurnaceFront(canvas: Canvas, ox: Int, oy: Int) {
        drawCobblestone(canvas, ox, oy, Random(12345))
        // Arch opening
        val p = Paint().apply { color = Color.rgb(30, 30, 30) }
        canvas.drawRect((ox + 8).toFloat(), (oy + 12).toFloat(), (ox + 24).toFloat(), (oy + 26).toFloat(), p)
        // Faint ember glow inside
        val glow = Paint().apply { color = Color.rgb(180, 60, 20) }
        canvas.drawRect((ox + 12).toFloat(), (oy + 20).toFloat(), (ox + 20).toFloat(), (oy + 25).toFloat(), glow)
    }

    private fun drawTorch(canvas: Canvas, ox: Int, oy: Int) {
        // Transparent background
        val bg = Paint().apply { color = Color.TRANSPARENT }
        canvas.drawRect(ox.toFloat(), oy.toFloat(), (ox + TILE_SIZE).toFloat(), (oy + TILE_SIZE).toFloat(), bg)
        // Wooden handle
        val handle = Paint().apply { color = Color.rgb(130, 90, 50) }
        canvas.drawRect((ox + 14).toFloat(), (oy + 12).toFloat(), (ox + 18).toFloat(), (oy + 28).toFloat(), handle)
        // Bright flame
        val flame = Paint().apply { color = Color.rgb(255, 170, 20) }
        canvas.drawRect((ox + 13).toFloat(), (oy + 4).toFloat(), (ox + 19).toFloat(), (oy + 12).toFloat(), flame)
        val core = Paint().apply { color = Color.rgb(255, 240, 120) }
        canvas.drawRect((ox + 14).toFloat(), (oy + 6).toFloat(), (ox + 18).toFloat(), (oy + 10).toFloat(), core)
    }

    private fun drawBookshelf(canvas: Canvas, ox: Int, oy: Int, rand: Random) {
        drawWoodPlanks(canvas, ox, oy)
        // Two shelves with book spines
        val bookColors = intArrayOf(
            Color.rgb(180, 40, 40), Color.rgb(40, 110, 190),
            Color.rgb(40, 160, 60), Color.rgb(180, 140, 30),
            Color.rgb(140, 50, 160), Color.rgb(210, 110, 30)
        )
        for (row in 0..1) {
            val sy = oy + 4 + row * 14
            var bx = ox + 4
            while (bx < ox + 28) {
                val bWidth = 2 + rand.nextInt(3)
                val c = bookColors[rand.nextInt(bookColors.size)]
                canvas.drawRect(bx.toFloat(), sy.toFloat(), (bx + bWidth).toFloat(), (sy + 10).toFloat(), Paint().apply { color = c })
                bx += bWidth + 1
            }
        }
    }

    private fun drawObsidian(canvas: Canvas, ox: Int, oy: Int, rand: Random) {
        for (y in 0 until TILE_SIZE) {
            for (x in 0 until TILE_SIZE) {
                val noise = rand.nextInt(25)
                val c = Color.rgb(18 + noise / 2, 10 + noise / 3, 30 + noise)
                canvas.drawPoint((ox + x).toFloat(), (oy + y).toFloat(), Paint().apply { color = c })
            }
        }
    }

    private fun drawClay(canvas: Canvas, ox: Int, oy: Int, rand: Random) {
        for (y in 0 until TILE_SIZE) {
            for (x in 0 until TILE_SIZE) {
                val noise = rand.nextInt(18) - 9
                val c = Color.rgb(160 + noise, 165 + noise, 175 + noise)
                canvas.drawPoint((ox + x).toFloat(), (oy + y).toFloat(), Paint().apply { color = c })
            }
        }
    }

    private fun drawCactusTop(canvas: Canvas, ox: Int, oy: Int) {
        val p = Paint().apply { color = Color.rgb(45, 125, 45) }
        canvas.drawRect(ox.toFloat(), oy.toFloat(), (ox + TILE_SIZE).toFloat(), (oy + TILE_SIZE).toFloat(), p)
        val spine = Paint().apply { color = Color.rgb(220, 220, 140) }
        canvas.drawCircle((ox + 16).toFloat(), (oy + 16).toFloat(), 3f, spine)
    }

    private fun drawCactusSide(canvas: Canvas, ox: Int, oy: Int, rand: Random) {
        val p = Paint().apply { color = Color.rgb(40, 120, 40) }
        canvas.drawRect(ox.toFloat(), oy.toFloat(), (ox + TILE_SIZE).toFloat(), (oy + TILE_SIZE).toFloat(), p)
        // Vertical ribs
        val darkRib = Paint().apply { color = Color.rgb(25, 80, 25) }
        for (r in 0..3) {
            val rx = ox + r * 8
            canvas.drawRect(rx.toFloat(), oy.toFloat(), (rx + 2).toFloat(), (oy + TILE_SIZE).toFloat(), darkRib)
        }
    }

    private fun drawTallGrass(canvas: Canvas, ox: Int, oy: Int, rand: Random) {
        val clear = Paint().apply { color = Color.TRANSPARENT }
        canvas.drawRect(ox.toFloat(), oy.toFloat(), (ox + TILE_SIZE).toFloat(), (oy + TILE_SIZE).toFloat(), clear)
        val grass = Paint().apply { color = Color.rgb(65, 155, 45) }
        for (b in 0..5) {
            val bx = ox + 4 + b * 4
            val h = 14 + rand.nextInt(12)
            canvas.drawRect(bx.toFloat(), (oy + TILE_SIZE - h).toFloat(), (bx + 2).toFloat(), (oy + TILE_SIZE).toFloat(), grass)
        }
    }

    private fun drawFlower(canvas: Canvas, ox: Int, oy: Int, petalColor: Int) {
        val clear = Paint().apply { color = Color.TRANSPARENT }
        canvas.drawRect(ox.toFloat(), oy.toFloat(), (ox + TILE_SIZE).toFloat(), (oy + TILE_SIZE).toFloat(), clear)
        // Stem
        val stem = Paint().apply { color = Color.rgb(50, 140, 40) }
        canvas.drawRect((ox + 15).toFloat(), (oy + 14).toFloat(), (ox + 17).toFloat(), (oy + 30).toFloat(), stem)
        // Petals
        val petals = Paint().apply { color = petalColor }
        canvas.drawCircle((ox + 16).toFloat(), (oy + 12).toFloat(), 6f, petals)
        // Center
        val center = Paint().apply { color = Color.rgb(255, 235, 60) }
        canvas.drawCircle((ox + 16).toFloat(), (oy + 12).toFloat(), 2.5f, center)
    }
}
