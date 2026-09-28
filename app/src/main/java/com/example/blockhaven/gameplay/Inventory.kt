package com.example.blockhaven.gameplay

class Inventory {
    companion object {
        const val HOTBAR_SIZE = 9
        const val MAIN_SIZE = 27
        const val TOTAL_SIZE = HOTBAR_SIZE + MAIN_SIZE
    }

    val slots = Array<ItemStack?>(TOTAL_SIZE) { null }
    var selectedHotbarIndex = 0

    fun getSelectedItem(): ItemStack? {
        return slots[selectedHotbarIndex]
    }

    fun getSlot(index: Int): ItemStack? {
        if (index !in 0 until TOTAL_SIZE) return null
        return slots[index]
    }

    fun setSlot(index: Int, stack: ItemStack?) {
        if (index in 0 until TOTAL_SIZE) {
            slots[index] = stack
        }
    }

    fun addItem(stack: ItemStack): Boolean {
        val def = ItemRegistry.get(stack.itemId)
        val maxStack = def.maxStackSize

        // 1. Try to merge into existing stacks
        for (i in 0 until TOTAL_SIZE) {
            val s = slots[i]
            if (s != null && s.itemId == stack.itemId && s.count < maxStack) {
                val canAdd = maxStack - s.count
                val adding = stack.count.coerceAtMost(canAdd)
                s.count += adding
                stack.count -= adding
                if (stack.count <= 0) return true
            }
        }

        // 2. Put in first empty slot
        for (i in 0 until TOTAL_SIZE) {
            if (slots[i] == null) {
                slots[i] = stack.copy()
                stack.count = 0
                return true
            }
        }
        return false
    }

    fun consumeSelectedItem(count: Int = 1): Boolean {
        val s = getSelectedItem() ?: return false
        s.count -= count
        if (s.count <= 0) {
            slots[selectedHotbarIndex] = null
        }
        return true
    }
}
