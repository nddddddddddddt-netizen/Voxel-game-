package com.example.blockhaven.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.blockhaven.audio.SoundEngine
import com.example.blockhaven.entity.GameMode
import com.example.blockhaven.storage.WorldEntity
import com.example.blockhaven.storage.WorldSaveRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Random

@Composable
fun MainMenuScreen(
    repository: WorldSaveRepository,
    soundEngine: SoundEngine,
    onStartGame: (worldId: Long, seed: Long, name: String, mode: GameMode) -> Unit,
    onOpenMultiplayer: () -> Unit,
    onOpenMods: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val worlds by repository.allWorlds.collectAsState(initial = emptyList())

    var isWorldSelectOpen by remember { mutableStateOf(false) }
    var isCreateWorldOpen by remember { mutableStateOf(false) }
    var worldToDelete by remember { mutableStateOf<WorldEntity?>(null) }

    var newWorldName by remember { mutableStateOf("My Haven") }
    var newWorldSeed by remember { mutableStateOf("${Random().nextInt(900000) + 100000}") }
    var newWorldMode by remember { mutableStateOf(GameMode.SURVIVAL) }
    var isAudioMuted by remember { mutableStateOf(soundEngine.masterVolume <= 0.05f) }

    // Subtle animated glow pulse for the title logo
    var pulsePhase by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            pulsePhase = (pulsePhase + 0.04f) % (2f * Math.PI.toFloat())
            delay(32)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. High-Fidelity Cinematic Voxel Landscape Background
        Image(
            painter = painterResource(id = R.drawable.img_menu_hero_landscape),
            contentDescription = "Blockhaven Cinematic Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 2. Realistic Vignette & Atmospheric Gradient Scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xA60A0E18),
                            Color(0x8C111827),
                            Color(0xD9070A10)
                        )
                    )
                )
        )

        // 3. Top Status & Quick Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Engine Version & Badge
            Surface(
                color = Color(0x66000000),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFF4CAF50), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "BLOCKHAVEN 3D • v1.5 Voxel Engine",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Audio & Quick Settings Controls
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    color = Color(0x66000000),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                    modifier = Modifier.size(42.dp)
                ) {
                    IconButton(
                        onClick = {
                            isAudioMuted = !isAudioMuted
                            soundEngine.masterVolume = if (isAudioMuted) 0f else 0.8f
                            if (!isAudioMuted) soundEngine.playUiClick()
                        }
                    ) {
                        Icon(
                            imageVector = if (isAudioMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                            contentDescription = "Toggle Audio",
                            tint = if (isAudioMuted) Color(0xFFEF5350) else Color(0xFF64B5F6),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // 4. Center Content (Main Menu or Modals)
        if (!isWorldSelectOpen && !isCreateWorldOpen) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Majestic Title
                Text(
                    text = "BLOCKHAVEN",
                    color = Color.White,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 5.sp,
                    modifier = Modifier.shadow(16.dp, ambientColor = Color(0xFFFF9800), spotColor = Color(0xFFFFB300))
                )

                Text(
                    text = "Infinite Procedural Voxel Sandbox • Native Android",
                    color = Color(0xFF90CAF9),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(36.dp))

                // Menu Buttons 2x2 Grid
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    MenuBigCard(
                        title = "Singleplayer",
                        arabicTitle = "اللعب الفردي والبقاء",
                        subtitle = "Survival & Creative Worlds",
                        icon = Icons.Default.PlayArrow,
                        accentColor = Color(0xFF2E7D32),
                        badge = "${worlds.size} Worlds",
                        onClick = {
                            soundEngine.playUiClick()
                            isWorldSelectOpen = true
                        }
                    )

                    MenuBigCard(
                        title = "Multiplayer",
                        arabicTitle = "اللعب الجماعي",
                        subtitle = "LAN & Direct Room Code",
                        icon = Icons.Default.Groups,
                        accentColor = Color(0xFF1565C0),
                        badge = "LAN Online",
                        onClick = {
                            soundEngine.playUiClick()
                            onOpenMultiplayer()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    MenuBigCard(
                        title = "Mods & Add-ons",
                        arabicTitle = "المودات والسكربتات",
                        subtitle = "Custom Blocks & Behavior",
                        icon = Icons.Default.Extension,
                        accentColor = Color(0xFF7B1FA2),
                        badge = "WASM Modding",
                        onClick = {
                            soundEngine.playUiClick()
                            onOpenMods()
                        }
                    )

                    MenuBigCard(
                        title = "Settings",
                        arabicTitle = "إعدادات الرسوم والصوت",
                        subtitle = "Graphics, FOV & Controls",
                        icon = Icons.Default.Settings,
                        accentColor = Color(0xFF37474F),
                        badge = "OpenGL ES 3.0",
                        onClick = {
                            soundEngine.playUiClick()
                            onOpenSettings()
                        }
                    )
                }
            }
        }

        // 5. Select World Dialog
        if (isWorldSelectOpen) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xF0151C28)),
                border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.2f)),
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.85f)
                    .height(340.dp)
                    .shadow(24.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Select World",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "اختر عالماً للمتابعة أو ابدأ مغامرة جديدة",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    soundEngine.playUiClick()
                                    isCreateWorldOpen = true
                                    isWorldSelectOpen = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Create", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("New World", fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    soundEngine.playUiClick()
                                    isWorldSelectOpen = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF455A64)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Back")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (worlds.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "No worlds found yet!",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "أنشئ عالمك الأول وابدأ رحلة البقاء والاستكشاف",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        soundEngine.playUiClick()
                                        isCreateWorldOpen = true
                                        isWorldSelectOpen = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Create Your First World", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(worlds) { worldItem ->
                                Surface(
                                    color = Color.White.copy(alpha = 0.07f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            // Voxel Icon Preview
                                            Surface(
                                                color = if (worldItem.gameMode == GameMode.CREATIVE.name) Color(0xFFFFB300) else Color(0xFF43A047),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.size(40.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.Public,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(14.dp))

                                            Column {
                                                Text(
                                                    worldItem.name,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 16.sp
                                                )
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Surface(
                                                        color = if (worldItem.gameMode == GameMode.CREATIVE.name) Color(0x33FFB300) else Color(0x3343A047),
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text(
                                                            text = worldItem.gameMode,
                                                            color = if (worldItem.gameMode == GameMode.CREATIVE.name) Color(0xFFFFD54F) else Color(0xFF81C784),
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        "Seed: ${worldItem.seed}",
                                                        color = Color.White.copy(alpha = 0.55f),
                                                        fontSize = 12.sp
                                                    )
                                                }
                                            }
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Button(
                                                onClick = {
                                                    soundEngine.playUiClick()
                                                    val mode = if (worldItem.gameMode == GameMode.CREATIVE.name) GameMode.CREATIVE else GameMode.SURVIVAL
                                                    onStartGame(worldItem.id, worldItem.seed, worldItem.name, mode)
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("Play", fontWeight = FontWeight.Bold)
                                            }

                                            IconButton(
                                                onClick = {
                                                    soundEngine.playBlockHit()
                                                    worldToDelete = worldItem
                                                }
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Delete World",
                                                    tint = Color(0xFFEF5350)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. Delete Confirmation Dialog
        worldToDelete?.let { targetWorld ->
            AlertDialog(
                onDismissRequest = { worldToDelete = null },
                containerColor = Color(0xFF1E2430),
                title = {
                    Text("Delete World?", color = Color.White, fontWeight = FontWeight.Bold)
                },
                text = {
                    Text(
                        "Are you sure you want to delete '${targetWorld.name}'? This action cannot be undone and will permanently remove world blocks and inventory.\n\nهل أنت متأكد من حذف هذا العالم نهائياً؟",
                        color = Color.White.copy(alpha = 0.8f)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            soundEngine.playBlockBreak()
                            scope.launch { repository.deleteWorld(targetWorld.id) }
                            worldToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                    ) {
                        Text("Delete Permanently")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { worldToDelete = null }) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                    }
                }
            )
        }

        // 7. Create New World Dialog
        if (isCreateWorldOpen) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xF0151C28)),
                border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.2f)),
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.75f)
                    .padding(8.dp)
                    .shadow(24.dp)
            ) {
                Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Create New World • إنشاء عالم جديد",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = newWorldName,
                        onValueChange = { newWorldName = it },
                        label = { Text("World Name / اسم العالم") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF64B5F6),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                        )
                    )

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newWorldSeed,
                            onValueChange = { newWorldSeed = it },
                            label = { Text("World Seed (Number)") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF64B5F6),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                            )
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        IconButton(
                            onClick = {
                                soundEngine.playUiClick()
                                newWorldSeed = "${Random().nextInt(900000) + 100000}"
                            },
                            modifier = Modifier
                                .size(50.dp)
                                .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                        ) {
                            Icon(Icons.Default.Casino, contentDescription = "Randomize Seed", tint = Color(0xFFFFB300))
                        }
                    }

                    // Game Mode Selection Cards
                    Text("Game Mode / نمط اللعب:", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        GameModeOptionCard(
                            title = "Survival",
                            desc = "Health, Hunger & Mining",
                            isSelected = newWorldMode == GameMode.SURVIVAL,
                            color = Color(0xFF2E7D32),
                            onClick = {
                                soundEngine.playUiClick()
                                newWorldMode = GameMode.SURVIVAL
                            }
                        )

                        GameModeOptionCard(
                            title = "Creative",
                            desc = "Unlimited Blocks & Flight",
                            isSelected = newWorldMode == GameMode.CREATIVE,
                            color = Color(0xFFFF8F00),
                            onClick = {
                                soundEngine.playUiClick()
                                newWorldMode = GameMode.CREATIVE
                            }
                        )
                    }

                    // Dialog Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                soundEngine.playUiClick()
                                isCreateWorldOpen = false
                                isWorldSelectOpen = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF455A64)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Cancel")
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Button(
                            onClick = {
                                soundEngine.playLevelUp()
                                val seedLong = newWorldSeed.toLongOrNull() ?: 12345678L
                                scope.launch {
                                    val id = repository.createWorld(newWorldName, seedLong, newWorldMode)
                                    onStartGame(id, seedLong, newWorldName, newWorldMode)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Create & Play", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuBigCard(
    title: String,
    arabicTitle: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    badge: String,
    onClick: () -> Unit
) {
    Surface(
        color = Color(0xE6161E2C),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.18f)),
        shadowElevation = 8.dp,
        modifier = Modifier
            .width(235.dp)
            .height(84.dp)
            .clickable(onClick = onClick)
            .testTag("menu_button_${title.lowercase().replace(" ", "_")}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = accentColor.copy(alpha = 0.25f),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = title, tint = accentColor, modifier = Modifier.size(26.dp))
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Surface(
                        color = Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = badge,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
                Text(arabicTitle, color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun GameModeOptionCard(
    title: String,
    desc: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) color.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f),
        border = BorderStroke(1.5.dp, if (isSelected) color else Color.White.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(2.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
            Text(title, color = if (isSelected) Color.White else Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(desc, color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp)
        }
    }
}
