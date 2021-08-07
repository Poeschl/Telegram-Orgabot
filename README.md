# Rosie - A Telegram meetup organisation bot

This Telegram bot reminds a group at a specific time about a future meeting and its preparations. It's configured with a yaml file and maybe in the future over a direct chat.

## Features

* Sends a reminder message on a specified interval to a telegram group
* Selects one of the group members to do the planing and reservations.
* Opens a poll with 4 random selected locations from a google sheet.
* Admin commands via private chat to trigger messages or change texts.

## Screenshot

![Screenshot](https://raw.githubusercontent.com/Poeschl/Telegram-Orgabot/master/doc/screenshot.png)

## Config

Example config:

```yaml
telegramBotToken: <api-key>
telegramBotUsername: rosie
managingGroup: 12345678
adminIds:
  - 12345678
cron:
  cron: "*/1 * * * *"
  onEvenWeeks: true
  onOddWeeks: true
  enabled: true
locationPoll:
  enabled: true
  googleCredentialPath: "credentials.json"
  sheetName: "Gastronomie"
  namesArea: "A1:A20"
knownUsers: { }
```

### Description

#### `telegramBotToken`

The telegram bot api key.

#### `telegramBotUsername`

The telegram bot username with out the '@'.

#### `managingGroup`

The group_id the bot should post its messages. Remember to add the bot there as a member.

#### `adminIds`

This list hold a list of admins which can control bot features and retrieve error messages.

#### `cron` > `cron`

The cron expression for the reminder post.

#### `cron` > `onEvenWeeks`

Enable the scheduler on even week numbers.

#### `cron` > `onOddWeeks`

Enable the scheduler on odd week numbers.

#### `cron` > `enabled`

Enable or disable the scheduler. When disabled the bot will not post any scheduled message.

#### `locationPoll` > `enabled`

Enable or disable the location poll messages.

#### `locationPoll` > `googleCredentialPath`

The file name of the `credentials.json` file for your google service account. It needs to be placed in the config folder.

#### `locationPoll` > `sheetName`

This is the name of your google table sheet which should be accessed. Your service account needs to be invited there.

#### `locationPoll` > `namesArea`

This defines the area where the location names can be found. Its specified like `B3:B18`. For now only one column is supported.

#### `knownUsers`

This should be left empty on the first start. The bot will store the members of the given group here to get a pool of people to select the organizer.

## Texts

All texts can be changed via the `messages.yaml` file beside the config file.

## Environment

The path the the config folder can be set with the environment variable `CONFIG_FOLDER`. It defaults to the local directory. Make sure the folder exists and is writeable. A empty
config file will be generated there at first start.
