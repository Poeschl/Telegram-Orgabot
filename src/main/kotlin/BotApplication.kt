import com.elbekD.bot.Bot
import listener.GroupUserSpy
import mu.KotlinLogging
import services.ConfigService
import services.MessageService

fun main() {
    BotApplication().run()
}

class BotApplication {

    companion object {
        private val LOGGER = KotlinLogging.logger { }
    }

    private val configService = ConfigService()
    private val messageService = MessageService()
    private val bot = Bot.createPolling(configService.config.telegramBotUsername, configService.config.telegramBotToken)
    private val userSpy = GroupUserSpy(configService, bot)

    fun run() {
        LOGGER.info { "Starting Orgabot Rosie. " }
        LOGGER.info { "Watching group '${configService.config.managingGroup}' as '${configService.config.telegramBotUsername}'" }

        bot.start()
    }
}
