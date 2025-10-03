package com.tanisha.aigeminisignin

import com.intellij.openapi.components.BaseState

/**
 * Represents a single message in the chat history for persistence.
 * We use a separate class here to be explicit about what is being saved.
 */
class HistoryMessage : BaseState() {
    var author: String? by string()
    var text: String? by string()
}

/**
 * The main state object that will be saved by the IDE.
 * It contains a list of all the messages in the chat history.
 */
class GeminiChatHistoryState : BaseState() {
    var messages: MutableList<HistoryMessage> by list()
}
