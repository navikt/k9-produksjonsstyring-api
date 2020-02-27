FROM navikt/java:11

COPY build/libs/app.jar ./
RUN chmod +x build/resources/main/scripts/run.sh
COPY build/resources/main/scripts /init-scripts/.
