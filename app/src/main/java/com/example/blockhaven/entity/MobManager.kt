package com.example.blockhaven.entity

import android.opengl.GLES30
import android.opengl.Matrix
import com.example.blockhaven.core.math.Vec3f
import com.example.blockhaven.render.ShaderUtils
import com.example.blockhaven.world.BlockType
import com.example.blockhaven.world.World
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.Random
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

enum class MobType(
    val mobName: String,
    val isHostile: Boolean,
    val maxHealth: Float,
    val speed: Float,
    val colorR: Float,
    val colorG: Float,
    val colorB: Float,
    val scaleX: Float,
    val scaleY: Float,
    val scaleZ: Float
) {
    BOAR("Voxel Boar", false, 10f, 2.2f, 0.65f, 0.42f, 0.28f, 0.8f, 0.8f, 1.2f),
    DEER("Voxel Deer", false, 12f, 3.8f, 0.78f, 0.55f, 0.32f, 0.7f, 1.4f, 1.0f),
    BIRD("Voxel Bird", false, 4f, 4.5f, 0.25f, 0.65f, 0.85f, 0.4f, 0.4f, 0.5f),
    SHADOW_LURKER("Shadow Lurker", true, 24f, 3.2f, 0.12f, 0.08f, 0.18f, 0.6f, 2.2f, 0.6f),
    CAVE_CRAWLER("Cave Crawler", true, 16f, 4.0f, 0.35f, 0.15f, 0.12f, 1.1f, 0.5f, 1.1f)
}

data class Mob(
    val id: Int,
    val type: MobType,
    var pos: Vec3f,
    var vel: Vec3f = Vec3f.ZERO,
    var yaw: Float = 0f,
    var health: Float = type.maxHealth,
    var wanderTimer: Float = 0f,
    var attackCooldown: Float = 0f
)

class MobManager {
    val mobs = mutableListOf<Mob>()
    private var nextMobId = 1
    private val rand = Random()

    private var program = 0
    private var uMVP = 0
    private var uColor = 0
    private var cubeVao = 0
    private var cubeVbo = 0

    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    fun init() {
        val vShader = """
            #version 300 es
            layout(location = 0) in vec3 aPos;
            layout(location = 1) in vec3 aNormal;
            uniform mat4 uMVP;
            out float vLighting;
            void main() {
                float light = max(dot(aNormal, normalize(vec3(0.3, 1.0, 0.5))), 0.25);
                vLighting = light;
                gl_Position = uMVP * vec4(aPos, 1.0);
            }
        """.trimIndent()

        val fShader = """
            #version 300 es
            precision mediump float;
            in float vLighting;
            uniform vec4 uColor;
            out vec4 fragColor;
            void main() {
                fragColor = vec4(uColor.rgb * vLighting, uColor.a);
            }
        """.trimIndent()

        program = ShaderUtils.createProgram(vShader, fShader)
        uMVP = GLES30.glGetUniformLocation(program, "uMVP")
        uColor = GLES30.glGetUniformLocation(program, "uColor")

        buildUnitCube()
    }

