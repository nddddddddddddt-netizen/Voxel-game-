package com.example.blockhaven.render

import android.opengl.GLES30
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.Random
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class SkyRenderer {
    private var skyProgram = 0
    private var uMVPMatrix = 0

    private var sunVao = 0
    private var sunVbo = 0

    private var moonVao = 0
    private var moonVbo = 0

    private var starsVao = 0
    private var starsVbo = 0
    private var starCount = 150

    private var cloudVao = 0
    private var cloudVbo = 0

    private val tempMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    fun init() {
        val vShader = """
            #version 300 es
            layout(location = 0) in vec3 aPosition;
            layout(location = 1) in vec4 aColor;
            uniform mat4 uMVP;
            out vec4 vColor;
            void main() {
                vColor = aColor;
                gl_Position = uMVP * vec4(aPosition, 1.0);
            }
        """.trimIndent()

        val fShader = """
            #version 300 es
            precision mediump float;
            in vec4 vColor;
            out vec4 fragColor;
            void main() {
                fragColor = vColor;
            }
        """.trimIndent()

        skyProgram = ShaderUtils.createProgram(vShader, fShader)
        uMVPMatrix = GLES30.glGetUniformLocation(skyProgram, "uMVP")

        buildSunAndMoon()
        buildStars()
        buildClouds()
    }

    private fun buildSunAndMoon() {
        // Sun quad billboard with radial glow gradient
        val sunSize = 14f
        val sunVerts = floatArrayOf(
            // Inner core (brilliant bright gold/white)
            -sunSize, -sunSize, 0f,   1.0f, 0.98f, 0.85f, 1.0f,
             sunSize, -sunSize, 0f,   1.0f, 0.98f, 0.85f, 1.0f,
             sunSize,  sunSize, 0f,   1.0f, 0.95f, 0.75f, 1.0f,

            -sunSize, -sunSize, 0f,   1.0f, 0.98f, 0.85f, 1.0f,
             sunSize,  sunSize, 0f,   1.0f, 0.95f, 0.75f, 1.0f,
            -sunSize,  sunSize, 0f,   1.0f, 0.95f, 0.75f, 1.0f,

            // Outer corona glow
            -sunSize * 2.2f, -sunSize * 2.2f, 0.1f,  1.0f, 0.85f, 0.4f, 0.35f,
             sunSize * 2.2f, -sunSize * 2.2f, 0.1f,  1.0f, 0.85f, 0.4f, 0.35f,
             sunSize * 2.2f,  sunSize * 2.2f, 0.1f,  1.0f, 0.85f, 0.4f, 0.35f,

            -sunSize * 2.2f, -sunSize * 2.2f, 0.1f,  1.0f, 0.85f, 0.4f, 0.35f,
             sunSize * 2.2f,  sunSize * 2.2f, 0.1f,  1.0f, 0.85f, 0.4f, 0.35f,
            -sunSize * 2.2f,  sunSize * 2.2f, 0.1f,  1.0f, 0.85f, 0.4f, 0.35f,
        )

        val vaos = IntArray(2)
        val vbos = IntArray(2)
        GLES30.glGenVertexArrays(2, vaos, 0)
        GLES30.glGenBuffers(2, vbos, 0)
        sunVao = vaos[0]; sunVbo = vbos[0]
        moonVao = vaos[1]; moonVbo = vbos[1]

        val sunBuf = ByteBuffer.allocateDirect(sunVerts.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        sunBuf.put(sunVerts).position(0)
        GLES30.glBindVertexArray(sunVao)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, sunVbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, sunVerts.size * 4, sunBuf, GLES30.GL_STATIC_DRAW)
        val stride = 7 * 4
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, 0)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 4, GLES30.GL_FLOAT, false, stride, 3 * 4)
        GLES30.glBindVertexArray(0)

        // Moon quad billboard
        val moonSize = 10f
        val moonVerts = floatArrayOf(
            -moonSize, -moonSize, 0f,   0.92f, 0.94f, 1.0f, 0.95f,
             moonSize, -moonSize, 0f,   0.92f, 0.94f, 1.0f, 0.95f,
             moonSize,  moonSize, 0f,   0.85f, 0.88f, 0.98f, 0.95f,

            -moonSize, -moonSize, 0f,   0.92f, 0.94f, 1.0f, 0.95f,
             moonSize,  moonSize, 0f,   0.85f, 0.88f, 0.98f, 0.95f,
            -moonSize,  moonSize, 0f,   0.85f, 0.88f, 0.98f, 0.95f,

            // Moon halo
            -moonSize * 1.8f, -moonSize * 1.8f, 0.1f,  0.7f, 0.8f, 1.0f, 0.25f,
             moonSize * 1.8f, -moonSize * 1.8f, 0.1f,  0.7f, 0.8f, 1.0f, 0.25f,
             moonSize * 1.8f,  moonSize * 1.8f, 0.1f,  0.7f, 0.8f, 1.0f, 0.25f,

            -moonSize * 1.8f, -moonSize * 1.8f, 0.1f,  0.7f, 0.8f, 1.0f, 0.25f,
             moonSize * 1.8f,  moonSize * 1.8f, 0.1f,  0.7f, 0.8f, 1.0f, 0.25f,
            -moonSize * 1.8f,  moonSize * 1.8f, 0.1f,  0.7f, 0.8f, 1.0f, 0.25f,
        )
        val moonBuf = ByteBuffer.allocateDirect(moonVerts.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        moonBuf.put(moonVerts).position(0)
        GLES30.glBindVertexArray(moonVao)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, moonVbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, moonVerts.size * 4, moonBuf, GLES30.GL_STATIC_DRAW)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, 0)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 4, GLES30.GL_FLOAT, false, stride, 3 * 4)
        GLES30.glBindVertexArray(0)
    }

    private fun buildStars() {
        val rand = Random(998877L)
        val starVerts = FloatArray(starCount * 6 * 7)
        var idx = 0

        for (i in 0 until starCount) {
            // Random point on upper celestial hemisphere
            var x = (rand.nextFloat() - 0.5f) * 2f
            var y = rand.nextFloat() * 0.85f + 0.15f
            var z = (rand.nextFloat() - 0.5f) * 2f
            val len = sqrt(x * x + y * y + z * z)
            x = (x / len) * 160f
            y = (y / len) * 160f
            z = (z / len) * 160f

            val sz = 0.55f + rand.nextFloat() * 0.45f
            val bright = 0.65f + rand.nextFloat() * 0.35f

            // Quad billboard
            starVerts[idx++] = x - sz; starVerts[idx++] = y; starVerts[idx++] = z - sz; starVerts[idx++] = 0.95f; starVerts[idx++] = 0.98f; starVerts[idx++] = 1.0f; starVerts[idx++] = bright
            starVerts[idx++] = x + sz; starVerts[idx++] = y; starVerts[idx++] = z - sz; starVerts[idx++] = 0.95f; starVerts[idx++] = 0.98f; starVerts[idx++] = 1.0f; starVerts[idx++] = bright
            starVerts[idx++] = x + sz; starVerts[idx++] = y; starVerts[idx++] = z + sz; starVerts[idx++] = 0.95f; starVerts[idx++] = 0.98f; starVerts[idx++] = 1.0f; starVerts[idx++] = bright

            starVerts[idx++] = x - sz; starVerts[idx++] = y; starVerts[idx++] = z - sz; starVerts[idx++] = 0.95f; starVerts[idx++] = 0.98f; starVerts[idx++] = 1.0f; starVerts[idx++] = bright
            starVerts[idx++] = x + sz; starVerts[idx++] = y; starVerts[idx++] = z + sz; starVerts[idx++] = 0.95f; starVerts[idx++] = 0.98f; starVerts[idx++] = 1.0f; starVerts[idx++] = bright
            starVerts[idx++] = x - sz; starVerts[idx++] = y; starVerts[idx++] = z + sz; starVerts[idx++] = 0.95f; starVerts[idx++] = 0.98f; starVerts[idx++] = 1.0f; starVerts[idx++] = bright
        }

        val vaos = IntArray(1); val vbos = IntArray(1)
        GLES30.glGenVertexArrays(1, vaos, 0); GLES30.glGenBuffers(1, vbos, 0)
        starsVao = vaos[0]; starsVbo = vbos[0]

        val buf = ByteBuffer.allocateDirect(starVerts.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        buf.put(starVerts).position(0)
        GLES30.glBindVertexArray(starsVao)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, starsVbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, starVerts.size * 4, buf, GLES30.GL_STATIC_DRAW)
        val stride = 7 * 4
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, 0)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 4, GLES30.GL_FLOAT, false, stride, 3 * 4)
        GLES30.glBindVertexArray(0)
    }

    private fun buildClouds() {
        val cSize = 350f
        val cloudY = 96f
        val verts = floatArrayOf(
            -cSize, cloudY, -cSize,  1f, 1f, 1f, 0.72f,
             cSize, cloudY, -cSize,  1f, 1f, 1f, 0.72f,
             cSize, cloudY,  cSize,  1f, 1f, 1f, 0.72f,
            -cSize, cloudY, -cSize,  1f, 1f, 1f, 0.72f,
             cSize, cloudY,  cSize,  1f, 1f, 1f, 0.72f,
            -cSize, cloudY,  cSize,  1f, 1f, 1f, 0.72f,
        )

        val vaos = IntArray(1); val vbos = IntArray(1)
        GLES30.glGenVertexArrays(1, vaos, 0); GLES30.glGenBuffers(1, vbos, 0)
        cloudVao = vaos[0]; cloudVbo = vbos[0]

        val buffer = ByteBuffer.allocateDirect(verts.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        buffer.put(verts).position(0)
        GLES30.glBindVertexArray(cloudVao)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, cloudVbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, verts.size * 4, buffer, GLES30.GL_STATIC_DRAW)
        val stride = 7 * 4
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, 0)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 4, GLES30.GL_FLOAT, false, stride, 3 * 4)
        GLES30.glBindVertexArray(0)
    }

    fun getSunDirection(timeOfDay: Float): FloatArray {
        val t = (timeOfDay / 24000f) * 2f * Math.PI.toFloat()
        val x = cos(t - Math.PI.toFloat() * 0.5f)
        val y = sin(t - Math.PI.toFloat() * 0.5f)
        val z = 0.25f
        val len = sqrt(x * x + y * y + z * z)
        return floatArrayOf(x / len, y / len, z / len)
    }

    fun getSkyClearColor(timeOfDay: Float): FloatArray {
        val t = (timeOfDay / 24000f)
        val sunAngle = t * 2f * Math.PI.toFloat()
        val height = sin(sunAngle - Math.PI.toFloat() * 0.5f)

        return when {
            height > 0.15f -> {
                // Day sunny sky
                floatArrayOf(0.42f, 0.68f, 0.98f, 1.0f)
            }
            height > -0.15f -> {
                // Sunset / Sunrise transition
                val blend = (height + 0.15f) / 0.30f
                val r = 0.88f * (1f - blend) + 0.42f * blend
                val g = 0.42f * (1f - blend) + 0.68f * blend
                val b = 0.28f * (1f - blend) + 0.98f * blend
                floatArrayOf(r, g, b, 1.0f)
            }
            else -> {
                // Night sky
                floatArrayOf(0.04f, 0.05f, 0.11f, 1.0f)
            }
        }
    }

    fun getFogColor(timeOfDay: Float): FloatArray {
        val sky = getSkyClearColor(timeOfDay)
        return floatArrayOf(sky[0] * 0.92f, sky[1] * 0.94f, sky[2] * 0.96f)
    }

    fun renderCelestialBodies(viewMatrix: FloatArray, projMatrix: FloatArray, playerX: Float, playerY: Float, playerZ: Float, timeOfDay: Float) {
        GLES30.glUseProgram(skyProgram)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE)
        GLES30.glDepthMask(false)

        val t = (timeOfDay / 24000f) * 2f * Math.PI.toFloat()
        val sunDist = 140f
        val sunDir = getSunDirection(timeOfDay)
        val sunHeight = sunDir[1]

        // 1. Render Stars (visible during evening / night)
        if (sunHeight < 0.15f) {
            val starAlpha = ((0.15f - sunHeight) / 0.25f).coerceIn(0f, 1f)
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, playerX, playerY, playerZ)
            Matrix.multiplyMM(mvMatrix, 0, viewMatrix, 0, modelMatrix, 0)
            Matrix.multiplyMM(mvpMatrix, 0, projMatrix, 0, mvMatrix, 0)
            GLES30.glUniformMatrix4fv(uMVPMatrix, 1, false, mvpMatrix, 0)

            GLES30.glBindVertexArray(starsVao)
            GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, starCount * 6)
            GLES30.glBindVertexArray(0)
        }

        // 2. Render Sun
        if (sunHeight > -0.2f) {
            val sunX = playerX + sunDir[0] * sunDist
            val sunY = playerY + sunDir[1] * sunDist
            val sunZ = playerZ + sunDir[2] * sunDist

            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, sunX, sunY, sunZ)
            // Billboard rotation towards player
            Matrix.rotateM(modelMatrix, 0, -Math.toDegrees(t.toDouble()).toFloat(), 0f, 0f, 1f)

            Matrix.multiplyMM(mvMatrix, 0, viewMatrix, 0, modelMatrix, 0)
            Matrix.multiplyMM(mvpMatrix, 0, projMatrix, 0, mvMatrix, 0)
            GLES30.glUniformMatrix4fv(uMVPMatrix, 1, false, mvpMatrix, 0)

            GLES30.glBindVertexArray(sunVao)
            GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 12)
            GLES30.glBindVertexArray(0)
        }

        // 3. Render Moon (opposite to sun)
        if (sunHeight < 0.2f) {
            val moonX = playerX - sunDir[0] * sunDist
            val moonY = playerY - sunDir[1] * sunDist
            val moonZ = playerZ - sunDir[2] * sunDist

            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, moonX, moonY, moonZ)
            Matrix.multiplyMM(mvMatrix, 0, viewMatrix, 0, modelMatrix, 0)
            Matrix.multiplyMM(mvpMatrix, 0, projMatrix, 0, mvMatrix, 0)
            GLES30.glUniformMatrix4fv(uMVPMatrix, 1, false, mvpMatrix, 0)

            GLES30.glBindVertexArray(moonVao)
            GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 12)
            GLES30.glBindVertexArray(0)
        }

        GLES30.glDepthMask(true)
        GLES30.glDisable(GLES30.GL_BLEND)
    }

    fun renderClouds(viewMatrix: FloatArray, projMatrix: FloatArray, playerX: Float, playerZ: Float, timeSec: Float) {
        GLES30.glUseProgram(skyProgram)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
        GLES30.glDepthMask(false)

        val cloudOffset = (timeSec * 0.85f) % 350f
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, playerX + cloudOffset, 0f, playerZ + cloudOffset * 0.4f)

        Matrix.multiplyMM(mvMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projMatrix, 0, mvMatrix, 0)

        GLES30.glUniformMatrix4fv(uMVPMatrix, 1, false, mvpMatrix, 0)

        GLES30.glBindVertexArray(cloudVao)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 6)
        GLES30.glBindVertexArray(0)

        GLES30.glDepthMask(true)
        GLES30.glDisable(GLES30.GL_BLEND)
    }
}
