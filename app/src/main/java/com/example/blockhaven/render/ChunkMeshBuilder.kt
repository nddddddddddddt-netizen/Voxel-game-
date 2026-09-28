package com.example.blockhaven.render

import android.opengl.GLES30
import com.example.blockhaven.world.BlockType
import com.example.blockhaven.world.Chunk
import com.example.blockhaven.world.World
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class ChunkMesh(
    val opaqueVao: Int,
    val opaqueVbo: Int,
    val opaqueVertexCount: Int,
    val transVao: Int,
    val transVbo: Int,
    val transVertexCount: Int
) {
    fun delete() {
        val vaos = intArrayOf(opaqueVao, transVao)
        val vbos = intArrayOf(opaqueVbo, transVbo)
        GLES30.glDeleteVertexArrays(2, vaos, 0)
        GLES30.glDeleteBuffers(2, vbos, 0)
    }
}

object ChunkMeshBuilder {
    // Face normal directions: 0 = Top (+Y), 1 = Bottom (-Y), 2 = North (-Z), 3 = South (+Z), 4 = East (+X), 5 = West (-X)
    private val FACE_NORMALS = arrayOf(
        floatArrayOf(0f, 1f, 0f),   // Top
        floatArrayOf(0f, -1f, 0f),  // Bottom
        floatArrayOf(0f, 0f, -1f),  // North
        floatArrayOf(0f, 0f, 1f),   // South
        floatArrayOf(1f, 0f, 0f),   // East
        floatArrayOf(-1f, 0f, 0f)   // West
    )

    // Directional face lighting multipliers
    private val FACE_LIGHTING = floatArrayOf(
        1.0f,   // Top
        0.55f,  // Bottom
        0.8f,   // North
        0.8f,   // South
        0.7f,   // East
        0.7f    // West
    )

    // Reusable scratch float buffers to avoid GC pressure
    private var opaqueData = FloatArray(16 * 16 * 128 * 6 * 4)
    private var transData = FloatArray(16 * 16 * 32 * 6 * 4)

