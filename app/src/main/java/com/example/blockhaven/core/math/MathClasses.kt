package com.example.blockhaven.core.math

import kotlin.math.sqrt

data class Vec3i(val x: Int, val y: Int, val z: Int) {
    operator fun plus(other: Vec3i) = Vec3i(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vec3i) = Vec3i(x - other.x, y - other.y, z - other.z)
    fun toVec3f() = Vec3f(x.toFloat(), y.toFloat(), z.toFloat())

    companion object {
        val ZERO = Vec3i(0, 0, 0)
        val UP = Vec3i(0, 1, 0)
        val DOWN = Vec3i(0, -1, 0)
        val NORTH = Vec3i(0, 0, -1)
        val SOUTH = Vec3i(0, 0, 1)
        val EAST = Vec3i(1, 0, 0)
        val WEST = Vec3i(-1, 0, 0)
    }
}

data class Vec3f(val x: Float, val y: Float, val z: Float) {
    operator fun plus(other: Vec3f) = Vec3f(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vec3f) = Vec3f(x - other.x, y - other.y, z - other.z)
    operator fun times(scalar: Float) = Vec3f(x * scalar, y * scalar, z * scalar)
    operator fun div(scalar: Float) = Vec3f(x / scalar, y / scalar, z / scalar)

    fun lengthSquared(): Float = x * x + y * y + z * z
    fun length(): Float = sqrt(lengthSquared())

    fun normalize(): Vec3f {
        val len = length()
        return if (len > 0.00001f) Vec3f(x / len, y / len, z / len) else Vec3f(0f, 0f, 0f)
    }

    fun distanceTo(other: Vec3f): Float = (this - other).length()

    companion object {
        val ZERO = Vec3f(0f, 0f, 0f)
    }
}

data class AABB(
    val minX: Float, val minY: Float, val minZ: Float,
    val maxX: Float, val maxY: Float, val maxZ: Float
) {
    fun intersects(other: AABB): Boolean {
        return (minX < other.maxX && maxX > other.minX) &&
                (minY < other.maxY && maxY > other.minY) &&
                (minZ < other.maxZ && maxZ > other.minZ)
    }

    fun offset(dx: Float, dy: Float, dz: Float): AABB {
        return AABB(minX + dx, minY + dy, minZ + dz, maxX + dx, maxY + dy, maxZ + dz)
    }
}

data class RaycastHit(
    val blockPos: Vec3i,
    val placePos: Vec3i,
    val faceNormal: Vec3i,
    val distance: Float
)
