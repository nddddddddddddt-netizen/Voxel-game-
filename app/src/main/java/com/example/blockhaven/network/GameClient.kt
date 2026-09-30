package com.example.blockhaven.network

import android.util.Log
import com.example.blockhaven.core.math.Vec3f
import com.example.blockhaven.world.World
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

enum class ConnectionStatus {
    DISCONNECTED, CONNECTING, CONNECTED, RECONNECTING, ERROR
}

class GameClient(private val scope: CoroutineScope) {
    private val TAG = "GameClient"

    private val _status = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val status = _status.asStateFlow()

    private val _statusMessage = MutableStateFlow("")
    val statusMessage = _statusMessage.asStateFlow()

    val remotePlayers = ConcurrentHashMap<Int, RemotePlayer>()
    val chatMessages = mutableListOf<String>()

    var localPlayerId = 0
    var activeRoomCode: String = ""
    var localPlayerName: String = ""
    var activeWorld: World? = null

    private var activeWebSocket: WebSocket? = null
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // Keep alive
        .pingInterval(15, TimeUnit.SECONDS)
        .build()

    fun connect(roomCode: String, playerName: String, serverAddress: String = "127.0.0.1:8080", world: World? = null) {
        disconnect()

        activeRoomCode = roomCode.trim().uppercase()
        localPlayerName = playerName.trim()
        activeWorld = world

        val cleanAddress = serverAddress.trim().removePrefix("http://").removePrefix("https://").removePrefix("ws://").removePrefix("wss://")
        val wsUrl = if (serverAddress.startsWith("wss://")) "wss://$cleanAddress" else "ws://$cleanAddress"

        _status.value = ConnectionStatus.CONNECTING
        _statusMessage.value = "Connecting to $wsUrl..."

        val request = try {
            Request.Builder().url(wsUrl).build()
        } catch (e: Exception) {
            _status.value = ConnectionStatus.ERROR
            _statusMessage.value = "Invalid server URL: ${e.message}"
            return
        }

        activeWebSocket = httpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                scope.launch(Dispatchers.Main) {
                    _status.value = ConnectionStatus.CONNECTED
                    _statusMessage.value = "Connected to $cleanAddress!"
                    chatMessages.add("System: Connected to server.")

                    // Send JOIN packet
                    val joinPacket = JSONObject().apply {
                        put("type", "JOIN")
                        put("roomCode", activeRoomCode.ifEmpty { "DEFAULT" })
                        put("playerName", localPlayerName.ifEmpty { "Player_${System.currentTimeMillis() % 1000}" })
                    }
                    webSocket.send(joinPacket.toString())
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                scope.launch(Dispatchers.Default) {
                    handleServerMessage(text)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                scope.launch(Dispatchers.Main) {
                    _status.value = ConnectionStatus.ERROR
                    val err = t.message ?: "Connection refused"
                    _statusMessage.value = "Server unreachable ($err). Run 'node server.js' to host."
                    chatMessages.add("System: $err")
                    Log.e(TAG, "WebSocket failure: $err", t)
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                scope.launch(Dispatchers.Main) {
                    _status.value = ConnectionStatus.DISCONNECTED
                    _statusMessage.value = "Disconnected ($reason)"
                    remotePlayers.clear()
                }
            }
        })
    }