    private fun buildUnitCube() {
        // Unit cube centered at base (x: -0.5..0.5, y: 0..1, z: -0.5..0.5)
        val verts = floatArrayOf(
            // Top (+Y)
            -0.5f, 1f, -0.5f,  0f, 1f, 0f,   -0.5f, 1f, 0.5f,   0f, 1f, 0f,   0.5f, 1f, 0.5f,    0f, 1f, 0f,
            -0.5f, 1f, -0.5f,  0f, 1f, 0f,   0.5f, 1f, 0.5f,    0f, 1f, 0f,   0.5f, 1f, -0.5f,   0f, 1f, 0f,
            // Bottom (-Y)
            -0.5f, 0f, 0.5f,   0f, -1f, 0f,  -0.5f, 0f, -0.5f,  0f, -1f, 0f,  0.5f, 0f, -0.5f,   0f, -1f, 0f,
            -0.5f, 0f, 0.5f,   0f, -1f, 0f,  0.5f, 0f, -0.5f,   0f, -1f, 0f,  0.5f, 0f, 0.5f,    0f, -1f, 0f,
            // North (-Z)
            0.5f, 0f, -0.5f,   0f, 0f, -1f,  -0.5f, 0f, -0.5f,  0f, 0f, -1f,  -0.5f, 1f, -0.5f,  0f, 0f, -1f,
            0.5f, 0f, -0.5f,   0f, 0f, -1f,  -0.5f, 1f, -0.5f,  0f, 0f, -1f,  0.5f, 1f, -0.5f,   0f, 0f, -1f,
            // South (+Z)
            -0.5f, 0f, 0.5f,   0f, 0f, 1f,   0.5f, 0f, 0.5f,    0f, 0f, 1f,   0.5f, 1f, 0.5f,    0f, 0f, 1f,
            -0.5f, 0f, 0.5f,   0f, 0f, 1f,   0.5f, 1f, 0.5f,    0f, 0f, 1f,   -0.5f, 1f, 0.5f,   0f, 0f, 1f,
            // East (+X)
            0.5f, 0f, 0.5f,    1f, 0f, 0f,   0.5f, 0f, -0.5f,   1f, 0f, 0f,   0.5f, 1f, -0.5f,   1f, 0f, 0f,
            0.5f, 0f, 0.5f,    1f, 0f, 0f,   0.5f, 1f, -0.5f,   1f, 0f, 0f,   0.5f, 1f, 0.5f,    1f, 0f, 0f,
            // West (-X)
            -0.5f, 0f, -0.5f,  -1f, 0f, 0f,  -0.5f, 0f, 0.5f,   -1f, 0f, 0f,  -0.5f, 1f, 0.5f,   -1f, 0f, 0f,
            -0.5f, 0f, -0.5f,  -1f, 0f, 0f,  -0.5f, 1f, 0.5f,   -1f, 0f, 0f,  -0.5f, 1f, -0.5f,  -1f, 0f, 0f
        )

        val vaos = IntArray(1)
        val vbos = IntArray(1)
        GLES30.glGenVertexArrays(1, vaos, 0)
        GLES30.glGenBuffers(1, vbos, 0)
        cubeVao = vaos[0]
        cubeVbo = vbos[0]

        val buffer: FloatBuffer = ByteBuffer.allocateDirect(verts.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        buffer.put(verts).position(0)

        GLES30.glBindVertexArray(cubeVao)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, cubeVbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, verts.size * 4, buffer, GLES30.GL_STATIC_DRAW)

        val stride = 6 * 4
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, 0)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, stride, 3 * 4)

        GLES30.glBindVertexArray(0)
    }

    fun spawnMob(type: MobType, pos: Vec3f): Mob {
        val mob = Mob(nextMobId++, type, pos)
        mobs.add(mob)
        return mob
    }

    fun update(world: World, player: Player, dt: Float) {
        // Spawn mobs around player dynamically
        if (mobs.size < 12 && rand.nextFloat() < 0.03f) {
            val angle = rand.nextFloat() * 2f * Math.PI.toFloat()
            val dist = 18f + rand.nextFloat() * 20f
            val mx = player.pos.x + cos(angle) * dist
            val mz = player.pos.z + sin(angle) * dist

            var groundY = -1
            for (y in 100 downTo 10) {
                if (BlockType.isSolid(world.getBlock(floor(mx).toInt(), y, floor(mz).toInt()))) {
                    groundY = y + 1
                    break
                }
            }

            if (groundY > 15) {
                val isNight = world.daylightFactor < 0.35f
                val type = if (isNight && rand.nextFloat() < 0.6f) {
                    if (rand.nextBoolean()) MobType.SHADOW_LURKER else MobType.CAVE_CRAWLER
                } else {
                    when (rand.nextInt(3)) {
                        0 -> MobType.BOAR
                        1 -> MobType.DEER
                        else -> MobType.BIRD
                    }
                }
                spawnMob(type, Vec3f(mx, groundY.toFloat(), mz))
            }
        }

        val iter = mobs.iterator()
        while (iter.hasNext()) {
            val mob = iter.next()

            // Despawn if too far
            val distToPlayer = mob.pos.distanceTo(player.pos)
            if (distToPlayer > 80f || mob.health <= 0f) {
                iter.remove()
                continue
            }

            // AI Logic
            mob.wanderTimer -= dt
            mob.attackCooldown -= dt

            var targetDirX = 0f
            var targetDirZ = 0f

            if (mob.type.isHostile) {
                // Chase player if in range (24 blocks)
                if (distToPlayer < 24f) {
                    val dx = player.pos.x - mob.pos.x
                    val dz = player.pos.z - mob.pos.z
                    val len = kotlin.math.sqrt(dx * dx + dz * dz)
                    if (len > 0.01f) {
                        targetDirX = dx / len
                        targetDirZ = dz / len
                        mob.yaw = Math.toDegrees(atan2(-dx.toDouble(), -dz.toDouble())).toFloat()
                    }

                    // Melee attack
                    if (distToPlayer < 1.6f && mob.attackCooldown <= 0f) {
                        player.health = (player.health - 3.5f).coerceAtLeast(0f)
                        mob.attackCooldown = 1.2f
                    }
                }
            } else {
                // Peaceful animals: flee if player too close (4 blocks), else wander
                if (distToPlayer < 4.5f) {
                    val dx = mob.pos.x - player.pos.x
                    val dz = mob.pos.z - player.pos.z
                    val len = kotlin.math.sqrt(dx * dx + dz * dz)
                    if (len > 0.01f) {
                        targetDirX = dx / len
                        targetDirZ = dz / len
                        mob.yaw = Math.toDegrees(atan2(-dx.toDouble(), -dz.toDouble())).toFloat()
                    }
                } else if (mob.wanderTimer <= 0f) {
                    mob.wanderTimer = 3f + rand.nextFloat() * 5f
                    val wAngle = rand.nextFloat() * 2f * Math.PI.toFloat()
                    targetDirX = cos(wAngle)
                    targetDirZ = sin(wAngle)
                    mob.yaw = Math.toDegrees(atan2(-targetDirX.toDouble(), -targetDirZ.toDouble())).toFloat()
                }
            }

            // Movement & simple physics
            val speed = mob.type.speed
            val newX = mob.pos.x + targetDirX * speed * dt
            val newZ = mob.pos.z + targetDirZ * speed * dt

            // Step up / obstacle jump
            var newY = mob.pos.y
            val blockInFront = world.getBlock(floor(newX).toInt(), floor(newY).toInt(), floor(newZ).toInt())
            if (BlockType.isSolid(blockInFront)) {
                // Can step up 1 block
                val blockAbove = world.getBlock(floor(newX).toInt(), floor(newY + 1).toInt(), floor(newZ).toInt())
                if (!BlockType.isSolid(blockAbove)) {
                    newY += 1.0f
                }
            } else {
                // Gravity / ground check
                val blockBelow = world.getBlock(floor(newX).toInt(), floor(newY - 0.2f).toInt(), floor(newZ).toInt())
                if (!BlockType.isSolid(blockBelow) && mob.type != MobType.BIRD) {
                    newY -= 9.8f * dt
                }
            }

            mob.pos = Vec3f(newX, newY, newZ)
        }
    }

    fun render(viewMatrix: FloatArray, projMatrix: FloatArray) {
        if (mobs.isEmpty()) return

        GLES30.glUseProgram(program)
        GLES30.glBindVertexArray(cubeVao)

        val vp = FloatArray(16)
        Matrix.multiplyMM(vp, 0, projMatrix, 0, viewMatrix, 0)

        for (mob in mobs) {
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, mob.pos.x, mob.pos.y, mob.pos.z)
            Matrix.rotateM(modelMatrix, 0, mob.yaw, 0f, 1f, 0f)
            Matrix.scaleM(modelMatrix, 0, mob.type.scaleX, mob.type.scaleY, mob.type.scaleZ)

            Matrix.multiplyMM(mvpMatrix, 0, vp, 0, modelMatrix, 0)
            GLES30.glUniformMatrix4fv(uMVP, 1, false, mvpMatrix, 0)

            GLES30.glUniform4f(uColor, mob.type.colorR, mob.type.colorG, mob.type.colorB, 1.0f)
            GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 36)
        }

        GLES30.glBindVertexArray(0)
    }
}