    fun build(chunk: Chunk, world: World): ChunkMesh? {
        val wx = chunk.chunkX * Chunk.WIDTH
        val wz = chunk.chunkZ * Chunk.DEPTH

        var oIndex = 0
        var tIndex = 0

        // Ensure arrays are large enough
        if (opaqueData.size < 60000) opaqueData = FloatArray(100000)
        if (transData.size < 30000) transData = FloatArray(50000)

        for (x in 0 until Chunk.WIDTH) {
            for (z in 0 until Chunk.DEPTH) {
                for (y in 0 until Chunk.HEIGHT) {
                    val blockId = chunk.getBlock(x, y, z)
                    if (blockId == BlockType.AIR) continue

                    val def = BlockType.get(blockId)
                    val bx = (wx + x).toFloat()
                    val by = y.toFloat()
                    val bz = (wz + z).toFloat()

                    val isTrans = def.isTransparent || def.isLiquid

                    if (def.isPlant) {
                        // Render crossed quads for plants (tall grass, flowers)
                        val uv = TextureAtlas.getUV(def.textureSide)
                        val light = computeVoxelLight(chunk, world, x, y, z, 0)
                        if (tIndex + 48 >= transData.size) {
                            transData = transData.copyOf(transData.size * 2)
                        }
                        tIndex = addCrossedQuads(transData, tIndex, bx, by, bz, uv, light)
                        continue
                    }

                    // Check all 6 faces: Top, Bottom, North, South, East, West
                    for (face in 0..5) {
                        val nx = x + when (face) { 4 -> 1; 5 -> -1; else -> 0 }
                        val ny = y + when (face) { 0 -> 1; 1 -> -1; else -> 0 }
                        val nz = z + when (face) { 3 -> 1; 2 -> -1; else -> 0 }

                        val neighborBlock = if (Chunk.isValidPos(nx, ny, nz)) {
                            chunk.getBlock(nx, ny, nz)
                        } else {
                            world.getBlock(wx + nx, ny, wz + nz)
                        }

                        var shouldRenderFace = false
                        if (def.isLiquid) {
                            // Water only renders against air or different non-water blocks
                            shouldRenderFace = neighborBlock != BlockType.WATER && (!BlockType.isSolid(neighborBlock) || BlockType.isTransparent(neighborBlock))
                        } else if (def.isTransparent) {
                            shouldRenderFace = neighborBlock != blockId && (!BlockType.isSolid(neighborBlock) || BlockType.isTransparent(neighborBlock))
                        } else {
                            // Opaque block: only render face if neighbor is not solid or is transparent
                            shouldRenderFace = !BlockType.isSolid(neighborBlock) || BlockType.isTransparent(neighborBlock)
                        }

                        if (!shouldRenderFace) continue

                        val tile = when (face) {
                            0 -> def.textureTop
                            1 -> def.textureBottom
                            else -> def.textureSide
                        }
                        val uv = TextureAtlas.getUV(tile)
                        val faceLight = computeVoxelLight(chunk, world, x, y, z, face) * FACE_LIGHTING[face]

                        // Compute Smooth Ambient Occlusion for each corner
                        val (ao0, ao1, ao2, ao3) = if (!def.isLiquid) {
                            computeFaceAo(chunk, world, wx, wz, x, y, z, face)
                        } else {
                            FaceAo(1f, 1f, 1f, 1f)
                        }

                        val faceMarker = if (def.isLiquid) 10f else face.toFloat()

                        if (isTrans) {
                            if (tIndex + 48 >= transData.size) {
                                transData = transData.copyOf(transData.size * 2)
                            }
                            tIndex = addFace(transData, tIndex, bx, by, bz, face, uv, faceLight, ao0, ao1, ao2, ao3, faceMarker)
                        } else {
                            if (oIndex + 48 >= opaqueData.size) {
                                opaqueData = opaqueData.copyOf(opaqueData.size * 2)
                            }
                            oIndex = addFace(opaqueData, oIndex, bx, by, bz, face, uv, faceLight, ao0, ao1, ao2, ao3, faceMarker)
                        }
                    }
                }
            }
        }

        // Upload to OpenGL buffers
        val oCount = oIndex / 8
        val tCount = tIndex / 8

        if (oCount == 0 && tCount == 0) return null

        val oBuffers = uploadMesh(opaqueData, oIndex)
        val tBuffers = uploadMesh(transData, tIndex)

        chunk.isMeshDirty = false
        return ChunkMesh(
            opaqueVao = oBuffers.first,
            opaqueVbo = oBuffers.second,
            opaqueVertexCount = oCount,
            transVao = tBuffers.first,
            transVbo = tBuffers.second,
            transVertexCount = tCount
        )
    }

    private fun computeVoxelLight(chunk: Chunk, world: World, lx: Int, ly: Int, lz: Int, face: Int): Float {
        val nx = lx + when (face) { 4 -> 1; 5 -> -1; else -> 0 }
        val ny = (ly + when (face) { 0 -> 1; 1 -> -1; else -> 0 }).coerceIn(0, Chunk.HEIGHT - 1)
        val nz = lz + when (face) { 3 -> 1; 2 -> -1; else -> 0 }

        val sun = if (Chunk.isValidPos(nx, ny, nz)) chunk.getSunlight(nx, ny, nz) else 15
        val blockL = if (Chunk.isValidPos(nx, ny, nz)) chunk.getBlockLight(nx, ny, nz) else 0

        val sunFactor = (sun / 15f) * world.daylightFactor
        val blockFactor = blockL / 15f
        return (sunFactor + blockFactor).coerceIn(0.12f, 1.0f)
    }

    data class FaceAo(val ao0: Float, val ao1: Float, val ao2: Float, val ao3: Float)