    private fun handleServerMessage(jsonString: String) {
        try {
            val obj = JSONObject(jsonString)
            when (obj.optString("type")) {
                "JOIN_ACK" -> {
                    localPlayerId = obj.optInt("playerId", 0)
                    val seed = obj.optLong("seed", 123456789L)
                    scope.launch(Dispatchers.Main) {
                        _statusMessage.value = "Joined Room $activeRoomCode (Player #$localPlayerId)"
                        chatMessages.add("System: Joined room $activeRoomCode with seed $seed")
                    }

                    // Apply pre-existing room block deltas
                    val deltas = obj.optJSONArray("deltas")
                    if (deltas != null && activeWorld != null) {
                        applyBlockDeltas(deltas)
                    }
                }

                "PLAYER_JOINED" -> {
                    val pId = obj.optInt("playerId")
                    val pName = obj.optString("name", "Player_$pId")
                    if (pId != localPlayerId) {
                        remotePlayers[pId] = RemotePlayer(pId, pName, Vec3f(8f, 66f, 8f), 0f, 0f)
                        scope.launch(Dispatchers.Main) {
                            chatMessages.add("$pName joined the room.")
                        }
                    }
                }

                "PLAYER_MOVE" -> {
                    val pId = obj.optInt("playerId")
                    if (pId != localPlayerId) {
                        val x = obj.optDouble("x", 8.0).toFloat()
                        val y = obj.optDouble("y", 66.0).toFloat()
                        val z = obj.optDouble("z", 8.0).toFloat()
                        val yaw = obj.optDouble("yaw", 0.0).toFloat()
                        val pitch = obj.optDouble("pitch", 0.0).toFloat()

                        val peer = remotePlayers[pId]
                        if (peer != null) {
                            peer.pos = Vec3f(x, y, z)
                            peer.yaw = yaw
                            peer.pitch = pitch
                        } else {
                            remotePlayers[pId] = RemotePlayer(pId, "Player_$pId", Vec3f(x, y, z), yaw, pitch)
                        }
                    }
                }

                "BLOCK_DELTA" -> {
                    val delta = obj.optJSONObject("delta")
                    if (delta != null && activeWorld != null) {
                        val x = delta.optInt("x")
                        val y = delta.optInt("y")
                        val z = delta.optInt("z")
                        val blockId = delta.optInt("blockId").toShort()
                        scope.launch(Dispatchers.Main) {
                            activeWorld?.setBlock(x, y, z, blockId)
                        }
                    }
                }

                "PLAYER_LEFT" -> {
                    val pId = obj.optInt("playerId")
                    val removed = remotePlayers.remove(pId)
                    if (removed != null) {
                        scope.launch(Dispatchers.Main) {
                            chatMessages.add("${removed.name} left the game.")
                        }
                    }
                }

                "CHAT_MSG" -> {
                    val sender = obj.optString("sender", "Unknown")
                    val msg = obj.optString("message", "")
                    scope.launch(Dispatchers.Main) {
                        chatMessages.add("$sender: $msg")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing server message: ${e.message}")
        }
    }

    private fun applyBlockDeltas(deltas: JSONArray) {
        for (i in 0 until deltas.length()) {
            val d = deltas.optJSONObject(i) ?: continue
            val x = d.optInt("x")
            val y = d.optInt("y")
            val z = d.optInt("z")
            val blockId = d.optInt("blockId").toShort()
            activeWorld?.setBlock(x, y, z, blockId)
        }
    }

    fun disconnect() {
        activeWebSocket?.close(1000, "User disconnected")
        activeWebSocket = null
        remotePlayers.clear()
        _status.value = ConnectionStatus.DISCONNECTED
        _statusMessage.value = "Disconnected"
    }

    fun sendPlayerPosition(pos: Vec3f, yaw: Float, pitch: Float) {
        if (_status.value != ConnectionStatus.CONNECTED) return
        val pkt = JSONObject().apply {
            put("type", "MOVE")
            put("x", pos.x.toDouble())
            put("y", pos.y.toDouble())
            put("z", pos.z.toDouble())
            put("yaw", yaw.toDouble())
            put("pitch", pitch.toDouble())
        }
        activeWebSocket?.send(pkt.toString())
    }

    fun sendBlockEdit(x: Int, y: Int, z: Int, blockId: Short) {
        if (_status.value != ConnectionStatus.CONNECTED) return
        val pkt = JSONObject().apply {
            put("type", "BLOCK_CHANGE")
            put("x", x)
            put("y", y)
            put("z", z)
            put("blockId", blockId.toInt())
        }
        activeWebSocket?.send(pkt.toString())
    }

    fun sendChat(message: String) {
        if (_status.value != ConnectionStatus.CONNECTED) return
        val pkt = JSONObject().apply {
            put("type", "CHAT")
            put("message", message)
        }
        activeWebSocket?.send(pkt.toString())
    }
}
