package com.tanisha.aigeminisignin

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

/**
 * This service is responsible for storing and retrieving the chat history.
 * The @State annotation tells the IDE to save the state of this service
 * in a file named `geminiChatHistory.xml` inside the project's .idea directory.
 */
@Service(Service.Level.PROJECT)
@State(
    name = "com.tanisha.aigeminisignin.GeminiChatHistoryState",
    storages = [Storage("geminiChatHistory.xml")]
)
class GeminiChatHistoryService : PersistentStateComponent<GeminiChatHistoryState> {

    private var internalState = GeminiChatHistoryState()

    /**
     * This method is called by the IDE to get the current state of the service
     * so it can be saved to the file.
     */
    override fun getState(): GeminiChatHistoryState {
        return internalState
    }

    /**
     * This method is called by the IDE when the project is opened. It provides
     * the state that was loaded from the file.
     */
    override fun loadState(state: GeminiChatHistoryState) {
        internalState = state
    }

    /**
     * Adds a new message to the history.
     */
    fun addMessage(message: HistoryMessage) {
        internalState.messages.add(message)
    }

    /**
     * Returns the current chat history.
     */
    fun getHistory(): List<HistoryMessage> {
        return internalState.messages.toList()
    }

    /**
     * Clears the entire chat history.
     */
    fun clearHistory() {
        internalState.messages.clear()
    }
}