package com.example.blockhaven.modding

import android.content.Context
import android.util.Log
import com.example.blockhaven.core.math.Vec3i
import com.example.blockhaven.entity.Player
import com.example.blockhaven.world.World
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

class ModManager(private val context: Context) {
    private val TAG = "ModManager"
    val scriptEngine = ModScriptEngine()

    private val _installedMods = MutableStateFlow<List<ModManifest>>(emptyList())
    val installedMods = _installedMods.asStateFlow()

    var isPowerModeEnabled = false // Settings > Developer > Power Mode toggle

    init {
        loadDefaultMods()
    }

    private fun loadDefaultMods() {
        val list = mutableListOf<ModManifest>()

        // 1. Load from assets/mods/sample_mod.json if present
        try {
            val assetManager = context.assets
            val modJson = assetManager.open("mods/sample_mod.json").bufferedReader().use { it.readText() }
            val hash = ModScriptEngine.computeSha256(modJson.toByteArray())
            val obj = JSONObject(modJson)

            val manifest = ModManifest(
                modId = obj.optString("id", "com.blockhaven.ruby_mod"),
                name = obj.optString("name", "Ruby & Garnet Mod"),
                version = obj.optString("version", "1.0.0"),
                author = obj.optString("author", "Blockhaven Community"),
                description = obj.optString("description", "Adds Ruby Gemstones, custom craft recipes, and mining gear."),
                level = ModSafetyLevel.LEVEL_1_SAFE_SANDBOX,
                language = "JSON / WASM",
                permissions = listOf("world.blocks", "custom.items", "recipes.register"),
                sha256Fingerprint = hash
            )
            list.add(manifest)

            // Register Ruby mod listener
            scriptEngine.registerListener(manifest.modId, object : ModEventListener {
                override fun onCommand(command: String, args: List<String>, player: Player, world: World): String? {
                    if (command == "ruby") {
                        return "Ruby Mod: Garnet crystal veins active at Y=12-32!"
                    }
                    return null
                }
            })
        } catch (e: Exception) {
            Log.w(TAG, "No sample_mod asset loaded: ${e.message}")
        }

        // 2. Crystal Caverns Mod
        val crystalSource = "/* Crystal Caverns Mod Script */ function onCommand(cmd) { return cmd === 'crystal'; }"
        val crystalHash = ModScriptEngine.computeSha256(crystalSource.toByteArray())
        val crystalMod = ModManifest(
            modId = "crystal_caverns",
            name = "Crystal Caverns",
            version = "1.2.0",
            author = "Blockhaven Studios",
            description = "Unearths glowing crystal geode formations deep underground with mystic resonance wands.",
            level = ModSafetyLevel.LEVEL_1_SAFE_SANDBOX,
            language = "TypeScript / WASM",
            permissions = listOf("world.blocks", "custom.items", "chat.commands"),
            sha256Fingerprint = crystalHash
        )
        list.add(crystalMod)

        // 3. Volcanic Forge Mod
        val volcanicSource = "/* Volcanic Forge Mod Script */ function onForge() { return true; }"
        val volcanicHash = ModScriptEngine.computeSha256(volcanicSource.toByteArray())
        val volcanicMod = ModManifest(
            modId = "volcanic_forge",
            name = "Volcanic Forge",
            version = "1.0.4",
            author = "Pyromancer Team",
            description = "Adds volcanic obsidian forging, magma core blocks, and blazing flame blades.",
            level = ModSafetyLevel.LEVEL_1_SAFE_SANDBOX,
            language = "Rust / WASM",
            permissions = listOf("world.blocks", "custom.items"),
            sha256Fingerprint = volcanicHash
        )
        list.add(volcanicMod)

        // 4. Sky & Atmosphere Mod
        val skySource = "/* Celestial Aurora Script */ function onCommand(c) { if (c === 'aurora') setTime(18000); }"
        val skyHash = ModScriptEngine.computeSha256(skySource.toByteArray())
        val skyMod = ModManifest(
            modId = "sky_aurora",
            name = "Celestial Aurora",
            version = "2.1.0",
            author = "Skyward Devs",
            description = "Adds shimmering polar auroras, dynamic atmospheric mist, and weather control commands.",
            level = ModSafetyLevel.LEVEL_1_SAFE_SANDBOX,
            language = "QuickJS",
            permissions = listOf("chat.commands"),
            sha256Fingerprint = skyHash
        )
        list.add(skyMod)

        _installedMods.value = list

        // Register active listeners
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
                    world.isRaining = false
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
