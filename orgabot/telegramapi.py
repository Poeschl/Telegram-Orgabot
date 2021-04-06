from logging import Handler

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


class TelegramLoggingHandler(Handler):
    """
    A handler class which writes logging records, appropriately formatted,
    to specified telegram channels.
    """

    terminator = '\n'

    def __init__(self, user_ids, bot):

        Handler.__init__(self)
        self.user_ids = user_ids
        self.bot = bot

    def flush(self):
        pass

    def emit(self, record):
        try:
            msg = self.format(record)
            self.send_to_users(msg + self.terminator)
            self.flush()
        except RecursionError:  # See issue 36272
            raise
        except Exception:
            self.handleError(record)

    def send_to_users(self, message):
        for user_id in self.user_ids:
            self.bot.send_message(chat_id=user_id, text=message)
