import logging
import os
import sched
import signal
import sys
from datetime import datetime
from random import choice, sample
from string import Template
from threading import Thread

from telegram import Bot, InlineKeyboardButton, InlineKeyboardMarkup, ChatAction
from telegram.ext import MessageHandler, Filters, CallbackQueryHandler

from config import Config, TELEGRAM_API_KEY, GROUP_ID, Messages, REMINDER_DATETIME, REMINDER_INTERVAL, DEBUG, KnownUsers, \
    GOOGLE_USER_CREDENTIALS_FILE, LOCATION_SHEET_NAME, LOCATION_SHEET_NAMES_AREA
from config import NOMINATE_GROUP_MEMBER
from sheets import SheetsInterface
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


class UserNominator:
    REROLL_ACTION = "nomiation_reroll"

    def __init__(self, bot: Bot, chat_id: int, known_users: KnownUsers, nominate_template: str, nominate_reroll_text: str,
                 nominate_reroll_notification: str):
        self.bot = bot
        self.chat_id = chat_id
        self.known_users = known_users
        self.nominate_template = nominate_template
        self.nominate_reroll_text = nominate_reroll_text
        self.nominate_reroll_notification = nominate_reroll_notification

    def spy_on_message(self, update, context):
        user_id = update.effective_user['id']
        username = update.effective_user['username']
        if user_id not in self.known_users.users:
            logging.info(f"New user {username} detected")
            self.known_users.insert_user(user_id, username)

    def reroll_nominee(self, update, context):
        context.bot.send_chat_action(chat_id=self.chat_id, action=ChatAction.TYPING)
        self.spy_on_message(update, context)
        issuer = update.effective_user["username"]
        logging.info(f"Reroll nominee on behave of '{issuer}'")

        message_id = update.effective_message["message_id"]

        nominee = self._get_nominee()
        notification = Template(self.nominate_reroll_notification).substitute(user=f"@{issuer}")
        roll = Template(self.nominate_template).substitute(user=f"@{nominee}")
        self.bot.edit_message_text(chat_id=self.chat_id, message_id=message_id, text=f"{notification}\n\n{roll}")

    def nominate_user(self):
        nominee = self._get_nominee()

        keyboard = [[InlineKeyboardButton(self.nominate_reroll_text, callback_data=self.REROLL_ACTION)]]
        reroll_keyboard = InlineKeyboardMarkup(keyboard)

        text = Template(self.nominate_template).substitute(user=f"@{nominee}")
        self.bot.send_message(chat_id=self.chat_id, text=text, reply_markup=reroll_keyboard)

    def _get_nominee(self):
        users = self.known_users.users
        nominee = choice(list(users.values()))
        logging.info(f"Nominate '{nominee}' for the organisation")
        return nominee


class LocationSuggester:
    REROLL_ACTION = 'location_reroll'

    def __init__(self, bot: Bot, chat_id: str, google_creds_file: str, sheetname: str, name_area: str, poll_question: str,
                 location_reroll_text: str, location_reroll_notification: str):
        self.bot = bot
        self.chat_id = chat_id
        self.google_creds_file = google_creds_file
        self.sheetname = sheetname
        self.name_area = name_area
        self.poll_question = poll_question
        self.location_reroll_text = location_reroll_text
        self.location_reroll_notification = location_reroll_notification
        self.last_message = None

    def suggest_locations(self):
        logging.info(f"Get locations from '{self.sheetname}' Sheet for location poll")
        chosen_locations = self._get_random_locations()
        logging.info(f"Selected locations: {chosen_locations}")

        keyboard = [[InlineKeyboardButton(self.location_reroll_text, callback_data=self.REROLL_ACTION)]]
        reroll_keyboard = InlineKeyboardMarkup(keyboard)

        self.last_message = self.bot.send_poll(chat_id=self.chat_id, is_anonymous=False, allows_multiple_answers=True,
                                               question=self.poll_question, options=chosen_locations, reply_markup=reroll_keyboard)

    def reroll_location(self, update, context):
        context.bot.send_chat_action(chat_id=self.chat_id, action=ChatAction.TYPING)
        issuer = update.effective_user["username"]
        logging.info(f"Reroll locations on behave of '{issuer}'")

        chosen_locations = self._get_random_locations()
        logging.info(f"Rerolled locations: {chosen_locations}")

        message_id = update.effective_message["message_id"]
        self.bot.delete_message(chat_id=self.chat_id, message_id=message_id)

        notification = Template(self.location_reroll_notification).substitute(user=f"@{issuer}")
        self.last_message = self.bot.send_poll(chat_id=self.chat_id, is_anonymous=False, allows_multiple_answers=True,
                                               question=f"{notification}\n\n{self.poll_question}", options=chosen_locations)

    def _get_random_locations(self):
        sheets = SheetsInterface(self.google_creds_file)

        area = sheets.read_col(self.sheetname, self.name_area)

        names = []
        for i in range(len(area)):
            names.append(area[i][0])

        return sample(names, 5)


