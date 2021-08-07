FROM openjdk:16-alpine

LABEL org.opencontainers.image.source="https://github.com/Poeschl/Telegram-Orgabot"
LABEL org.opencontainers.image.url="https://github.com/Poeschl/Telegram-Orgabot"
LABEL org.opencontainers.image.authors="Poeschl <Poeschl@users.noreply.github.com>"

ENV CONFIG_FOLDER=config

ENTRYPOINT ["java", "-jar", "/app/Telegram-Orga-Bot-Rosie.jar"]
ADD "build/libs/Telegram-Orga-Bot-Rosie-*.jar" /app/Telegram-Orga-Bot-Rosie.jar
