import logging
import sched
import signal
from datetime import datetime
from random import choice
from string import Template
from threading import Thread

from telegram import Bot
from telegram.ext import MessageHandler, Filters

from config import Config, TELEGRAM_API_KEY, GROUP_ID, Messages, REMINDER_DATETIME, REMINDER_INTERVAL, DEBUG, KnownUsers
from telegramapi import TelegramEndpoint


class RepeatingReminder:

    def __init__(self, bot: Bot, chat_id: int, reminder_message: str, start_datetime: datetime, interval_days: int):
        self.bot = bot
        self.chat_id = chat_id
        self.message = reminder_message
        self.start_datetime = start_datetime
        self.interval_seconds = interval_days * 24 * 60 * 60
        self.scheduler = sched.scheduler()
        self.callback = None

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
        self.stop()
        logging.info("Send reminder to group.")
        self.bot.send_message(chat_id=self.chat_id, text=self.message)
        if self.callback is not None:
            self.callback()

        self.scheduler.enter(self.interval_seconds, 1, self.reminder)
        logging.info("Next reminder in %d seconds.", self.interval_seconds)
        self.scheduler.run()


class UserNominator():

    def __init__(self, bot: Bot, chat_id: int, nominate_template: str, known_users: KnownUsers):
        self.bot = bot
        self.chat_id = chat_id
        self.nominate_template = nominate_template
        self.known_users = known_users

    def spy_on_message(self, update, context):
        user_id = update.effective_user['id']
        username = update.effective_user['username']
        if user_id not in self.known_users.users:
            logging.info(f"New user {username} detected")
            self.known_users.insert_user(user_id, username)

    def nominate_user(self):
        users = self.known_users.users
        nominee = choice(list(users.values()))

        logging.info(f"Nominate '{nominee}' for the organisation")
        text = Template(self.nominate_template).substitute(user=f"@{nominee}")
        self.bot.send_message(chat_id=self.chat_id, text=text)


class GracefulKiller:
    exit_callback = None

    def __init__(self, callback):
        signal.signal(signal.SIGINT, self.exit_gracefully)
        signal.signal(signal.SIGTERM, self.exit_gracefully)
        self.exit_callback = callback

    def exit_gracefully(self, signum, frame):
        self.exit_callback()


def debug_input(reminder_func, nomination_func):
    debug_options = {
        1: 'Reminder',
        2: 'User nomination'
    }

    while True:
        print(f"Debug events:\n{debug_options}")
        try:
            debug_event = int(input("Run debug event: "))
            if debug_event < 1 or debug_event > len(debug_options):
                assert ValueError

            if debug_event == 1:
                class AsyncThread(Thread):
                    def run(self) -> None:
                        reminder_func()

                AsyncThread().start()

            elif debug_event == 2:
                nomination_func()

        except ValueError:
            print("Error! This is not a valid number. Try again.")


def main():
    logging.basicConfig(level=logging.INFO)
    config = Config()
    messages = Messages()
    telegram_api = TelegramEndpoint(config.get_config(TELEGRAM_API_KEY))

    logging.info('Started Orgabot')
    logging.info("Group Id: %s", config.get_config(GROUP_ID))

    telegram_api.start()

    reminder = RepeatingReminder(telegram_api.get_bot(),
                                 config.get_config(GROUP_ID),
                                 messages.get_message("reminder_text"),
                                 config.get_config(REMINDER_DATETIME),
                                 config.get_config(REMINDER_INTERVAL))

    user_nominator = UserNominator(telegram_api.get_bot(),
                                   config.get_config(GROUP_ID),
                                   messages.get_message("nomination_text"),
                                   KnownUsers())
    telegram_api.register_command_handler(MessageHandler(Filters.all, user_nominator.spy_on_message))

    def on_remind():
        user_nominator.nominate_user()

    reminder.callback = on_remind

    class ReminderThread(Thread):
        def run(self) -> None:
            reminder.start()

    ReminderThread().start()

    def on_exit():
        reminder.stop()

    GracefulKiller(on_exit)

    if config.get_config(DEBUG):
        debug_input(reminder.reminder, user_nominator.nominate_user)


if __name__ == '__main__':
    main()
