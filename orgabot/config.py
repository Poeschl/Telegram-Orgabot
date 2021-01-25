import logging
import os
import shutil
from os import path

from croniter import croniter
from yaml import SafeLoader, dump, load

DEBUG = 'debug'
TELEGRAM_API_KEY = 'telegram_api_key'
GROUP_ID = 'group_id'
REMINDER_CRON = 'reminder_cron'
REMINDER_EVEN_WEEKS = 'reminder_cron_even_weeks'
REMINDER_ODD_WEEKS = 'reminder_cron_odd_weeks'
NOMINATE_GROUP_MEMBER = 'nomnate_group_member'
LOCATION_ENABLED = 'location_enabled'
GOOGLE_USER_CREDENTIALS_FILE = 'google_user_credentials_file'
LOCATION_SHEET_NAME = 'location_sheet_name'
LOCATION_SHEET_NAMES_AREA = 'location_sheet_names_area'

DEFAULT_CONFIG_FOLDER = 'config'


class Config:
    config_data = {
        DEBUG: False,
        TELEGRAM_API_KEY: '',
        GROUP_ID: '',
        REMINDER_CRON: '*/1 * * * 1',
        REMINDER_EVEN_WEEKS: True,
        REMINDER_ODD_WEEKS: True,
        NOMINATE_GROUP_MEMBER: True,
        LOCATION_ENABLED: True,
        GOOGLE_USER_CREDENTIALS_FILE: '',
        LOCATION_SHEET_NAME: '',
        LOCATION_SHEET_NAMES_AREA: ''
    }

    def __init__(self):
        config_folder = os.getenv('CONFIG_FILE', DEFAULT_CONFIG_FOLDER)
        if not path.exists(config_folder):
            os.mkdir(config_folder)
        self.config_file = config_folder + '/config.yaml'
        self._parse_config()
        self._verify_config()

    def _parse_config(self):
        if path.exists(self.config_file):
            with open(self.config_file, 'r+') as file:
                stored_config_data = load(file, SafeLoader)
                self.config_data = {**self.config_data, **stored_config_data}
                file.seek(0)
                dump(self.config_data, file)
                file.truncate()
        else:
            self.save_config()

    def _verify_config(self):
        if int(self.config_data[GROUP_ID]) >= 0:
            logging.error("The group id is not referencing a group! The bot may not work as intended")
        if not croniter.is_valid(self.config_data[REMINDER_CRON]):
            logging.error("The reminder cron is not valid. The bot may not work as intended")

    def get_config(self, key: str):
        return self.config_data[key]

    def set_config(self, key: str, value):
        self.config_data[key] = value

    def save_config(self):
        with open(self.config_file, 'w') as file:
            dump(self.config_data, file)

    @staticmethod
    def get_config_folder():
        return os.getenv('CONFIG_FILE', DEFAULT_CONFIG_FOLDER)


class Messages:
    included_message_file = 'orgabot/messages.yaml'
    message_data = {}

    def __init__(self):
        config_folder = os.getenv('CONFIG_FILE', DEFAULT_CONFIG_FOLDER)
        if not path.exists(config_folder):
            os.mkdir(config_folder)
        self.message_file = config_folder + '/message.yaml'
        self._parse_messages()

    def _parse_messages(self):
        if path.exists(self.message_file):
            with open(self.message_file) as file:
                stored_message_data = load(file, SafeLoader)
                self.message_data = {**self.message_data, **stored_message_data}
        else:
            shutil.copy(self.included_message_file, self.message_file)
            with open(self.message_file, 'w') as file:
                self.message_data = load(file, SafeLoader)

    def get_message(self, key: str):
        return self.message_data[key]


class KnownUsers:
    users = {}

    def __init__(self):
        config_folder = os.getenv('CONFIG_FILE', DEFAULT_CONFIG_FOLDER)
        if not path.exists(config_folder):
            os.mkdir(config_folder)
        self.users_file = config_folder + '/knownUsers.yaml'
        self._parse_config()

    def _parse_config(self):
        if path.exists(self.users_file):
            with open(self.users_file) as file:
                self.users = load(file, SafeLoader)
        else:
            with open(self.users_file, 'w') as file:
                dump(self.users, file)

    def insert_user(self, user_id: str, name: str):
        self.users[user_id] = name
        with open(self.users_file, 'w') as file:
            dump(self.users, file)
