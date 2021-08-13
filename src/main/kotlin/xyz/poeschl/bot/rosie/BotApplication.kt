package xyz.poeschl.bot.rosie

import com.elbekD.bot.Bot
import mu.KotlinLogging
import xyz.poeschl.bot.rosie.announcer.EventAnnouncer
import xyz.poeschl.bot.rosie.announcer.LocationPollCreator
import xyz.poeschl.bot.rosie.announcer.OrganizerChooser
import xyz.poeschl.bot.rosie.listener.AdminCommandsListener
import xyz.poeschl.bot.rosie.listener.GroupUserSpy
import xyz.poeschl.bot.rosie.services.*
import java.nio.file.Files
import java.nio.file.Path

fun main() {
    val configFolder = System.getenv("CONFIG_FOLDER") ?: "."
    val configPath = Path.of(configFolder)
    Files.createDirectories(configPath)

    BotApplication(configPath).run()
}

class BotApplication(configFolder: Path) {

    companion object {
        private val LOGGER = KotlinLogging.logger { }
    }

    init {
        LOGGER.info { "Starting Orgabot Rosie" }
    }

    private val configService = ConfigService(configFolder)
    private val messageService = MessageService(configFolder)
    private val bot = Bot.createPolling(configService.config.telegramBotUsername, configService.config.telegramBotToken)

    private val userSpy = GroupUserSpy(bot, configService)
    private val eventNotificationScheduler = EventNotificationScheduler(configService)
    private val sheetService = GoogleSheetService(configFolder, configService)

    private val eventService = EventService()
    private val eventAnnouncer = EventAnnouncer(bot, configService, messageService, eventService)
    private val organizerChooser = OrganizerChooser(bot, configService, messageService, eventService)
    private val locationPoller = LocationPollCreator(bot, configService, messageService, eventService, sheetService)

    private val adminCommandsListener = AdminCommandsListener(bot, configService, eventAnnouncer, organizerChooser, locationPoller)

    fun run() {
        LOGGER.info { "Watching group '${configService.config.managingGroup}' as '${configService.config.telegramBotUsername}'" }
        eventNotificationScheduler.scheduleNextExecution {
            eventAnnouncer.announceEventPlaning()
            organizerChooser.announceOrganizer()
            locationPoller.createLocationPoll()
        }

        LOGGER.info { "Started components. Waiting for chat messages" }
        bot.start()
    }
}
