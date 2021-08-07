package services

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.decodeFromString
import mu.KotlinLogging
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.pathString

class MessageService(configFolder: Path) {
    companion object {
        private val LOGGER = KotlinLogging.logger { }
    }

    private val messageFile: File = File("${configFolder.pathString}/messages.yaml")
    private lateinit var loadedMessages: MutableMap<String, String>

    init {
        if (messageFile.exists()) {
            readMessageFile()
        } else {
            val messagesTemplate = MessageService::class.java.getResource("/messages.yaml")
            if (messagesTemplate != null) {
                messagesTemplate.openStream().use {
                    Files.copy(it, messageFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
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
        val configString = messageFile.readText()
        loadedMessages = Yaml.default.decodeFromString(configString)
    }

    private fun saveMessageFile() {
        val configString = Yaml.default.encodeToString(MapSerializer(String.serializer(), String.serializer()), loadedMessages)
        messageFile.writeText(configString)
    }
}
