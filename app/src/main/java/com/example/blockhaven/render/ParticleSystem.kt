package com.example.blockhaven.render

import android.opengl.GLES30
import com.example.blockhaven.core.math.Vec3f
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.Random

data class Particle(
    var pos: Vec3f,
    var vel: Vec3f,
    var colorR: Float,
    var colorG: Float,
    var colorB: Float,
    var life: Float,
    var maxLife: Float,
    var size: Float
)

class ParticleSystem {
    private val particles = mutableListOf<Particle>()
    private val rand = Random()

    private var program = 0
    private var uMVP = 0
    private var vao = 0
    private var vbo = 0

    private val maxParticles = 600
    private val vertexData = FloatArray(maxParticles * 6 * 7) // 600 max particles * 6 verts * 7 floats
    private lateinit var gpuBuffer: FloatBuffer

    fun init() {
        gpuBuffer = ByteBuffer.allocateDirect(vertexData.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()

        val vShader = """
            #version 300 es
            layout(location = 0) in vec3 aPos;
            layout(location = 1) in vec4 aColor;
            uniform mat4 uMVP;
            out vec4 vColor;
            void main() {
                vColor = aColor;
                gl_Position = uMVP * vec4(aPos, 1.0);
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

        program = ShaderUtils.createProgram(vShader, fShader)
        uMVP = GLES30.glGetUniformLocation(program, "uMVP")

        val vaos = IntArray(1)
        val vbos = IntArray(1)
        GLES30.glGenVertexArrays(1, vaos, 0)
        GLES30.glGenBuffers(1, vbos, 0)
        vao = vaos[0]
        vbo = vbos[0]

        GLES30.glBindVertexArray(vao)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertexData.size * 4, null, GLES30.GL_DYNAMIC_DRAW)

        val stride = 7 * 4
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, 0)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 4, GLES30.GL_FLOAT, false, stride, 3 * 4)

        GLES30.glBindVertexArray(0)
    }

    fun spawnBreakParticles(pos: Vec3f, r: Float, g: Float, b: Float, count: Int = 14) {
        for (i in 0 until count) {
            if (particles.size >= maxParticles - 10) particles.removeAt(0)
            val p = Particle(
                pos = pos + Vec3f(rand.nextFloat() * 0.8f + 0.1f, rand.nextFloat() * 0.8f + 0.1f, rand.nextFloat() * 0.8f + 0.1f),
                vel = Vec3f(
                    (rand.nextFloat() - 0.5f) * 4.5f,
                    rand.nextFloat() * 4f + 1.2f,
                    (rand.nextFloat() - 0.5f) * 4.5f
                ),
                colorR = (r + (rand.nextFloat() - 0.5f) * 0.15f).coerceIn(0f, 1f),
                colorG = (g + (rand.nextFloat() - 0.5f) * 0.15f).coerceIn(0f, 1f),
                colorB = (b + (rand.nextFloat() - 0.5f) * 0.15f).coerceIn(0f, 1f),
                life = 0f,
                maxLife = 0.5f + rand.nextFloat() * 0.4f,
                size = 0.07f + rand.nextFloat() * 0.05f
            )
            particles.add(p)
        }
    }

    fun spawnFootstep(pos: Vec3f, r: Float, g: Float, b: Float) {
        for (i in 0..3) {
            if (particles.size >= maxParticles - 10) particles.removeAt(0)
            particles.add(
                Particle(
                    pos = pos + Vec3f((rand.nextFloat() - 0.5f) * 0.35f, 0.05f, (rand.nextFloat() - 0.5f) * 0.35f),
                    vel = Vec3f((rand.nextFloat() - 0.5f) * 0.6f, rand.nextFloat() * 0.8f + 0.2f, (rand.nextFloat() - 0.5f) * 0.6f),
                    colorR = r, colorG = g, colorB = b,
                    life = 0f, maxLife = 0.35f, size = 0.045f
                )
            )
        }
    }

    fun spawnWaterSplash(pos: Vec3f) {
        for (i in 0..10) {
            if (particles.size >= maxParticles - 10) particles.removeAt(0)
            particles.add(
                Particle(
                    pos = pos + Vec3f((rand.nextFloat() - 0.5f) * 0.5f, 0.1f, (rand.nextFloat() - 0.5f) * 0.5f),
                    vel = Vec3f((rand.nextFloat() - 0.5f) * 2.5f, rand.nextFloat() * 3f + 1.5f, (rand.nextFloat() - 0.5f) * 2.5f),
                    colorR = 0.65f, colorG = 0.85f, colorB = 1.0f,
                    life = 0f, maxLife = 0.5f, size = 0.05f
                )
            )
        }
    }

    fun spawnHitSpark(pos: Vec3f) {
        for (i in 0..8) {
            if (particles.size >= maxParticles - 10) particles.removeAt(0)
            particles.add(
                Particle(
                    pos = pos,
                    vel = Vec3f((rand.nextFloat() - 0.5f) * 5f, rand.nextFloat() * 4f + 2f, (rand.nextFloat() - 0.5f) * 5f),
                    colorR = 1.0f, colorG = 0.85f, colorB = 0.2f,
                    life = 0f, maxLife = 0.3f, size = 0.06f
                )
            )
        }
    }

    fun spawnWeather(center: Vec3f, rainStrength: Float, isSnow: Boolean = false) {
        if (rainStrength <= 0.05f) return
        val count = (12f * rainStrength).toInt().coerceIn(1, 16)
        for (i in 0 until count) {
            if (particles.size >= maxParticles - 10) particles.removeAt(0)
            val rx = (rand.nextFloat() - 0.5f) * 28f
            val rz = (rand.nextFloat() - 0.5f) * 28f
            val ry = rand.nextFloat() * 12f + 8f

            if (isSnow) {
                particles.add(
                    Particle(
                        pos = center + Vec3f(rx, ry, rz),
                        vel = Vec3f((rand.nextFloat() - 0.5f) * 1.5f, -2.5f, (rand.nextFloat() - 0.5f) * 1.5f),
                        colorR = 0.95f, colorG = 0.98f, colorB = 1.0f,
                        life = 0f, maxLife = 2.0f, size = 0.06f
                    )
                )
            } else {
                particles.add(
                    Particle(
                        pos = center + Vec3f(rx, ry, rz),
                        vel = Vec3f(0f, -22f, 0f),
                        colorR = 0.45f, colorG = 0.70f, colorB = 0.95f,
                        life = 0f, maxLife = 0.65f, size = 0.04f
                    )
                )
            }
        }
    }

    fun update(dt: Float) {
        val iter = particles.iterator()
        while (iter.hasNext()) {
            val p = iter.next()
            p.life += dt
            if (p.life >= p.maxLife) {
                iter.remove()
                continue
            }
            // Gravity
            p.vel = Vec3f(p.vel.x * 0.95f, p.vel.y - 14f * dt, p.vel.z * 0.95f)
            p.pos = p.pos + p.vel * dt
        }
    }

    fun render(vpMatrix: FloatArray) {
        if (particles.isEmpty()) return

        var idx = 0
        for (p in particles) {
            if (idx + 42 >= vertexData.size) break
            val alpha = (1f - (p.life / p.maxLife)).coerceIn(0f, 1f)
            val s = p.size
            val x = p.pos.x
            val y = p.pos.y
            val z = p.pos.z

            // Billboard quad facing camera (2 triangles, 6 vertices)
            // v0
            vertexData[idx++] = x - s; vertexData[idx++] = y - s; vertexData[idx++] = z
            vertexData[idx++] = p.colorR; vertexData[idx++] = p.colorG; vertexData[idx++] = p.colorB; vertexData[idx++] = alpha
            // v1
            vertexData[idx++] = x + s; vertexData[idx++] = y - s; vertexData[idx++] = z
            vertexData[idx++] = p.colorR; vertexData[idx++] = p.colorG; vertexData[idx++] = p.colorB; vertexData[idx++] = alpha
            // v2
            vertexData[idx++] = x + s; vertexData[idx++] = y + s; vertexData[idx++] = z
            vertexData[idx++] = p.colorR; vertexData[idx++] = p.colorG; vertexData[idx++] = p.colorB; vertexData[idx++] = alpha

            // v0
            vertexData[idx++] = x - s; vertexData[idx++] = y - s; vertexData[idx++] = z
            vertexData[idx++] = p.colorR; vertexData[idx++] = p.colorG; vertexData[idx++] = p.colorB; vertexData[idx++] = alpha
            // v2
            vertexData[idx++] = x + s; vertexData[idx++] = y + s; vertexData[idx++] = z
            vertexData[idx++] = p.colorR; vertexData[idx++] = p.colorG; vertexData[idx++] = p.colorB; vertexData[idx++] = alpha
            // v3
            vertexData[idx++] = x - s; vertexData[idx++] = y + s; vertexData[idx++] = z
            vertexData[idx++] = p.colorR; vertexData[idx++] = p.colorG; vertexData[idx++] = p.colorB; vertexData[idx++] = alpha
        }

        val vertCount = idx / 7

        GLES30.glUseProgram(program)
        GLES30.glUniformMatrix4fv(uMVP, 1, false, vpMatrix, 0)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

        gpuBuffer.clear()
        gpuBuffer.put(vertexData, 0, idx).position(0)

        GLES30.glBindVertexArray(vao)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo)
        GLES30.glBufferSubData(GLES30.GL_ARRAY_BUFFER, 0, idx * 4, gpuBuffer)

        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, vertCount)

        GLES30.glBindVertexArray(0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
        GLES30.glDisable(GLES30.GL_BLEND)
    }
}
