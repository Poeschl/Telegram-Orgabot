import announcer.EventAnnouncer
import announcer.OrganizerChooser
import com.elbekD.bot.Bot
import listener.AdminCommandsListener
import listener.GroupUserSpy
import mu.KotlinLogging
import services.ConfigService
import services.EventService
import services.MessageService

fun main() {
    BotApplication().run()
}

class BotApplication {

    companion object {
        private val LOGGER = KotlinLogging.logger { }
    }

    init {
        LOGGER.info { "Starting Orgabot Rosie" }
    }

    private val configService = ConfigService()
    private val messageService = MessageService()
    private val bot = Bot.createPolling(configService.config.telegramBotUsername, configService.config.telegramBotToken)

    private val userSpy = GroupUserSpy(bot, configService)
    private val eventService = EventService()
    private val eventAnnouncer = EventAnnouncer(bot, configService, messageService, eventService)
    private val organizerChooser = OrganizerChooser(bot, configService, messageService, eventService)

    private val adminCommandsListener = AdminCommandsListener(bot, configService, eventAnnouncer, organizerChooser)

    fun run() {
        LOGGER.info { "Watching group '${configService.config.managingGroup}' as '${configService.config.telegramBotUsername}'" }

        LOGGER.info { "Started components. Waiting for chat messages" }
        bot.start()
    }
}
