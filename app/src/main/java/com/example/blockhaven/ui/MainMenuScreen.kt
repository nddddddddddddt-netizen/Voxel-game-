package com.example.blockhaven.ui

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.blockhaven.audio.SoundEngine
import com.example.blockhaven.entity.GameMode
import com.example.blockhaven.storage.WorldEntity
import com.example.blockhaven.storage.WorldSaveRepository
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

    var newWorldName by remember { mutableStateOf("New Haven") }
    var newWorldSeed by remember { mutableStateOf("${Random().nextInt(999999) + 100000}") }
    var newWorldMode by remember { mutableStateOf(GameMode.SURVIVAL) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E293B),
                        Color(0xFF090D16)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (!isWorldSelectOpen && !isCreateWorldOpen) {
            // Main menu buttons
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "BLOCKHAVEN",
                    color = Color.White,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 4.sp
                )

                Text(
                    text = "High-Performance 3D Voxel Sandbox",
                    color = Color(0xFF64B5F6),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(28.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    MenuBigButton(
                        title = "Singleplayer",
                        subtitle = "Worlds & Survival",
                        icon = Icons.Default.PlayArrow,
                        color = Color(0xFF2E7D32),
                        onClick = {
                            soundEngine.playUiClick()
                            isWorldSelectOpen = true
                        }
                    )

                    MenuBigButton(
                        title = "Multiplayer",
                        subtitle = "LAN & Room Code",
                        icon = Icons.Default.Groups,
                        color = Color(0xFF1565C0),
                        onClick = {
                            soundEngine.playUiClick()
                            onOpenMultiplayer()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    MenuBigButton(
                        title = "Mods & Add-ons",
                        subtitle = "WASM & Power Mode",
                        icon = Icons.Default.Extension,
                        color = Color(0xFF6A1B9A),
                        onClick = {
                            soundEngine.playUiClick()
                            onOpenMods()
                        }
                    )

                    MenuBigButton(
                        title = "Settings",
                        subtitle = "Graphics, Audio & Touch",
                        icon = Icons.Default.Settings,
                        color = Color(0xFF37474F),
                        onClick = {
                            soundEngine.playUiClick()
                            onOpenSettings()
                        }
                    )
                }
            }
        } else if (isWorldSelectOpen) {
            // Worlds list screen
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E222B)),
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(320.dp)
                    .padding(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Select World", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    soundEngine.playUiClick()
                                    isCreateWorldOpen = true
                                    isWorldSelectOpen = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Create", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Create World")
                            }

                            Button(
                                onClick = {
                                    soundEngine.playUiClick()
                                    isWorldSelectOpen = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF455A64))
                            ) {
                                Text("Back")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (worlds.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("No worlds found.", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        soundEngine.playUiClick()
                                        isCreateWorldOpen = true
                                        isWorldSelectOpen = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                                ) {
                                    Text("Create Your First World")
                                }
                            }
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(worlds) { worldItem ->
                                Surface(
                                    color = Color.White.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(worldItem.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                            Text(
                                                "${worldItem.gameMode} | Seed: ${worldItem.seed}",
                                                color = Color.White.copy(alpha = 0.6f),
                                                fontSize = 12.sp
                                            )
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = {
                                                    soundEngine.playUiClick()
                                                    val mode = if (worldItem.gameMode == GameMode.CREATIVE.name) GameMode.CREATIVE else GameMode.SURVIVAL
                                                    onStartGame(worldItem.id, worldItem.seed, worldItem.name, mode)
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                                            ) {
                                                Text("Play")
                                            }

                                            IconButton(
                                                onClick = {
                                                    soundEngine.playBlockBreak()
                                                    scope.launch { repository.deleteWorld(worldItem.id) }
                                                }
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF5350))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else if (isCreateWorldOpen) {
            // Create world screen
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E222B)),
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .padding(8.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Create New World", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = newWorldName,
                        onValueChange = { newWorldName = it },
                        label = { Text("World Name") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    OutlinedTextField(
                        value = newWorldSeed,
                        onValueChange = { newWorldSeed = it },
                        label = { Text("World Seed (Number)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    // Game Mode Selection
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Game Mode:", color = Color.White, modifier = Modifier.width(100.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = newWorldMode == GameMode.SURVIVAL,
                                onClick = { newWorldMode = GameMode.SURVIVAL },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF64B5F6))
                            )
                            Text("Survival", color = Color.White)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = newWorldMode == GameMode.CREATIVE,
                                onClick = { newWorldMode = GameMode.CREATIVE },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF64B5F6))
                            )
                            Text("Creative", color = Color.White)
                        }
                    }

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
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF455A64))
                        ) {
                            Text("Cancel")
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Button(
                            onClick = {
                                soundEngine.playUiClick()
                                val seedLong = newWorldSeed.toLongOrNull() ?: 12345678L
                                scope.launch {
                                    val id = repository.createWorld(newWorldName, seedLong, newWorldMode)
                                    onStartGame(id, seedLong, newWorldName, newWorldMode)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                        ) {
                            Text("Create & Play", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MenuBigButton(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        color = color,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White.copy(alpha = 0.25f)),
        modifier = Modifier
            .width(220.dp)
            .height(72.dp)
            .clickable(onClick = onClick)
            .testTag("menu_button_${title.lowercase().replace(" ", "_")}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = title, tint = Color.White, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(subtitle, color = Color.White.copy(alpha = 0.75f), fontSize = 11.sp)
            }
        }
    }
}
