package xyz.poeschl.bot.rosie.listener

import com.elbekD.bot.Bot
import com.elbekD.bot.types.Message
import mu.KotlinLogging
import xyz.poeschl.bot.rosie.services.ConfigService

class GroupUserSpy(bot: Bot, private val configService: ConfigService) {

    companion object {
        private val LOGGER = KotlinLogging.logger { }
    }

    init {
        bot.onMessage(this::onMessage)
    }

    private fun onMessage(message: Message) {
        if (message.from != null && message.chat.id == configService.config.managingGroup) {
            val userId = message.from!!.id
            val username = message.from!!.username!!

            if (!configService.config.knownUsers.containsKey(userId)) {
                LOGGER.info { "Insert '${username}' to known users" }
                val configWithNewUser = configService.config
                configWithNewUser.knownUsers[userId] = username
                configService.save(configWithNewUser)
            }
        }
    }
}