    private fun isOccluding(chunk: Chunk, world: World, wx: Int, wz: Int, x: Int, y: Int, z: Int): Boolean {
        if (y !in 0 until Chunk.HEIGHT) return false
        val block = if (Chunk.isValidPos(x, y, z)) {
            chunk.getBlock(x, y, z)
        } else {
            world.getBlock(wx + x, y, wz + z)
        }
        return block != BlockType.AIR && BlockType.isSolid(block) && !BlockType.isTransparent(block) && !BlockType.isPlant(block)
    }

    private fun vertexAo(side1: Boolean, side2: Boolean, corner: Boolean): Float {
        if (side1 && side2) return 0.38f
        var count = 0
        if (side1) count++
        if (side2) count++
        if (corner) count++
        return when (count) {
            0 -> 1.0f
            1 -> 0.80f
            2 -> 0.62f
            else -> 0.44f
        }
    }

    private fun computeFaceAo(chunk: Chunk, world: World, wx: Int, wz: Int, x: Int, y: Int, z: Int, face: Int): FaceAo {
        return when (face) {
            0 -> { // Top (+Y)
                val w = isOccluding(chunk, world, wx, wz, x - 1, y + 1, z)
                val e = isOccluding(chunk, world, wx, wz, x + 1, y + 1, z)
                val n = isOccluding(chunk, world, wx, wz, x, y + 1, z - 1)
                val s = isOccluding(chunk, world, wx, wz, x, y + 1, z + 1)
                val wn = isOccluding(chunk, world, wx, wz, x - 1, y + 1, z - 1)
                val ws = isOccluding(chunk, world, wx, wz, x - 1, y + 1, z + 1)
                val en = isOccluding(chunk, world, wx, wz, x + 1, y + 1, z - 1)
                val es = isOccluding(chunk, world, wx, wz, x + 1, y + 1, z + 1)
                FaceAo(vertexAo(w, n, wn), vertexAo(w, s, ws), vertexAo(e, s, es), vertexAo(e, n, en))
            }
            1 -> { // Bottom (-Y)
                val w = isOccluding(chunk, world, wx, wz, x - 1, y - 1, z)
                val e = isOccluding(chunk, world, wx, wz, x + 1, y - 1, z)
                val n = isOccluding(chunk, world, wx, wz, x, y - 1, z - 1)
                val s = isOccluding(chunk, world, wx, wz, x, y - 1, z + 1)
                val wn = isOccluding(chunk, world, wx, wz, x - 1, y - 1, z - 1)
                val ws = isOccluding(chunk, world, wx, wz, x - 1, y - 1, z + 1)
                val en = isOccluding(chunk, world, wx, wz, x + 1, y - 1, z - 1)
                val es = isOccluding(chunk, world, wx, wz, x + 1, y - 1, z + 1)
                FaceAo(vertexAo(w, s, ws), vertexAo(w, n, wn), vertexAo(e, n, en), vertexAo(e, s, es))
            }
            2 -> { // North (-Z)
                val w = isOccluding(chunk, world, wx, wz, x - 1, y, z - 1)
                val e = isOccluding(chunk, world, wx, wz, x + 1, y, z - 1)
                val u = isOccluding(chunk, world, wx, wz, x, y + 1, z - 1)
                val d = isOccluding(chunk, world, wx, wz, x, y - 1, z - 1)
                val dw = isOccluding(chunk, world, wx, wz, x - 1, y - 1, z - 1)
                val de = isOccluding(chunk, world, wx, wz, x + 1, y - 1, z - 1)
                val uw = isOccluding(chunk, world, wx, wz, x - 1, y + 1, z - 1)
                val ue = isOccluding(chunk, world, wx, wz, x + 1, y + 1, z - 1)
                FaceAo(vertexAo(e, d, de), vertexAo(w, d, dw), vertexAo(w, u, uw), vertexAo(e, u, ue))
            }
            3 -> { // South (+Z)
                val w = isOccluding(chunk, world, wx, wz, x - 1, y, z + 1)
                val e = isOccluding(chunk, world, wx, wz, x + 1, y, z + 1)
                val u = isOccluding(chunk, world, wx, wz, x, y + 1, z + 1)
                val d = isOccluding(chunk, world, wx, wz, x, y - 1, z + 1)
                val dw = isOccluding(chunk, world, wx, wz, x - 1, y - 1, z + 1)
                val de = isOccluding(chunk, world, wx, wz, x + 1, y - 1, z + 1)
                val uw = isOccluding(chunk, world, wx, wz, x - 1, y + 1, z + 1)
                val ue = isOccluding(chunk, world, wx, wz, x + 1, y + 1, z + 1)
                FaceAo(vertexAo(w, d, dw), vertexAo(e, d, de), vertexAo(e, u, ue), vertexAo(w, u, uw))
            }
            4 -> { // East (+X)
                val s = isOccluding(chunk, world, wx, wz, x + 1, y, z + 1)
                val n = isOccluding(chunk, world, wx, wz, x + 1, y, z - 1)
                val u = isOccluding(chunk, world, wx, wz, x + 1, y + 1, z)
                val d = isOccluding(chunk, world, wx, wz, x + 1, y - 1, z)
                val ds = isOccluding(chunk, world, wx, wz, x + 1, y - 1, z + 1)
                val dn = isOccluding(chunk, world, wx, wz, x + 1, y - 1, z - 1)
                val us = isOccluding(chunk, world, wx, wz, x + 1, y + 1, z + 1)
                val un = isOccluding(chunk, world, wx, wz, x + 1, y + 1, z - 1)
                FaceAo(vertexAo(s, d, ds), vertexAo(n, d, dn), vertexAo(n, u, un), vertexAo(s, u, us))
            }
            else -> { // West (-X)
                val s = isOccluding(chunk, world, wx, wz, x - 1, y, z + 1)
                val n = isOccluding(chunk, world, wx, wz, x - 1, y, z - 1)
                val u = isOccluding(chunk, world, wx, wz, x - 1, y + 1, z)
                val d = isOccluding(chunk, world, wx, wz, x - 1, y - 1, z)
                val ds = isOccluding(chunk, world, wx, wz, x - 1, y - 1, z + 1)
                val dn = isOccluding(chunk, world, wx, wz, x - 1, y - 1, z - 1)
                val us = isOccluding(chunk, world, wx, wz, x - 1, y + 1, z + 1)
                val un = isOccluding(chunk, world, wx, wz, x - 1, y + 1, z - 1)
                FaceAo(vertexAo(n, d, dn), vertexAo(s, d, ds), vertexAo(s, u, us), vertexAo(n, u, un))
            }
        }
    }

