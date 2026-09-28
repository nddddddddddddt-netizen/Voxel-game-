package com.example.blockhaven.modding

import android.content.Context
import com.example.blockhaven.core.math.Vec3i
import com.example.blockhaven.entity.Player
import com.example.blockhaven.world.World
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ModManager(private val context: Context) {
    val scriptEngine = ModScriptEngine()

    private val _installedMods = MutableStateFlow<List<ModManifest>>(emptyList())
    val installedMods = _installedMods.asStateFlow()

    var isPowerModeEnabled = false // Settings > Developer > Power Mode toggle

    init {
        loadDefaultMods()
    }

    private fun loadDefaultMods() {
        val list = mutableListOf<ModManifest>()

        // 1. Crystal Caverns Mod
        val crystalMod = ModManifest(
            modId = "crystal_caverns",
            name = "Crystal Caverns",
            version = "1.2.0",
            author = "Blockhaven Studios",
            description = "Unearths glowing crystal geode formations deep underground with mystic resonance wands.",
            level = ModSafetyLevel.LEVEL_1_SAFE_SANDBOX,
            language = "TypeScript / WASM",
            permissions = listOf("world.blocks", "custom.items", "chat.commands")
        )
        list.add(crystalMod)

        // 2. Volcanic Forge Mod
        val volcanicMod = ModManifest(
            modId = "volcanic_forge",
            name = "Volcanic Forge",
            version = "1.0.4",
            author = "Pyromancer Team",
            description = "Adds volcanic obsidian forging, magma core blocks, and blazing flame blades.",
            level = ModSafetyLevel.LEVEL_1_SAFE_SANDBOX,
            language = "Rust / WASM",
            permissions = listOf("world.blocks", "custom.items")
        )
        list.add(volcanicMod)

        // 3. Sky & Atmosphere Mod
        val skyMod = ModManifest(
            modId = "sky_aurora",
            name = "Celestial Aurora",
            version = "2.1.0",
            author = "Skyward Devs",
            description = "Adds shimmering polar auroras, dynamic atmospheric mist, and weather control commands.",
            level = ModSafetyLevel.LEVEL_1_SAFE_SANDBOX,
            language = "QuickJS",
            permissions = listOf("chat.commands")
        )
        list.add(skyMod)

        _installedMods.value = list

        // Register default behavior listeners
        registerBuiltinListeners()
    }

    private fun registerBuiltinListeners() {
        // Crystal Caverns listener
        scriptEngine.registerListener("crystal_caverns", object : ModEventListener {
            override fun onCommand(command: String, args: List<String>, player: Player, world: World): String? {
                if (command == "crystal") {
                    return "Crystal resonance active! Depth level: ${player.pos.y.toInt()}"
                }
                return null
            }
        })

        // Sky & Aurora listener
        scriptEngine.registerListener("sky_aurora", object : ModEventListener {
            override fun onCommand(command: String, args: List<String>, player: Player, world: World): String? {
                if (command == "aurora") {
                    world.timeOfDay = 18000f // Set to midnight for aurora viewing
                    return "Night sky cleared! Celestial aurora dancing overhead."
                }
                return null
            }
        })
    }

    fun toggleMod(modId: String) {
        val updated = _installedMods.value.map {
            if (it.modId == modId) it.copy(isEnabled = !it.isEnabled) else it
        }
        _installedMods.value = updated

        val mod = updated.find { it.modId == modId }
        if (mod != null && !mod.isEnabled) {
            scriptEngine.unregisterListener(modId)
        } else if (mod != null && mod.isEnabled) {
            registerBuiltinListeners()
        }
    }

    fun deleteMod(modId: String) {
        scriptEngine.unregisterListener(modId)
        _installedMods.value = _installedMods.value.filter { it.modId != modId }
    }

    fun installMod(manifest: ModManifest) {
        val current = _installedMods.value.filter { it.modId != manifest.modId }.toMutableList()
        current.add(manifest)
        _installedMods.value = current
    }

    fun checkConflicts(): List<String> {
        val conflicts = mutableListOf<String>()
        val ids = mutableSetOf<String>()
        for (m in _installedMods.value) {
            if (!ids.add(m.modId)) {
                conflicts.add("Duplicate mod ID found: ${m.modId}")
            }
        }
        return conflicts
    }
}
