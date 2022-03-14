package xyz.poeschl.bot.rosie.services

class EventService {

    var eventContext: EventContext? = null
        private set

    fun setContext(context: EventContext) {
        this.eventContext = context
    }

    fun clear() {
        eventContext = null
    }
}

data class EventContext(
    var organizerId: Long, var organizerUsername: String, var organizerRerollLimit: Int = 1, var locationRerollLimit: Int = 3,
    var locationPollId: String? = null
)
