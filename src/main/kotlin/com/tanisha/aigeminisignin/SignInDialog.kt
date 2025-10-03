package com.tanisha.aigeminisignin

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPasswordField
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.*

class SignInDialog(private val projectRef: Project?) : DialogWrapper(projectRef) {

    private val credentialsService = service<CredentialsService>()

    private lateinit var apiKeyField: JBPasswordField
    private lateinit var rememberKey: JBCheckBox
    private lateinit var showKeyToggle: JToggleButton
    private lateinit var bannerLabel: JBLabel
    private lateinit var testBtn: JButton

    init {
        title = "Gemini — Sign In"
        isModal = true
        setOKButtonText("Save & Close")
        init()
    }

    override fun createCenterPanel(): JComponent {
        val root = JPanel(BorderLayout())
        root.border = JBUI.Borders.empty(8, 12)

        bannerLabel = JBLabel("").apply {
            isVisible = false
            foreground = JBColor(0xD32020, 0xFF6B6B)
            border = JBUI.Borders.emptyBottom(8)
        }
        root.add(bannerLabel, BorderLayout.NORTH)

        val panel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints().apply {
            gridx = 0
            gridy = 0
            weightx = 0.0
            weighty = 0.0
            insets = JBUI.insets(4)
            anchor = GridBagConstraints.WEST
            fill = GridBagConstraints.NONE
        }

        panel.add(JBLabel("API key:"), gbc)

        gbc.gridx = 1
        gbc.weightx = 1.0
        gbc.fill = GridBagConstraints.HORIZONTAL
        apiKeyField = JBPasswordField().apply {
            echoChar = '•'
            toolTipText = "Paste your Gemini API key"
            text = loadSavedKey().orEmpty()
        }
        panel.add(apiKeyField, gbc)

        gbc.gridx = 2
        gbc.weightx = 0.0
        gbc.fill = GridBagConstraints.NONE
        showKeyToggle = JToggleButton("Show").apply {
            addActionListener {
                apiKeyField.echoChar = if (isSelected) 0.toChar() else '•'
                text = if (isSelected) "Hide" else "Show"
            }
        }
        panel.add(showKeyToggle, gbc)

        gbc.gridy++
        gbc.gridx = 1
        gbc.gridwidth = 2
        rememberKey = JBCheckBox("Remember this key securely", loadSavedKey() != null)
        panel.add(rememberKey, gbc)

        gbc.gridy++
        val link = JButton("Get an API key").apply {
            isBorderPainted = false
            isContentAreaFilled = false
            addActionListener { com.intellij.ide.BrowserUtil.browse("https://ai.google.dev/gemini-api/docs/api-key") }
        }
        panel.add(link, gbc)
//temp
        gbc.gridy++
        testBtn = JButton("Test connection").apply {
            addActionListener { doTestConnection() }
        }
        panel.add(testBtn, gbc)

        root.add(panel, BorderLayout.CENTER)
        return root
    }

    override fun doValidate(): ValidationInfo? {
        val key = String(apiKeyField.password).trim()
        if (key.isEmpty()) return ValidationInfo("API key cannot be empty.", apiKeyField)
        if (key.length < 20) return ValidationInfo("This doesn’t look like a valid key.", apiKeyField)
        return null
    }

    override fun doOKAction() {
        val v = doValidate()
        if (v != null) {
            setErrorBanner(v.message)
            return
        }
        clearBanner()
        val key = String(apiKeyField.password).trim()
        if (rememberKey.isSelected) {
            credentialsService.setApiKey(key)
        } else {
            credentialsService.clearApiKey()
        }
        super.doOKAction()
    }

    private fun setErrorBanner(msg: String) {
        bannerLabel.text = "⚠ $msg"
        bannerLabel.foreground = JBColor(0xD32020, 0xFF6B6B)
        bannerLabel.isVisible = true
    }

    private fun setSuccessBanner() {
        bannerLabel.text = "✓ Connection successful!"
        bannerLabel.foreground = JBColor(0x2E7D32, 0x81C784)
        bannerLabel.isVisible = true
    }

    private fun clearBanner() {
        bannerLabel.text = ""
        bannerLabel.isVisible = false
    }

    private fun loadSavedKey(): String? {
        return credentialsService.getApiKey()
    }

    private fun doTestConnection() {
        val v = doValidate()
        if (v != null) {
            setErrorBanner(v.message)
            return
        }
        clearBanner()
        val key = String(apiKeyField.password).trim()

        ProgressManager.getInstance().run(object : Task.Backgroundable(projectRef, "Testing Gemini connection", false) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                // Use the central, smart client for the connection test
                val client = GeminiClient(key)
                val result = client.testConnection()

                ApplicationManager.getApplication().invokeLater {
                    if (result.startsWith("Success:")) {
                        setSuccessBanner()
                    } else {
                        // The result from the client is the detailed error message
                        setErrorBanner(result)
                    }
                }
            }
        })
    }
}
