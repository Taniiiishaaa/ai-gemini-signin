package com.tanisha.aigeminisignin

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.components.service
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task

class RunScanAction : AnAction(), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        val editor = e.getData(CommonDataKeys.EDITOR)

        if (project == null || editor == null) {
            Messages.showErrorDialog("No project or editor found.", "Scan Error")
            return
        }

        val document = editor.document
        val file = com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().getFile(document)

        if (file == null) {
            Messages.showErrorDialog("Could not get file content.", "Scan Error")
            return
        }

        val fileContent = document.text
        val fileName = file.name

        val apiKey = service<CredentialsService>().getApiKey()
        if (apiKey.isNullOrBlank()) {
            Messages.showWarningDialog(project, "Please sign in to AI Gemini first (Tools -> AI Gemini Sign-In).", "AI Gemini Scan")
            return
        }

        Messages.showInfoMessage(project, "Scanning file: $fileName. Please wait...", "AI Gemini Scan")

        object : Task.Backgroundable(project, "AI Gemini: Scanning file", false) {
            override fun run(indicator: ProgressIndicator) {
                val client = GeminiClient(apiKey)
                val prompt = """Analyze the following code from file '$fileName' for potential issues, improvements, or explanations:

```
$fileContent
```"""
                val result = client.sendText(prompt)

                ApplicationManager.getApplication().invokeLater {
                    Messages.showInfoMessage(project, result, "AI Gemini Scan Result for $fileName")
                }
            }
        }.queue()
    }
}
