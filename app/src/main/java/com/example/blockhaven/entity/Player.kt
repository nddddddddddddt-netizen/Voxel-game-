package com.example.blockhaven.entity

import com.example.blockhaven.core.math.AABB
import com.example.blockhaven.core.math.RaycastHit
import com.example.blockhaven.core.math.Vec3f
import com.example.blockhaven.core.math.Vec3i
import com.example.blockhaven.world.BlockType
import com.example.blockhaven.world.Chunk
import com.example.blockhaven.world.World
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

enum class GameMode {
    SURVIVAL, CREATIVE
}

class Player(var pos: Vec3f = Vec3f(8.5f, 75f, 8.5f)) {
    var vel: Vec3f = Vec3f.ZERO
    var yaw: Float = 0f    // Degrees
    var pitch: Float = 0f  // Degrees (-89 to 89)

    var gameMode: GameMode = GameMode.SURVIVAL

    val width = 0.6f
    val height = 1.8f
    val eyeHeight = 1.62f

    var onGround = false
    var isInWater = false
    var isSneaking = false
    var isFlying = false
    var isMining = false

    var health: Float = 20f
    var maxHealth: Float = 20f
    var hunger: Float = 20f
    var air: Float = 10f // Seconds underwater

    private var fallDistance = 0f
    var miningProgress = 0f
    var targetedHit: RaycastHit? = null

    // Hand swinging animation state for first person view
    var swingTime = 0f

    // Walking animation phase and natural head bobbing
    var walkPhase = 0f
    var walkBobY = 0f
    var walkBobX = 0f

    fun getEyePos(): Vec3f {
        val yBase = if (isSneaking) eyeHeight - 0.2f else eyeHeight
        val yOffset = yBase + (if (onGround) walkBobY * 0.45f else 0f)
        val xOffset = if (onGround) walkBobX * 0.25f else 0f
        return Vec3f(pos.x + xOffset, pos.y + yOffset, pos.z)
    }

    /**
     * Guarantees player is not stuck inside solid terrain (spawning safety fix)
     */
    fun ensureSafeGrounded(world: World) {
        val ix = floor(pos.x).toInt()
        val iy = floor(pos.y).toInt()
        val iz = floor(pos.z).toInt()

        val bFeet = world.getBlock(ix, iy, iz)
        val bHead = world.getBlock(ix, iy + 1, iz)
        val isStuck = (bFeet != BlockType.AIR && BlockType.isSolid(bFeet) && !BlockType.isPlant(bFeet)) ||
                      (bHead != BlockType.AIR && BlockType.isSolid(bHead) && !BlockType.isPlant(bHead))

        if (isStuck) {
            // Find the closest clear 2-block vertical column above solid ground
            var safeY = -1
            for (y in iy until (Chunk.HEIGHT - 3)) {
                val below = world.getBlock(ix, y - 1, iz)
                val feet = world.getBlock(ix, y, iz)
                val head = world.getBlock(ix, y + 1, iz)
                if (below != BlockType.AIR && BlockType.isSolid(below) &&
                    (feet == BlockType.AIR || BlockType.isPlant(feet)) &&
                    head == BlockType.AIR) {
                    safeY = y
                    break
                }
            }

            if (safeY != -1) {
                pos = Vec3f(pos.x, safeY.toFloat() + 0.05f, pos.z)
                vel = Vec3f.ZERO
                onGround = true
            } else {
                // If not found searching up, search downwards from surface
                for (y in (Chunk.HEIGHT - 5) downTo 2) {
                    val b = world.getBlock(ix, y, iz)
                    if (b != BlockType.AIR && BlockType.isSolid(b) && !BlockType.isPlant(b)) {
                        pos = Vec3f(pos.x, (y + 1).toFloat() + 0.05f, pos.z)
                        vel = Vec3f.ZERO
                        onGround = true
                        break
                    }
                }
            }
        }
    }

    fun getLookDirection(): Vec3f {
        val radYaw = Math.toRadians(yaw.toDouble()).toFloat()
        val radPitch = Math.toRadians(pitch.toDouble()).toFloat()

        val x = -sin(radYaw) * cos(radPitch)
        val y = -sin(radPitch)
        val z = -cos(radYaw) * cos(radPitch)
        return Vec3f(x, y, z).normalize()
    }

