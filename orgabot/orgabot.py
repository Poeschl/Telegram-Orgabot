import logging
import sched
from datetime import datetime
from threading import Thread

from telegram import Bot

from config import Config, TELEGRAM_API_KEY, GROUP_ID, Messages, REMINDER_DATETIME, REMINDER_INTERVAL
from telegramapi import TelegramEndpoint


class RepeatingReminder:

    def __init__(self, bot: Bot, chat_id: int, reminder_message: str, start_datetime: datetime, interval_days: int):
        self.bot = bot
        self.chat_id = chat_id
        self.message = reminder_message
        self.start_datetime = start_datetime
        self.interval_seconds = interval_days * 24 * 60 * 60
        self.scheduler = sched.scheduler()

    def start(self):
        if self.start_datetime <= datetime.now():
            logging.error("Reminder start time is in the past. To enable the reminder it must be in the future!")
            return

        delta = self.start_datetime - datetime.now()
        self.scheduler.enter(delta.total_seconds(), 1, self.reminder)
        logging.info("First reminder in %d seconds.", delta.total_seconds())
        self.scheduler.run()

    def stop(self):
        for event in self.scheduler.queue:
            self.scheduler.cancel(event)

    def reminder(self):
        logging.info("Send reminder to group.")
        self.bot.send_message(chat_id=self.chat_id, text=self.message)
        self.scheduler.enter(self.interval_seconds, 1, self.reminder)

        logging.info("Next reminder in %d seconds.", self.interval_seconds)
        self.scheduler.run()


def main():
    logging.basicConfig(level=logging.INFO)
    config = Config()
    messages = Messages()
    telegram_api = TelegramEndpoint(config.get_config(TELEGRAM_API_KEY))

    logging.info('Started Orgabot')
    logging.info("Group Id: %s", config.get_config(GROUP_ID))

    telegram_api.start()

    class ReminderThread(Thread):
        def run(self) -> None:
            reminder = RepeatingReminder(telegram_api.get_bot(),
                                         config.get_config(GROUP_ID),
                                         messages.get_message("reminder_text"),
                                         config.get_config(REMINDER_DATETIME),
                                         config.get_config(REMINDER_INTERVAL))
            reminder.start()
    ReminderThread().start()


if __name__ == '__main__':
    main()
