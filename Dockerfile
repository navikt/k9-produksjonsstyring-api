FROM navikt/java:11

COPY build/libs/app.jar ./
RUN chmod +x scripts/run.sh
COPY scripts /init-scripts
