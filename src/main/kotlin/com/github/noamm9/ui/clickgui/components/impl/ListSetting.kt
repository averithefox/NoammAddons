package com.github.noamm9.ui.clickgui.components.impl

import com.github.noamm9.config.Savable
import com.github.noamm9.ui.clickgui.components.Setting
import com.github.noamm9.ui.clickgui.components.Style
import com.github.noamm9.ui.utils.Animation
import com.github.noamm9.ui.utils.TextInputHandler
import com.github.noamm9.utils.render.Render2D
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.input.MouseButtonInfo
import org.lwjgl.glfw.GLFW
import java.awt.Color
import kotlin.math.max

class ListSetting<T: MutableCollection<String>>(
    name: String,
    defaultValue: T
): Setting<T>(name, defaultValue), Savable {
    private var expanded = false
    private var input = ""
    private var scrollOffset = 0f

    private val openAnim = Animation(250)
    private val hoverAnim = Animation(200)
    private val inputHandler = TextInputHandler(
        textProvider = { input },
        textSetter = { input = it }
    )

    private val inputHeight = 24
    private val entryHeight = 16
    private val listMaxHeight = entryHeight * 6

    override val height: Int
        get() = 20 + (openAnim.value * expandedHeight()).toInt()

    override fun draw(ctx: GuiGraphics, mouseX: Int, mouseY: Int) {
        val isHovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + 20
        openAnim.update(if (expanded) 1f else 0f)
        hoverAnim.update(if (isHovered) 1f else 0f)

        Style.drawBackground(ctx, x, y, width, 20f)
        Style.drawHoverBar(ctx, x, y, 20f, hoverAnim.value)
        Style.drawNudgedText(ctx, name, x + 8f, y + 6f, hoverAnim.value)

        val count = "§7${value.size}"
        Render2D.drawString(ctx, count, x + width - 26f, y + 6f, Color.WHITE)
        Render2D.drawString(ctx, if (expanded) "§7-" else "§7+", x + width - 14f, y + 6f, Color.WHITE)

        if (openAnim.value <= 0.01f) return

        val contentY = y + 20f
        val contentHeight = expandedHeight() * openAnim.value
        ctx.enableScissor(x, y + 20, x + width, y + height)
        Render2D.drawRect(ctx, x + 4f, contentY, width - 8f, contentHeight, Color(5, 5, 5, 150))
        drawInput(ctx, mouseX, mouseY, contentY + 3f)
        drawEntries(ctx, mouseX, mouseY, contentY + inputHeight)
        ctx.disableScissor()
    }

    private fun drawInput(ctx: GuiGraphics, mouseX: Int, mouseY: Int, inputY: Float) {
        val inputX = x + 8f
        val buttonW = 34f
        val inputW = width - 20f - buttonW
        val inputH = 18f

        Render2D.drawRect(ctx, inputX, inputY, inputW, inputH, Color(30, 30, 30, 180))
        Render2D.drawRect(ctx, inputX, inputY + inputH - 1f, inputW * if (inputHandler.listening) 1f else 0f, 1f, Style.accentColor)

        inputHandler.x = inputX
        inputHandler.y = inputY
        inputHandler.width = inputW
        inputHandler.height = inputH

        if (input.isBlank() && ! inputHandler.listening) Render2D.drawString(ctx, "§8Add item...", inputX + 4f, inputY + 5f)
        else inputHandler.draw(ctx, mouseX.toFloat(), mouseY.toFloat())

        val buttonX = inputX + inputW + 4f
        val hovered = mouseX >= buttonX && mouseX <= buttonX + buttonW && mouseY >= inputY && mouseY <= inputY + inputH
        Render2D.drawRect(ctx, buttonX, inputY, buttonW, inputH, if (hovered) Style.accentColor else Color(35, 35, 35, 200))
        Render2D.drawCenteredString(ctx, "Add", buttonX + buttonW / 2f, inputY + 5f, Color.WHITE)
    }

    private fun drawEntries(ctx: GuiGraphics, mouseX: Int, mouseY: Int, listY: Float) {
        val entries = value.toList()
        val viewableHeight = minOf(listMaxHeight, max(entryHeight, entries.size * entryHeight)).toFloat()
        val contentHeight = entries.size * entryHeight
        val maxScroll = max(0f, contentHeight - viewableHeight)
        scrollOffset = scrollOffset.coerceIn(0f, maxScroll)

        ctx.enableScissor(x, listY.toInt(), x + width, (listY + viewableHeight).toInt())

        if (entries.isEmpty()) {
            Render2D.drawString(ctx, "§8No entries", x + 12f, listY + 5f)
        }
        else {
            var entryY = listY - scrollOffset
            entries.forEach { entry ->
                if (entryY + entryHeight > listY && entryY < listY + viewableHeight) {
                    val hovered = mouseX >= x + 4 && mouseX <= x + width - 4 && mouseY >= entryY && mouseY < entryY + entryHeight
                    if (hovered) Render2D.drawRect(ctx, x + 4f, entryY, width - 8f, entryHeight.toFloat(), Color(255, 255, 255, 20))

                    Render2D.drawString(ctx, entry, x + 12f, entryY + 4f, if (hovered) Color.WHITE else Color.GRAY)
                    Render2D.drawString(ctx, "§cx", x + width - 17f, entryY + 4f)
                }
                entryY += entryHeight
            }
        }

        ctx.disableScissor()

        if (maxScroll > 0) {
            val barHeight = (viewableHeight / contentHeight) * viewableHeight
            val barY = listY + ((scrollOffset / maxScroll) * (viewableHeight - barHeight))
            Render2D.drawRect(ctx, x + width - 6f, barY, 2f, barHeight, Style.accentColor)
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + 20) {
            if (button == 0) {
                expanded = ! expanded
                Style.playClickSound(1f)
                return true
            }
        }

        if (! expanded) return false

        val event = MouseButtonEvent(mouseX, mouseY, MouseButtonInfo(button, GLFW.GLFW_PRESS))
        if (inputHandler.mouseClicked(mouseX.toFloat(), mouseY.toFloat(), event)) return true

        if (button == 0 && isAddButton(mouseX, mouseY)) {
            addInput()
            return true
        }

        val hit = hoveredEntry(mouseX, mouseY) ?: return false
        if (button == 0) {
            value.remove(hit)
            Style.playClickSound(0.9f)
        }
        return true
    }

    override fun mouseScrolled(mouseX: Int, mouseY: Int, delta: Double): Boolean {
        if (! expanded) return false
        if (mouseX < x || mouseX > x + width || mouseY < y || mouseY > y + height) return false
        scrollOffset -= (delta * 15).toFloat()
        return true
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (! expanded) return false
        val handled = inputHandler.keyPressed(KeyEvent(keyCode, scanCode, modifiers))
        if (keyCode == GLFW.GLFW_KEY_ENTER) {
            addInput()
            return true
        }
        return handled
    }

    override fun charTyped(codePoint: Char, modifiers: Int): Boolean {
        if (! expanded) return false
        return inputHandler.keyTyped(CharacterEvent(codePoint.code, modifiers))
    }

    override fun mouseReleased(button: Int) {
        inputHandler.mouseReleased()
    }

    override fun write() = JsonArray(value.map(::JsonPrimitive))

    override fun read(element: JsonElement?) {
        val values = when (element) {
            is JsonArray -> element.jsonArray.mapNotNull { it.jsonPrimitive.contentOrNull }
            is JsonPrimitive -> element.contentOrNull?.split(',', '\n')?.map(String::trim)
            else -> null
        } ?: return

        value.clear()
        value.addAll(values.filter(String::isNotBlank))
    }

    private fun addInput() {
        val entry = input.trim().takeIf(String::isNotBlank) ?: return
        value.add(entry.lowercase())
        input = ""
        Style.playClickSound(1.1f)
    }

    private fun hoveredEntry(mouseX: Double, mouseY: Double): String? {
        val listY = y + 20 + inputHeight
        val relativeY = (mouseY - listY) + scrollOffset
        val index = (relativeY / entryHeight).toInt()
        if (mouseX < x || mouseX > x + width || mouseY < listY || mouseY > listY + listMaxHeight) return null
        return value.toList().getOrNull(index)
    }

    private fun isAddButton(mouseX: Double, mouseY: Double): Boolean {
        val inputX = x + 8f
        val buttonW = 34f
        val inputW = width - 20f - buttonW
        val buttonX = inputX + inputW + 4f
        val buttonY = y + 23f
        return mouseX >= buttonX && mouseX <= buttonX + buttonW && mouseY >= buttonY && mouseY <= buttonY + 18f
    }

    private fun expandedHeight(): Int {
        val visibleEntryCount = value.size.coerceIn(1, 6)
        return inputHeight + visibleEntryCount * entryHeight + 4
    }
}

fun ListSetting(name: String, defaultValue: MutableSet<String>) = ListSetting<MutableSet<String>>(name, defaultValue)
