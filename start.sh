#!/usr/bin/env bash
set -e

python3.8 -m venv venv
source venv/bin/activate

pip3 install -r requirements.txt

PYTHONPATH="$(printf "%s:" orgabot/)"
export PYTHONPATH=$PYTHONPATH PYTHONUNBUFFERED=1
exec python3 -u orgabot/orgabot.py
