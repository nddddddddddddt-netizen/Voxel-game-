package com.example.blockhaven.world

import kotlin.math.floor

class FastNoise(seed: Long) {
    private val perm = IntArray(512)

    init {
        val p = IntArray(256) { it }
        // Deterministic shuffle with seed
        var s = seed
        for (i in 255 downTo 1) {
            s = s * 6364136223846793005L + 1442695040888963407L
            val j = ((s ushr 32) and 0x7FFFFFFF).toInt() % (i + 1)
            val tmp = p[i]
            p[i] = p[j]
            p[j] = tmp
        }
        for (i in 0 until 256) {
            perm[i] = p[i]
            perm[i + 256] = p[i]
        }
    }

    private fun fade(t: Float): Float = t * t * t * (t * (t * 6f - 15f) + 10f)
    private fun lerp(t: Float, a: Float, b: Float): Float = a + t * (b - a)

    private fun grad(hash: Int, x: Float, y: Float): Float {
        val h = hash and 7
        val u = if (h < 4) x else y
        val v = if (h < 4) y else x
        return (if ((h and 1) == 0) u else -u) + (if ((h and 2) == 0) v else -v)
    }

    private fun grad3D(hash: Int, x: Float, y: Float, z: Float): Float {
        val h = hash and 15
        val u = if (h < 8) x else y
        val v = if (h < 4) y else if (h == 12 || h == 14) x else z
        return (if ((h and 1) == 0) u else -u) + (if ((h and 2) == 0) v else -v)
    }

    fun perlin2D(x: Float, y: Float): Float {
        val xi = floor(x).toInt() and 255
        val yi = floor(y).toInt() and 255

        val xf = x - floor(x)
        val yf = y - floor(y)

        val u = fade(xf)
        val v = fade(yf)

        val aa = perm[perm[xi] + yi]
        val ab = perm[perm[xi] + yi + 1]
        val ba = perm[perm[xi + 1] + yi]
        val bb = perm[perm[xi + 1] + yi + 1]

        val x1 = lerp(u, grad(aa, xf, yf), grad(ba, xf - 1f, yf))
        val x2 = lerp(u, grad(ab, xf, yf - 1f), grad(bb, xf - 1f, yf - 1f))

        return lerp(v, x1, x2)
    }

    fun perlin3D(x: Float, y: Float, z: Float): Float {
        val xi = floor(x).toInt() and 255
        val yi = floor(y).toInt() and 255
        val zi = floor(z).toInt() and 255

        val xf = x - floor(x)
        val yf = y - floor(y)
        val zf = z - floor(z)

        val u = fade(xf)
        val v = fade(yf)
        val w = fade(zf)

        val a = perm[xi] + yi
        val aa = perm[a] + zi
        val ab = perm[a + 1] + zi
        val b = perm[xi + 1] + yi
        val ba = perm[b] + zi
        val bb = perm[b + 1] + zi

        val l1 = lerp(u, grad3D(perm[aa], xf, yf, zf), grad3D(perm[ba], xf - 1f, yf, zf))
        val l2 = lerp(u, grad3D(perm[ab], xf, yf - 1f, zf), grad3D(perm[bb], xf - 1f, yf - 1f, zf))
        val l3 = lerp(u, grad3D(perm[aa + 1], xf, yf, zf - 1f), grad3D(perm[ba + 1], xf - 1f, yf, zf - 1f))
        val l4 = lerp(u, grad3D(perm[ab + 1], xf, yf - 1f, zf - 1f), grad3D(perm[bb + 1], xf - 1f, yf - 1f, zf - 1f))

        return lerp(w, lerp(v, l1, l2), lerp(v, l3, l4))
    }

    fun fractal2D(x: Float, y: Float, octaves: Int = 4, persistence: Float = 0.5f, lacunarity: Float = 2.0f): Float {
        var total = 0f
        var frequency = 1f
        var amplitude = 1f
        var maxValue = 0f
        for (i in 0 until octaves) {
            total += perlin2D(x * frequency, y * frequency) * amplitude
            maxValue += amplitude
            amplitude *= persistence
            frequency *= lacunarity
        }
        return total / maxValue
    }

    fun fractal3D(x: Float, y: Float, z: Float, octaves: Int = 3, persistence: Float = 0.5f, lacunarity: Float = 2.0f): Float {
        var total = 0f
        var frequency = 1f
        var amplitude = 1f
        var maxValue = 0f
        for (i in 0 until octaves) {
            total += perlin3D(x * frequency, y * frequency, z * frequency) * amplitude
            maxValue += amplitude
            amplitude *= persistence
            frequency *= lacunarity
        }
        return total / maxValue
    }
}
