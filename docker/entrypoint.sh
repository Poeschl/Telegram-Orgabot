#!/usr/bin/env sh
PYTHONPATH="$(printf "%s:" /home/orgabot/)"

[ -n "$CONFIG_FOLDER" ] && echo "Set permissions on folder $CONFIG_FOLDER" && chown bot:bot "$CONFIG_FOLDER"

su bot -c "export PYTHONPATH=$PYTHONPATH PYTHONUNBUFFERED=1 && printf 'PYTHONPATH: $PYTHONPATH\n' && python3 -u orgabot/orgabot.py"