class GracefulKiller:
    exit_callback = None

    def __init__(self, callback):
        signal.signal(signal.SIGINT, self.exit_gracefully)
        signal.signal(signal.SIGTERM, self.exit_gracefully)
        self.exit_callback = callback

    def exit_gracefully(self, signum, frame):
        self.exit_callback()
        os.kill(os.getpid(), 9)


def debug_input(reminder_func, nomination_func, location_func):
    debug_options = {
        1: 'Reminder',
        2: 'User nomination',
        3: 'Suggest location'
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
                if nomination_func is not None:
                    nomination_func()
            elif debug_event == 3:
                if location_func is not None:
                    location_func()

        except ValueError:
            print("Error! This is not a valid number. Try again.")


def main():
    logging.basicConfig(level=logging.INFO)
    error_handler = logging.root.handlers[0]
    log_handler = logging.StreamHandler(sys.stdout)
    log_handler.setFormatter(error_handler.formatter)
    logging.root.removeHandler(error_handler)
    logging.root.addHandler(log_handler)

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

    if config.get_config(NOMINATE_GROUP_MEMBER):
        user_nominator = UserNominator(telegram_api.get_bot(),
                                       config.get_config(GROUP_ID),
                                       KnownUsers(),
                                       messages.get_message("nomination_text"),
                                       messages.get_message("nomination_reroll_button"),
                                       messages.get_message("nomination_reroll_notification"))
        telegram_api.register_command_handler(MessageHandler(Filters.all, user_nominator.spy_on_message))
        telegram_api.register_command_handler(CallbackQueryHandler(user_nominator.reroll_nominee, pattern=user_nominator.REROLL_ACTION))

    if config.get_config(GOOGLE_USER_CREDENTIALS_FILE) is not None:
        location_suggester = LocationSuggester(telegram_api.get_bot(),
                                               config.get_config(GROUP_ID),
                                               f"{config.get_config_folder()}/{config.get_config(GOOGLE_USER_CREDENTIALS_FILE)}",
                                               config.get_config(LOCATION_SHEET_NAME),
                                               config.get_config(LOCATION_SHEET_NAMES_AREA),
                                               messages.get_message("location_suggestion_question_text"),
                                               messages.get_message("location_reroll_button"),
                                               messages.get_message("location_reroll_notification"))
        telegram_api.register_command_handler(CallbackQueryHandler(location_suggester.reroll_location,
                                                                   pattern=location_suggester.REROLL_ACTION))

    def on_remind():
        if config.get_config(NOMINATE_GROUP_MEMBER):
            user_nominator.nominate_user()
        if config.get_config(GOOGLE_USER_CREDENTIALS_FILE) is not None:
            location_suggester.suggest_locations()

    reminder.callback = on_remind

    class ReminderThread(Thread):
        def run(self) -> None:
            reminder.start()

    ReminderThread().start()

    def on_exit():
        reminder.stop()

    GracefulKiller(on_exit)

    if config.get_config(DEBUG):
        debug_input(reminder.reminder, user_nominator.nominate_user, location_suggester.suggest_locations)


if __name__ == '__main__':
    main()
