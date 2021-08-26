package xyz.poeschl.bot.rosie.listener

import com.elbekD.bot.Bot
import com.elbekD.bot.feature.chain.chain
import com.elbekD.bot.feature.chain.jumpToAndFire
import com.elbekD.bot.types.BotCommand
import com.elbekD.bot.types.Message
import com.elbekD.bot.util.Action
import com.github.shyiko.skedule.InvalidScheduleException
import com.github.shyiko.skedule.Schedule
import mu.KotlinLogging
import xyz.poeschl.bot.rosie.announcer.EventAnnouncer
import xyz.poeschl.bot.rosie.announcer.LocationPollCreator
import xyz.poeschl.bot.rosie.announcer.OrganizerChooser
import xyz.poeschl.bot.rosie.services.ConfigService

class AdminCommandsListener(
    private val bot: Bot, private val configService: ConfigService,
    private val eventAnnouncer: EventAnnouncer, private val organizerChooser: OrganizerChooser, private val locationPoller: LocationPollCreator
) {

    companion object {
        private val LOGGER = KotlinLogging.logger { }
        private const val COMMAND_HELP = "/help"
        private const val COMMAND_CANCEL = "/cancel"
        private const val COMMAND_EMPTY = "/empty"
        private const val COMMAND_REMINDER = "/sendreminder"
        private const val COMMAND_ORGANIZER = "/sendorganizerseletion"
        private const val COMMAND_ORGANIZER_REROLL = "/rerollorganizer"
        private const val COMMAND_LOCATION_POLL = "/sendlocationpoll"
        private const val COMMAND_LOCATION_REROLL = "/rerolllocation"
        private const val COMMAND_LOCATION_SETTINGS = "/locationsettings"
        private const val COMMAND_LOCATION_SETTINGS_TAGS = "/tags"
        private const val COMMAND_CRON_SETTINGS = "/cronsettings"
        private const val COMMAND_CRON_SETTINGS_SCHEDULE = "/schedule"
        private const val COMMAND_CRON_SETTINGS_EVENWEEKS = "/toggleevenweeks"
        private const val COMMAND_CRON_SETTINGS_ODDWEEKS = "/toggleoddweeks"
        private const val COMMAND_SETTINGS_ENABLE = "/enable"
        private const val COMMAND_SETTINGS_DISABLE = "/disable"
    }

    init {
        setupCommands()
        setupSimpleCommands()
        setupLocationSettings()
        setupCronSettings()
    }

    private fun setupCommands() {
        bot.setMyCommands(
            listOf(
                BotCommand(COMMAND_HELP, "Shows a help text"),
                BotCommand(COMMAND_REMINDER, "Sends the reminder manually. Will reset the current event!"),
                BotCommand(COMMAND_ORGANIZER, "Sends the initial organizer announce message for the current event context"),
                BotCommand(COMMAND_ORGANIZER_REROLL, "Force a re-roll of the events organizer"),
                BotCommand(COMMAND_LOCATION_POLL, "Sends the location poll message"),
                BotCommand(COMMAND_LOCATION_REROLL, "Force a re-roll of the locations"),
                BotCommand(COMMAND_LOCATION_SETTINGS, "Settings for the location poll"),
                BotCommand(COMMAND_CRON_SETTINGS, "Settings for the cron messages")
            )
        )
        LOGGER.info { "Setup admin commands" }
    }

    private fun setupSimpleCommands() {
        bot.onCommand(COMMAND_HELP) { msg, _ ->
            logAdminCmd(msg)
            bot.sendMessage(
                msg.chat.id, text = "The bot can be controlled via several commands over the private chat. Admin permissions are needed.\n" +
                        "For the full list of commands and descriptions use the integrate commands feature of Telegram."
            )
        }

        bot.onCommand(COMMAND_REMINDER) { msg, _ ->
            if (validAdminCommand(msg)) {
                logAdminCmd(msg)
                eventAnnouncer.announceEventPlaning()
            }
        }

        bot.onCommand(COMMAND_ORGANIZER) { msg, _ ->
            if (validAdminCommand(msg)) {
                logAdminCmd(msg)
                organizerChooser.announceOrganizer()
            }
        }

        bot.onCommand(COMMAND_ORGANIZER_REROLL) { msg, _ ->
            if (validAdminCommand(msg)) {
                logAdminCmd(msg)
                organizerChooser.forceReroll(msg.from!!)
            }
        }

        bot.onCommand(COMMAND_LOCATION_POLL) { msg, _ ->
            if (validAdminCommand(msg)) {
                logAdminCmd(msg)
                locationPoller.createLocationPoll()
            }
        }

        bot.onCommand(COMMAND_LOCATION_REROLL) { msg, _ ->
            if (validAdminCommand(msg)) {
                logAdminCmd(msg)
                locationPoller.forceReroll(msg.from!!)
            }
        }
    }

    private fun setupLocationSettings() {
        bot.chain(label = "LOCATION_START", predicate = { it.text == COMMAND_LOCATION_SETTINGS }) { msg ->
            logAdminCmd(msg)
            bot.sendMessage(
                msg.chat.id,
                """
                Current: 
                enabled: ${configService.config.locationPoll.enabled}
                tags: ${configService.config.locationPoll.filterTags.joinToString(", ")}
                    
                Available settings:                
                $COMMAND_SETTINGS_ENABLE
                $COMMAND_SETTINGS_DISABLE
                $COMMAND_LOCATION_SETTINGS_TAGS
                
                or $COMMAND_CANCEL
                """.trimIndent(),
                parseMode = "Markdown"
            )
        }.then { msg ->
            when (msg.text) {
                COMMAND_CANCEL -> bot.jumpToAndFire("LOCATION_CANCEL", msg)
                COMMAND_LOCATION_SETTINGS_TAGS -> bot.jumpToAndFire("TAG_START", msg)
                COMMAND_SETTINGS_ENABLE -> bot.jumpToAndFire("ENABLE", msg)
                COMMAND_SETTINGS_DISABLE -> bot.jumpToAndFire("DISABLE", msg)
            }
        }.then(label = "LOCATION_CANCEL", isTerminal = true) { msg ->
            bot.sendMessage(msg.chat.id, "Canceled")
        }.then(label = "ENABLE", isTerminal = true) { msg ->
            LOGGER.info { "${msg.from!!.username} enabled location poll" }
            val config = configService.config
            config.locationPoll.enabled = true
            configService.save(config)
            bot.sendMessage(msg.chat.id, "Location poll enabled")

        }.then(label = "DISABLE", isTerminal = true) { msg ->
            LOGGER.info { "${msg.from!!.username} disabled location poll" }
            val config = configService.config
            config.locationPoll.enabled = false
            configService.save(config)
            bot.sendMessage(msg.chat.id, "Location poll disabled")

        }.then(label = "TAG_START") { msg ->
            logAdminCmd(msg)
            bot.sendChatAction(msg.chat.id, Action.Typing)
            val knownTags = locationPoller.getAvailableTags()
            bot.sendMessage(
                msg.chat.id,
                """
                Reply with a comma-separated list of tags.
                Active tags:
                ```
                ${configService.config.locationPoll.filterTags.joinToString(",")}
                ```
                Known tags:
                ```
                ${knownTags.joinToString(",")}
                ```
                or /empty or /cancel
                """.trimIndent(),
                parseMode = "Markdown"
            )
        }.then { msg ->
            val text = msg.text
            if (text != null) {
                when {
                    text == COMMAND_CANCEL -> bot.jumpToAndFire("TAG_RESULT", msg)
                    text == COMMAND_EMPTY -> bot.jumpToAndFire("TAG_CLEAR", msg)
                    text.startsWith("/").not() && text.split(",").isNotEmpty() -> bot.jumpToAndFire("TAG_SET", msg)
                    else -> bot.jumpToAndFire("TAG_START", msg)
                }
            } else {
                bot.jumpToAndFire("TAG_START", msg)
            }
        }.then(label = "TAG_CLEAR") { msg ->
            LOGGER.info { "${msg.from!!.username} cleared location tags" }

            val config = configService.config
            config.locationPoll.filterTags = emptyList()
            configService.save(config)
            bot.jumpToAndFire("TAG_RESULT", msg)
        }.then(label = "TAG_SET") { msg ->
            val text = msg.text!!
            val availableTags = locationPoller.getAvailableTags()
            val inputTags = text.split(",").map { it.lowercase().trim() }.filter { availableTags.contains(it) }

            LOGGER.info { "${msg.from!!.username} set location tags '${inputTags.joinToString(", ")}'" }

            val config = configService.config
            config.locationPoll.filterTags = inputTags
            configService.save(config)
            bot.jumpToAndFire("TAG_RESULT", msg)
        }.then(label = "TAG_RESULT", isTerminal = true) { msg ->
            bot.sendMessage(
                msg.chat.id,
                "Active location tags: '`${configService.config.locationPoll.filterTags.joinToString(",")}`'",
                parseMode = "Markdown"
            )
        }.build()
    }

    private fun setupCronSettings() {
        bot.chain(label = "CRON_START", predicate = { it.text == COMMAND_CRON_SETTINGS }) { msg ->
            logAdminCmd(msg)
            bot.sendMessage(
                msg.chat.id,
                """
                Current: 
                schedule: ${configService.config.cron.schedule}
                evenWeeks: ${configService.config.cron.onEvenWeeks}
                oddWeeks: ${configService.config.cron.onOddWeeks}   
                
                Available settings:                
                $COMMAND_SETTINGS_ENABLE
                $COMMAND_SETTINGS_DISABLE
                $COMMAND_CRON_SETTINGS_SCHEDULE
                $COMMAND_CRON_SETTINGS_EVENWEEKS
                $COMMAND_CRON_SETTINGS_ODDWEEKS
                
                or $COMMAND_CANCEL
                """.trimIndent(),
                parseMode = "Markdown"
            )
        }.then { msg ->
            when (msg.text) {
                COMMAND_CANCEL -> bot.jumpToAndFire("CRON_CANCEL", msg)
                COMMAND_CRON_SETTINGS_SCHEDULE -> bot.jumpToAndFire("SCHEDULE", msg)
                COMMAND_SETTINGS_ENABLE -> bot.jumpToAndFire("ENABLE", msg)
                COMMAND_SETTINGS_DISABLE -> bot.jumpToAndFire("DISABLE", msg)
                COMMAND_CRON_SETTINGS_EVENWEEKS -> bot.jumpToAndFire("EVEN_WEEKS_TOGGLE", msg)
                COMMAND_CRON_SETTINGS_ODDWEEKS -> bot.jumpToAndFire("ODD_WEEKS_TOGGLE", msg)
            }
        }.then(label = "CRON_CANCEL", isTerminal = true) { msg ->
            bot.sendMessage(msg.chat.id, "Canceled")

        }.then(label = "ENABLE", isTerminal = true) { msg ->
            LOGGER.info { "${msg.from!!.username} enabled cron" }
            val config = configService.config
            config.cron.enabled = true
            configService.save(config)
            bot.sendMessage(msg.chat.id, "Cron enabled")

        }.then(label = "DISABLE", isTerminal = true) { msg ->
            LOGGER.info { "${msg.from!!.username} disabled cron" }
            val config = configService.config
            config.cron.enabled = false
            configService.save(config)
            bot.sendMessage(msg.chat.id, "Cron disabled")

        }.then(label = "CRON_CANCEL", isTerminal = true) { msg ->
            bot.sendMessage(msg.chat.id, "Canceled")
        }.then(label = "SCHEDULE") { msg ->
            logAdminCmd(msg)
            bot.sendMessage(
                msg.chat.id,
                """
                Current schedule: '${configService.config.cron.schedule}'
                
                Type the new expression ([Examples](https://schedule.readthedocs.io/en/stable/examples.html))              
                or $COMMAND_CANCEL
                """.trimIndent(),
                parseMode = "Markdown"
            )
        }.then { msg ->
            val text = msg.text
            if (text == COMMAND_CANCEL) {
                bot.jumpToAndFire("CRON_CANCEL", msg)
            } else {
                LOGGER.info { "Set cron schedule" }
                try {
                    Schedule.parse(text)

                    LOGGER.info { "Set schedule to '$text'" }

                    val config = configService.config
                    config.cron.schedule = text!!
                    configService.save(config)
                    bot.jumpToAndFire("CRON_END", msg)
                } catch (ex: InvalidScheduleException) {
                    LOGGER.info { "Invalid input '$text'" }
                    bot.jumpToAndFire("SCHEDULE", msg)
                }
            }
        }.then(label = "CRON_END", isTerminal = true) { msg ->
            bot.sendMessage(
                msg.chat.id, """
                Set settings: 
                '${configService.config.cron.schedule}'
                evenWeeks: ${configService.config.cron.onEvenWeeks}
                oddWeeks: ${configService.config.cron.onOddWeeks}
                """.trimIndent()
            )
        }.then(label = "EVEN_WEEKS_TOGGLE") { msg ->
            val config = configService.config
            LOGGER.info { "Set on even weeks to ${!config.cron.onEvenWeeks}" }

            config.cron.onEvenWeeks = !config.cron.onEvenWeeks
            configService.save(config)
            bot.jumpToAndFire("CRON_END", msg)
        }.then(label = "ODD_WEEKS_TOGGLE") { msg ->
            val config = configService.config
            LOGGER.info { "Set on odd weeks to ${!config.cron.onEvenWeeks}" }

            config.cron.onOddWeeks = !config.cron.onOddWeeks
            configService.save(config)
            bot.jumpToAndFire("CRON_END", msg)
        }.build()
    }

    private fun validAdminCommand(msg: Message) =
        configService.config.adminIds.contains(msg.from?.id) && msg.chat.type == "private"

    private fun logAdminCmd(msg: Message) {
        LOGGER.info { "Execute admin command '${msg.text}' for '${msg.from!!.username}'" }
    }
}
