package com.example.blockhaven.network

import com.example.blockhaven.core.math.Vec3f
import com.example.blockhaven.world.World
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

enum class ConnectionStatus {
    DISCONNECTED, CONNECTING, CONNECTED, RECONNECTING, ERROR
}

class GameClient(private val scope: CoroutineScope) {
    private val _status = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val status = _status.asStateFlow()

    private val _statusMessage = MutableStateFlow("")
    val statusMessage = _statusMessage.asStateFlow()

    val remotePlayers = ConcurrentHashMap<Int, RemotePlayer>()
    val chatMessages = mutableListOf<String>()

    var localPlayerId = 1
    private var syncJob: Job? = null

    fun connect(roomCode: String, playerName: String, serverAddress: String = "127.0.0.1:8080") {
        scope.launch(Dispatchers.IO) {
            _status.value = ConnectionStatus.CONNECTING
            _statusMessage.value = "Connecting to room $roomCode at $serverAddress..."

            // Simulated handshake & connection establishment
            delay(800)

            _status.value = ConnectionStatus.CONNECTED
            _statusMessage.value = "Connected to room $roomCode as $playerName!"
            chatMessages.add("System: Joined room $roomCode successfully.")

            // Spawn simulated peer players if in room for testing
            if (roomCode.isNotEmpty()) {
                remotePlayers[2] = RemotePlayer(2, "Alex_Crafter", Vec3f(12f, 65f, 14f), 45f, 0f)
            }
        }
    }

    fun disconnect() {
        syncJob?.cancel()
        remotePlayers.clear()
        _status.value = ConnectionStatus.DISCONNECTED
        _statusMessage.value = "Disconnected"
    }

    fun sendPlayerPosition(pos: Vec3f, yaw: Float, pitch: Float) {
        if (_status.value != ConnectionStatus.CONNECTED) return
        // Transmit packet to server (via WebSocket / UDP)
    }

    fun sendBlockEdit(x: Int, y: Int, z: Int, blockId: Short) {
        if (_status.value != ConnectionStatus.CONNECTED) return
        // Transmit block delta to peers
    }

    fun sendChat(message: String) {
        if (_status.value != ConnectionStatus.CONNECTED) return
        chatMessages.add("You: $message")
    }

    fun startPeerSimulation(world: World) {
        syncJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(100)
                // Smooth peer interpolation
                for ((id, peer) in remotePlayers) {
                    val angle = (System.currentTimeMillis() % 10000) / 10000f * 2f * Math.PI.toFloat()
                    peer.yaw = (peer.yaw + 2f) % 360f
                }
            }
        }
    }
}
