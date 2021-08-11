package services

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import mu.KotlinLogging
import java.io.File
import java.nio.file.Path
import kotlin.io.path.pathString

class ConfigService(configFolder: Path) {

    companion object {
        private val LOGGER = KotlinLogging.logger { }
    }

    private val configFile: File = File("${configFolder.pathString}/config.yaml")

    var config: Config = Config("", "", managingGroup = 0)
        private set

    init {
        if (configFile.exists()) {
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
        val configString = configFile.readText()
        config = Yaml.default.decodeFromString(configString)
    }

    private fun saveConfigFile() {
        val configString = Yaml.default.encodeToString(Config.serializer(), config)
        configFile.writeText(configString)
        LOGGER.info { "Config saved" }
    }
}

@Serializable
data class Config(
    val telegramBotToken: String, val telegramBotUsername: String, val managingGroup: Long, val adminIds: List<Int> = emptyList(),
    val cron: CronConfig = CronConfig(), val locationPoll: LocationPollConfig = LocationPollConfig(), val knownUsers: MutableMap<Int, String> = mutableMapOf()
)

@Serializable
data class CronConfig(val schedule: String = "every hour", val onEvenWeeks: Boolean = true, val onOddWeeks: Boolean = true, val enabled: Boolean = false)

@Serializable
data class LocationPollConfig(
    val enabled: Boolean = true, val googleCredentialPath: String = "", val sheetId: String = "", val namesArea: String = "", val tagsArea: String = "",
    val locationsAmount: Int = 4, var filterTags: List<String> = listOf()
)
