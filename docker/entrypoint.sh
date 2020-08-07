#!/usr/bin/env sh
PYTHONPATH="$(printf "%s:" /home/orgabot/)"

[ -n "$CONFIG_FILE" ] && FOLDER="$(dirname "$CONFIG_FILE")" && echo "Set permissions on folder $FOLDER" && chown bot:bot "$FOLDER"

su bot -c "export PYTHONPATH=$PYTHONPATH PYTHONUNBUFFERED=1 && printf 'PYTHONPATH: $PYTHONPATH\n' && python3 -u orgabot/orgabot.py"
