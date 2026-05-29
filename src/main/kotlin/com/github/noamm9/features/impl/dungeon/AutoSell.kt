package com.github.noamm9.features.impl.dungeon

import com.github.noamm9.config.Config
import com.github.noamm9.event.impl.TickEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.impl.ButtonSetting
import com.github.noamm9.ui.clickgui.components.impl.DropdownSetting
import com.github.noamm9.ui.clickgui.components.impl.ListSetting
import com.github.noamm9.ui.clickgui.components.impl.SliderSetting
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.ChatUtils.unformattedText
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.ClickType

object AutoSell : Feature(
    name = "Auto Sell",
    description = "Automatically sells configured items in trades and cookie menus. (/autosell)"
) {
    val sellList by ListSetting("Sell List", mutableSetOf())
        .withDescription("Item names to sell. Matching is case-insensitive and partial.")
    private val delay by SliderSetting("Delay", 100L, 75L, 300L, 5L, "ms")
        .withDescription("The delay between each sell action.")
    private val clickType by DropdownSetting("Click Type", 0, listOf("Shift", "Middle", "Left"))
        .withDescription("The click type to use when selling items.")
    private val addDefaults by ButtonSetting("Add Defaults") { addDefaults() }

    private val validMenuTitles = setOf("trades", "booster cookie", "farm merchant", "ophelia")
    private var lastClick = 0L

    override fun init() {
        register<TickEvent.End> {
            if (sellList.value.isEmpty()) return@register

            val now = System.currentTimeMillis()
            if (now - lastClick < delay.value) return@register

            val screen = mc.screen as? AbstractContainerScreen<*> ?: return@register
            if (screen.title.unformattedText.lowercase() !in validMenuTitles) return@register

            val matchingSlot = screen.menu.slots.firstOrNull { slot ->
                !slot.item.isEmpty && slot.container is Inventory && matchesSellList(slot.item.hoverName.unformattedText)
            } ?: return@register

            clickSlot(matchingSlot.index)
            lastClick = now
        }
    }

    fun addItem(item: String): Boolean {
        val normalized = item.normalizeItemName() ?: return false
        return sellList.value.add(normalized)
    }

    fun removeItem(item: String): Boolean {
        val normalized = item.normalizeItemName() ?: return false
        return sellList.value.remove(normalized)
    }

    fun clearItems() {
        sellList.value.clear()
    }

    fun addDefaults() {
        sellList.value.addAll(defaultItems)
        ChatUtils.modMessage("§aAdded default items to auto sell list.")
        Config.save()
    }

    private fun matchesSellList(displayName: String): Boolean {
        val cleanName = displayName.lowercase()
        return sellList.value.any(cleanName::contains)
    }

    private fun clickSlot(slotIndex: Int) {
        val player = mc.player ?: return
        val gameMode = mc.gameMode ?: return
        val (button, action) = when (clickType.value) {
            0 -> 0 to ClickType.QUICK_MOVE
            1 -> 2 to ClickType.CLONE
            else -> 0 to ClickType.PICKUP
        }

        gameMode.handleInventoryMouseClick(player.containerMenu.containerId, slotIndex, button, action, player)
    }

    private fun String.normalizeItemName(): String? {
        return trim().lowercase().takeIf { it.isNotBlank() }
    }

    val defaultItems = arrayOf(
        "enchanted ice", "rotten", "skeleton master", "skeleton grunt", "cutlass",
        "skeleton lord", "skeleton soldier", "zombie soldier", "zombie knight", "zombie commander", "zombie lord",
        "skeletor", "super heavy", "heavy", "sniper helmet", "dreadlord", "earth shard", "zombie commander whip",
        "machine gun", "sniper bow", "soulstealer bow", "silent death", "training weight",
        "beating heart", "premium flesh", "mimic fragment", "enchanted rotten flesh", "sign",
        "enchanted bone", "defuse kit", "optical lens", "tripwire hook", "button", "carpet", "lever", "diamond atom",
        "healing viii splash potion", "healing 8 splash potion", "candycomb"
    )
}
