package listener

import announcer.EventAnnouncer
import announcer.LocationPollCreator
import announcer.OrganizerChooser
import com.elbekD.bot.Bot
import com.elbekD.bot.feature.chain.chain
import com.elbekD.bot.feature.chain.jumpToAndFire
import com.elbekD.bot.types.BotCommand
import com.elbekD.bot.types.Message
import mu.KotlinLogging
import services.ConfigService

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
        private const val COMMAND_SET_TAG_FILTER = "/setlocationfilter"
    }

    init {
        setupCommands()
        setupSimpleCommands()
        setupSettingsChanger()
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
                BotCommand(COMMAND_SET_TAG_FILTER, "Set the tags for the location poll")
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

    private fun setupSettingsChanger() {
        bot.chain(label = "TAG_START", predicate = { it.text == COMMAND_SET_TAG_FILTER }) { msg ->
            logAdminCmd(msg)
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
            val config = configService.config
            config.locationPoll.filterTags = emptyList()
            configService.save(config)
            bot.jumpToAndFire("TAG_RESULT", msg)
        }.then(label = "TAG_SET") { msg ->
            val text = msg.text!!
            val availableTags = locationPoller.getAvailableTags()
            val inputTags = text.split(",").map { it.lowercase().trim() }.filter { availableTags.contains(it) }

            val config = configService.config
            config.locationPoll.filterTags = inputTags
            configService.save(config)
            bot.jumpToAndFire("TAG_RESULT", msg)
        }.then(label = "TAG_RESULT") { msg ->
            bot.sendMessage(
                msg.chat.id,
                "Active location tags: '`${configService.config.locationPoll.filterTags.joinToString(",")}`'",
                parseMode = "Markdown"
            )
        }.build()
    }

    private fun validAdminCommand(msg: Message) =
        configService.config.adminIds.contains(msg.from?.id) && msg.chat.type == "private"

    private fun logAdminCmd(msg: Message) {
        LOGGER.info { "Execute admin command '${msg.text}' for '${msg.from!!.username}'" }
    }
}
