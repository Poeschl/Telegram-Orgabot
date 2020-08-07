from telegram.ext import Updater


class TelegramEndpoint:

    def __init__(self, api_token: str):
        self.updater = Updater(api_token, use_context=True)

    def register_command_handler(self, handler):
        self.updater.dispatcher.add_handler(handler)

    def start(self):
        self.updater.start_polling()

    def shutdown(self):
        self.updater.stop()

    def get_bot(self):
        return self.updater.bot
