package com.example.blockhaven.ui

import android.content.Context
import android.opengl.GLSurfaceView
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.blockhaven.audio.SoundEngine
import com.example.blockhaven.core.math.Vec3f
import com.example.blockhaven.entity.GameMode
import com.example.blockhaven.entity.MobManager
import com.example.blockhaven.entity.Player
import com.example.blockhaven.gameplay.CraftingManager
import com.example.blockhaven.gameplay.Inventory
import com.example.blockhaven.gameplay.ItemRegistry
import com.example.blockhaven.gameplay.ItemStack
import com.example.blockhaven.gameplay.SmeltingManager
import com.example.blockhaven.render.ParticleSystem
import com.example.blockhaven.render.VoxelRenderer
import com.example.blockhaven.storage.SettingsRepository
import com.example.blockhaven.world.BlockType
import com.example.blockhaven.world.Chunk
import com.example.blockhaven.world.World
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sqrt

@Composable
fun GameScreen(
    world: World,
    player: Player,
    inventory: Inventory,
    soundEngine: SoundEngine,
    settings: SettingsRepository,
    onExitToMenu: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val vibrator = remember { context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator }

    // Touch controls state
    var joystickOffset by remember { mutableStateOf(Offset.Zero) }
    var isJumping by remember { mutableStateOf(false) }

    // Dialog state
    var isInventoryOpen by remember { mutableStateOf(false) }
    var isPauseOpen by remember { mutableStateOf(false) }
    var selectedHotbarSlot by remember { mutableIntStateOf(inventory.selectedHotbarIndex) }

    // Entities and renderer
    val mobManager = remember { MobManager() }
    val particles = remember { ParticleSystem() }
    val renderer = remember {
        VoxelRenderer(world, player, mobManager, particles, inventory).apply {
            fov = settings.fov
            renderDistance = settings.renderDistance
        }
    }

    var glView: GLSurfaceView? by remember { mutableStateOf(null) }

    // Keep inventory reference in sync with renderer
    LaunchedEffect(inventory.selectedHotbarIndex) {
        renderer.inventory = inventory
    }

    // Vibration helper
    fun triggerHaptic(durationMs: Long = 20) {
        if (settings.hapticFeedback && vibrator?.hasVibrator() == true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs)
            }
        }
    }

    // Main update loop
    LaunchedEffect(Unit) {
        // Crucial spawn safety check: make sure player isn't stuck inside terrain blocks
        player.ensureSafeGrounded(world)
        var lastStepSound = System.currentTimeMillis()
        while (isActive) {
            val moveX = (joystickOffset.x / 50f).coerceIn(-1f, 1f)
            val moveZ = (-joystickOffset.y / 50f).coerceIn(-1f, 1f)

            player.update(world, 0.016f, moveX, moveZ, isJumping)
            world.updatePlayerPosition(player.pos)
            mobManager.update(world, player, 0.016f)

            // Step sound and footstep dust
            if (player.onGround && (moveX != 0f || moveZ != 0f)) {
                val now = System.currentTimeMillis()
                if (now - lastStepSound > 340) {
                    lastStepSound = now
                    val belowBlock = world.getBlock(floor(player.pos.x).toInt(), floor(player.pos.y - 0.2f).toInt(), floor(player.pos.z).toInt())
                    soundEngine.playFootstep(BlockType.get(belowBlock).soundType)
                    val dustColor = BlockType.getParticleColor(belowBlock)
                    particles.spawnFootstep(player.pos, dustColor[0], dustColor[1], dustColor[2])
                }
            } else if (player.isInWater && (moveX != 0f || moveZ != 0f)) {
                val now = System.currentTimeMillis()
                if (now - lastStepSound > 450) {
                    lastStepSound = now
                    soundEngine.playWaterSplash()
                    particles.spawnWaterSplash(player.pos)
                }
            }

            delay(16)
        }
    }

    BackHandler {
        if (isInventoryOpen) {
            isInventoryOpen = false
        } else if (isPauseOpen) {
            isPauseOpen = false
        } else {
            isPauseOpen = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. 3D OpenGL Canvas
        AndroidView(
            factory = { ctx ->
                GLSurfaceView(ctx).apply {
                    setEGLContextClientVersion(3)
                    setRenderer(renderer)
                    renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                    glView = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Right Half Touch Area: Swipe to look around
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 220.dp)
                .pointerInput(settings.lookSensitivity, settings.invertY) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val sens = settings.lookSensitivity * 0.16f
                        val inv = if (settings.invertY) -1f else 1f

                        player.yaw = (player.yaw + dragAmount.x * sens) % 360f
                        player.pitch = (player.pitch + dragAmount.y * sens * inv).coerceIn(-89f, 89f)
                    }
                }
        )

        // 3. Left Half Touch Area: Virtual Floating Joystick
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 24.dp, bottom = 24.dp)
                .size(140.dp)
                .background(Color.Black.copy(alpha = settings.buttonOpacity * 0.4f), CircleShape)
                .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = { joystickOffset = Offset.Zero },
                        onDragCancel = { joystickOffset = Offset.Zero },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val newOffset = joystickOffset + dragAmount
                            val maxRadius = 50f
                            val len = sqrt(newOffset.x * newOffset.x + newOffset.y * newOffset.y)
                            joystickOffset = if (len > maxRadius) {
                                Offset((newOffset.x / len) * maxRadius, (newOffset.y / len) * maxRadius)
                            } else {
                                newOffset
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(joystickOffset.x.roundToInt(), joystickOffset.y.roundToInt()) }
                    .size(54.dp)
                    .background(Color.White.copy(alpha = settings.buttonOpacity * 0.85f), CircleShape)
            )
        }

        // 4. Center Crosshair
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(16.dp)
                    .height(2.dp)
                    .background(Color.White.copy(alpha = 0.85f))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(2.dp)
                    .height(16.dp)
                    .background(Color.White.copy(alpha = 0.85f))
            )
        }

        // 5. Action Buttons (Jump, Sneak, Mine, Place, Inventory, Menu)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // MINE / ATTACK Button
                    ActionButton(
                        icon = Icons.Default.FitnessCenter,
                        label = "Mine",
                        opacity = settings.buttonOpacity,
                        onClick = {
                            player.swingTime = 1.0f
                            soundEngine.playBlockHit()
                            triggerHaptic(25)

                            player.targetedHit?.let { hit ->
                                val b = world.getBlock(hit.blockPos.x, hit.blockPos.y, hit.blockPos.z)
                                if (b != BlockType.BEDROCK && b != BlockType.AIR) {
                                    val def = BlockType.get(b)
                                    val pColor = BlockType.getParticleColor(b)
                                    particles.spawnBreakParticles(
                                        Vec3f(hit.blockPos.x.toFloat(), hit.blockPos.y.toFloat(), hit.blockPos.z.toFloat()),
                                        pColor[0], pColor[1], pColor[2],
                                        16
                                    )
                                    particles.spawnHitSpark(Vec3f(hit.blockPos.x + 0.5f, hit.blockPos.y + 0.5f, hit.blockPos.z + 0.5f))
                                    world.setBlock(hit.blockPos.x, hit.blockPos.y, hit.blockPos.z, BlockType.AIR)
                                    soundEngine.playBlockBreak()

                                    // Add to inventory if not creative
                                    if (def.dropItem != BlockType.AIR) {
                                        inventory.addItem(ItemStack(def.dropItem.toInt(), 1))
                                    }
                                }
                            }
                        }
                    )

                    // BUILD / PLACE Button
                    ActionButton(
                        icon = Icons.Default.Build,
                        label = "Build",
                        opacity = settings.buttonOpacity,
                        onClick = {
                            player.targetedHit?.let { hit ->
                                val held = inventory.getSelectedItem()
                                if (held != null) {
                                    val def = ItemRegistry.get(held.itemId)
                                    if (def.isBlock && def.blockId != BlockType.AIR) {
                                        // Check player collision before placing
                                        val placeBox = com.example.blockhaven.core.math.AABB(
                                            hit.placePos.x.toFloat(), hit.placePos.y.toFloat(), hit.placePos.z.toFloat(),
                                            hit.placePos.x + 1f, hit.placePos.y + 1f, hit.placePos.z + 1f
                                        )
                                        if (!player.getBoundingBox().intersects(placeBox)) {
                                            world.setBlock(hit.placePos.x, hit.placePos.y, hit.placePos.z, def.blockId)
                                            if (def.blockId == BlockType.TORCH) {
                                                soundEngine.playTorchPlace()
                                            } else {
                                                soundEngine.playBlockPlace()
                                            }
                                            player.swingTime = 0.8f
                                            triggerHaptic(15)

                                            if (player.gameMode == GameMode.SURVIVAL) {
                                                inventory.consumeSelectedItem(1)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // SNEAK / CROUCH Button
                    ActionButton(
                        icon = Icons.Default.Shield,
                        label = if (player.isSneaking) "Stand" else "Sneak",
                        opacity = settings.buttonOpacity,
                        isActive = player.isSneaking,
                        onClick = {
                            player.isSneaking = !player.isSneaking
                            triggerHaptic(15)
                        }
                    )

                    // JUMP Button
                    ActionButton(
                        icon = Icons.Default.KeyboardArrowUp,
                        label = "Jump",
                        opacity = settings.buttonOpacity,
                        onClick = {
                            isJumping = true
                            triggerHaptic(15)
                            scope.launch {
                                delay(220)
                                isJumping = false
                            }
                        }
                    )
                }
            }
        }

        // 6. Top Status Bar (Health, Hunger, Air, Time, Coordinates, Menu, Inventory)
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Player stats
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                // Health
                Surface(
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("❤️", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${player.health.toInt()}/20", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                // Coordinates & Biome
                Surface(
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "XYZ: ${player.pos.x.toInt()}, ${player.pos.y.toInt()}, ${player.pos.z.toInt()} | FPS: ${renderer.currentFps}",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Top action buttons: Inventory & Pause
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    color = Color.Black.copy(alpha = settings.buttonOpacity),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable {
                        soundEngine.playUiClick()
                        isInventoryOpen = true
                    }
                ) {
                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Inventory2, contentDescription = "Inventory", tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Inventory", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Surface(
                    color = Color.Black.copy(alpha = settings.buttonOpacity),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable {
                        soundEngine.playUiClick()
                        isPauseOpen = true
                    }
                ) {
                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Pause, contentDescription = "Pause Menu", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // 7. Hotbar (9 slots at bottom center)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                .border(2.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                .padding(4.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (i in 0 until 9) {
                    val stack = inventory.getSlot(i)
                    val isSelected = (i == selectedHotbarSlot)
                    val def = stack?.let { ItemRegistry.get(it.itemId) }

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                if (isSelected) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.1f),
                                RoundedCornerShape(6.dp)
                            )
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) Color(0xFF64B5F6) else Color.White.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable {
                                selectedHotbarSlot = i
                                inventory.selectedHotbarIndex = i
                                soundEngine.playUiClick()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (stack != null && def != null) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = def.name.take(3),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (stack.count > 1) {
                                    Text(
                                        text = "${stack.count}",
                                        color = Color(0xFFFFD54F),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 8. Inventory & Crafting Dialog Overlay
        AnimatedVisibility(
            visible = isInventoryOpen,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            InventoryScreen(
                inventory = inventory,
                soundEngine = soundEngine,
                onClose = { isInventoryOpen = false }
            )
        }

        // 9. Pause Menu Dialog Overlay
        AnimatedVisibility(
            visible = isPauseOpen,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            PauseMenuDialog(
                player = player,
                soundEngine = soundEngine,
                settings = settings,
                onResume = { isPauseOpen = false },
                onExit = onExitToMenu
            )
        }
    }
}

@Composable
fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    opacity: Float,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        color = if (isActive) Color(0xFF1976D2).copy(alpha = opacity) else Color.Black.copy(alpha = opacity),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White.copy(alpha = 0.4f)),
        modifier = Modifier
            .size(56.dp)
            .clickable(onClick = onClick)
            .testTag("action_button_${label.lowercase()}")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(24.dp))
            Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun PauseMenuDialog(
    player: Player,
    soundEngine: SoundEngine,
    settings: SettingsRepository,
    onResume: () -> Unit,
    onExit: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E222B)),
            modifier = Modifier
                .width(360.dp)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Game Paused", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)

                // Game mode switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Game Mode: ${player.gameMode.name}", color = Color.White.copy(alpha = 0.9f))
                    Button(
                        onClick = {
                            player.gameMode = if (player.gameMode == GameMode.SURVIVAL) GameMode.CREATIVE else GameMode.SURVIVAL
                            soundEngine.playUiClick()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF37474F))
                    ) {
                        Text("Switch")
                    }
                }

                Button(
                    onClick = {
                        soundEngine.playUiClick()
                        onResume()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Text("Resume Game", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        soundEngine.playUiClick()
                        onExit()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                ) {
                    Text("Save & Exit to Menu", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
