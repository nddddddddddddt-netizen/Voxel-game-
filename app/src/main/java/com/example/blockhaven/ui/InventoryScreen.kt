package com.example.blockhaven.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.blockhaven.audio.SoundEngine
import com.example.blockhaven.gameplay.CraftingManager
import com.example.blockhaven.gameplay.Inventory
import com.example.blockhaven.gameplay.ItemRegistry
import com.example.blockhaven.gameplay.ItemStack
import com.example.blockhaven.gameplay.SmeltingManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun InventoryScreen(
    inventory: Inventory,
    soundEngine: SoundEngine,
    onClose: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    // Crafting 3x3 state
    val craftGrid = remember { Array<ItemStack?>(9) { null } }
    var craftResult by remember { mutableStateOf<ItemStack?>(null) }

    // Furnace state
    var furnaceInput by remember { mutableStateOf<ItemStack?>(null) }
    var furnaceFuel by remember { mutableStateOf<ItemStack?>(null) }
    var furnaceOutput by remember { mutableStateOf<ItemStack?>(null) }
    var furnaceProgress by remember { mutableFloatStateOf(0f) }

    // Helper to evaluate recipe
    fun updateCraftResult() {
        craftResult = CraftingManager.findMatchingResult(craftGrid, 3, 3)
    }

    // Furnace cooking loop
    LaunchedEffect(furnaceInput, furnaceFuel) {
        while (isActive) {
            val input = furnaceInput
            val fuel = furnaceFuel
            if (input != null && fuel != null && fuel.count > 0 && input.count > 0) {
                val possibleOutput = SmeltingManager.getResult(input.itemId)
                if (possibleOutput != null) {
                    furnaceProgress += 0.2f
                    if (furnaceProgress >= 1.0f) {
                        furnaceProgress = 0f
                        input.count -= 1
                        if (input.count <= 0) furnaceInput = null

                        fuel.count -= 1
                        if (fuel.count <= 0) furnaceFuel = null

                        if (furnaceOutput == null) {
                            furnaceOutput = possibleOutput
                        } else if (furnaceOutput?.itemId == possibleOutput.itemId) {
                            furnaceOutput?.count = (furnaceOutput?.count ?: 0) + possibleOutput.count
                        }
                        soundEngine.playBlockBreak()
                    }
                }
            } else {
                furnaceProgress = 0f
            }
            delay(500)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E222B)),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .height(340.dp)
                .padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Header with Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = Color.White,
                        modifier = Modifier.width(320.dp)
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0; soundEngine.playUiClick() },
                            text = { Text("Crafting", fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1; soundEngine.playUiClick() },
                            text = { Text("Furnace", fontWeight = FontWeight.Bold) }
                        )
                    }

                    IconButton(onClick = { soundEngine.playUiClick(); onClose() }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (selectedTab == 0) {
                    // Crafting Tab
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Left: 3x3 Crafting Grid -> Result
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.weight(0.42f)
                        ) {
                            Text("3×3 Crafting Grid", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // 3x3 grid
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    for (r in 0..2) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            for (c in 0..2) {
                                                val idx = r * 3 + c
                                                val stack = craftGrid[idx]
                                                CraftSlot(stack = stack, onClick = {
                                                    // Pull selected hotbar item into this slot
                                                    val held = inventory.getSelectedItem()
                                                    if (held != null) {
                                                        if (stack == null) {
                                                            craftGrid[idx] = ItemStack(held.itemId, 1)
                                                            inventory.consumeSelectedItem(1)
                                                        } else if (stack.itemId == held.itemId) {
                                                            stack.count += 1
                                                            inventory.consumeSelectedItem(1)
                                                        }
                                                        soundEngine.playBlockPlace()
                                                        updateCraftResult()
                                                    } else if (stack != null) {
                                                        // Return to inventory
                                                        inventory.addItem(stack.copy())
                                                        craftGrid[idx] = null
                                                        soundEngine.playUiClick()
                                                        updateCraftResult()
                                                    }
                                                })
                                            }
                                        }
                                    }
                                }

                                Text("➔", color = Color.White, fontSize = 20.sp)

                                // Craft Output Slot
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(Color(0xFF2E7D32).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                        .border(2.dp, Color(0xFF81C784), RoundedCornerShape(8.dp))
                                        .clickable {
                                            craftResult?.let { result ->
                                                inventory.addItem(result.copy())
                                                soundEngine.playBlockBreak()
                                                // Consume ingredients
                                                for (i in 0..8) {
                                                    val ing = craftGrid[i]
                                                    if (ing != null) {
                                                        ing.count -= 1
                                                        if (ing.count <= 0) craftGrid[i] = null
                                                    }
                                                }
                                                updateCraftResult()
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    craftResult?.let { res ->
                                        val def = ItemRegistry.get(res.itemId)
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(def.name.take(3), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            if (res.count > 1) {
                                                Text("${res.count}", color = Color(0xFFFFD54F), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Right: Inventory storage (9 hotbar + 27 main)
                        Column(modifier = Modifier.weight(0.58f)) {
                            Text("Inventory Storage", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))

                            // 27 Main slots (3 rows of 9)
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                for (row in 0..2) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                        for (col in 0..8) {
                                            val slotIdx = 9 + row * 9 + col
                                            val stack = inventory.getSlot(slotIdx)
                                            SlotView(stack = stack, onClick = {
                                                // Swap with hotbar selected
                                                val hotbarStack = inventory.getSelectedItem()
                                                inventory.setSlot(slotIdx, hotbarStack)
                                                inventory.setSlot(inventory.selectedHotbarIndex, stack)
                                                soundEngine.playUiClick()
                                            })
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Hotbar (Quick Access)", color = Color(0xFF64B5F6), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))

                            // 9 Hotbar slots
                            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                for (i in 0..8) {
                                    val stack = inventory.getSlot(i)
                                    val isSelected = (i == inventory.selectedHotbarIndex)
                                    SlotView(stack = stack, isHighlighted = isSelected, onClick = {
                                        inventory.selectedHotbarIndex = i
                                        soundEngine.playUiClick()
                                    })
                                }
                            }
                        }
                    }
                } else {
                    // Furnace Smelting Tab
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Input (Ore / Food / Sand)", color = Color.White, fontSize = 12.sp)
                            CraftSlot(stack = furnaceInput, onClick = {
                                val held = inventory.getSelectedItem()
                                if (held != null) {
                                    furnaceInput = held.copy()
                                    inventory.consumeSelectedItem(held.count)
                                    soundEngine.playBlockPlace()
                                } else if (furnaceInput != null) {
                                    inventory.addItem(furnaceInput!!)
                                    furnaceInput = null
                                    soundEngine.playUiClick()
                                }
                            })

                            Icon(Icons.Default.LocalFireDepartment, contentDescription = "Fire", tint = Color(0xFFFF7043))

                            Text("Fuel (Coal / Wood)", color = Color.White, fontSize = 12.sp)
                            CraftSlot(stack = furnaceFuel, onClick = {
                                val held = inventory.getSelectedItem()
                                if (held != null) {
                                    furnaceFuel = held.copy()
                                    inventory.consumeSelectedItem(held.count)
                                    soundEngine.playBlockPlace()
                                } else if (furnaceFuel != null) {
                                    inventory.addItem(furnaceFuel!!)
                                    furnaceFuel = null
                                    soundEngine.playUiClick()
                                }
                            })
                        }

                        Spacer(modifier = Modifier.width(32.dp))

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Progress", color = Color.White, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { furnaceProgress },
                                modifier = Modifier.width(80.dp),
                                color = Color(0xFFFF9800)
                            )
                        }

                        Spacer(modifier = Modifier.width(32.dp))

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Smelted Result", color = Color.White, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            CraftSlot(stack = furnaceOutput, onClick = {
                                furnaceOutput?.let {
                                    inventory.addItem(it)
                                    furnaceOutput = null
                                    soundEngine.playBlockBreak()
                                }
                            })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CraftSlot(
    stack: ItemStack?,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (stack != null) {
            val def = ItemRegistry.get(stack.itemId)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(def.name.take(3), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                if (stack.count > 1) {
                    Text("${stack.count}", color = Color(0xFFFFD54F), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SlotView(
    stack: ItemStack?,
    isHighlighted: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(
                if (isHighlighted) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.12f),
                RoundedCornerShape(4.dp)
            )
            .border(
                1.dp,
                if (isHighlighted) Color(0xFF64B5F6) else Color.White.copy(alpha = 0.2f),
                RoundedCornerShape(4.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (stack != null) {
            val def = ItemRegistry.get(stack.itemId)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(def.name.take(3), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                if (stack.count > 1) {
                    Text("${stack.count}", color = Color(0xFFFFD54F), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
