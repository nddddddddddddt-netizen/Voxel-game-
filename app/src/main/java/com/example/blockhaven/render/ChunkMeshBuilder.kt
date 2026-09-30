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

class RawChunkMeshData(
    val opaqueData: FloatArray,
    val opaqueLength: Int,
    val transData: FloatArray,
    val transLength: Int
)

object ChunkMeshBuilder {
    private val FACE_LIGHTING = floatArrayOf(
        1.0f,   // Top
        0.55f,  // Bottom
        0.8f,   // North
        0.8f,   // South
        0.7f,   // East
        0.7f    // West
    )

    data class FaceAo(val ao0: Float, val ao1: Float, val ao2: Float, val ao3: Float)

    /**
     * Phase 1: Pure CPU calculation (runs on background thread, no GL context needed)
     * Features greedy row-merging to reduce vertex count.
     */
    fun buildRaw(chunk: Chunk, world: World): RawChunkMeshData? {
        val wx = chunk.chunkX * Chunk.WIDTH
        val wz = chunk.chunkZ * Chunk.DEPTH

        var opaqueData = FloatArray(32000)
        var transData = FloatArray(16000)
        var oIndex = 0
        var tIndex = 0

        for (face in 0..5) {
            val lightMultiplier = FACE_LIGHTING[face]

            for (y in 0 until Chunk.HEIGHT) {
                for (z in 0 until Chunk.DEPTH) {
                    var x = 0
                    while (x < Chunk.WIDTH) {
                        val blockId = chunk.getBlock(x, y, z)
                        if (blockId == BlockType.AIR) {
                            x++
                            continue
                        }

                        val def = BlockType.get(blockId)
                        val isTrans = def.isTransparent || def.isLiquid

                        if (def.isPlant) {
                            if (face == 0) { // Only add plants once
                                val uv = TextureAtlas.getUV(def.textureSide)
                                val light = computeVoxelLight(chunk, world, x, y, z, 0)
                                if (tIndex + 48 >= transData.size) {
                                    transData = transData.copyOf(transData.size * 2)
                                }
                                val bx = (wx + x).toFloat()
                                val by = y.toFloat()
                                val bz = (wz + z).toFloat()
                                tIndex = addCrossedQuads(transData, tIndex, bx, by, bz, uv, light)
                            }
                            x++
                            continue
                        }

                        // Check face visibility
                        val nx = x + when (face) { 4 -> 1; 5 -> -1; else -> 0 }
                        val ny = y + when (face) { 0 -> 1; 1 -> -1; else -> 0 }
                        val nz = z + when (face) { 3 -> 1; 2 -> -1; else -> 0 }

                        val neighborBlock = if (Chunk.isValidPos(nx, ny, nz)) {
                            chunk.getBlock(nx, ny, nz)
                        } else {
                            world.getBlock(wx + nx, ny, wz + nz)
                        }

                        val shouldRenderFace = if (def.isLiquid) {
                            neighborBlock != BlockType.WATER && (!BlockType.isSolid(neighborBlock) || BlockType.isTransparent(neighborBlock))
                        } else if (def.isTransparent) {
                            neighborBlock != blockId && (!BlockType.isSolid(neighborBlock) || BlockType.isTransparent(neighborBlock))
                        } else {
                            !BlockType.isSolid(neighborBlock) || BlockType.isTransparent(neighborBlock)
                        }

                        if (!shouldRenderFace) {
                            x++
                            continue
                        }

                        val tile = when (face) {
                            0 -> def.textureTop
                            1 -> def.textureBottom
                            else -> def.textureSide
                        }
                        val uv = TextureAtlas.getUV(tile)
                        val faceLight = computeVoxelLight(chunk, world, x, y, z, face) * lightMultiplier
                        val faceAo = if (!def.isLiquid) {
                            computeFaceAo(chunk, world, wx, wz, x, y, z, face)
                        } else {
                            FaceAo(1f, 1f, 1f, 1f)
                        }

                        // Greedy Run-Length along X axis for faces 0, 1, 2, 3
                        var run = 1
                        if (face in 0..3 && !def.isLiquid) {
                            while (x + run < Chunk.WIDTH) {
                                val nextB = chunk.getBlock(x + run, y, z)
                                if (nextB != blockId) break

                                val nnx = (x + run) + when (face) { 4 -> 1; 5 -> -1; else -> 0 }
                                val nny = y + when (face) { 0 -> 1; 1 -> -1; else -> 0 }
                                val nnz = z + when (face) { 3 -> 1; 2 -> -1; else -> 0 }
                                val nextNeighbor = if (Chunk.isValidPos(nnx, nny, nnz)) chunk.getBlock(nnx, nny, nnz) else world.getBlock(wx + nnx, nny, wz + nnz)
                                val nextShouldRender = !BlockType.isSolid(nextNeighbor) || BlockType.isTransparent(nextNeighbor)
                                if (!nextShouldRender) break

                                val nextAo = computeFaceAo(chunk, world, wx, wz, x + run, y, z, face)
                                if (nextAo != faceAo) break

                                run++
                            }
                        }

                        val bx = (wx + x).toFloat()
                        val by = y.toFloat()
                        val bz = (wz + z).toFloat()
                        val faceMarker = if (def.isLiquid) 10f else face.toFloat()

                        if (isTrans) {
                            if (tIndex + 48 >= transData.size) {
                                transData = transData.copyOf(transData.size * 2)
                            }
                            tIndex = addGreedyFace(transData, tIndex, bx, by, bz, run.toFloat(), face, uv, faceLight, faceAo, faceMarker)
                        } else {
                            if (oIndex + 48 >= opaqueData.size) {
                                opaqueData = opaqueData.copyOf(opaqueData.size * 2)
                            }
                            oIndex = addGreedyFace(opaqueData, oIndex, bx, by, bz, run.toFloat(), face, uv, faceLight, faceAo, faceMarker)
                        }

                        x += run
                    }
                }
            }
        }

        if (oIndex == 0 && tIndex == 0) return null
        return RawChunkMeshData(opaqueData, oIndex, transData, tIndex)
    }

