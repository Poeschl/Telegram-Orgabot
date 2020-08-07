import os
import shutil
from datetime import datetime
from os import path

from yaml import SafeLoader, dump, load

DEBUG = 'debug'
TELEGRAM_API_KEY = 'telegram_api_key'
GROUP_ID = 'group_id'
REMINDER_DATETIME = 'reminder_datetime'
REMINDER_INTERVAL = 'reminder_interval_days'
NOMINATE_GROUP_MEMBER = 'nomnate_group_member'

DEFAULT_CONFIG_FOLDER = 'config'


class Config:

    config_data = {
        DEBUG: False,
        TELEGRAM_API_KEY: '',
        GROUP_ID: '',
        REMINDER_DATETIME: datetime.now(),
        REMINDER_INTERVAL: 14,
        NOMINATE_GROUP_MEMBER: True
    }

    def __init__(self):
        config_folder = os.getenv('CONFIG_FILE', DEFAULT_CONFIG_FOLDER)
        if not path.exists(config_folder):
            os.mkdir(config_folder)
        self.config_file = config_folder + '/config.yaml'
        self._parse_config()

    def _parse_config(self):
        if path.exists(self.config_file):
            with open(self.config_file) as file:
                stored_config_data = load(file, SafeLoader)
                self.config_data = {**self.config_data, **stored_config_data}
        else:
            with open(self.config_file,  'w') as file:
                dump(self.config_data, file)

    def get_config(self, key: str):
        return self.config_data[key]


class Messages:
    included_message_file = 'orgabot/messages.yaml'
    message_data = {}

    def __init__(self):
        config_folder = os.getenv('CONFIG_FILE', DEFAULT_CONFIG_FOLDER)
        if not path.exists(config_folder):
            os.mkdir(config_folder)
        self.message_file = config_folder + '/message.yaml'
        self._parse_config()

    def _parse_config(self):
        if path.exists(self.message_file):
            with open(self.message_file) as file:
                stored_message_data = load(file, SafeLoader)
                self.message_data = {**self.message_data, **stored_message_data}
        else:
            shutil.copy(self.included_message_file, self.message_file)
            with open(self.message_file,  'w') as file:
                self.message_data = load(file, SafeLoader)

    def get_message(self, key: str):
        return self.message_data[key]
