FROM alpine:3.12.0 AS BUILD

ADD requirements.txt requirements.txt

RUN apk add -Uu --no-cache --purge \
        python3=3.8.5-r0  \
        python3-dev=3.8.5-r0 \
        py3-pip=20.1.1-r0 \
        py3-wheel=0.34.2-r1 \
        py3-setuptools=47.0.0-r0 \
        py3-cryptography=2.9.2-r0 \
        build-base=0.5-r2

RUN pip3 install --user -r requirements.txt

FROM alpine:3.12.0 AS RUN

ENV WORKING_DIR /home/bot
WORKDIR ${WORKING_DIR}

RUN apk add -Uu --no-cache --purge python3=3.8.5-r0

RUN addgroup -g 900 -S bot && adduser -u 900 -s /bin/sh -SDH bot bot && mkdir -p ${WORKING_DIR}
COPY docker/entrypoint.sh /usr/local/bin
RUN chmod 500 /usr/local/bin/entrypoint.sh && chown bot:bot /usr/local/bin/entrypoint.sh && ln -s /usr/local/bin/entrypoint.sh
ENTRYPOINT ["entrypoint.sh"]

COPY --chown=bot:bot --from=BUILD /root/.local /home/bot/.local
COPY --from=BUILD /usr/lib/python3.8/site-packages /usr/lib/python3.8/site-packages

ADD orgabot ${WORKING_DIR}/orgabot
