package com.example.blockhaven.modding

import android.util.Log
import com.example.blockhaven.core.math.Vec3f
import com.example.blockhaven.core.math.Vec3i
import com.example.blockhaven.entity.GameMode
import com.example.blockhaven.entity.Player
import com.example.blockhaven.gameplay.ItemRegistry
import com.example.blockhaven.gameplay.ItemStack
import com.example.blockhaven.world.BlockType
import com.example.blockhaven.world.World
import org.json.JSONObject
import java.security.MessageDigest

interface ModEventListener {
    fun onBlockBreak(pos: Vec3i, blockId: Short, player: Player): Boolean = false
    fun onBlockPlace(pos: Vec3i, blockId: Short, player: Player): Boolean = false
    fun onCommand(command: String, args: List<String>, player: Player, world: World): String? = null
    fun onTick(world: World, dt: Float) {}
}

class ModScriptEngine {
    private val TAG = "ModScriptEngine"
    private val activeListeners = mutableMapOf<String, ModEventListener>()
    private val loadedScripts = mutableMapOf<String, String>()

    fun registerScript(modId: String, scriptSource: String) {
        loadedScripts[modId] = scriptSource
        // Parse and create script evaluator
        val listener = createScriptListener(modId, scriptSource)
        activeListeners[modId] = listener
    }

    fun unregisterScript(modId: String) {
        loadedScripts.remove(modId)
        activeListeners.remove(modId)
    }

    fun registerListener(modId: String, listener: ModEventListener) {
        activeListeners[modId] = listener
    }

    fun unregisterListener(modId: String) {
        activeListeners.remove(modId)
    }

    fun dispatchBlockBreak(modId: String, pos: Vec3i, blockId: Short, player: Player): Boolean {
        return try {
            activeListeners[modId]?.onBlockBreak(pos, blockId, player) ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Sandboxed error in mod $modId onBlockBreak: ${e.message}")
            false
        }
    }

    fun dispatchBlockPlace(modId: String, pos: Vec3i, blockId: Short, player: Player): Boolean {
        return try {
            activeListeners[modId]?.onBlockPlace(pos, blockId, player) ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Sandboxed error in mod $modId onBlockPlace: ${e.message}")
            false
        }
    }

    fun dispatchCommand(rawCommand: String, player: Player, world: World): String? {
        val parts = rawCommand.trim().removePrefix("/").split("\\s+".toRegex())
        if (parts.isEmpty()) return null
        val cmd = parts[0].lowercase()
        val args = parts.drop(1)

        // 1. Built-in Core Engine Commands
        when (cmd) {
            "time" -> {
                if (args.isEmpty()) return "World time: ${world.timeOfDay.toInt()}"
                val targetTime = when (args[0].lowercase()) {
                    "day", "dawn" -> 1000f
                    "noon" -> 6000f
                    "sunset" -> 12000f
                    "night", "midnight" -> 18000f
                    else -> args[0].toFloatOrNull() ?: return "Usage: /time <day|noon|sunset|night|number>"
                }
                world.timeOfDay = targetTime % world.dayDurationTicks
                return "Set time to ${targetTime.toInt()}"
            }

            "weather" -> {
                if (args.isEmpty()) return "Weather is currently: ${if (world.isRaining) "Raining" else "Clear"}"
                return when (args[0].lowercase()) {
                    "rain", "storm" -> {
                        world.isRaining = true
                        world.rainStrength = 1.0f
                        "Weather changed to rain."
                    }
                    "clear", "sun" -> {
                        world.isRaining = false
                        world.rainStrength = 0.0f
                        "Weather changed to clear sky."
                    }
                    else -> "Usage: /weather <rain|clear>"
                }
            }

            "gamemode", "gm" -> {
                if (args.isEmpty()) return "Current gamemode: ${player.gameMode}"
                return when (args[0].lowercase()) {
                    "c", "creative", "1" -> {
                        player.gameMode = GameMode.CREATIVE
                        player.isFlying = true
                        "Set game mode to Creative Mode."
                    }
                    "s", "survival", "0" -> {
                        player.gameMode = GameMode.SURVIVAL
                        player.isFlying = false
                        "Set game mode to Survival Mode."
                    }
                    else -> "Usage: /gamemode <survival|creative>"
                }
            }

            "heal" -> {
                player.health = player.maxHealth
                player.hunger = 20f
                player.air = 10f
                return "Player health, hunger, and air fully restored."
            }

            "tp", "teleport" -> {
                if (args.size < 3) return "Usage: /tp <x> <y> <z>"
                val tx = args[0].toFloatOrNull() ?: return "Invalid X"
                val ty = args[1].toFloatOrNull() ?: return "Invalid Y"
                val tz = args[2].toFloatOrNull() ?: return "Invalid Z"
                player.pos = Vec3f(tx, ty, tz)
                player.vel = Vec3f.ZERO
                player.ensureSafeGrounded(world)
                return "Teleported to ($tx, $ty, $tz)"
            }

            "fly" -> {
                player.isFlying = !player.isFlying
                return "Flying mode: ${player.isFlying}"
            }
        }

        // 2. Dispatch to Active Mod Listeners
        for ((modId, listener) in activeListeners) {
            try {
                val resp = listener.onCommand(cmd, args, player, world)
                if (resp != null) return "[$modId] $resp"
            } catch (e: Exception) {
                Log.e(TAG, "Sandboxed error in mod $modId command: ${e.message}")
            }
        }
        return null
    }

    fun dispatchTick(world: World, dt: Float) {
        for ((modId, listener) in activeListeners) {
            try {
                listener.onTick(world, dt)
            } catch (e: Exception) {
                Log.e(TAG, "Sandboxed error in mod $modId onTick: ${e.message}")
            }
        }
    }

    private fun createScriptListener(modId: String, script: String): ModEventListener {
        return object : ModEventListener {
            override fun onCommand(command: String, args: List<String>, player: Player, world: World): String? {
                if (script.contains("cmd:$command") || script.contains("\"$command\"")) {
                    return "Script executed command '$command' with args ${args.joinToString(", ")}"
                }
                return null
            }
        }
    }

    companion object {
        fun computeSha256(content: ByteArray): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(content)
            return hash.joinToString("") { "%02x".format(it) }
        }
    }
}
