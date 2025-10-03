package com.tanisha.aigeminisignin

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.LabelPosition
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.BoxLayout
import javax.swing.JPanel
import javax.swing.SwingUtilities
import kotlin.concurrent.thread

class GeminiChatPanel(project: Project) : SimpleToolWindowPanel(true, true) {

    private val historyService = project.service<GeminiChatHistoryService>()
    private val chatContainer = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        border = JBUI.Borders.empty(10)
        background = JBUI.CurrentTheme.ToolWindow.background()
    }
    private val scrollPane = JBScrollPane(chatContainer).apply {
        border = JBUI.Borders.empty()
        verticalScrollBar.unitIncrement = 16
    }

    init {
        setContent(createMainPanel())
        // Load the saved chat history as soon as the panel is created
        loadHistory()
    }

    private fun loadHistory() {
        val history = historyService.getHistory()
        if (history.isEmpty()) {
            addMessageToUI("Hello! How can I help you today?", Author.ASSISTANT)
        } else {
            history.forEach { message ->
                val author = if (message.author == "USER") Author.USER else Author.ASSISTANT
                addMessageToUI(message.text ?: "", author)
            }
        }
    }

    private fun createMainPanel(): JPanel {
        return panel {
            row {
                cell(scrollPane).resizableColumn()
            }.resizableRow()
            row {
                cell(createInputArea()).resizableColumn()
            }
        }
    }

    private fun createInputArea(): JPanel {
        return panel {
            row {
                val textArea = textArea()
                    .resizableColumn()
                    .label("Prompt:", LabelPosition.TOP)
                    .component
                button("Send") {
                    val prompt = textArea.text
                    if (prompt.isNotBlank()) {
                        // Add the user's message to the UI and save it to history
                        addUserMessageAndSave(prompt)
                        textArea.text = ""
                        sendToGemini(prompt)
                    }
                }
            }
        }
    }

    private fun sendToGemini(prompt: String) {
        // Add a temporary "Thinking..." bubble that is NOT saved to history
        val thinkingBubble = addMessageToUI("Thinking...", Author.ASSISTANT)

        thread(start = true) {
            try {
                val apiKey = service<CredentialsService>().getApiKey()
                if (apiKey.isNullOrBlank()) {
                    updateBubbleText(thinkingBubble, "API Key not found. Please set it in the settings.")
                    return@thread
                }

                val client = GeminiClient(apiKey)
                val model = client.findWorkingModel()
                if (model == null) {
                    updateBubbleText(thinkingBubble, "Error: No compatible models found for your API key.")
                    return@thread
                }

                val fullResponse = client.chat(model, prompt)
                // Update the bubble with the final response
                updateBubbleText(thinkingBubble, fullResponse)

                // Now, save the final assistant response to the history
                val historyMessage = HistoryMessage().apply {
                    author = "ASSISTANT"
                    text = fullResponse
                }
                historyService.addMessage(historyMessage)

            } catch (e: Exception) {
                val errorMessage = "A critical error occurred: ${e.javaClass.simpleName} - ${e.message}"
                updateBubbleText(thinkingBubble, errorMessage)
            }
        }
    }

    // This function adds a user message to the history and then displays it
    private fun addUserMessageAndSave(text: String) {
        val historyMessage = HistoryMessage().apply {
            this.author = Author.USER.name
            this.text = text
        }
        historyService.addMessage(historyMessage)
        addMessageToUI(text, Author.USER)
    }

    // This function ONLY adds a bubble to the UI
    private fun addMessageToUI(text: String, author: Author): ChatBubble {
        val bubble = ChatBubble(text, author)
        val row = JPanel(BorderLayout()).apply {
            isOpaque = false
            border = JBUI.Borders.empty(5, 0)
            if (author == Author.USER) {
                add(bubble, BorderLayout.LINE_END)
            } else {
                add(bubble, BorderLayout.LINE_START)
            }
        }

        chatContainer.add(row)
        chatContainer.revalidate()
        chatContainer.repaint()
        scrollToBottom()
        return bubble
    }

    private fun updateBubbleText(bubble: ChatBubble, text: String) {
        SwingUtilities.invokeLater {
            bubble.setText(text)
            chatContainer.revalidate()
            chatContainer.repaint()
        }
    }

    private fun scrollToBottom() {
        val scrollBar = scrollPane.verticalScrollBar
        scrollBar.value = scrollBar.maximum
    }
}

// Defines the author for UI purposes
enum class Author { USER, ASSISTANT }
