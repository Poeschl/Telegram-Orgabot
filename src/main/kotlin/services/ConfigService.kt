package services

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import mu.KotlinLogging
import java.io.File

class ConfigService {

    companion object {
        private val CONFIG_FILE: File = File("config/config.yaml")
        private val LOGGER = KotlinLogging.logger { }
    }

    var config: Config = Config("", "", managingGroup = 0)
        private set

    init {
        if (CONFIG_FILE.exists()) {
            readConfigfile()
        } else {
            saveConfigFile()
        }
    }

    fun reload() {
        readConfigfile()
        LOGGER.info { "Config reloaded" }
    }

    fun save(newConfig: Config) {
        config = newConfig
        saveConfigFile()
    }

    private fun readConfigfile() {
        val configString = CONFIG_FILE.readText()
        config = Yaml.default.decodeFromString(configString)
    }

    private fun saveConfigFile() {
        val configString = Yaml.default.encodeToString(Config.serializer(), config)
        CONFIG_FILE.writeText(configString)
        LOGGER.info { "Config saved" }
    }
}

@Serializable
data class Config(
    val telegramBotToken: String, val telegramBotUsername: String, val debug: Boolean = false, val managingGroup: Long, val adminIds: List<Int> = emptyList(),
    val cron: CronConfig = CronConfig(), val locationPoll: LocationPollConfig = LocationPollConfig(), val knownUsers: MutableMap<Int, String> = mutableMapOf()
)

@Serializable
data class CronConfig(val cron: String = "0 0 * * *", val onEvenWeeks: Boolean = true, val onOddWeeks: Boolean = true, val enabled: Boolean = false)

@Serializable
data class LocationPollConfig(val enabled: Boolean = true, val googleCredentialPath: String = "", val sheetName: String = "", val namesArea: String = "")
