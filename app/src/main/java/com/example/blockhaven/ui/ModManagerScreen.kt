package com.example.blockhaven.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.blockhaven.audio.SoundEngine
import com.example.blockhaven.modding.ModManager
import com.example.blockhaven.modding.ModManifest
import com.example.blockhaven.modding.ModSafetyLevel
import com.example.blockhaven.storage.SettingsRepository

@Composable
fun ModManagerScreen(
    modManager: ModManager,
    settings: SettingsRepository,
    soundEngine: SoundEngine,
    onBack: () -> Unit
) {
    val installedMods by modManager.installedMods.collectAsState()
    var selectedModForDetails by remember { mutableStateOf<ModManifest?>(null) }
    var showPowerModeWarning by remember { mutableStateOf(false) }
    var conflictMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { soundEngine.playUiClick(); onBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mods & Add-ons Manager", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Check conflicts
                    Button(
                        onClick = {
                            soundEngine.playUiClick()
                            val conflicts = modManager.checkConflicts()
                            conflictMessage = if (conflicts.isEmpty()) "All installed mods are compatible! No ID conflicts detected." else conflicts.joinToString("\n")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5))
                    ) {
                        Text("Check Conflicts")
                    }

                    // Power mode developer toggle
                    Surface(
                        color = if (settings.powerModeEnabled) Color(0xFFD84315) else Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Level 2 Power Mode",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Switch(
                                checked = settings.powerModeEnabled,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        showPowerModeWarning = true
                                    } else {
                                        settings.powerModeEnabled = false
                                        modManager.isPowerModeEnabled = false
                                    }
                                    soundEngine.playUiClick()
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Mod list
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(installedMods) { mod ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E222B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(mod.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = if (mod.level == ModSafetyLevel.LEVEL_1_SAFE_SANDBOX) Color(0xFF2E7D32) else Color(0xFFC2185B),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            if (mod.level == ModSafetyLevel.LEVEL_1_SAFE_SANDBOX) "L1: Safe WASM" else "L2: Power Mode",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("v${mod.version} (${mod.language})", color = Color(0xFF90CAF9), fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(mod.description, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IconButton(onClick = {
                                    soundEngine.playUiClick()
                                    selectedModForDetails = mod
                                }) {
                                    Icon(Icons.Default.Info, contentDescription = "Details", tint = Color(0xFF64B5F6))
                                }

                                Switch(
                                    checked = mod.isEnabled,
                                    onCheckedChange = {
                                        soundEngine.playUiClick()
                                        modManager.toggleMod(mod.modId)
                                    }
                                )

                                IconButton(onClick = {
                                    soundEngine.playBlockBreak()
                                    modManager.deleteMod(mod.modId)
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF5350))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Details Modal
        selectedModForDetails?.let { mod ->
            AlertDialog(
                onDismissRequest = { selectedModForDetails = null },
                title = { Text(mod.name, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Author: ${mod.author}")
                        Text("Version: ${mod.version}")
                        Text("Language runtime: ${mod.language}")
                        Text("Safety sandbox: ${mod.level.name}")
                        Text("Permissions: ${mod.permissions.joinToString(", ")}")
                        Text("SHA-256: ${mod.sha256Fingerprint.take(24)}...")
                    }
                },
                confirmButton = {
                    Button(onClick = { selectedModForDetails = null }) {
                        Text("OK")
                    }
                }
            )
        }

        // Power Mode Safety Confirmation Dialog
        if (showPowerModeWarning) {
            AlertDialog(
                onDismissRequest = { showPowerModeWarning = false },
                icon = { Icon(Icons.Default.Warning, contentDescription = "Warning", tint = Color(0xFFFF9800)) },
                title = { Text("Enable Level 2 Power Mode?", fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "Level 2 Power Mode allows loading native compiled ARM64 (.so) libraries and Android DEX/JAR code. " +
                        "Only enable this mode for trusted mods created by verified developers. " +
                        "This mode is strictly isolated to the Blockhaven game environment."
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            settings.powerModeEnabled = true
                            modManager.isPowerModeEnabled = true
                            showPowerModeWarning = false
                            soundEngine.playUiClick()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD84315))
                    ) {
                        Text("Enable Power Mode")
                    }
                },
                dismissButton = {
                    Button(onClick = { showPowerModeWarning = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Conflict check message dialog
        conflictMessage?.let { msg ->
            AlertDialog(
                onDismissRequest = { conflictMessage = null },
                icon = { Icon(Icons.Default.CheckCircle, contentDescription = "Check", tint = Color(0xFF4CAF50)) },
                title = { Text("Mod Compatibility Status") },
                text = { Text(msg) },
                confirmButton = {
                    Button(onClick = { conflictMessage = null }) {
                        Text("OK")
                    }
                }
            )
        }
    }
}