    private fun addFace(
        data: FloatArray, offset: Int,
        x: Float, y: Float, z: Float,
        face: Int, uv: FloatArray, light: Float,
        ao0: Float, ao1: Float, ao2: Float, ao3: Float,
        faceMarker: Float
    ): Int {
        var idx = offset
        val u0 = uv[0]; val v0 = uv[1]; val u1 = uv[2]; val v1 = uv[3]
        val fn = faceMarker

        // 2 Triangles (6 vertices) per quad face with per-vertex AO
        when (face) {
            0 -> { // Top (+Y)
                // v0: (x, y+1, z)
                data[idx++] = x; data[idx++] = y + 1f; data[idx++] = z; data[idx++] = u0; data[idx++] = v0; data[idx++] = ao0; data[idx++] = light; data[idx++] = fn
                // v1: (x, y+1, z+1)
                data[idx++] = x; data[idx++] = y + 1f; data[idx++] = z + 1f; data[idx++] = u0; data[idx++] = v1; data[idx++] = ao1; data[idx++] = light; data[idx++] = fn
                // v2: (x+1, y+1, z+1)
                data[idx++] = x + 1f; data[idx++] = y + 1f; data[idx++] = z + 1f; data[idx++] = u1; data[idx++] = v1; data[idx++] = ao2; data[idx++] = light; data[idx++] = fn

                // v0
                data[idx++] = x; data[idx++] = y + 1f; data[idx++] = z; data[idx++] = u0; data[idx++] = v0; data[idx++] = ao0; data[idx++] = light; data[idx++] = fn
                // v2
                data[idx++] = x + 1f; data[idx++] = y + 1f; data[idx++] = z + 1f; data[idx++] = u1; data[idx++] = v1; data[idx++] = ao2; data[idx++] = light; data[idx++] = fn
                // v3: (x+1, y+1, z)
                data[idx++] = x + 1f; data[idx++] = y + 1f; data[idx++] = z; data[idx++] = u1; data[idx++] = v0; data[idx++] = ao3; data[idx++] = light; data[idx++] = fn
            }
            1 -> { // Bottom (-Y)
                data[idx++] = x; data[idx++] = y; data[idx++] = z + 1f; data[idx++] = u0; data[idx++] = v0; data[idx++] = ao0; data[idx++] = light; data[idx++] = fn
                data[idx++] = x; data[idx++] = y; data[idx++] = z; data[idx++] = u0; data[idx++] = v1; data[idx++] = ao1; data[idx++] = light; data[idx++] = fn
                data[idx++] = x + 1f; data[idx++] = y; data[idx++] = z; data[idx++] = u1; data[idx++] = v1; data[idx++] = ao2; data[idx++] = light; data[idx++] = fn

                data[idx++] = x; data[idx++] = y; data[idx++] = z + 1f; data[idx++] = u0; data[idx++] = v0; data[idx++] = ao0; data[idx++] = light; data[idx++] = fn
                data[idx++] = x + 1f; data[idx++] = y; data[idx++] = z; data[idx++] = u1; data[idx++] = v1; data[idx++] = ao2; data[idx++] = light; data[idx++] = fn
                data[idx++] = x + 1f; data[idx++] = y; data[idx++] = z + 1f; data[idx++] = u1; data[idx++] = v0; data[idx++] = ao3; data[idx++] = light; data[idx++] = fn
            }
            2 -> { // North (-Z)
                data[idx++] = x + 1f; data[idx++] = y; data[idx++] = z; data[idx++] = u0; data[idx++] = v1; data[idx++] = ao0; data[idx++] = light; data[idx++] = fn
                data[idx++] = x; data[idx++] = y; data[idx++] = z; data[idx++] = u1; data[idx++] = v1; data[idx++] = ao1; data[idx++] = light; data[idx++] = fn
                data[idx++] = x; data[idx++] = y + 1f; data[idx++] = z; data[idx++] = u1; data[idx++] = v0; data[idx++] = ao2; data[idx++] = light; data[idx++] = fn

                data[idx++] = x + 1f; data[idx++] = y; data[idx++] = z; data[idx++] = u0; data[idx++] = v1; data[idx++] = ao0; data[idx++] = light; data[idx++] = fn
                data[idx++] = x; data[idx++] = y + 1f; data[idx++] = z; data[idx++] = u1; data[idx++] = v0; data[idx++] = ao2; data[idx++] = light; data[idx++] = fn
                data[idx++] = x + 1f; data[idx++] = y + 1f; data[idx++] = z; data[idx++] = u0; data[idx++] = v0; data[idx++] = ao3; data[idx++] = light; data[idx++] = fn
            }
            3 -> { // South (+Z)
                data[idx++] = x; data[idx++] = y; data[idx++] = z + 1f; data[idx++] = u0; data[idx++] = v1; data[idx++] = ao0; data[idx++] = light; data[idx++] = fn
                data[idx++] = x + 1f; data[idx++] = y; data[idx++] = z + 1f; data[idx++] = u1; data[idx++] = v1; data[idx++] = ao1; data[idx++] = light; data[idx++] = fn
                data[idx++] = x + 1f; data[idx++] = y + 1f; data[idx++] = z + 1f; data[idx++] = u1; data[idx++] = v0; data[idx++] = ao2; data[idx++] = light; data[idx++] = fn

                data[idx++] = x; data[idx++] = y; data[idx++] = z + 1f; data[idx++] = u0; data[idx++] = v1; data[idx++] = ao0; data[idx++] = light; data[idx++] = fn
                data[idx++] = x + 1f; data[idx++] = y + 1f; data[idx++] = z + 1f; data[idx++] = u1; data[idx++] = v0; data[idx++] = ao2; data[idx++] = light; data[idx++] = fn
                data[idx++] = x; data[idx++] = y + 1f; data[idx++] = z + 1f; data[idx++] = u0; data[idx++] = v0; data[idx++] = ao3; data[idx++] = light; data[idx++] = fn
            }
            4 -> { // East (+X)
                data[idx++] = x + 1f; data[idx++] = y; data[idx++] = z + 1f; data[idx++] = u0; data[idx++] = v1; data[idx++] = ao0; data[idx++] = light; data[idx++] = fn
                data[idx++] = x + 1f; data[idx++] = y; data[idx++] = z; data[idx++] = u1; data[idx++] = v1; data[idx++] = ao1; data[idx++] = light; data[idx++] = fn
                data[idx++] = x + 1f; data[idx++] = y + 1f; data[idx++] = z; data[idx++] = u1; data[idx++] = v0; data[idx++] = ao2; data[idx++] = light; data[idx++] = fn

                data[idx++] = x + 1f; data[idx++] = y; data[idx++] = z + 1f; data[idx++] = u0; data[idx++] = v1; data[idx++] = ao0; data[idx++] = light; data[idx++] = fn
                data[idx++] = x + 1f; data[idx++] = y + 1f; data[idx++] = z; data[idx++] = u1; data[idx++] = v0; data[idx++] = ao2; data[idx++] = light; data[idx++] = fn
                data[idx++] = x + 1f; data[idx++] = y + 1f; data[idx++] = z + 1f; data[idx++] = u0; data[idx++] = v0; data[idx++] = ao3; data[idx++] = light; data[idx++] = fn
            }
            5 -> { // West (-X)
                data[idx++] = x; data[idx++] = y; data[idx++] = z; data[idx++] = u0; data[idx++] = v1; data[idx++] = ao0; data[idx++] = light; data[idx++] = fn
                data[idx++] = x; data[idx++] = y; data[idx++] = z + 1f; data[idx++] = u1; data[idx++] = v1; data[idx++] = ao1; data[idx++] = light; data[idx++] = fn
                data[idx++] = x; data[idx++] = y + 1f; data[idx++] = z + 1f; data[idx++] = u1; data[idx++] = v0; data[idx++] = ao2; data[idx++] = light; data[idx++] = fn

                data[idx++] = x; data[idx++] = y; data[idx++] = z; data[idx++] = u0; data[idx++] = v1; data[idx++] = ao0; data[idx++] = light; data[idx++] = fn
                data[idx++] = x; data[idx++] = y + 1f; data[idx++] = z + 1f; data[idx++] = u1; data[idx++] = v0; data[idx++] = ao2; data[idx++] = light; data[idx++] = fn
                data[idx++] = x; data[idx++] = y + 1f; data[idx++] = z; data[idx++] = u0; data[idx++] = v0; data[idx++] = ao3; data[idx++] = light; data[idx++] = fn
            }
        }
        return idx
    }