    fun getBoundingBox(): AABB {
        val hw = width * 0.5f
        return AABB(pos.x - hw, pos.y, pos.z - hw, pos.x + hw, pos.y + height, pos.z + hw)
    }

    fun update(world: World, dt: Float, moveX: Float, moveZ: Float, jumpPressed: Boolean) {
        val clampedDt = dt.coerceIn(0.001f, 0.05f)

        // Hand swing animation update
        if (swingTime > 0f) {
            swingTime = (swingTime - clampedDt * 3.5f).coerceAtLeast(0f)
        }

        // Raycast look target
        val eyePos = getEyePos()
        val lookDir = getLookDirection()
        targetedHit = world.raycast(eyePos, lookDir, maxDistance = if (gameMode == GameMode.CREATIVE) 8f else 5.5f)

        // Check if inside water
        val headBlock = world.getBlock(floor(pos.x).toInt(), floor(pos.y + eyeHeight).toInt(), floor(pos.z).toInt())
        val feetBlock = world.getBlock(floor(pos.x).toInt(), floor(pos.y).toInt(), floor(pos.z).toInt())
        isInWater = (headBlock == BlockType.WATER || feetBlock == BlockType.WATER)

        // Underwater air logic
        if (headBlock == BlockType.WATER && gameMode == GameMode.SURVIVAL) {
            air -= clampedDt
            if (air <= 0f) {
                health = (health - clampedDt * 2f).coerceAtLeast(0f)
            }
        } else {
            air = (air + clampedDt * 4f).coerceAtMost(10f)
        }

        // Movement vector relative to camera yaw
        val radYaw = Math.toRadians(yaw.toDouble()).toFloat()
        val sinY = sin(radYaw)
        val cosY = cos(radYaw)

        // Forward: -cosY (z), -sinY (x); Strafe Right: cosY (x), -sinY (z)
        val forwardX = -sinY * moveZ
        val forwardZ = -cosY * moveZ
        val strafeX = cosY * moveX
        val strafeZ = -sinY * moveX

        val moveSpeed = when {
            isFlying -> 14f
            isInWater -> 3.2f
            isSneaking -> 2.4f
            else -> 4.8f
        }

        val targetVelX = (forwardX + strafeX) * moveSpeed
        val targetVelZ = (forwardZ + strafeZ) * moveSpeed

        // Smooth horizontal acceleration
        val accel = if (onGround || isFlying) 12f else 3f
        val newVelX = vel.x + (targetVelX - vel.x) * (accel * clampedDt).coerceAtMost(1f)
        val newVelZ = vel.z + (targetVelZ - vel.z) * (accel * clampedDt).coerceAtMost(1f)

        // Update walk bobbing
        val horizSpeed = kotlin.math.sqrt(newVelX * newVelX + newVelZ * newVelZ)
        if (onGround && horizSpeed > 0.3f) {
            walkPhase += horizSpeed * clampedDt * 2.8f
            walkBobY = kotlin.math.sin(walkPhase * 2f) * 0.05f
            walkBobX = kotlin.math.cos(walkPhase) * 0.035f
        } else {
            walkBobY *= 0.82f
            walkBobX *= 0.82f
        }

        // Vertical movement
        var newVelY = vel.y
        if (isFlying) {
            newVelY = when {
                jumpPressed -> 6f
                isSneaking -> -6f
                else -> 0f
            }
        } else if (isInWater) {
            newVelY = when {
                jumpPressed -> 3.2f
                else -> -1.2f
            }
        } else {
            // Apply gravity
            newVelY -= 26.0f * clampedDt
            if (newVelY < -40f) newVelY = -40f

            if (jumpPressed && onGround) {
                newVelY = 8.8f
                onGround = false
            }
        }

        vel = Vec3f(newVelX, newVelY, newVelZ)

        // Move with collision resolution
        moveWithCollision(world, vel.x * clampedDt, vel.y * clampedDt, vel.z * clampedDt)
    }

