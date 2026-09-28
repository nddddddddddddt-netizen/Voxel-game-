package com.example

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.lifecycleScope
import com.example.blockhaven.audio.SoundEngine
import com.example.blockhaven.core.math.Vec3f
import com.example.blockhaven.entity.GameMode
import com.example.blockhaven.entity.Player
import com.example.blockhaven.gameplay.Inventory
import com.example.blockhaven.gameplay.ItemRegistry
import com.example.blockhaven.gameplay.ItemStack
import com.example.blockhaven.modding.ModManager
import com.example.blockhaven.network.GameClient
import com.example.blockhaven.storage.BlockhavenDatabase
import com.example.blockhaven.storage.SettingsRepository
import com.example.blockhaven.storage.WorldSaveRepository
import com.example.blockhaven.ui.GameScreen
import com.example.blockhaven.ui.MainMenuScreen
import com.example.blockhaven.ui.ModManagerScreen
import com.example.blockhaven.ui.MultiplayerScreen
import com.example.blockhaven.ui.SettingsScreen
import com.example.blockhaven.world.BlockType
import com.example.blockhaven.world.World
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

enum class AppScreen {
    MAIN_MENU,
    IN_GAME,
    MULTIPLAYER,
    MODS,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    private lateinit var database: BlockhavenDatabase
    private lateinit var repository: WorldSaveRepository
    private lateinit var settings: SettingsRepository
    private lateinit var soundEngine: SoundEngine
    private lateinit var modManager: ModManager
    private lateinit var gameClient: GameClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideSystemBars()

        database = BlockhavenDatabase.getDatabase(this)
        repository = WorldSaveRepository(database.worldDao())
        settings = SettingsRepository(this)
        soundEngine = SoundEngine(lifecycleScope).apply {
            masterVolume = settings.masterVolume
            sfxVolume = settings.sfxVolume
            musicVolume = settings.musicVolume
            start()
        }
        modManager = ModManager(this)
        gameClient = GameClient(lifecycleScope)

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0F172A)
                ) {
                    BlockhavenAppRoot(
                        repository = repository,
                        settings = settings,
                        soundEngine = soundEngine,
                        modManager = modManager,
                        gameClient = gameClient
                    )
                }
            }
        }
    }

    private fun hideSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
            )
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
    }

    override fun onDestroy() {
        super.onDestroy()
        soundEngine.stop()
    }
}

@Composable
fun BlockhavenAppRoot(
    repository: WorldSaveRepository,
    settings: SettingsRepository,
    soundEngine: SoundEngine,
    modManager: ModManager,
    gameClient: GameClient
) {
    val scope = rememberCoroutineScope()
    var currentScreen by remember { mutableStateOf(AppScreen.MAIN_MENU) }

    // Active world and player session
    var activeWorldId by remember { mutableStateOf(1L) }
    var activeWorld by remember { mutableStateOf<World?>(null) }
    var activePlayer by remember { mutableStateOf<Player?>(null) }
    var activeInventory by remember { mutableStateOf<Inventory?>(null) }

    when (currentScreen) {
        AppScreen.MAIN_MENU -> {
            MainMenuScreen(
                repository = repository,
                soundEngine = soundEngine,
                onStartGame = { worldId, seed, name, mode ->
                    val world = World(seed, scope).apply {
                        renderDistance = settings.renderDistance
                    }
                    val safeSpawn = world.findSafeSpawn(8.5f, 8.5f)
                    val player = Player(safeSpawn).apply {
                        gameMode = mode
                    }
                    val inventory = Inventory()

                    // Starting kit
                    inventory.setSlot(0, ItemStack(ItemRegistry.WOOD_PICKAXE, 1))
                    inventory.setSlot(1, ItemStack(BlockType.WOOD_PLANKS.toInt(), 16))
                    inventory.setSlot(2, ItemStack(BlockType.TORCH.toInt(), 8))
                    inventory.setSlot(3, ItemStack(BlockType.DIRT.toInt(), 32))

                    activeWorldId = worldId
                    activeWorld = world
                    activePlayer = player
                    activeInventory = inventory

                    // Load saved state asynchronously if exists
                    scope.launch {
                        repository.loadWorld(worldId, world, player)
                    }

                    currentScreen = AppScreen.IN_GAME
                },
                onOpenMultiplayer = { currentScreen = AppScreen.MULTIPLAYER },
                onOpenMods = { currentScreen = AppScreen.MODS },
                onOpenSettings = { currentScreen = AppScreen.SETTINGS }
            )
        }

        AppScreen.IN_GAME -> {
            val w = activeWorld
            val p = activePlayer
            val inv = activeInventory

            if (w != null && p != null && inv != null) {
                GameScreen(
                    world = w,
                    player = p,
                    inventory = inv,
                    soundEngine = soundEngine,
                    settings = settings,
                    onExitToMenu = {
                        scope.launch {
                            repository.saveWorldState(activeWorldId, w, p)
                            currentScreen = AppScreen.MAIN_MENU
                        }
                    }
                )
            } else {
                currentScreen = AppScreen.MAIN_MENU
            }
        }

        AppScreen.MULTIPLAYER -> {
            MultiplayerScreen(
                gameClient = gameClient,
                soundEngine = soundEngine,
                onBack = { currentScreen = AppScreen.MAIN_MENU }
            )
        }

        AppScreen.MODS -> {
            ModManagerScreen(
                modManager = modManager,
                settings = settings,
                soundEngine = soundEngine,
                onBack = { currentScreen = AppScreen.MAIN_MENU }
            )
        }

        AppScreen.SETTINGS -> {
            SettingsScreen(
                settings = settings,
                soundEngine = soundEngine,
                onBack = { currentScreen = AppScreen.MAIN_MENU }
            )
        }
    }
}
