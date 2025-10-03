package com.tanisha.aigeminisignin

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.components.service

class SignInAction : AnAction(), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        if (SignInDialog(e.project).showAndGet()) {
            val apiKey = service<CredentialsService>().getApiKey()
            if (apiKey != null) {
                val client = GeminiClient(apiKey)
                val result = client.testConnection()
                Messages.showInfoMessage(e.project, result, "AI Gemini Connection Test")
            } else {
                Messages.showWarningDialog(e.project, "API key not found.", "AI Gemini")
            }
        }
    }
}