    /**
     * Phase 2: Uploads prepared float array to OpenGL buffers (called on GL Thread in microseconds)
     */
    fun upload(raw: RawChunkMeshData): ChunkMesh {
        val oCount = raw.opaqueLength / 8
        val tCount = raw.transLength / 8

        val oBuffers = uploadMesh(raw.opaqueData, raw.opaqueLength)
        val tBuffers = uploadMesh(raw.transData, raw.transLength)

        return ChunkMesh(
            opaqueVao = oBuffers.first,
            opaqueVbo = oBuffers.second,
            opaqueVertexCount = oCount,
            transVao = tBuffers.first,
            transVbo = tBuffers.second,
            transVertexCount = tCount
        )
    }

    /**
     * Synchronous build for direct rendering fallback
     */
    fun build(chunk: Chunk, world: World): ChunkMesh? {
        val raw = buildRaw(chunk, world) ?: return null
        chunk.isMeshDirty = false
        return upload(raw)
    }

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

    private fun addGreedyFace(
        data: FloatArray, offset: Int,
        x: Float, y: Float, z: Float, width: Float,
        face: Int, uv: FloatArray, light: Float,
        ao: FaceAo, faceMarker: Float
    ): Int {
        var idx = offset
        val u0 = uv[0]; val v0 = uv[1]; val u1 = uv[2]; val v1 = uv[3]
        val fn = faceMarker
        val ao0 = ao.ao0; val ao1 = ao.ao1; val ao2 = ao.ao2; val ao3 = ao.ao3

        when (face) {
            0 -> { // Top (+Y)
                data[idx++] = x; data[idx++] = y + 1f; data[idx++] = z; data[idx++] = u0; data[idx++] = v0; data[idx++] = ao0; data[idx++] = light; data[idx++] = fn
                data[idx++] = x; data[idx++] = y + 1f; data[idx++] = z + 1f; data[idx++] = u0; data[idx++] = v1; data[idx++] = ao1; data[idx++] = light; data[idx++] = fn
                data[idx++] = x + width; data[idx++] = y + 1f; data[idx++] = z + 1f; data[idx++] = u1; data[idx++] = v1; data[idx++] = ao2; data[idx++] = light; data[idx++] = fn

                data[idx++] = x; data[idx++] = y + 1f; data[idx++] = z; data[idx++] = u0; data[idx++] = v0; data[idx++] = ao0; data[idx++] = light; data[idx++] = fn
                data[idx++] = x + width; data[idx++] = y + 1f; data[idx++] = z + 1f; data[idx++] = u1; data[idx++] = v1; data[idx++] = ao2; data[idx++] = light; data[idx++] = fn
                data[idx++] = x + width; data[idx++] = y + 1f; data[idx++] = z; data[idx++] = u1; data[idx++] = v0; data[idx++] = ao3; data[idx++] = light; data[idx++] = fn
            }
            1 -> { // Bottom (-Y)
                data[idx++] = x; data[idx++] = y; data[idx++] = z + 1f; data[idx++] = u0; data[idx++] = v0; data[idx++] = ao0; data[idx++] = light; data[idx++] = fn
                data[idx++] = x; data[idx++] = y; data[idx++] = z; data[idx++] = u0; data[idx++] = v1; data[idx++] = ao1; data[idx++] = light; data[idx++] = fn
                data[idx++] = x + width; data[idx++] = y; data[idx++] = z; data[idx++] = u1; data[idx++] = v1; data[idx++] = ao2; data[idx++] = light; data[idx++] = fn

                data[idx++] = x; data[idx++] = y; data[idx++] = z + 1f; data[idx++] = u0; data[idx++] = v0; data[idx++] = ao0; data[idx++] = light; data[idx++] = fn
                data[idx++] = x + width; data[idx++] = y; data[idx++] = z; data[idx++] = u1; data[idx++] = v1; data[idx++] = ao2; data[idx++] = light; data[idx++] = fn
                data[idx++] = x + width; data[idx++] = y; data[idx++] = z + 1f; data[idx++] = u1; data[idx++] = v0; data[idx++] = ao3; data[idx++] = light; data[idx++] = fn
            }
            2 -> { // North (-Z)
                data[idx++] = x + width; data[idx++] = y; data[idx++] = z; data[idx++] = u0; data[idx++] = v1; data[idx++] = ao0; data[idx++] = light; data[idx++] = fn
                data[idx++] = x; data[idx++] = y; data[idx++] = z; data[idx++] = u1; data[idx++] = v1; data[idx++] = ao1; data[idx++] = light; data[idx++] = fn
                data[idx++] = x; data[idx++] = y + 1f; data[idx++] = z; data[idx++] = u1; data[idx++] = v0; data[idx++] = ao2; data[idx++] = light; data[idx++] = fn

                data[idx++] = x + width; data[idx++] = y; data[idx++] = z; data[idx++] = u0; data[idx++] = v1; data[idx++] = ao0; data[idx++] = light; data[idx++] = fn
                data[idx++] = x; data[idx++] = y + 1f; data[idx++] = z; data[idx++] = u1; data[idx++] = v0; data[idx++] = ao2; data[idx++] = light; data[idx++] = fn
                data[idx++] = x + width; data[idx++] = y + 1f; data[idx++] = z; data[idx++] = u0; data[idx++] = v0; data[idx++] = ao3; data[idx++] = light; data[idx++] = fn
            }
            3 -> { // South (+Z)
                data[idx++] = x; data[idx++] = y; data[idx++] = z + 1f; data[idx++] = u0; data[idx++] = v1; data[idx++] = ao0; data[idx++] = light; data[idx++] = fn
                data[idx++] = x + width; data[idx++] = y; data[idx++] = z + 1f; data[idx++] = u1; data[idx++] = v1; data[idx++] = ao1; data[idx++] = light; data[idx++] = fn
                data[idx++] = x + width; data[idx++] = y + 1f; data[idx++] = z + 1f; data[idx++] = u1; data[idx++] = v0; data[idx++] = ao2; data[idx++] = light; data[idx++] = fn

                data[idx++] = x; data[idx++] = y; data[idx++] = z + 1f; data[idx++] = u0; data[idx++] = v1; data[idx++] = ao0; data[idx++] = light; data[idx++] = fn
                data[idx++] = x + width; data[idx++] = y + 1f; data[idx++] = z + 1f; data[idx++] = u1; data[idx++] = v0; data[idx++] = ao2; data[idx++] = light; data[idx++] = fn
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
            else -> { // West (-X)
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

        data[idx++] = x; data[idx++] = y; data[idx++] = z; data[idx++] = u0; data[idx++] = v1; data[idx++] = 1f; data[idx++] = light; data[idx++] = fn
        data[idx++] = x + 1f; data[idx++] = y; data[idx++] = z + 1f; data[idx++] = u1; data[idx++] = v1; data[idx++] = 1f; data[idx++] = light; data[idx++] = fn
        data[idx++] = x + 1f; data[idx++] = y + 1f; data[idx++] = z + 1f; data[idx++] = u1; data[idx++] = v0; data[idx++] = 1f; data[idx++] = light; data[idx++] = fn

        data[idx++] = x; data[idx++] = y; data[idx++] = z; data[idx++] = u0; data[idx++] = v1; data[idx++] = 1f; data[idx++] = light; data[idx++] = fn
        data[idx++] = x + 1f; data[idx++] = y + 1f; data[idx++] = z + 1f; data[idx++] = u1; data[idx++] = v0; data[idx++] = 1f; data[idx++] = light; data[idx++] = fn
        data[idx++] = x; data[idx++] = y + 1f; data[idx++] = z; data[idx++] = u0; data[idx++] = v0; data[idx++] = 1f; data[idx++] = light; data[idx++] = fn

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

        val stride = 8 * 4 // x, y, z, u, v, ao, light, faceNormal

        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, 0)

        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, stride, 3 * 4)

        GLES30.glEnableVertexAttribArray(2)
        GLES30.glVertexAttribPointer(2, 3, GLES30.GL_FLOAT, false, stride, 5 * 4)

        GLES30.glBindVertexArray(0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)

        return Pair(vao, vbo)
    }
}
