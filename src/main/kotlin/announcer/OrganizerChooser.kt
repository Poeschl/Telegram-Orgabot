package announcer

import com.elbekD.bot.Bot
import com.elbekD.bot.types.CallbackQuery
import com.elbekD.bot.types.InlineKeyboardButton
import com.elbekD.bot.types.InlineKeyboardMarkup
import com.elbekD.bot.types.User
import mu.KotlinLogging
import services.ConfigService
import services.EventContext
import services.EventService
import services.MessageService

class OrganizerChooser(private val bot: Bot, private val configService: ConfigService, private val messageService: MessageService, private val eventService: EventService) {

    companion object {
        private val LOGGER = KotlinLogging.logger { }
        private const val REROLL_ACTION = "chooser_reroll"
    }

    init {
        bot.onCallbackQuery(REROLL_ACTION, this::rerollOrganizer)
    }

    fun announceOrganizer() {
        val eventContext = eventService.eventContext
        if (eventContext != null) {
            LOGGER.info { "Select a organizer for the event." }
            val organizer = selectOrganizer(configService.config.knownUsers)

            val keyboard = createRerollKeyboard()
            val text = messageService.getMessageFor("nomination_text").replace("\$user", "@${organizer.second}")
            bot.sendMessage(configService.config.managingGroup, text = text, markup = keyboard)

            eventContext.organizerId = organizer.first
            eventContext.organizerUsername = organizer.second
            eventService.setContext(eventContext)
        }
    }

    fun forceReroll(executingUser: User) {
        val eventContext = eventService.eventContext
        if (eventContext != null) {
            LOGGER.info { "Force rerolling organizer '${eventContext.organizerUsername}'" }

            val newOrganizer = selectOrganizer(configService.config.knownUsers)

            eventContext.organizerId = newOrganizer.first
            eventContext.organizerUsername = newOrganizer.second
            eventService.setContext(eventContext)

            sendRerollMessage(eventContext, executingUser.username!!, newOrganizer.second)
        }

    }

    private fun rerollOrganizer(callbackQuery: CallbackQuery) {
        val eventContext = eventService.eventContext
        if (eventContext != null
            && callbackQuery.from.id == eventContext.organizerId
            && eventContext.organizerRerollLimit > 0
        ) {
            LOGGER.info { "Rerolling organizer '${eventContext.organizerUsername}'" }
            val lastSelectedUserId = eventContext.organizerId
            val lastSelectedUsername = eventContext.organizerUsername
            val knownUsersMinusLastOne = configService.config.knownUsers.filter { it.key != lastSelectedUserId }
            val newOrganizer = selectOrganizer(knownUsersMinusLastOne)

            eventContext.organizerRerollLimit -= 1
            eventContext.organizerId = newOrganizer.first
            eventContext.organizerUsername = newOrganizer.second
            eventService.setContext(eventContext)

            // Remove the reroll button from the original message
            bot.editMessageReplyMarkup(callbackQuery.message!!.message_id, markup = null)

            sendRerollMessage(eventContext, lastSelectedUsername, newOrganizer.second)

        } else {
            LOGGER.debug { "'${callbackQuery.from.username}' was not allowed to re-roll." }
        }
    }

    private fun sendRerollMessage(eventContext: EventContext, lastOrganizer: String, newOrganizer: String) {
        val keyboard = when {
            eventContext.organizerRerollLimit > 1 -> createRerollKeyboard()
            else -> null
        }

        val rerollText = messageService.getMessageFor("nomination_reroll_text")
            .replace("\$user", "@${lastOrganizer}")
            .replace("\$newUser", "@${newOrganizer}")
        bot.sendMessage(configService.config.managingGroup, text = rerollText, markup = keyboard)
    }

    private fun selectOrganizer(userMap: Map<Int, String>): Pair<Int, String> {
        val organizerId = userMap.keys.random()
        val organizerName = userMap[organizerId]!!

        return Pair(organizerId, organizerName)
    }

    private fun createRerollKeyboard(): InlineKeyboardMarkup {
        val rerollText = messageService.getMessageFor("nomination_reroll_button")
        val rerollbuttons = InlineKeyboardButton(rerollText, callback_data = REROLL_ACTION)
        return InlineKeyboardMarkup(listOf(listOf(rerollbuttons)))
    }
}