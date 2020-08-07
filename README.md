# Orgabot

This Telegram bot reminds a group at a specific time about a future meeting and its preparations.
Its configured with a yaml file and maybe in the future over a direct chat.

## Features

* Sends a reminder message on a specified interval to a group
* Selectes one of the group members to do the planing and reservations.
* Opens a poll with 4 random selected locations from a google sheet table for 12h.

## Config

Example config:
```yaml
telegram_api_key: <api-key>
group_id: 12345678
reminder_datetime: 2020-08-07 08:00:00
reminder_interval_days: 14
nominate_group_member: true
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

#### `nominate_group_member`
If true a group member will be chosen randomly to organize the next event.

#### `google_user_credentials_file`
The file name of the `credentials.json` file for your google service account. It needs to be placed in the config folder.

#### `location_sheet_name`
This is the name of your google table sheet which should be accessed. Your service account needs to be invited there.

#### `location_sheet_names_area`
This defines the area where the location names can be found. Its specified like `B3:B18`. For now only one column is supported.

### Texts
All texts can be changed via the `message.yaml` file beside the config file.

## Environment

The path the the config file can be set with the environment variable `CONFIG_FILE`. It defaults to `./config.yaml`.
If not existing a file will be generated at first start.
