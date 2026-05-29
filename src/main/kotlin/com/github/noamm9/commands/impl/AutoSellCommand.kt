package com.github.noamm9.commands.impl

import com.github.noamm9.commands.BaseCommand
import com.github.noamm9.commands.CommandNodeBuilder
import com.github.noamm9.config.Config
import com.github.noamm9.features.impl.dungeon.AutoSell
import com.github.noamm9.utils.ChatUtils
import com.mojang.brigadier.arguments.StringArgumentType

object AutoSellCommand: BaseCommand("autosell", mutableSetOf("asell")) {
    override fun CommandNodeBuilder.build() {
        runs { sendUsage() }

        literal("toggle") {
            runs {
                AutoSell.toggle()
                Config.save()
                ChatUtils.modMessage("§aAuto Sell ${if (AutoSell.enabled) "enabled" else "disabled"}.")
            }
        }

        literal("add") {
            runs { ChatUtils.modMessage("§cUsage: /autosell add <item name>") }
            argument("item", StringArgumentType.greedyString()) {
                suggests { AutoSell.defaultItems.asIterable() }
                runs {
                    val item = StringArgumentType.getString(it, "item")
                    if (AutoSell.addItem(item)) {
                        Config.save()
                        ChatUtils.modMessage("§aAdded §f$item §ato auto sell list.")
                    }
                    else ChatUtils.modMessage("§e$item §7is already in the auto sell list.")
                }
            }
        }

        literal("remove") {
            runs { ChatUtils.modMessage("§cUsage: /autosell remove <item name>") }
            argument("item", StringArgumentType.greedyString()) {
                suggests { AutoSell.sellList.value }
                runs {
                    val item = StringArgumentType.getString(it, "item")
                    if (AutoSell.removeItem(item)) {
                        Config.save()
                        ChatUtils.modMessage("§aRemoved §f$item §afrom auto sell list.")
                    }
                    else ChatUtils.modMessage("§c$item is not in the auto sell list.")
                }
            }
        }

        literal("list") {
            runs {
                val items = AutoSell.sellList.value
                if (items.isEmpty()) ChatUtils.modMessage("§7Auto sell list is empty.")
                else ChatUtils.modMessage("§aAuto sell list (${items.size}): §f${items.joinToString(", ")}")
            }
        }

        literal("clear") {
            runs {
                AutoSell.clearItems()
                Config.save()
                ChatUtils.modMessage("§aCleared auto sell list.")
            }
        }

        literal("defaults") {
            runs { AutoSell.addDefaults() }
        }
    }

    private fun sendUsage() {
        ChatUtils.modMessage(
            """
            §6Auto Sell §7(${if (AutoSell.enabled) "enabled" else "disabled"}, ${AutoSell.sellList.value.size} items)
            §e/autosell toggle §7- Toggle the feature
            §e/autosell add <item> §7- Add a match
            §e/autosell remove <item> §7- Remove a match
            §e/autosell list §7- Show current matches
            §e/autosell defaults §7- Add default dungeon drops
            §e/autosell clear §7- Clear all matches
            """.trimIndent()
        )
    }
}
