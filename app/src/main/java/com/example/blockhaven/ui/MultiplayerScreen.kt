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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CellWifi
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import com.example.blockhaven.network.ConnectionStatus
import com.example.blockhaven.network.GameClient

@Composable
fun MultiplayerScreen(
    gameClient: GameClient,
    soundEngine: SoundEngine,
    onBack: () -> Unit
) {
    val status by gameClient.status.collectAsState()
    val statusMsg by gameClient.statusMessage.collectAsState()

    var playerName by remember { mutableStateOf("Crafter_${(100..999).random()}") }
    var roomCodeInput by remember { mutableStateOf("HAVEN7") }
    var serverHostInput by remember { mutableStateOf("127.0.0.1:8080") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
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
                    Text("Multiplayer & Servers", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }

                Surface(
                    color = when (status) {
                        ConnectionStatus.CONNECTED -> Color(0xFF2E7D32)
                        ConnectionStatus.CONNECTING, ConnectionStatus.RECONNECTING -> Color(0xFFF57C00)
                        else -> Color(0xFF455A64)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        status.name,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Column: Connect controls
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E222B)),
                    modifier = Modifier
                        .weight(0.55f)
                        .fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Connection Details", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)

                        OutlinedTextField(
                            value = playerName,
                            onValueChange = { playerName = it },
                            label = { Text("Player Handle") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )

                        OutlinedTextField(
                            value = roomCodeInput,
                            onValueChange = { roomCodeInput = it.uppercase() },
                            label = { Text("Private Room Code (e.g. HAVEN7)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )

                        OutlinedTextField(
                            value = serverHostInput,
                            onValueChange = { serverHostInput = it },
                            label = { Text("Dedicated Server IP:Port") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (status == ConnectionStatus.CONNECTED) {
                                Button(
                                    onClick = {
                                        soundEngine.playBlockBreak()
                                        gameClient.disconnect()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Disconnect")
                                }
                            } else {
                                Button(
                                    onClick = {
                                        soundEngine.playUiClick()
                                        gameClient.connect(roomCodeInput, playerName, serverHostInput)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Connect Room", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (statusMsg.isNotEmpty()) {
                            Text(statusMsg, color = Color(0xFF90CAF9), fontSize = 12.sp)
                        }
                    }
                }

                // Right Column: Connected Peers
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E222B)),
                    modifier = Modifier
                        .weight(0.45f)
                        .fillMaxSize()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Group, contentDescription = "Players", tint = Color(0xFF64B5F6))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Room Players (${if (status == ConnectionStatus.CONNECTED) gameClient.remotePlayers.size + 1 else 0})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (status != ConnectionStatus.CONNECTED) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Connect to a room to view peers", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                item {
                                    Surface(color = Color.White.copy(alpha = 0.08f), shape = RoundedCornerShape(6.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Person, contentDescription = "You", tint = Color(0xFF4CAF50), modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("$playerName (You - Host)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                items(gameClient.remotePlayers.values.toList()) { peer ->
                                    Surface(color = Color.White.copy(alpha = 0.08f), shape = RoundedCornerShape(6.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Person, contentDescription = "Peer", tint = Color(0xFF29B6F6), modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(peer.name, color = Color.White, fontSize = 13.sp)
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
}
