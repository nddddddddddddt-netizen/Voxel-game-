package com.example.blockhaven.modding

enum class ModSafetyLevel {
    LEVEL_1_SAFE_SANDBOX,
    LEVEL_2_POWER_MODE
}

data class ModPermission(
    val id: String,
    val title: String,
    val description: String,
    val isDangerous: Boolean = false
) {
    companion object {
        val PERM_WORLD_BLOCKS = ModPermission("world.blocks", "Modify World Blocks", "Allows mod to place and break voxels")
        val PERM_CUSTOM_ITEMS = ModPermission("custom.items", "Register Custom Items", "Allows registering new block and item types")
        val PERM_SPAWN_ENTITIES = ModPermission("entities.spawn", "Spawn Custom Mobs", "Allows spawning creatures in the world")
        val PERM_CHAT_COMMANDS = ModPermission("chat.commands", "Handle Commands", "Allows listening and responding to /commands")
        val PERM_FILESYSTEM = ModPermission("filesystem.storage", "Isolated Mod Storage", "Allows saving mod configuration files")
        val PERM_NATIVE_EXECUTION = ModPermission("native.code", "Native Execution (Power Mode)", "Allows executing compiled arm64/DEX code", isDangerous = true)
    }
}

data class ModManifest(
    val modId: String,
    val name: String,
    val version: String,
    val author: String,
    val description: String,
    val level: ModSafetyLevel = ModSafetyLevel.LEVEL_1_SAFE_SANDBOX,
    val language: String = "JavaScript", // QuickJS, Lua, Rust/Wasm, Kotlin
    val permissions: List<String> = listOf("world.blocks", "custom.items"),
    val sha256Fingerprint: String = "a1b2c3d4e5f67890abcdef1234567890abcdef1234567890abcdef1234567890",
    var isEnabled: Boolean = true
)
