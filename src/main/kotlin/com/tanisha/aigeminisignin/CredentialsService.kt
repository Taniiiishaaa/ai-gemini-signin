package com.tanisha.aigeminisignin


import com.intellij.credentialStore.CredentialAttributes
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.components.Service
import com.intellij.credentialStore.Credentials

@Service(Service.Level.APP)
class CredentialsService {
    private val attrs = CredentialAttributes("com.tanisha.ai.gemini.signin.apiKey")

    fun getApiKey(): String? =
        PasswordSafe.instance.get(attrs)?.getPasswordAsString()?.takeIf { it.isNotBlank() }

    fun setApiKey(apiKey: String) {
        PasswordSafe.instance.set(attrs, Credentials(null, apiKey))
    }

    fun clearApiKey() {
        PasswordSafe.instance.set(attrs, null)
    }
}