    private fun moveWithCollision(world: World, dx: Float, dy: Float, dz: Float) {
        val originalBox = getBoundingBox()

        // 1. Resolve Y axis first
        var actualDy = dy
        val expandedY = originalBox.offset(0f, actualDy, 0f)
        val collidingBlocksY = getSurroundingSolidBoxes(world, expandedY)

        for (blockBox in collidingBlocksY) {
            if (actualDy > 0f && originalBox.maxY <= blockBox.minY) {
                val maxAllowed = blockBox.minY - originalBox.maxY - 0.001f
                if (maxAllowed < actualDy) actualDy = maxAllowed.coerceAtLeast(0f)
            } else if (actualDy < 0f && originalBox.minY >= blockBox.maxY) {
                val minAllowed = blockBox.maxY - originalBox.minY + 0.001f
                if (minAllowed > actualDy) actualDy = minAllowed.coerceAtMost(0f)
            }
        }

        val wasFalling = vel.y < 0f
        pos = Vec3f(pos.x, pos.y + actualDy, pos.z)
        val collidedY = actualDy != dy

        if (collidedY) {
            if (wasFalling) {
                onGround = true
                if (fallDistance > 3.5f && gameMode == GameMode.SURVIVAL && !isInWater) {
                    val damage = (fallDistance - 3.5f) * 1.5f
                    health = (health - damage).coerceAtLeast(0f)
                }
                fallDistance = 0f
            }
            vel = Vec3f(vel.x, 0f, vel.z)
        } else {
            onGround = false
            if (vel.y < 0f) {
                fallDistance -= actualDy
            }
        }

        // 2. Resolve X axis
        var actualDx = dx
        val boxAfterY = getBoundingBox()
        val expandedX = boxAfterY.offset(actualDx, 0f, 0f)
        val collidingBlocksX = getSurroundingSolidBoxes(world, expandedX)

        for (blockBox in collidingBlocksX) {
            if (actualDx > 0f && boxAfterY.maxX <= blockBox.minX) {
                val maxAllowed = blockBox.minX - boxAfterY.maxX - 0.001f
                if (maxAllowed < actualDx) actualDx = maxAllowed.coerceAtLeast(0f)
            } else if (actualDx < 0f && boxAfterY.minX >= blockBox.maxX) {
                val minAllowed = blockBox.maxX - boxAfterY.minX + 0.001f
                if (minAllowed > actualDx) actualDx = minAllowed.coerceAtMost(0f)
            }
        }
        pos = Vec3f(pos.x + actualDx, pos.y, pos.z)
        if (actualDx != dx) vel = Vec3f(0f, vel.y, vel.z)

        // 3. Resolve Z axis
        var actualDz = dz
        val boxAfterX = getBoundingBox()
        val expandedZ = boxAfterX.offset(0f, 0f, actualDz)
        val collidingBlocksZ = getSurroundingSolidBoxes(world, expandedZ)

        for (blockBox in collidingBlocksZ) {
            if (actualDz > 0f && boxAfterX.maxZ <= blockBox.minZ) {
                val maxAllowed = blockBox.minZ - boxAfterX.maxZ - 0.001f
                if (maxAllowed < actualDz) actualDz = maxAllowed.coerceAtLeast(0f)
            } else if (actualDz < 0f && boxAfterX.minZ >= blockBox.maxZ) {
                val minAllowed = blockBox.maxZ - boxAfterX.minZ + 0.001f
                if (minAllowed > actualDz) actualDz = minAllowed.coerceAtMost(0f)
            }
        }
        pos = Vec3f(pos.x, pos.y, pos.z + actualDz)
        if (actualDz != dz) vel = Vec3f(vel.x, vel.y, 0f)
    }

    private fun getSurroundingSolidBoxes(world: World, box: AABB): List<AABB> {
        val list = mutableListOf<AABB>()
        val minX = floor(box.minX).toInt()
        val maxX = floor(box.maxX).toInt()
        val minY = floor(box.minY).toInt()
        val maxY = floor(box.maxY).toInt()
        val minZ = floor(box.minZ).toInt()
        val maxZ = floor(box.maxZ).toInt()

        for (x in minX..maxX) {
            for (y in minY..maxY) {
                for (z in minZ..maxZ) {
                    val b = world.getBlock(x, y, z)
                    if (b != BlockType.AIR && BlockType.isSolid(b) && !BlockType.isPlant(b)) {
                        list.add(AABB(x.toFloat(), y.toFloat(), z.toFloat(), (x + 1).toFloat(), (y + 1).toFloat(), (z + 1).toFloat()))
                    }
                }
            }
        }
        return list
    }
}
