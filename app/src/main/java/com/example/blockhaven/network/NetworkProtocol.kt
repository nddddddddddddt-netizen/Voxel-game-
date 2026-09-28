package com.example.blockhaven.network

import com.example.blockhaven.core.math.Vec3f

sealed class GamePacket {
    data class JoinRequest(val roomCode: String, val playerName: String, val clientVersion: String = "1.0.0") : GamePacket()
    data class JoinResponse(val success: Boolean, val assignedId: Int, val spawnPos: Vec3f, val seed: Long, val message: String) : GamePacket()
    data class PlayerUpdate(val playerId: Int, val pos: Vec3f, val yaw: Float, val pitch: Float) : GamePacket()
    data class BlockDelta(val playerId: Int, val x: Int, val y: Int, val z: Int, val blockId: Short) : GamePacket()
    data class ChatMessage(val sender: String, val message: String) : GamePacket()
    data class PlayerLeft(val playerId: Int) : GamePacket()
}

data class RemotePlayer(
    val id: Int,
    val name: String,
    var pos: Vec3f,
    var yaw: Float,
    var pitch: Float
)
