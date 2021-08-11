package announcer

import com.elbekD.bot.Bot
import com.elbekD.bot.types.CallbackQuery
import com.elbekD.bot.types.InlineKeyboardButton
import com.elbekD.bot.types.InlineKeyboardMarkup
import com.elbekD.bot.types.User
import mu.KotlinLogging
import services.*

class LocationPollCreator(
    private val bot: Bot, private val configService: ConfigService, private val messageService: MessageService, private val eventService: EventService,
    private val sheetService: GoogleSheetService
) {

    companion object {
        private val LOGGER = KotlinLogging.logger { }
        private const val REROLL_ACTION = "location_reroll"
    }

    init {
        bot.onCallbackQuery(REROLL_ACTION, this::rerollLocations)
    }

    fun createLocationPoll() {
        val eventContext = eventService.eventContext
        if (eventContext != null && configService.config.locationPoll.enabled) {
            LOGGER.info { "Create location poll for the event." }

            val filterTags = configService.config.locationPoll.filterTags
            val selectedLocations = selectLocations(sheetService.getLocations(), filterTags)

            sendPoll(selectedLocations, eventContext)
        }
    }

    fun forceReroll(executingUser: User) {
        val eventContext = eventService.eventContext
        if (eventContext != null) {
            LOGGER.info { "Force rerolling locations" }

            val filterTags = configService.config.locationPoll.filterTags
            val newLocations = selectLocations(sheetService.getLocations(), filterTags)

            sendRerollMessage(eventContext)
            sendPoll(newLocations, eventContext)
        }
    }

    private fun rerollLocations(callbackQuery: CallbackQuery) {
        val eventContext = eventService.eventContext
        if (eventContext != null
            && callbackQuery.from.id == eventContext.organizerId
            && eventContext.locationRerollLimit > 0
        ) {
            bot.answerCallbackQuery(callbackQuery.id)

            LOGGER.info { "Rerolling locations" }
            val filterTags = configService.config.locationPoll.filterTags
            val newLocations = selectLocations(sheetService.getLocations(), filterTags)

            eventContext.locationRerollLimit -= 1
            eventService.setContext(eventContext)

            // Remove the old poll
            bot.deleteMessage(callbackQuery.message!!.chat.id, messageId = callbackQuery.message!!.message_id)

            sendRerollMessage(eventContext)
            sendPoll(newLocations, eventContext)

        } else {
            val text = messageService.getMessageFor("location_reroll_not_successful")
            bot.answerCallbackQuery(callbackQuery.id, text, alert = true)
            LOGGER.info { "'${callbackQuery.from.username}' was not allowed to re-roll." }
        }
    }

    private fun sendRerollMessage(eventContext: EventContext) {
        val rerollText = messageService.getMessageFor("location_reroll_notification")
            .replace("\$user", "@${eventContext.organizerUsername}")
            .replace("\$count", "${eventContext.locationRerollLimit}")
        bot.sendMessage(configService.config.managingGroup, text = rerollText)
    }

    private fun sendPoll(locations: List<GoogleSheetService.Location>, eventContext: EventContext) {
        val keyboard = when {
            eventContext.locationRerollLimit > 1 -> createRerollKeyboard()
            else -> null
        }

        val text = messageService.getMessageFor("location_suggestion_question_text")
        val showOnlyText = messageService.getMessageFor("location_poll_show_text")
        val pollOptions = locations.map { it.name }.toMutableList()
        pollOptions.add(showOnlyText)
        bot.sendPoll(configService.config.managingGroup, text, pollOptions, anonymous = true, allowsMultipleAnswers = true, markup = keyboard)
    }

    private fun createRerollKeyboard(): InlineKeyboardMarkup {
        val rerollText = messageService.getMessageFor("location_reroll_button")
        val rerollbuttons = InlineKeyboardButton(rerollText, callback_data = REROLL_ACTION)
        return InlineKeyboardMarkup(listOf(listOf(rerollbuttons)))
    }

    private fun selectLocations(locations: List<GoogleSheetService.Location>, filterTags: List<String>): List<GoogleSheetService.Location> {
        val filteredLocations = locations.filter { locationTag -> locationTag.tags.map { it.lowercase() }.containsAll(filterTags.map { it.lowercase() }) }
        val randomIndexes = IntRange(0, filteredLocations.size).shuffled().slice(1..configService.config.locationPoll.locationsAmount)
        return filteredLocations.slice(randomIndexes)
    }
}
