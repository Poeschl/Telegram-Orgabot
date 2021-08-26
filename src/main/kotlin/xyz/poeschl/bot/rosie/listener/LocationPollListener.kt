package xyz.poeschl.bot.rosie.listener

import com.elbekD.bot.Bot
import com.elbekD.bot.types.Poll
import mu.KotlinLogging
import xyz.poeschl.bot.rosie.services.ConfigService
import xyz.poeschl.bot.rosie.services.EventService
import xyz.poeschl.bot.rosie.services.MessageService

class LocationPollListener(private val bot: Bot, private val configService: ConfigService, private val messageService: MessageService, private val eventService: EventService) {

    companion object {
        private val LOGGER = KotlinLogging.logger { }
    }

    init {
        bot.onAnyUpdate { if (it.poll != null) onPollUpdate(it.poll!!) }
    }

    private fun onPollUpdate(poll: Poll) {
        val eventContext = eventService.eventContext
        val config = configService.config
        if (eventContext != null && poll.id == eventContext.locationPollId && config.locationPoll.reminderVoterPercentage > 0) {
            val orgaGroupMemberCount = bot.getChatMembersCount(config.managingGroup).get() - 1.0 //Subtract bot
            if ((poll.total_voter_count / orgaGroupMemberCount) >= config.locationPoll.reminderVoterPercentage) {
                val message = messageService.getMessageFor("location_poll_reminder")
                bot.sendMessage(config.managingGroup, message)
                eventContext.locationPollId = null
            }
        }
    }
}
