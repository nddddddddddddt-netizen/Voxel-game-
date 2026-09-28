package com.example.blockhaven.modding

import android.util.Log
import com.example.blockhaven.core.math.Vec3i
import com.example.blockhaven.entity.Player
import com.example.blockhaven.world.World

interface ModEventListener {
    fun onBlockBreak(pos: Vec3i, blockId: Short, player: Player): Boolean = false
    fun onBlockPlace(pos: Vec3i, blockId: Short, player: Player): Boolean = false
    fun onCommand(command: String, args: List<String>, player: Player, world: World): String? = null
    fun onTick(world: World, dt: Float) {}
}

class ModScriptEngine {
    private val TAG = "ModScriptEngine"
    private val listeners = mutableMapOf<String, ModEventListener>()

    fun registerListener(modId: String, listener: ModEventListener) {
        listeners[modId] = listener
    }

    fun unregisterListener(modId: String) {
        listeners.remove(modId)
    }

    fun dispatchBlockBreak(modId: String, pos: Vec3i, blockId: Short, player: Player): Boolean {
        return try {
            listeners[modId]?.onBlockBreak(pos, blockId, player) ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Error in mod $modId onBlockBreak: ${e.message}")
            false
        }
    }

    fun dispatchBlockPlace(modId: String, pos: Vec3i, blockId: Short, player: Player): Boolean {
        return try {
            listeners[modId]?.onBlockPlace(pos, blockId, player) ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Error in mod $modId onBlockPlace: ${e.message}")
            false
        }
    }

    fun dispatchCommand(command: String, args: List<String>, player: Player, world: World): String? {
        for ((modId, listener) in listeners) {
            try {
                val resp = listener.onCommand(command, args, player, world)
                if (resp != null) return "[$modId] $resp"
            } catch (e: Exception) {
                Log.e(TAG, "Error in mod $modId onCommand: ${e.message}")
            }
        }
        return null
    }

    fun dispatchTick(world: World, dt: Float) {
        for ((modId, listener) in listeners) {
            try {
                listener.onTick(world, dt)
            } catch (e: Exception) {
                Log.e(TAG, "Error in mod $modId onTick: ${e.message}")
            }
        }
    }
}
