package com.example.blockhaven.render

import android.opengl.GLES30
import android.opengl.Matrix
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

    private val vertexData = FloatArray(500 * 6 * 7) // 500 max particles * 6 verts * 7 floats

    fun init() {
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

    fun spawnBreakParticles(pos: Vec3f, r: Float, g: Float, b: Float, count: Int = 12) {
        for (i in 0 until count) {
            if (particles.size >= 400) particles.removeAt(0)
            val p = Particle(
                pos = pos + Vec3f(rand.nextFloat() * 0.8f + 0.1f, rand.nextFloat() * 0.8f + 0.1f, rand.nextFloat() * 0.8f + 0.1f),
                vel = Vec3f(
                    (rand.nextFloat() - 0.5f) * 4f,
                    rand.nextFloat() * 4f + 1f,
                    (rand.nextFloat() - 0.5f) * 4f
                ),
                colorR = (r + (rand.nextFloat() - 0.5f) * 0.2f).coerceIn(0f, 1f),
                colorG = (g + (rand.nextFloat() - 0.5f) * 0.2f).coerceIn(0f, 1f),
                colorB = (b + (rand.nextFloat() - 0.5f) * 0.2f).coerceIn(0f, 1f),
                life = 0f,
                maxLife = 0.6f + rand.nextFloat() * 0.4f,
                size = 0.08f + rand.nextFloat() * 0.06f
            )
            particles.add(p)
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
            p.vel = Vec3f(p.vel.x * 0.95f, p.vel.y - 15f * dt, p.vel.z * 0.95f)
            p.pos = p.pos + p.vel * dt
        }
    }

    fun render(vpMatrix: FloatArray) {
        if (particles.isEmpty()) return

        var idx = 0
        for (p in particles) {
            val alpha = (1f - (p.life / p.maxLife)).coerceIn(0f, 1f)
            val s = p.size
            val x = p.pos.x
            val y = p.pos.y
            val z = p.pos.z

            // Billboard quad facing camera
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

        val buffer: FloatBuffer = ByteBuffer.allocateDirect(idx * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        buffer.put(vertexData, 0, idx).position(0)

        GLES30.glBindVertexArray(vao)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo)
        GLES30.glBufferSubData(GLES30.GL_ARRAY_BUFFER, 0, idx * 4, buffer)

        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, vertCount)

        GLES30.glBindVertexArray(0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
        GLES30.glDisable(GLES30.GL_BLEND)
    }
}
