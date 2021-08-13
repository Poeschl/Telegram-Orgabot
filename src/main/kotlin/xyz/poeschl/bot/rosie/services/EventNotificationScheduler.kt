package xyz.poeschl.bot.rosie.services

import com.github.shyiko.skedule.InvalidScheduleException
import com.github.shyiko.skedule.Schedule
import mu.KotlinLogging
import java.time.ZonedDateTime
import java.time.temporal.WeekFields
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

class EventNotificationScheduler(private val configService: ConfigService) {

    companion object {
        private val LOGGER = KotlinLogging.logger { }
    }

    private val executor = ScheduledThreadPoolExecutor(1)

    init {
        executor.removeOnCancelPolicy = true
    }

    fun scheduleNextExecution(action: () -> Unit) {
        try {
            val schedule = Schedule.parse(configService.config.cron.schedule)
            val now = ZonedDateTime.now()
            val nextExecution = schedule.next(now)
            executor.schedule({
                executeWithWeekNumbers(action, configService.config.cron.onOddWeeks, configService.config.cron.onEvenWeeks)

                //reschedule automatically
                scheduleNextExecution(action)
            }, nextExecution.toEpochSecond() - now.toEpochSecond(), TimeUnit.SECONDS)

            LOGGER.info { "Schedule notification for $nextExecution" }
        } catch (ex: InvalidScheduleException) {
            LOGGER.error(ex) { "Could not schedule next notification!" }
        }
    }

    private fun executeWithWeekNumbers(action: () -> Unit, oddWeeks: Boolean, evenWeeks: Boolean) {
        val weeknumber = ZonedDateTime.now().get(WeekFields.ISO.weekOfWeekBasedYear())

        if ((evenWeeks && weeknumber % 2 == 0) || (oddWeeks && weeknumber % 2 != 0)) {
            LOGGER.info { "Trigger notification event" }
            action.invoke()
        }
    }
}
