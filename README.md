# Orgabot

This Telegram bot reminds a group at a specific time about a future meeting and its preparations.
Its configured with a yaml file anc maybe in the future over a direct chat.

## Features

* Sends a reminder message on a specified interval to a group

## Config

Example config:
```yaml
telegram_api_key: <api-key>
group_id: 12345678
reminder_datetime: 2020-08-07 08:00:00
reminder_interval_days: 14
```

### Description

#### `telegram_api_key`
The telegram bot api key.

#### `group_id`
The group_id the bot should post its messages. Remember to add him there as a member.

#### `reminder_datetime`
The date and time for the first reminder message.

#### `reminder_interval_days`
The interval in days in which the message should be repeated.

## Environment

The path the the config file can be set with the environment variable `CONFIG_FILE`. It defaults to `./config.yaml`.
If not existing a file will be generated at first start.
