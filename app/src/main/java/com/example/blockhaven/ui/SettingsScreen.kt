package com.example.blockhaven.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.example.blockhaven.storage.SettingsRepository

@Composable
fun SettingsScreen(
    settings: SettingsRepository,
    soundEngine: SoundEngine,
    onBack: () -> Unit
) {
    var renderDist by remember { mutableFloatStateOf(settings.renderDistance.toFloat()) }
    var fov by remember { mutableFloatStateOf(settings.fov) }
    var lookSens by remember { mutableFloatStateOf(settings.lookSensitivity) }
    var invertY by remember { mutableStateOf(settings.invertY) }
    var btnOpacity by remember { mutableFloatStateOf(settings.buttonOpacity) }
    var haptics by remember { mutableStateOf(settings.hapticFeedback) }

    var masterVol by remember { mutableFloatStateOf(settings.masterVolume) }
    var sfxVol by remember { mutableFloatStateOf(settings.sfxVolume) }
    var musicVol by remember { mutableFloatStateOf(settings.musicVolume) }
    var thermalThrottle by remember { mutableStateOf(settings.thermalThrottling) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { soundEngine.playUiClick(); onBack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Settings & Customization", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Video & Display Card
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E222B)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Graphics & Performance", color = Color(0xFF64B5F6), fontWeight = FontWeight.Bold, fontSize = 15.sp)

                        Text("Render Distance: ${renderDist.toInt()} Chunks", color = Color.White, fontSize = 13.sp)
                        Slider(
                            value = renderDist,
                            onValueChange = {
                                renderDist = it
                                settings.renderDistance = it.toInt()
                            },
                            valueRange = 2f..8f,
                            steps = 5
                        )

                        Text("Field of View (FOV): ${fov.toInt()}°", color = Color.White, fontSize = 13.sp)
                        Slider(
                            value = fov,
                            onValueChange = {
                                fov = it
                                settings.fov = it
                            },
                            valueRange = 60f..100f
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Dynamic Thermal Throttling", color = Color.White, fontSize = 13.sp)
                                Text("Lowers quality if phone warms up", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                            }
                            Switch(
                                checked = thermalThrottle,
                                onCheckedChange = {
                                    thermalThrottle = it
                                    settings.thermalThrottling = it
                                    soundEngine.playUiClick()
                                }
                            )
                        }
                    }
                }

                // Controls & Audio Card
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E222B)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Touch Controls & Audio", color = Color(0xFF64B5F6), fontWeight = FontWeight.Bold, fontSize = 15.sp)

                        Text("Look Sensitivity: ${String.format("%.1f", lookSens)}x", color = Color.White, fontSize = 13.sp)
                        Slider(
                            value = lookSens,
                            onValueChange = {
                                lookSens = it
                                settings.lookSensitivity = it
                            },
                            valueRange = 0.5f..2.5f
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Invert Look Pitch (Y Axis)", color = Color.White, fontSize = 13.sp)
                            Switch(
                                checked = invertY,
                                onCheckedChange = {
                                    invertY = it
                                    settings.invertY = it
                                    soundEngine.playUiClick()
                                }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Haptic Feedback on Touch", color = Color.White, fontSize = 13.sp)
                            Switch(
                                checked = haptics,
                                onCheckedChange = {
                                    haptics = it
                                    settings.hapticFeedback = it
                                    soundEngine.playUiClick()
                                }
                            )
                        }

                        Text("Master Volume: ${(masterVol * 100).toInt()}%", color = Color.White, fontSize = 13.sp)
                        Slider(
                            value = masterVol,
                            onValueChange = {
                                masterVol = it
                                settings.masterVolume = it
                                soundEngine.masterVolume = it
                            },
                            valueRange = 0f..1f
                        )

                        Text("Music Volume: ${(musicVol * 100).toInt()}%", color = Color.White, fontSize = 13.sp)
                        Slider(
                            value = musicVol,
                            onValueChange = {
                                musicVol = it
                                settings.musicVolume = it
                                soundEngine.musicVolume = it
                            },
                            valueRange = 0f..1f
                        )
                    }
                }
            }
        }
    }
}