    private fun addCrossedQuads(data: FloatArray, offset: Int, x: Float, y: Float, z: Float, uv: FloatArray, light: Float): Int {
        var idx = offset
        val u0 = uv[0]; val v0 = uv[1]; val u1 = uv[2]; val v1 = uv[3]
        val fn = 0f

        // Diagonal 1: (x, z) to (x+1, z+1)
        data[idx++] = x; data[idx++] = y; data[idx++] = z; data[idx++] = u0; data[idx++] = v1; data[idx++] = 1f; data[idx++] = light; data[idx++] = fn
        data[idx++] = x + 1f; data[idx++] = y; data[idx++] = z + 1f; data[idx++] = u1; data[idx++] = v1; data[idx++] = 1f; data[idx++] = light; data[idx++] = fn
        data[idx++] = x + 1f; data[idx++] = y + 1f; data[idx++] = z + 1f; data[idx++] = u1; data[idx++] = v0; data[idx++] = 1f; data[idx++] = light; data[idx++] = fn

        data[idx++] = x; data[idx++] = y; data[idx++] = z; data[idx++] = u0; data[idx++] = v1; data[idx++] = 1f; data[idx++] = light; data[idx++] = fn
        data[idx++] = x + 1f; data[idx++] = y + 1f; data[idx++] = z + 1f; data[idx++] = u1; data[idx++] = v0; data[idx++] = 1f; data[idx++] = light; data[idx++] = fn
        data[idx++] = x; data[idx++] = y + 1f; data[idx++] = z; data[idx++] = u0; data[idx++] = v0; data[idx++] = 1f; data[idx++] = light; data[idx++] = fn

        // Diagonal 2: (x+1, z) to (x, z+1)
        data[idx++] = x + 1f; data[idx++] = y; data[idx++] = z; data[idx++] = u0; data[idx++] = v1; data[idx++] = 1f; data[idx++] = light; data[idx++] = fn
        data[idx++] = x; data[idx++] = y; data[idx++] = z + 1f; data[idx++] = u1; data[idx++] = v1; data[idx++] = 1f; data[idx++] = light; data[idx++] = fn
        data[idx++] = x; data[idx++] = y + 1f; data[idx++] = z + 1f; data[idx++] = u1; data[idx++] = v0; data[idx++] = 1f; data[idx++] = light; data[idx++] = fn

        data[idx++] = x + 1f; data[idx++] = y; data[idx++] = z; data[idx++] = u0; data[idx++] = v1; data[idx++] = 1f; data[idx++] = light; data[idx++] = fn
        data[idx++] = x; data[idx++] = y + 1f; data[idx++] = z + 1f; data[idx++] = u1; data[idx++] = v0; data[idx++] = 1f; data[idx++] = light; data[idx++] = fn
        data[idx++] = x + 1f; data[idx++] = y + 1f; data[idx++] = z; data[idx++] = u0; data[idx++] = v0; data[idx++] = 1f; data[idx++] = light; data[idx++] = fn

        return idx
    }

    private fun uploadMesh(data: FloatArray, length: Int): Pair<Int, Int> {
        if (length == 0) return Pair(0, 0)

        val vaos = IntArray(1)
        val vbos = IntArray(1)
        GLES30.glGenVertexArrays(1, vaos, 0)
        GLES30.glGenBuffers(1, vbos, 0)

        val vao = vaos[0]
        val vbo = vbos[0]

        val buffer: FloatBuffer = ByteBuffer.allocateDirect(length * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        buffer.put(data, 0, length)
        buffer.position(0)

        GLES30.glBindVertexArray(vao)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, length * 4, buffer, GLES30.GL_STATIC_DRAW)

        val stride = 8 * 4 // 8 floats: x, y, z, u, v, ao, light, faceNormal

        // Position: attribute 0 (3 floats)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, 0)

        // UV: attribute 1 (2 floats)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, stride, 3 * 4)

        // AO & Light & Normal: attribute 2 (3 floats)
        GLES30.glEnableVertexAttribArray(2)
        GLES30.glVertexAttribPointer(2, 3, GLES30.GL_FLOAT, false, stride, 5 * 4)

        GLES30.glBindVertexArray(0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)

        return Pair(vao, vbo)
    }
}
