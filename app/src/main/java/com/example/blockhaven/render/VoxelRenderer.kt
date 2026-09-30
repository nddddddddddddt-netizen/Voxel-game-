package com.example.blockhaven.render

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.blockhaven.core.math.Vec3f
import com.example.blockhaven.entity.MobManager
import com.example.blockhaven.entity.Player
import com.example.blockhaven.gameplay.Inventory
import com.example.blockhaven.world.BlockType
import com.example.blockhaven.world.World
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class VoxelRenderer(
    val world: World,
    val player: Player,
    val mobManager: MobManager,
    val particles: ParticleSystem,
    var inventory: Inventory? = null
) : GLSurfaceView.Renderer {

    private var program = 0
    private var uMVPMatrix = 0
    private var uModelMatrix = 0
    private var uTexture = 0
    private var uSunFactor = 0
    private var uSunDir = 0
    private var uCameraPos = 0
    private var uFogColor = 0
    private var uTime = 0
    private var uIsUnderwater = 0
    private var uTorchPos = 0
    private var uTorchLight = 0

    private val viewMatrix = FloatArray(16)
    private val projMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    private val identityModel = FloatArray(16).apply { Matrix.setIdentityM(this, 0) }

    val skyRenderer = SkyRenderer()
    private val chunkMeshes = ConcurrentHashMap<Long, ChunkMesh>()
    private val pendingMeshes = ConcurrentHashMap<Long, RawChunkMeshData>()
    private val inProgressChunks = java.util.concurrent.ConcurrentHashMap.newKeySet<Long>()

    // Selection box program
    private var wireProgram = 0
    private var uWireMVP = 0
    private var uWireColor = 0
    private var wireVao = 0
    private var wireVbo = 0

    // 3D First Person Hand and Held Item
    private var armVao = 0
    private var armVbo = 0
    private var itemCubeVao = 0
    private var itemCubeVbo = 0

    var fov = 75f
    var renderDistance = 4
    private var aspectRatio = 16f / 9f

    private var lastTimeNanos = System.nanoTime()
    private var elapsedSeconds = 0f
    var currentFps = 60

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glDepthFunc(GLES30.GL_LEQUAL)
        GLES30.glEnable(GLES30.GL_CULL_FACE)
        GLES30.glCullFace(GLES30.GL_BACK)

        // Init procedural texture atlas
        TextureAtlas.init()

        // Init enhanced voxel shader
        initVoxelShader()

        // Init celestial sky dome, sun, moon, stars, clouds
        skyRenderer.init()

        // Init mobs and particles
        mobManager.init()
        particles.init()

        // Init selection box & 3D first person models
        initWireframeBox()
        initFirstPersonModels()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        aspectRatio = width.toFloat() / height.toFloat().coerceAtLeast(1f)
        Matrix.perspectiveM(projMatrix, 0, fov, aspectRatio, 0.1f, 300f)
    }

    private fun initVoxelShader() {
        val vShader = """
            #version 300 es
            layout(location = 0) in vec3 aPosition;
            layout(location = 1) in vec2 aTexCoord;
            layout(location = 2) in vec3 aAoLightNormal; // x=ao, y=light, z=marker

            uniform mat4 uMVP;
            uniform mat4 uModel;
            uniform float uTime;

            out vec2 vTexCoord;
            out vec3 vWorldPos;
            out vec3 vNormal;
            out float vAo;
            out float vLight;
            out float vDist;

            void main() {
                vTexCoord = aTexCoord;
                vAo = aAoLightNormal.x;
                vLight = aAoLightNormal.y;

                vec3 pos = aPosition;
                // Water wave displacement animation
                if (aAoLightNormal.z == 10.0) {
                    pos.y += sin(pos.x * 2.2 + uTime * 3.0) * cos(pos.z * 2.2 + uTime * 2.2) * 0.055;
                }

                // Normal derivation from face marker
                float fn = aAoLightNormal.z;
                if (fn == 0.0 || fn == 10.0) vNormal = vec3(0.0, 1.0, 0.0);
                else if (fn == 1.0) vNormal = vec3(0.0, -1.0, 0.0);
                else if (fn == 2.0) vNormal = vec3(0.0, 0.0, -1.0);
                else if (fn == 3.0) vNormal = vec3(0.0, 0.0, 1.0);
                else if (fn == 4.0) vNormal = vec3(1.0, 0.0, 0.0);
                else vNormal = vec3(-1.0, 0.0, 0.0);

                vec4 worldPos4 = uModel * vec4(pos, 1.0);
                vWorldPos = worldPos4.xyz;
                gl_Position = uMVP * vec4(pos, 1.0);
                vDist = gl_Position.w;
            }
        """.trimIndent()

        val fShader = """
            #version 300 es
            precision highp float;

            in vec2 vTexCoord;
            in vec3 vWorldPos;
            in vec3 vNormal;
            in float vAo;
            in float vLight;
            in float vDist;

            uniform sampler2D uTexture;
            uniform vec3 uSunDir;
            uniform vec3 uCameraPos;
            uniform vec3 uFogColor;
            uniform float uSunFactor;
            uniform float uTime;
            uniform int uIsUnderwater;
            uniform vec3 uTorchPos;
            uniform float uTorchLight;

            out vec4 fragColor;

            void main() {
                vec4 tex = texture(uTexture, vTexCoord);
                if (tex.a < 0.1) discard;

                // Emissive check (Torches, Luminite, Pyrite flame, glowing crystals)
                bool isEmissive = (tex.r > 0.82 && tex.g > 0.72 && tex.b < 0.35 && vLight > 0.55) ||
                                  (tex.b > 0.85 && tex.r > 0.65 && vLight > 0.55);

                // Direct sunlight diffuse calculation
                float nDotL = max(dot(vNormal, uSunDir), 0.0);
                float diffuse = mix(0.55, 1.0, nDotL * uSunFactor);

                // Voxel Ambient Occlusion smooth darkening
                float ao = clamp(vAo, 0.35, 1.0);
                vec3 litColor = tex.rgb * vLight * diffuse * ao;

                // Dynamic handheld torch point light
                if (uTorchLight > 0.05) {
                    float torchDist = length(uTorchPos - vWorldPos);
                    if (torchDist < 12.0) {
                        float torchAtten = clamp(1.0 - (torchDist / 12.0), 0.0, 1.0);
                        torchAtten = torchAtten * torchAtten * uTorchLight;
                        vec3 warmTorch = vec3(1.0, 0.72, 0.35);
                        litColor += tex.rgb * warmTorch * torchAtten * 1.5;
                    }
                }

                // Emissive bloom / glow
                if (isEmissive) {
                    litColor = mix(litColor, tex.rgb * 1.5, 0.75);
                }

                // Specular sun reflection for water
                vec3 viewDir = normalize(uCameraPos - vWorldPos);
                vec3 halfDir = normalize(uSunDir + viewDir);
                float spec = pow(max(dot(vNormal, halfDir), 0.0), 32.0);
                if (tex.a < 0.95 && !isEmissive) {
                    litColor += vec3(1.0, 0.96, 0.85) * spec * 0.45 * uSunFactor;
                }

                // Atmospheric Rayleigh scattering distance fog
                float fogNear = (uIsUnderwater == 1) ? 2.0 : 35.0;
                float fogFar = (uIsUnderwater == 1) ? 22.0 : 135.0;
                float fogFactor = clamp((vDist - fogNear) / (fogFar - fogNear), 0.0, 1.0);
                fogFactor = pow(fogFactor, 1.4);

                vec3 finalFogColor = (uIsUnderwater == 1) ? vec3(0.06, 0.30, 0.52) : uFogColor;
                
                // Horizon sun scattering glow in distance
                float sunScattering = max(dot(viewDir, -uSunDir), 0.0);
                vec3 atmosphericHaze = mix(finalFogColor, vec3(1.0, 0.72, 0.42) * uSunFactor, pow(sunScattering, 4.0) * 0.3);

                vec3 finalRgb = mix(litColor, atmosphericHaze, fogFactor);

                // Contrast & vibrance grading
                finalRgb = pow(finalRgb, vec3(0.94));

                fragColor = vec4(finalRgb, tex.a);
            }
        """.trimIndent()

        program = ShaderUtils.createProgram(vShader, fShader)
        uMVPMatrix = GLES30.glGetUniformLocation(program, "uMVP")
        uModelMatrix = GLES30.glGetUniformLocation(program, "uModel")
        uTexture = GLES30.glGetUniformLocation(program, "uTexture")
        uSunFactor = GLES30.glGetUniformLocation(program, "uSunFactor")
        uSunDir = GLES30.glGetUniformLocation(program, "uSunDir")
        uCameraPos = GLES30.glGetUniformLocation(program, "uCameraPos")
        uFogColor = GLES30.glGetUniformLocation(program, "uFogColor")
        uTime = GLES30.glGetUniformLocation(program, "uTime")
        uIsUnderwater = GLES30.glGetUniformLocation(program, "uIsUnderwater")
        uTorchPos = GLES30.glGetUniformLocation(program, "uTorchPos")
        uTorchLight = GLES30.glGetUniformLocation(program, "uTorchLight")
    }

    private fun initWireframeBox() {
        val vShader = """
            #version 300 es
            layout(location = 0) in vec3 aPos;
            uniform mat4 uMVP;
            void main() {
                gl_Position = uMVP * vec4(aPos, 1.0);
            }
        """.trimIndent()

        val fShader = """
            #version 300 es
            precision mediump float;
            uniform vec4 uColor;
            out vec4 fragColor;
            void main() {
                fragColor = uColor;
            }
        """.trimIndent()

        wireProgram = ShaderUtils.createProgram(vShader, fShader)
        uWireMVP = GLES30.glGetUniformLocation(wireProgram, "uMVP")
        uWireColor = GLES30.glGetUniformLocation(wireProgram, "uColor")

        val e = 0.003f
        val l = 1f + e
        val o = -e
        val lines = floatArrayOf(
            o, o, o,  l, o, o,    l, o, o,  l, o, l,    l, o, l,  o, o, l,    o, o, l,  o, o, o,
            o, l, o,  l, l, o,    l, l, o,  l, l, l,    l, l, l,  o, l, l,    o, l, l,  o, l, o,
            o, o, o,  o, l, o,    l, o, o,  l, l, o,    l, o, l,  l, l, l,    o, o, l,  o, l, l
        )

        val vaos = IntArray(1); val vbos = IntArray(1)
        GLES30.glGenVertexArrays(1, vaos, 0); GLES30.glGenBuffers(1, vbos, 0)
        wireVao = vaos[0]; wireVbo = vbos[0]

        val buffer = ByteBuffer.allocateDirect(lines.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        buffer.put(lines).position(0)

        GLES30.glBindVertexArray(wireVao)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, wireVbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, lines.size * 4, buffer, GLES30.GL_STATIC_DRAW)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 3 * 4, 0)
        GLES30.glBindVertexArray(0)
    }

    private fun initFirstPersonModels() {
        // 3D Arm Cuboid (width: 0.16, height: 0.55, depth: 0.16)
        val hw = 0.08f; val hh = 0.28f; val hd = 0.08f
        val armVerts = createCubeVertices(-hw, -hh, -hd, hw, hh, hd, 9) // Texture 9 = Wood / Sleeve

        val vaos = IntArray(2); val vbos = IntArray(2)
        GLES30.glGenVertexArrays(2, vaos, 0)
        GLES30.glGenBuffers(2, vbos, 0)
        armVao = vaos[0]; armVbo = vbos[0]
        itemCubeVao = vaos[1]; itemCubeVbo = vbos[1]

        val armBuf = ByteBuffer.allocateDirect(armVerts.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        armBuf.put(armVerts).position(0)
        GLES30.glBindVertexArray(armVao)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, armVbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, armVerts.size * 4, armBuf, GLES30.GL_STATIC_DRAW)
        val stride = 8 * 4
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, 0)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, stride, 3 * 4)
        GLES30.glEnableVertexAttribArray(2)
        GLES30.glVertexAttribPointer(2, 3, GLES30.GL_FLOAT, false, stride, 5 * 4)
        GLES30.glBindVertexArray(0)

        // 3D Mini Held Item Cube
        val itemVerts = createCubeVertices(-0.1f, -0.1f, -0.1f, 0.1f, 0.1f, 0.1f, 0)
        val itemBuf = ByteBuffer.allocateDirect(itemVerts.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        itemBuf.put(itemVerts).position(0)
        GLES30.glBindVertexArray(itemCubeVao)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, itemCubeVbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, itemVerts.size * 4, itemBuf, GLES30.GL_DYNAMIC_DRAW)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, 0)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, stride, 3 * 4)
        GLES30.glEnableVertexAttribArray(2)
        GLES30.glVertexAttribPointer(2, 3, GLES30.GL_FLOAT, false, stride, 5 * 4)
        GLES30.glBindVertexArray(0)
    }

    private fun createCubeVertices(minX: Float, minY: Float, minZ: Float, maxX: Float, maxY: Float, maxZ: Float, tileIdx: Int): FloatArray {
        val uv = TextureAtlas.getUV(tileIdx)
        val u0 = uv[0]; val v0 = uv[1]; val u1 = uv[2]; val v1 = uv[3]
        val list = mutableListOf<Float>()

        fun addQ(x0: Float, y0: Float, z0: Float, x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float, x3: Float, y3: Float, z3: Float, fn: Float) {
            // Tri 1
            list.addAll(listOf(x0, y0, z0, u0, v0, 1f, 1f, fn))
            list.addAll(listOf(x1, y1, z1, u0, v1, 1f, 1f, fn))
            list.addAll(listOf(x2, y2, z2, u1, v1, 1f, 1f, fn))
            // Tri 2
            list.addAll(listOf(x0, y0, z0, u0, v0, 1f, 1f, fn))
            list.addAll(listOf(x2, y2, z2, u1, v1, 1f, 1f, fn))
            list.addAll(listOf(x3, y3, z3, u1, v0, 1f, 1f, fn))
        }

        // Top
        addQ(minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, 0f)
        // Bottom
        addQ(minX, minY, maxZ, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, 1f)
        // North
        addQ(maxX, minY, minZ, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, 2f)
        // South
        addQ(minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, 3f)
        // East
        addQ(maxX, minY, maxZ, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, 4f)
        // West
        addQ(minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, 5f)

        return list.toFloatArray()
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = System.nanoTime()
        val dt = ((now - lastTimeNanos) / 1_000_000_000.0).toFloat().coerceIn(0.001f, 0.05f)
        lastTimeNanos = now
        elapsedSeconds += dt
        currentFps = (1f / dt).toInt().coerceIn(10, 120)

        // Advance world time and particles
        world.tick(dt)
        particles.update(dt)
        if (world.rainStrength > 0.05f) {
            particles.spawnWeather(player.pos, world.rainStrength)
        }

        // Camera setup
        val eyePos = player.getEyePos()
        val lookDir = player.getLookDirection()
        val center = eyePos + lookDir

        Matrix.setLookAtM(
            viewMatrix, 0,
            eyePos.x, eyePos.y, eyePos.z,
            center.x, center.y, center.z,
            0f, 1f, 0f
        )

        Matrix.perspectiveM(projMatrix, 0, fov, aspectRatio, 0.1f, 300f)
        Matrix.multiplyMM(mvpMatrix, 0, projMatrix, 0, viewMatrix, 0)

        // Dynamic Sky clear color
        val skyCol = skyRenderer.getSkyClearColor(world.timeOfDay)
        GLES30.glClearColor(skyCol[0], skyCol[1], skyCol[2], skyCol[3])
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        // 1. Render Celestial Bodies (Sun, Moon, Stars)
        skyRenderer.renderCelestialBodies(viewMatrix, projMatrix, eyePos.x, eyePos.y, eyePos.z, world.timeOfDay)

        // 2. Render Volumetric Clouds
        skyRenderer.renderClouds(viewMatrix, projMatrix, player.pos.x, player.pos.z, elapsedSeconds)

        // 3. Render Voxel Chunks
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glDepthMask(true)
        GLES30.glUseProgram(program)
        GLES30.glUniformMatrix4fv(uMVPMatrix, 1, false, mvpMatrix, 0)
        GLES30.glUniformMatrix4fv(uModelMatrix, 1, false, identityModel, 0)
        GLES30.glUniform1f(uSunFactor, world.daylightFactor)
        GLES30.glUniform1f(uTime, elapsedSeconds)
        GLES30.glUniform1i(uIsUnderwater, if (player.isInWater) 1 else 0)

        val sunDir = skyRenderer.getSunDirection(world.timeOfDay)
        GLES30.glUniform3f(uSunDir, sunDir[0], sunDir[1], sunDir[2])
        GLES30.glUniform3f(uCameraPos, eyePos.x, eyePos.y, eyePos.z)

        val fogCol = skyRenderer.getFogColor(world.timeOfDay)
        GLES30.glUniform3f(uFogColor, fogCol[0], fogCol[1], fogCol[2])

        val heldItem = inventory?.getSelectedItem()
        val holdsTorch = heldItem?.itemId == BlockType.TORCH.toInt() || heldItem?.itemId == BlockType.LUMINITE_ORE.toInt()
        val torchStrength = if (holdsTorch) 1.0f else 0.0f
        GLES30.glUniform3f(uTorchPos, eyePos.x, eyePos.y, eyePos.z)
        GLES30.glUniform1f(uTorchLight, torchStrength)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, TextureAtlas.textureId)
        GLES30.glUniform1i(uTexture, 0)

        // 1. Process prepared raw meshes from background threads (instant GPU upload)
        if (pendingMeshes.isNotEmpty()) {
            val iter = pendingMeshes.entries.iterator()
            var uploadsThisFrame = 0
            while (iter.hasNext() && uploadsThisFrame < 4) {
                val entry = iter.next()
                iter.remove()
                val oldMesh = chunkMeshes[entry.key]
                oldMesh?.delete()
                chunkMeshes[entry.key] = ChunkMeshBuilder.upload(entry.value)
                uploadsThisFrame++
            }
        }

        // 2. Queue dirty/new chunks for background building without blocking render loop
        for ((key, chunk) in world.chunks) {
            val mesh = chunkMeshes[key]
            if ((chunk.isMeshDirty || mesh == null) && !inProgressChunks.contains(key)) {
                inProgressChunks.add(key)
                world.worldScope.launch(kotlinx.coroutines.Dispatchers.Default) {
                    val raw = ChunkMeshBuilder.buildRaw(chunk, world)
                    if (raw != null) {
                        pendingMeshes[key] = raw
                    }
                    inProgressChunks.remove(key)
                    chunk.isMeshDirty = false
                }
            }

            if (mesh != null && mesh.opaqueVertexCount > 0) {
                GLES30.glBindVertexArray(mesh.opaqueVao)
                GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, mesh.opaqueVertexCount)
            }
        }

        // 3. Clean up unloaded chunk meshes
        val unloadedKeys = chunkMeshes.keys.filter { !world.chunks.containsKey(it) }
        for (k in unloadedKeys) {
            chunkMeshes.remove(k)?.delete()
            pendingMeshes.remove(k)
            inProgressChunks.remove(k)
        }

        // 4. Render Mobs
        mobManager.render(viewMatrix, projMatrix)

        // 5. Render Particle System
        particles.render(mvpMatrix)

        // 6. Render Translucent Chunks (Water, Glass, Leaves)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
        GLES30.glDepthMask(false)
        GLES30.glUseProgram(program)
        GLES30.glUniformMatrix4fv(uMVPMatrix, 1, false, mvpMatrix, 0)

        for ((_, mesh) in chunkMeshes) {
            if (mesh.transVertexCount > 0) {
                GLES30.glBindVertexArray(mesh.transVao)
                GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, mesh.transVertexCount)
            }
        }

        GLES30.glDepthMask(true)
        GLES30.glDisable(GLES30.GL_BLEND)

        // 7. Render Glowing Selection Box on targeted voxel
        player.targetedHit?.let { hit ->
            renderSelectionBox(hit.blockPos.x.toFloat(), hit.blockPos.y.toFloat(), hit.blockPos.z.toFloat())
        }

        // 8. Render First Person 3D Hand and Tool/Block
        renderFirstPersonHand()
    }

    private fun renderSelectionBox(bx: Float, by: Float, bz: Float) {
        GLES30.glUseProgram(wireProgram)
        val selModel = FloatArray(16)
        val selMvp = FloatArray(16)
        Matrix.setIdentityM(selModel, 0)
        Matrix.translateM(selModel, 0, bx, by, bz)
        Matrix.multiplyMM(selMvp, 0, mvpMatrix, 0, selModel, 0)

        val pulse = (sin(elapsedSeconds * 6f) * 0.15f + 0.85f).coerceIn(0f, 1f)
        GLES30.glUniform4f(uWireColor, 0.15f, 0.95f * pulse, 1.0f * pulse, 0.9f)
        GLES30.glUniformMatrix4fv(uWireMVP, 1, false, selMvp, 0)
        GLES30.glBindVertexArray(wireVao)
        GLES30.glLineWidth(4.0f)
        GLES30.glDrawArrays(GLES30.GL_LINES, 0, 24)
        GLES30.glBindVertexArray(0)
    }

    private fun renderFirstPersonHand() {
        GLES30.glClear(GLES30.GL_DEPTH_BUFFER_BIT)
        GLES30.glUseProgram(program)

        val handProj = FloatArray(16)
        val handView = FloatArray(16)
        val handModel = FloatArray(16)
        val handVp = FloatArray(16)
        val handMvp = FloatArray(16)

        Matrix.perspectiveM(handProj, 0, 62f, aspectRatio, 0.05f, 10f)
        Matrix.setLookAtM(handView, 0, 0f, 0f, 0f, 0f, 0f, -1f, 0f, 1f, 0f)
        Matrix.multiplyMM(handVp, 0, handProj, 0, handView, 0)

        // Walking bobbing sway
        val bobX = sin(player.walkPhase) * 0.03f
        val bobY = abs(cos(player.walkPhase)) * 0.025f

        // Mining / hitting swing animation curve
        val swing = sin(player.swingTime * Math.PI.toFloat())

        // 1. Render 3D Arm
        Matrix.setIdentityM(handModel, 0)
        Matrix.translateM(
            handModel, 0,
            0.36f + bobX - swing * 0.25f,
            -0.34f + bobY - swing * 0.18f,
            -0.65f - swing * 0.15f
        )
        Matrix.rotateM(handModel, 0, -22f - swing * 45f, 1f, 0f, 0f)
        Matrix.rotateM(handModel, 0, 24f + swing * 25f, 0f, 1f, 0f)
        Matrix.rotateM(handModel, 0, -10f - swing * 20f, 0f, 0f, 1f)

        Matrix.multiplyMM(handMvp, 0, handVp, 0, handModel, 0)

        GLES30.glUniformMatrix4fv(uMVPMatrix, 1, false, handMvp, 0)
        GLES30.glUniformMatrix4fv(uModelMatrix, 1, false, handModel, 0)
        GLES30.glUniform1f(uSunFactor, 1.0f)
        GLES30.glUniform3f(uSunDir, 0.4f, 0.8f, 0.4f)
        GLES30.glUniform1i(uIsUnderwater, 0)

        GLES30.glBindVertexArray(armVao)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 36)
        GLES30.glBindVertexArray(0)

        // 2. Render 3D Held Item in hand
        val heldItem = inventory?.getSelectedItem()
        if (heldItem != null) {
            val itemModel = FloatArray(16)
            Matrix.setIdentityM(itemModel, 0)
            Matrix.translateM(
                itemModel, 0,
                0.32f + bobX - swing * 0.35f,
                -0.18f + bobY - swing * 0.22f,
                -0.58f - swing * 0.2f
            )
            Matrix.rotateM(itemModel, 0, -15f - swing * 55f, 1f, 0f, 0f)
            Matrix.rotateM(itemModel, 0, 45f + swing * 30f, 0f, 1f, 0f)

            val itemMvp = FloatArray(16)
            Matrix.multiplyMM(itemMvp, 0, handVp, 0, itemModel, 0)

            GLES30.glUniformMatrix4fv(uMVPMatrix, 1, false, itemMvp, 0)
            GLES30.glUniformMatrix4fv(uModelMatrix, 1, false, itemModel, 0)

            GLES30.glBindVertexArray(itemCubeVao)
            GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 36)
            GLES30.glBindVertexArray(0)
        }
    }
}
