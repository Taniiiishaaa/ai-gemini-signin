package com.tanisha.aigeminisignin

import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import java.awt.*
import javax.swing.JLabel
import javax.swing.JPanel

class ChatBubble(initialText: String, private val author: Author) : JPanel(BorderLayout()) {

    private val label: JLabel

    init {
        isOpaque = false

        label = JLabel().apply {
            font = JBUI.Fonts.label(13f)
            border = JBUI.Borders.empty(8, 10)
        }

        if (author == Author.USER) {
            background = JBColor(0xDDEBFF, 0x455A7C) // Light blue
            label.foreground = JBColor(0x000000, 0xFFFFFF) // Black on light, white on dark
        } else {
            background = JBColor(0xF1F3F4, 0x3C3F41) // Light gray
            label.foreground = JBColor(0x000000, 0xFFFFFF) // Black on light, white on dark
        }

        setText(initialText)
        add(label, BorderLayout.CENTER)
    }

    fun appendText(chunk: String) {
        val currentText = label.text.substringAfter("<body>").substringBefore("</body>")
        val newText = currentText + chunk.replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>")
        setText(newText, false)
    }

    fun setText(text: String, isNewText: Boolean = true) {
        val safeText = if (isNewText) text.replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>") else text
        label.text = "<html><body style='width: 300px;'>$safeText</body></html>"
    }

    override fun paintComponent(g: Graphics) {
        if (width <= 0 || height <= 0) {
            super.paintComponent(g)
            return
        }
        val g2 = g.create() as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2.color = background
        val arc = minOf(width, height, 20)
        g2.fillRoundRect(0, 0, width, height, arc, arc)
        g2.dispose()
        super.paintComponent(g)
    }
}