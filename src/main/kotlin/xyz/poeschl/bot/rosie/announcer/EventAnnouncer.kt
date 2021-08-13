package xyz.poeschl.bot.rosie.announcer

import com.elbekD.bot.Bot
import mu.KotlinLogging
import xyz.poeschl.bot.rosie.services.ConfigService
import xyz.poeschl.bot.rosie.services.EventContext
import xyz.poeschl.bot.rosie.services.EventService
import xyz.poeschl.bot.rosie.services.MessageService

class EventAnnouncer(private val bot: Bot, private val configService: ConfigService, private val messageService: MessageService, private val eventService: EventService) {

    companion object {
        private val LOGGER = KotlinLogging.logger { }
    }

    /**
     * This method starts a new event planing.
     *
     * This will reset any EventContext stored in the EventService.
     */
    fun announceEventPlaning() {
        LOGGER.info { "Send event reminder to group." }
        LOGGER.debug { "Reset EventContext" }
        eventService.clear()
        eventService.setContext(EventContext(-1, ""))

        val text = messageService.getMessageFor("reminder_text")
        bot.sendMessage(configService.config.managingGroup, text = text)
    }

}
