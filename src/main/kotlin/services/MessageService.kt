package services

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.decodeFromString
import mu.KotlinLogging
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class MessageService {
    companion object {
        private val MESSAGE_FILE: File = File("config/messages.yaml")
        private val LOGGER = KotlinLogging.logger { }
    }

    private lateinit var loadedMessages: MutableMap<String, String>

    init {
        if (MESSAGE_FILE.exists()) {
            readMessageFile()
        } else {
            val messagesTemplate = MessageService::class.java.getResource("messages.yaml")
            if (messagesTemplate != null) {
                messagesTemplate.openStream().use {
                    Files.copy(it, MESSAGE_FILE.toPath(), StandardCopyOption.REPLACE_EXISTING)
                }
            } else {
                LOGGER.error { "Could not find messages.yaml in resources" }
            }
        }
    }

    fun getMessageFor(key: String): String {
        return loadedMessages.getOrDefault(key, key)
    }

    fun setMessageFor(key: String, value: String) {
        loadedMessages[key] = value
        saveMessageFile()
        reload()
    }

    fun reload() {
        readMessageFile()
    }

    private fun readMessageFile() {
        val configString = MESSAGE_FILE.readText()
        loadedMessages = Yaml.default.decodeFromString(configString)
    }

    private fun saveMessageFile() {
        val configString = Yaml.default.encodeToString(MapSerializer(String.serializer(), String.serializer()), loadedMessages)
        MESSAGE_FILE.writeText(configString)
    }
}
