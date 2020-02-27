FROM navikt/java:11

COPY build/libs/app.jar ./
COPY build/resources/main/scripts ./
RUN chmod +x scripts/run.sh
COPY scripts /init-scripts
